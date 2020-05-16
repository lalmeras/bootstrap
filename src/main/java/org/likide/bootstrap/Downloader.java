package org.likide.bootstrap;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

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
			try {
				HttpResponse<InputStream> response = client.sendAsync(request, BodyHandlers.ofInputStream()).join();
				InputStream is = response.body();
				byte[] data;
				try (FileOutputStream fos = new FileOutputStream(condaTempFile.toFile())) {
					while ((data = is.readNBytes(500000)).length != 0) {
						fos.write(data);
						System.out.print(".");
					}
				}
				LOGGER.info("Conda installer saved to {}", condaTempFile.toAbsolutePath());
				return condaTempFile;
			} catch (RuntimeException | IOException e) {
				Files.deleteIfExists(condaTempFile);
				String message = String.format("Download failure for %s (%s)", url, e.getMessage());
				throw new DownloadFailureException(message, e);
			}
		}

}
