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
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.function.Consumer;

import org.jline.terminal.Terminal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Downloader {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Bootstrap.class);
	
	static Path download(String url, Terminal terminal) throws DownloadFailureException {
		HttpClient client = HttpClient.newBuilder().followRedirects(Redirect.NORMAL).build();
		HttpRequest request = HttpRequest.newBuilder()
				.GET()
				.uri(URI.create(url)).build();
		Path condaTempFile = createTempFile();
		
		try {
			// handle progress notification
			// request
			// -> BodyHandlers.ofPublisher : perform request, provides HTTP body as a publisher
			// -> ProgressDelegateSubscriber : get data from publisher and push it to subscriber, updating counter
			// -> BodySubscribers.ofFile : store result in file
			// -> condaTempFile
			
			// send the request and wait for header reception
			HttpResponse<Flow.Publisher<List<ByteBuffer>>> response =
					client.sendAsync(request, BodyHandlers.ofPublisher()).join();
			
			if (response.statusCode() != 200) {
				throw new DownloadFailureException(
						String.format("Download failure (%d) for %s", response.statusCode(), url));
			}
			
			// get the BODY publisher
			Flow.Publisher<List<ByteBuffer>> bodyPublisher = response.body();
			final long contentLength = response.headers().firstValueAsLong("content-length").orElse(0l);
			
			// subscribe ProgressDelegateSubscriber that updates progress counter and pass content to a file subscriber
			BodySubscriber<Path> fileSubscriber = BodySubscribers.ofFile(condaTempFile);
			
			// prepare progress bar
			ProgressBar progressBar = new ProgressBar(terminal, contentLength, Long::sum).start();
			
			// wait for download completion
			bodyPublisher.subscribe(new ProgressDelegateSubscriber(fileSubscriber, progressBar));
			CompletableFuture<Path> future = fileSubscriber.getBody().toCompletableFuture();
			future.get();
			
			// end progress bar
			progressBar.stop();
			
			LOGGER.info("File downloaded {}", contentLength);
			LOGGER.info("Conda installer saved to {}", condaTempFile.toAbsolutePath());
			return condaTempFile;
		} catch (RuntimeException | InterruptedException | ExecutionException e) {
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			try {
				Files.deleteIfExists(condaTempFile);
			} catch (IOException ioe) {
				LOGGER.warn("Temporary file cannot be cleaned: {}", condaTempFile, ioe);
			}
			String message = String.format("Download failure for %s (%s)", url, e.getMessage());
			throw new DownloadFailureException(message, e);
		}
	}

	private static Path createTempFile() throws DownloadFailureException {
		try {
			return Files.createTempFile(
					"bootstrap-miniconda-", ".sh",
					PosixFilePermissions.asFileAttribute(
							PosixFilePermissions.fromString("rwx------")));
		} catch (IOException e) {
			throw new DownloadFailureException("Exception while creating temporary download file", e);
		}
	}

	public static class ProgressDelegateSubscriber implements Subscriber<List<ByteBuffer>> {

		private final Subscriber<List<ByteBuffer>> delegate;
		private final Consumer<Long> counter;
		
		public ProgressDelegateSubscriber(Subscriber<List<ByteBuffer>> delegate, Consumer<Long> counter) {
			this.delegate = delegate;
			this.counter = counter;
		}

		@Override
		public void onSubscribe(Subscription subscription) {
			delegate.onSubscribe(subscription);
		}

		@Override
		public void onNext(List<ByteBuffer> item) {
			counter.accept(item.stream().mapToLong(ByteBuffer::limit).sum());
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
