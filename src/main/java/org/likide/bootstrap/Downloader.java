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
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp.Capability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Downloader {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Bootstrap.class);
	
	static Path download(String url) throws IOException, DownloadFailureException {
		HttpClient client = HttpClient.newBuilder().followRedirects(Redirect.NORMAL).build();
		HttpRequest request = HttpRequest.newBuilder()
				.GET()
				.uri(URI.create(url)).build();
		Path condaTempFile = Files.createTempFile(
				"bootstrap-miniconda-", ".sh",
				PosixFilePermissions.asFileAttribute(
						PosixFilePermissions.fromString("rwx------")));
		Terminal terminal = null;
		try {
			terminal = TerminalBuilder.terminal();
		} catch (IOException e) {
			// pass
		}
		final Terminal finalTerminal = terminal;
		
		try {
			final AtomicInteger previousCounter = new AtomicInteger();
			final AtomicInteger counter = new AtomicInteger();
			BodySubscriber<Path> fileSubscriber = BodySubscribers.ofFile(condaTempFile);
			HttpResponse<Flow.Publisher<List<ByteBuffer>>> response = client.sendAsync(request, BodyHandlers.ofPublisher()).join();
			Flow.Publisher<List<ByteBuffer>> is = response.body();
			is.subscribe(new ProgressDelegateSubscriber(fileSubscriber, counter));
			CompletableFuture<Path> future = fileSubscriber.getBody().toCompletableFuture();
			ScheduledExecutorService progressExecutor = Executors.newSingleThreadScheduledExecutor();
			progressExecutor.scheduleAtFixedRate(() -> displayProgress(previousCounter, counter, finalTerminal), 0, 200, TimeUnit.MILLISECONDS);
			future.get();
			progressExecutor.shutdown();
			progressExecutor.awaitTermination(10, TimeUnit.SECONDS);
			displayProgress(previousCounter, counter, finalTerminal);
			System.out.println();
			
			LOGGER.info("File downloaded {}", counter.get());
			LOGGER.info("Conda installer saved to {}", condaTempFile.toAbsolutePath());
			return condaTempFile;
		} catch (RuntimeException | InterruptedException | ExecutionException e) {
			Files.deleteIfExists(condaTempFile);
			String message = String.format("Download failure for %s (%s)", url, e.getMessage());
			throw new DownloadFailureException(message, e);
		}
	}

	private static void displayProgress(AtomicInteger previousCounter, AtomicInteger counter, Terminal terminal) {
		int width = 80;
		if (terminal != null && terminal.getBooleanCapability(Capability.columns)) {
			width = terminal.getWidth();
		}
		int maxBytes = 48;
		int maxChars = width - 3;
		int currentCounter = counter.get();
		int currentBytes = currentCounter / 1000000;
		int currentChars = maxChars * currentBytes / maxBytes;
		currentChars = Math.min(maxChars, currentChars);
		if (terminal != null && terminal.getBooleanCapability(Capability.cursor_left)) {
			if (currentBytes < maxBytes) {
				System.out.print(String.format("\u001b[%dD[%s>%s]", width, "-".repeat(currentChars), " ".repeat(maxChars - currentChars - 3)));
			} else {
				System.out.print(String.format("\u001b[%dD[%s]", width, "-".repeat(maxChars)));
			}
		} else {
			int previousBytes = previousCounter.get() / 1000000;
			int previousChars = maxChars * previousBytes / maxBytes;
			System.out.print(".".repeat(currentChars - previousChars));
			previousCounter.set(currentCounter);
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
			counter.addAndGet(item.stream().mapToInt(i -> i.limit()).sum());
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
