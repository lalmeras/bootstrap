package org.likide.bootstrap;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp.Capability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Downloader {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Bootstrap.class);
	
	static Path download(String url, Terminal terminal) throws IOException, DownloadFailureException {
		HttpClient client = HttpClient.newBuilder().followRedirects(Redirect.NORMAL).build();
		HttpRequest request = HttpRequest.newBuilder()
				.GET()
				.uri(URI.create(url)).build();
		Path condaTempFile = Files.createTempFile(
				"bootstrap-miniconda-", ".sh",
				PosixFilePermissions.asFileAttribute(
						PosixFilePermissions.fromString("rwx------")));
		
		try {
			final AtomicInteger previousCounter = new AtomicInteger();
			final AtomicInteger counter = new AtomicInteger();
			
			// handle progress notification
			// request
			// -> BodyHandlers.ofPublisher : perform request, provides HTTP body as a publisher
			// -> ProgressDelegateSubscriber : get data from publisher and push it to subscriber, updating counter
			// -> BodySubscribers.ofFile : store result in file
			// -> condaTempFile
			
			// send the request and wait for header reception
			HttpResponse<Flow.Publisher<List<ByteBuffer>>> response =
					client.sendAsync(request, BodyHandlers.ofPublisher()).join();
			
			// get the BODY publisher
			Flow.Publisher<List<ByteBuffer>> bodyPublisher = response.body();
			final int contentLength = (int) response.headers().firstValueAsLong("content-length").orElse(0l);
			
			// subscribe ProgressDelegateSubscriber that updates progress counter and pass content to a file subscriber
			BodySubscriber<Path> fileSubscriber = BodySubscribers.ofFile(condaTempFile);
			bodyPublisher.subscribe(new ProgressDelegateSubscriber(fileSubscriber, counter));
			
			// display progress in a separate thread
			ScheduledExecutorService progressExecutor = Executors.newSingleThreadScheduledExecutor();
			progressExecutor.scheduleAtFixedRate(
					() -> displayProgress(previousCounter, contentLength, counter, terminal, false), 0, 200, TimeUnit.MILLISECONDS);
			
			// wait for download completion
			CompletableFuture<Path> future = fileSubscriber.getBody().toCompletableFuture();
			future.get();
			progressExecutor.shutdown();
			progressExecutor.awaitTermination(10, TimeUnit.SECONDS);
			
			// refresh progress display
			displayProgress(previousCounter, contentLength, counter, terminal, true);
			
			LOGGER.info("File downloaded {}", counter.get());
			LOGGER.info("Conda installer saved to {}", condaTempFile.toAbsolutePath());
			return condaTempFile;
		} catch (RuntimeException | InterruptedException | ExecutionException e) {
			Files.deleteIfExists(condaTempFile);
			String message = String.format("Download failure for %s (%s)", url, e.getMessage());
			throw new DownloadFailureException(message, e);
		}
	}

	private static void displayProgress(AtomicInteger previousCounter, int contentLength, AtomicInteger counter,
			Terminal terminal, boolean end) {
		// check terminal size
		int width = 80;
		int keep = 10 + 3;
		if (terminal != null && terminal.getWidth() > 0) {
			width = terminal.getWidth();
		}
		
		// compute ratio
		// maxChars is width minus reserved chars for brackets and extra informations
		// currentChars is advancement in char length (progress bar)
		// ratio is a percentage
		int maxBytes = contentLength;
		int maxChars = width - keep;
		int currentCounter = counter.get();
		int currentBytes = currentCounter;
		int currentChars = (int) (maxChars * ((double) currentBytes / maxBytes));
		int ratio = (int) (100 * ((double) currentBytes / maxBytes));
		
		// ensure that maxChars does not overflow maxChars
		currentChars = Math.min(maxChars, currentChars);
		
		if (terminal != null && terminal.getStringCapability(Capability.parm_left_cursor) != null) {
			// print progress-bar if line clearing is available
			terminal.puts(Capability.parm_left_cursor, width);
			terminal.flush();
			if (currentBytes < maxBytes) {
				terminal.writer().append(String.format("[%s>%s] %3d%%",
						"-".repeat(currentChars),
						" ".repeat(maxChars - currentChars - keep),
						ratio)).flush();;
			} else {
				terminal.writer().append(String.format("[%s] %3d%%", "-".repeat(maxChars - keep + 1), ratio)).flush();
			}
		} else {
			// print dot-bar if line clearing is not available
			int previousBytes = previousCounter.get();
			int previousChars = (int) (maxChars * ((double) previousBytes / maxBytes));
			terminal.writer().append(".".repeat(currentChars - previousChars)).flush();
			previousCounter.set(currentCounter);
		}
		if (end) {
			// for last output, add newline
			System.out.println();
		}
	}

	public static class ProgressDelegateSubscriber implements Subscriber<List<ByteBuffer>> {

		private final Subscriber<List<ByteBuffer>> delegate;
		private final AtomicInteger counter;
		
		public ProgressDelegateSubscriber(Subscriber<List<ByteBuffer>> delegate, AtomicInteger counter) {
			this.delegate = delegate;
			this.counter = counter;
		}

		@Override
		public void onSubscribe(Subscription subscription) {
			delegate.onSubscribe(subscription);
		}

		@Override
		public void onNext(List<ByteBuffer> item) {
			counter.addAndGet(item.stream().mapToInt(ByteBuffer::limit).sum());
			delegate.onNext(item);
		}

		@Override
		public void onError(Throwable throwable) {
			delegate.onError(throwable);
		}

		@Override
		public void onComplete() {
			delegate.onComplete();
		}
		
	}

}
