package org.likide.bootstrap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Callable;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.likide.bootstrap.impl.Constants;
import org.likide.bootstrap.impl.DownloadFailureException;
import org.likide.bootstrap.impl.Downloader;
import org.likide.bootstrap.impl.FileItem;
import org.likide.bootstrap.impl.FileRegistry;
import org.likide.bootstrap.impl.MinicondaVersion;
import org.likide.bootstrap.impl.ProcessFailureException;
import org.likide.bootstrap.logging.LoggingConfiguration;
import org.likide.bootstrap.logging.LoggingManager;
import org.likide.bootstrap.picocli.DefaultProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.slf4j.event.Level;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.PicocliException;
import picocli.CommandLine.Spec;

@Command(
		name = "bootstrap",
		mixinStandardHelpOptions = true,
		defaultValueProvider = DefaultProvider.class
)
public class Bootstrap implements Callable<Integer> {

	private static final LoggingManager LOGGING_MANAGER = LoggingManager.getInstance();

	static {
		LOGGING_MANAGER.init();
		// configure log4j and slf4j before loading
		// configure jline
		System.setProperty("org.jline.terminal.exec", "true");
		System.setProperty("org.jline.terminal.jna", "true");
		System.setProperty("org.jline.terminal.jansi", "false");
		
		// bind jline logging to slf4j
		java.util.logging.Logger.getLogger("org.jline").setLevel(java.util.logging.Level.ALL);
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(Bootstrap.class);

	@Option(
			names = { "--name", "--env-name", "-n" },
			description = "Name for env installation, --env-prefix or --env-name is required"
	)
	private String envName;

	@Option(
			names = { "--env-prefix", "-p" },
			description = "Path for env installation, --env-prefix or --env-name is required"
	)
	private String envPrefix;

	@Option(
			names = { "--environment" },
			description = "environment.yml for your conda environment, default to ./environment.yml"
	)
	private String environmentYml;

	@Option(
			names = { "--no-environment" },
			description = "Do not use ./environment.yml to define conda environment"
	)
	private boolean noEnvironmentYml;

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

	@Option(
			names = { "--miniconda-version" },
			description = "Miniconda (python) version (${COMPLETION-CANDIDATES})",
			defaultValue = "3"
			
	)
	private MinicondaVersion minicondaVersion;

	@Option(
			names = { "--miniconda-url" },
			description = "Download URL for miniconda url. Default value determined from 'Miniconda version' if omitted"
	)
	private String minicondaUrl;

	@Option(
			names = { "--miniconda-prefix" },
			description = "Path for miniconda installation, defaults to ~/miniconda(2|3)/"
	)
	private String minicondaPrefix;

	@Option(
			names = { "--verbose", "-v" }
	)
	private boolean[] verbose = new boolean[0];

	@Option(
			names = { "--debug", "-d" }
	)
	private boolean debug = false;

	@Spec
	private CommandSpec spec;

	private FileRegistry fileRegistry;
	private Terminal terminal;

	public static void main(String... args) {
		CommandLine commandLine = new CommandLine(new Bootstrap());
		int exitCode = commandLine.execute(args);
		System.exit(exitCode);
	}

	public Bootstrap() {
		super();
	}

	/**
	 * Perform initialization and catch-finally handling, wrapping {@link Bootstrap#doCall()}
	 */
	@Override
	public Integer call() {
		try {
			validate();
			init();
			reconfigureLogging();
			computeDefaults();
			return doCall();
		} catch (PicocliException p) {
			throw p;
		} catch (RuntimeException e) {
			LOGGER.error("Fatal error", e);
			return 1;
		} finally {
			cleanFiles();
		}
	}

	private void validate() {
		if (envPrefix == null && envName == null) {
			Collection<ArgSpec> args = new ArrayList<>();
			args.add(spec.findOption("--env-prefix"));
			args.add(spec.findOption("--env-name"));
			throw new CommandLine.MissingParameterException(spec.commandLine(), args,
					"--env-prefix or --env-name argument is required");
		}
	}

	/**
	 * Initiate file registry and terminal handling.
	 */
	private void init() {
		Terminal temp = null;
		try {
			temp = TerminalBuilder.terminal();
		} catch (IOException e) {
			LOGGER.warn("Failed to initialize jline terminal");
		}
		terminal = temp;
		fileRegistry = new FileRegistry();
	}

	/**
	 * File cleanup processing. Errors during cleanup are logged.
	 */
	private void cleanFiles() {
		if (fileRegistry != null) {
			for (FileItem item : fileRegistry.getFiles()) {
				try {
					if (!Files.deleteIfExists(item.getPath())) {
						LOGGER.warn("Temporary file cannot be cleaned: {}", item.getPath());
					} else {
						LOGGER.info("Temporary file removed: {}", item.getPath());
					}
				} catch (IOException | RuntimeException e) {
					LOGGER.warn("Temporary file cannot be cleaned: {}", item.getPath(), e);
				}
			}
		}
	}

	/**
	 * Perform effective stuff.
	 */
	private Integer doCall() {
		try {
			installMiniconda();
		} catch (DownloadFailureException | ProcessFailureException e) {
			LOGGER.error("Error during miniconda installation", e);
		}
		try {
			checkRemoveAndInstallEnvironment();
		} catch (ProcessFailureException e) {
			LOGGER.error("Error during environment creation", e);
		}
		return 0;
	}

	/**
	 * Miniconda installation (download then execute).
	 */
	private void installMiniconda() throws DownloadFailureException, ProcessFailureException {
		LOGGER.info("Check for Miniconda environment {}", minicondaPrefix);
		Path target = Paths.get(minicondaPrefix);
		Path conda = Paths.get(target.toString(), "bin/conda");
		if (target.toFile().exists() && conda.toFile().exists() && conda.toFile().canExecute()) {
			LOGGER.info("Miniconda already installed in {}; skip installation", target);
			LOGGER.info("Use --reset-conda to overwrite an existing installation");
		} else if (target.toFile().exists()) {
			LOGGER.error("Miniconda target folder {} already exists, bin/conda is missing", target);
			LOGGER.warn("Use --reset-conda to overwrite target folder with a new installation");
		} else {
			LOGGER.info("Download miniconda installer ({})", minicondaUrl);
			Path minicondaInstaller = Downloader.download(minicondaUrl, terminal);
			fileRegistry.addFile(new FileItem(minicondaInstaller));
			try {
				ProcessBuilder processBuilder = new ProcessBuilder();
				processBuilder.command(minicondaInstaller.toAbsolutePath().toString(),
						"-u", "-b", "-p", minicondaPrefix);
				Process process = processBuilder.inheritIO().start();
				int status = process.waitFor();
				if (status != 0) {
					throw new ProcessFailureException("Miniconda installer error");
				}
			} catch (IOException | InterruptedException e) {
				if (e instanceof InterruptedException) {
					Thread.currentThread().interrupt();
				}
				throw new ProcessFailureException("Miniconda installer error", e);
			}
		}
	}

	private boolean checkExistingEnvironment() {
		LOGGER.info("Check for environment {}", envPrefix != null ? envPrefix : envName);
		try {
			ProcessBuilder processBuilder = binCondaProcess("list");
			if (envPrefix != null) {
				processBuilder.command().add("-p");
				processBuilder.command().add(envPrefix);
			} else {
				processBuilder.command().add("-n");
				processBuilder.command().add(envName);
			}
			if (environmentYml != null) {
				processBuilder.command().add("-f");
				processBuilder.command().add(environmentYml);
			}
			Process process = processBuilder.inheritIO().start();
			int status = process.waitFor();
			return status == 0;
		} catch (IOException | InterruptedException e) {
			LOGGER.debug("Error checking environment {}", envPrefix != null ? envPrefix : envName, e);
			// TODO : effectively handle interruption
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			return false;
		}
	}

	/**
	 * Environment creation.
	 */
	private void checkRemoveAndInstallEnvironment() throws ProcessFailureException {
		if (checkExistingEnvironment() && ! resetEnvironment) {
			LOGGER.info("Environment already exists {}; skip creation", envPrefix != null ? envPrefix : envName);
			LOGGER.info("Use --reset-env to overwrite an existing environment");
		} else {
			if (resetEnvironment) {
				LOGGER.info("Remove exsiting environment");
				removeEnvironment();
			}
			LOGGER.info("Creating environment {}", envPrefix != null ? envPrefix : envName);
			installEnvironment();
		}
	}

	private void removeEnvironment() throws ProcessFailureException {
		try {
			ProcessBuilder processBuilder = binCondaProcess("env", "remove", "-y");
			if (envPrefix != null) {
				processBuilder.command().add("-p");
				processBuilder.command().add(envPrefix);
			} else {
				processBuilder.command().add("-n");
				processBuilder.command().add(envName);
			}
			Process process = processBuilder.inheritIO().start();
			int status = process.waitFor();
			if (status != 0) {
				throw new ProcessFailureException("Environment creation error");
			}
		} catch (IOException | InterruptedException e) {
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			throw new ProcessFailureException("Environment creation error", e);
		}
	}

	private void installEnvironment() throws ProcessFailureException {
		try {
			ProcessBuilder processBuilder = binCondaProcess("create", "-y");
			if (envPrefix != null) {
				processBuilder.command().add("-p");
				processBuilder.command().add(envPrefix);
			} else {
				processBuilder.command().add("-n");
				processBuilder.command().add(envName);
			}
			if (environmentYml != null) {
				processBuilder.command().add("-f");
				processBuilder.command().add(environmentYml);
			}
			Process process = processBuilder.inheritIO().start();
			int status = process.waitFor();
			if (status != 0) {
				throw new ProcessFailureException("Environment creation error");
			}
		} catch (IOException | InterruptedException e) {
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			throw new ProcessFailureException("Environment creation error", e);
		}
	}

	private Path binConda() {
		return Path.of(minicondaPrefix, "bin/conda");
	}

	private ProcessBuilder binCondaProcess(String... args) {
		ProcessBuilder processBuilder = new ProcessBuilder();
		processBuilder.command(binConda().toAbsolutePath().toString());
		processBuilder.command().addAll(Arrays.asList(args));
		return processBuilder;
	}

	/**
	 * Compute dynamic defaults, as miniconda URL based on the miniconda version.
	 */
	private void computeDefaults() {
		if (minicondaUrl == null) {
			switch (minicondaVersion) {
			case V2:
				minicondaUrl = Constants.DEFAULTS.MINICONDA2_URL;
				break;
			case V3:
				minicondaUrl = Constants.DEFAULTS.MINICONDA3_URL;
				break;
			default:
				throw new IllegalStateException(String.format("No miniconda download URL for version %s", minicondaVersion));
			}
			LOGGER.info("Using default Miniconda URL : {}", minicondaUrl);
		} else {
			LOGGER.info("Using Miniconda URL : {}", minicondaUrl);
		}
		if (minicondaPrefix == null) {
			switch (minicondaVersion) {
			case V2:
				minicondaPrefix = System.getProperty("user.home") + "/miniconda2";
				break;
			case V3:
				minicondaPrefix = System.getProperty("user.home") + "/miniconda3";
				break;
			default:
				throw new IllegalStateException(String.format("No miniconda prefix for version %s", minicondaVersion));
			}
			LOGGER.info("Using default Miniconda prefix : {}", minicondaPrefix);
		} else {
			LOGGER.info("Using Miniconda prefix : {}", minicondaPrefix);
		}
		if (envName != null) {
			LOGGER.info("Using environment name : {}", envName);
		} else if (envPrefix != null) {
			LOGGER.info("Using environment prefix : {}", envPrefix);
		}
		if (environmentYml == null && ! noEnvironmentYml) {
			environmentYml = "./environment.yml";
			LOGGER.info("Using default {} location", environmentYml);
		} else if (environmentYml != null) {
			LOGGER.info("Using environment definition {}", environmentYml);
		} else {
			LOGGER.info("Initialize conda environment without environment.yml file");
		}
	}

	/**
	 * Reload logging framework configuration based on verbosity and debugging cli parameters.
	 */
	private void reconfigureLogging() {
		LoggingConfiguration configuration = LOGGING_MANAGER.newConfiguration();
		if (verbose.length > 1) {
			configuration.defaultLevel(Level.TRACE);
		} else if (verbose.length > 0) {
			configuration.defaultLevel(Level.INFO);
		}
		if (debug) {
			configuration.includeStacktrace(true);
		}
		
		LOGGING_MANAGER.reconfigure(configuration);
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
		LOGGER.debug("Verbosity configuration applied");
		LOGGER.debug("Terminal width: {}", terminal.getWidth());
	}

}
