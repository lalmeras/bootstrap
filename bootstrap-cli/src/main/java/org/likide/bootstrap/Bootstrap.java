package org.likide.bootstrap;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp.Capability;
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
						message(MessageLevel.warn, Stage.cleaning, null, "Temporary file cannot be cleaned: %s", item.getPath());
					} else {
						message(MessageLevel.info, Stage.cleaning, null, "Temporary file removed: {}", item.getPath());
					}
				} catch (IOException | RuntimeException e) {
					message(MessageLevel.warn, Stage.cleaning, null, "Temporary file cannot be cleaned: {}", item.getPath(), e);
				}
			}
		}
	}

	/**
	 * Perform effective stuff.
	 */
	private Integer doCall() {
		try {
			checkRemoveAndInstallMiniconda();
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
	private void checkRemoveAndInstallMiniconda() throws DownloadFailureException, ProcessFailureException {
		String context = minicondaPrefix;
		message(MessageLevel.info, Stage.m_check, context, "check existing installation");
		Path target = Paths.get(context);
		Path conda = Paths.get(target.toString(), "bin/conda");
		if (resetConda && target.toFile().exists()) {
			try (Stream<Path> paths = Files.walk(target)) {
				message(MessageLevel.info, Stage.m_remove, context, "delete installation");
				paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
			} catch (IOException e) {
				throw new ProcessFailureException(String.format("error deleting %s", target), e);
			}
		}
		if (target.toFile().exists() && conda.toFile().exists() && conda.toFile().canExecute()) {
			message(MessageLevel.info, Stage.m_check, context, "already exists; installation skipped");
			message(MessageLevel.info, Stage.m_check, context, "use --reset-conda to overwrite");
		} else if (target.toFile().exists()) {
			message(MessageLevel.error, Stage.m_check, context, "installation exists but bin/conda is missing");
			message(MessageLevel.warn, Stage.m_check, context, "use --reset-conda to overwrite");
		} else {
			message(MessageLevel.info, Stage.m_download, context, "download installer (%s)", minicondaUrl);
			Path minicondaInstaller = Downloader.download(minicondaUrl, terminal);
			fileRegistry.addFile(new FileItem(minicondaInstaller));
			try {
				Stage stage = Stage.m_install;
				String[] command = new String[] { minicondaInstaller.toAbsolutePath().toString(),
						"-u", "-b", "-p", context };
				int status = execute(stage, context, command);
				if (status != 0) {
					throw new ProcessFailureException(String.format("command failed with status %d", status));
				}
			} catch (IOException | InterruptedException e) {
				if (e instanceof InterruptedException) {
					Thread.currentThread().interrupt();
				}
				throw new ProcessFailureException("Miniconda installer error", e);
			}
		}
	}

	private int execute(Stage stage, String context, String[] command)
			throws IOException, InterruptedException {
		ProcessBuilder processBuilder = new ProcessBuilder();
		processBuilder.command(command);
		processBuilder.redirectInput();
		processBuilder.redirectErrorStream(true);
		processBuilder.redirectOutput();
		message(MessageLevel.info, stage, context, "launch %s",
				Arrays.stream(command).map(s -> "'" + s.replace("'", "''\'")+ "'")
				.collect(Collectors.joining(" ")));
		Process process = processBuilder.start();
		CharsetDecoder cd = Charset.defaultCharset().newDecoder()
				.onMalformedInput(CodingErrorAction.REPLACE)
				.onUnmappableCharacter(CodingErrorAction.REPLACE);
		BufferedReader stream = new BufferedReader(
				new InputStreamReader(process.getInputStream(), cd));
		StringWriter stringWriter = new StringWriter();
		Writer writer = new StreamWriter(stringWriter);
		stream.transferTo(writer);
		int status = process.waitFor();
		if (status != 0) {
			message(MessageLevel.error, stage, context, "command output on failure");
			printCommandOutput(stringWriter);
		} else {
			if (debug) {
				message(MessageLevel.debug, stage, context, "command output");
				printCommandOutput(stringWriter);
			}
			message(MessageLevel.info, stage, context, "done");
		}
		return status;
	}

	private class StreamWriter extends Writer {
		
		private StringWriter stringWriter;
		private int newlines = 0;
		
		public StreamWriter(StringWriter stringWriter) {
			this.stringWriter = stringWriter;
		}

		@Override
		public void write(char[] cbuf, int off, int len) throws IOException {
			char[] slice = Arrays.copyOfRange(cbuf, off, off + len);
			String string = new String(slice);
			String[] lines = string.split("\n", -1);
			for (String line : Arrays.copyOfRange(lines, 0, lines.length - 1)) {
				if (newlines == 5) {
					try {
						Thread.sleep(1000);
						IntStream.range(1, 6).forEach(i -> {
							terminal.puts(Capability.cursor_up);
							terminal.puts(Capability.parm_left_cursor, terminal.getWidth());
							terminal.puts(Capability.clr_eol, 2);
							terminal.writer().flush();
						});
						newlines = 0;
					} catch (InterruptedException e) {}
				}
				newlines++;
				System.out.println(line);
			}
			System.out.print(lines[lines.length - 1]);
			stringWriter.write(cbuf, off, len);
		}

		@Override
		public void flush() throws IOException {
			stringWriter.flush();
		}

		@Override
		public void close() throws IOException {
			stringWriter.close();
		}
		
	}

	private void printCommandOutput(Writer writer) {
		Arrays.stream(writer.toString().split("\n")).map(s -> " > " + s).forEach(System.out::println);
	}

	private boolean checkExistingEnvironment() {
		String context = envPrefix != null ? envPrefix : envName;
		message(MessageLevel.info, Stage.e_check, context, "check existing environment");
		try {
			List<String> command = binCondaProcess("list");
			if (envPrefix != null) {
				command.add("-p");
				command.add(envPrefix);
			} else {
				command.add("-n");
				command.add(envName);
			}
			if (environmentYml != null) {
				command.add("-f");
				command.add(environmentYml);
			}
			int status = execute(Stage.e_check, context, command.toArray(new String[0]));
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
		String context = envPrefix != null ? envPrefix : envName;
		if (checkExistingEnvironment() && ! resetEnvironment) {
			message(MessageLevel.info, Stage.e_check, context, "already exists; skip creation");
			message(MessageLevel.info, Stage.e_check, context, "use --reset-env to overwrite");
		} else {
			if (resetEnvironment) {
				message(MessageLevel.info, Stage.e_remove, context, "remove existing environment");
				removeEnvironment();
			}
			message(MessageLevel.info, Stage.e_remove, context, "initializing environment");
			installEnvironment();
		}
	}

	private void removeEnvironment() throws ProcessFailureException {
		try {
			String context = envPrefix != null ? envPrefix : envName;
			List<String> command = binCondaProcess("env", "remove", "-y");
			if (envPrefix != null) {
				command.add("-p");
				command.add(envPrefix);
			} else {
				command.add("-n");
				command.add(envName);
			}
			int status = execute(Stage.e_remove, context, command.toArray(new String[0]));
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
			String context = envPrefix != null ? envPrefix : envName;
			List<String> command = binCondaProcess("create", "-y");
			if (envPrefix != null) {
				command.add("-p");
				command.add(envPrefix);
			} else {
				command.add("-n");
				command.add(envName);
			}
			if (environmentYml != null) {
				command.add("-f");
				command.add(environmentYml);
			}
			int status = execute(Stage.e_remove, context, command.toArray(new String[0]));
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

	private List<String> binCondaProcess(String... args) {
		List<String> command = new ArrayList<>();
		command.add(binConda().toAbsolutePath().toString());
		command.addAll(Arrays.asList(args));
		return command;
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
			message(MessageLevel.info, Stage.p_config, null, "Using default Miniconda URL: %s", minicondaUrl);
		} else {
			message(MessageLevel.info, Stage.p_config, null, "Using Miniconda URL: %s", minicondaUrl);
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
			message(MessageLevel.info, Stage.p_config, null, "Using default Miniconda prefix: %s", minicondaPrefix);
		} else {
			message(MessageLevel.info, Stage.p_config, null, "Using Miniconda prefix: %s", minicondaPrefix);
		}
		if (envName != null) {
			message(MessageLevel.info, Stage.p_config, null, "Using environment name: %s", envName);
		} else if (envPrefix != null) {
			message(MessageLevel.info, Stage.p_config, null, "Using environment prefix: %s", envPrefix);
		}
		if (environmentYml == null && ! noEnvironmentYml) {
			environmentYml = "./environment.yml";
			message(MessageLevel.info, Stage.p_config, null, "Using default environment definition location: %s", environmentYml);
		} else if (environmentYml != null) {
			message(MessageLevel.info, Stage.p_config, null, "Using environment definition location: %s", environmentYml);
		} else {
			message(MessageLevel.info, Stage.p_config, null, "Initialize conda environment without environment.yml file");
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
		message(MessageLevel.debug, Stage.p_logging, null, "Verbosity configuration applied");
		message(MessageLevel.debug, Stage.p_logging, null, "Terminal width: %d", terminal.getWidth());
	}

	private void message(MessageLevel level, Stage stage, String context, String message, Object... args) {
		String formattedMessage;
		if (args.length > 0) {
			formattedMessage = String.format(message, args);
		} else {
			formattedMessage = message;
		}
		String fullMessage;
		if (context == null) {
			fullMessage = String.format("%-20s - %s", getStageName(stage), formattedMessage);
		} else {
			fullMessage = String.format("%-20s [%s] - %s", getStageName(stage), context, formattedMessage);
		}
		System.out.println(fullMessage);
	}

	private String getStageName(Stage stage) {
		List<Stage> stages = new ArrayList<>();
		stages.add(stage);
		Stage parent = stage.parent;
		while (parent != null) {
			stages.add(0, parent);
			parent = parent.parent;
		}
		return stages.stream().map(Stage::getName).collect(Collectors.joining("/"));
	}

	private enum MessageLevel {
		info,
		debug,
		warn,
		error;
	}

	private enum Stage {
		setup(null, "prepare"),
		miniconda(null, "miniconda"),
		environment(null, "environment"),
		cleaning(null, "cleaning"),
		p_config(Stage.setup, "config"),
		p_logging(Stage.setup, "logging"),
		m_check(Stage.miniconda, "check"),
		m_download(Stage.miniconda, "download"),
		m_remove(Stage.miniconda, "remove"),
		m_install(Stage.miniconda, "install"),
		e_check(Stage.environment, "check"),
		e_remove(Stage.environment, "remove"),
		e_install(Stage.environment, "install");
		
		private final Stage parent;
		private final String name;
		
		private Stage(Stage parent, String name) {
			this.parent = parent;
			this.name = name;
		}
		
		private String getName() {
			return name;
		}
	}

}
