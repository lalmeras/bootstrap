package org.likide.bootstrap;

import java.io.FileNotFoundException;
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
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
		name = "bootstrap",
		mixinStandardHelpOptions = true,
		defaultValueProvider = DefaultProvider.class
)
public class Bootstrap implements Callable<Integer> {

	public static final Logger LOGGER = LoggerFactory.getLogger(Bootstrap.class);

	@Option(
			names = { "--name", "-n" },
			description = "Conda environment name"
	)
	private String name;

	@Option(
			names = { "--requirements", "-r" },
			description = "environment.yml for your conda environment"
	)
	private String environmentYml;

	@Option(
			names = { "--reset-env" },
			description = "Delete existing environment"
	)
	private boolean resetEnvironment = false;

	@Option(
			names = { "--reset-conda" },
			description = "Delete existing conda installation (DANGER)"
	)
	private boolean resetConda = false;

	@Option(
			names = { "--skip-activate-script" },
			description = "Do not install activation script"
	)
	private boolean skipActivateScript = false;

	@Option(
			names = { "--profile-dir" },
			description = "Path for bootstrap.conf and activation scripts",
			negatable = true
	)
	private boolean profileDir = false;

	public static void main(String... args) {
		int exitCode = new CommandLine(new Bootstrap()).execute(args);
		System.exit(exitCode);
	}

	@Override
	public Integer call() throws Exception {
		LOGGER.warn("Test {}", name);
//		download();
		return 0;
	}

	private void download() throws IOException, FileNotFoundException {
		HttpClient client = HttpClient.newBuilder().followRedirects(Redirect.NORMAL).build();
		HttpRequest request = HttpRequest.newBuilder()
				.GET()
				.uri(URI.create(Constants.DEFAULTS.MINICONDA_URL)).build();
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
			LOGGER.warn("Conda installer saved to {}", condaTempFile.toAbsolutePath());
		} finally {
//			Files.deleteIfExists(condaTempFile);
		}
	}

}
