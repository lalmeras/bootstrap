package org.likide.bootstrap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.logging.LogManager;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.likide.bootstrap.Constants.SystemProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
		name = "bootstrap",
		mixinStandardHelpOptions = true,
		defaultValueProvider = DefaultProvider.class
)
public class Bootstrap implements Callable<Integer> {

	static {
		// configure log4j and slf4j before loading
		System.out.println(System.currentTimeMillis());
		System.setProperty(SystemProperties.LOG4J2_DISABLE_JMX, "true");
		System.setProperty(SystemProperties.LOG4J2_LEVEL, "warn");
		System.setProperty(SystemProperties.LOG4J2_CONFIG_THROWABLE, "%notEmpty{ -%throwable{short.message}{separator()}}");
		
		// configure jline
		System.setProperty("org.jline.terminal.exec", "true");
		System.setProperty("org.jline.terminal.jna", "true");
		System.setProperty("org.jline.terminal.jansi", "false");
		
		// bind jline logging to slf4j
		java.util.logging.Logger.getLogger("org.jline").setLevel(java.util.logging.Level.ALL);
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
		System.out.println(System.currentTimeMillis());
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(Bootstrap.class);

	static {
		System.out.println(System.currentTimeMillis());
	}

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
			names = { "--verbose", "-v" }
	)
	private boolean[] verbose = new boolean[0];

	@Option(
			names = { "--debug", "-d" }
	)
	private boolean debug = false;

	private final FileRegistry fileRegistry;
	private final Terminal terminal;

	public static void main(String... args) {
		System.out.println(System.currentTimeMillis());
		CommandLine commandLine = new CommandLine(new Bootstrap());
		System.out.println(System.currentTimeMillis());
		int exitCode = commandLine.execute(args);
		System.out.println(System.currentTimeMillis());
		System.exit(exitCode);
	}

	public Bootstrap() {
		System.out.println(System.currentTimeMillis());
		Terminal temp = null;
		try {
			temp = TerminalBuilder.terminal();
		} catch (IOException e) {
			LOGGER.warn("Failed to initialize jline terminal");
		}
		terminal = temp;
		fileRegistry = new FileRegistry();
		System.out.println(System.currentTimeMillis());
	}

	@Override
	public Integer call() {
		System.out.println(System.currentTimeMillis());
		try {
			return doCall();
		} catch (DownloadFailureException e) {
			LOGGER.error("Fatal error", e);
			return 1;
		} finally {
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

	private Integer doCall() throws DownloadFailureException {
		reconfigureLogging();
		computeDefaults();
		return 0;
//		
//		installMiniconda();
//		
//		return 0;
	}

	private void installMiniconda() throws DownloadFailureException {
		LOGGER.info("Download miniconda installer ({})", minicondaUrl);
		Path minicondaInstaller = Downloader.download(minicondaUrl, terminal);
		fileRegistry.addFile(new FileItem(minicondaInstaller));
	}

	private void computeDefaults() {
		if (minicondaUrl == null) {
			switch (minicondaVersion) {
			case _2: minicondaUrl = Constants.DEFAULTS.MINICONDA2_URL; break;
			case _3: minicondaUrl = Constants.DEFAULTS.MINICONDA2_URL; break;
			default:
				throw new IllegalStateException(String.format("No miniconda download URL for version %s", minicondaVersion));
			}
			LOGGER.info("Using default Miniconda URL : {}", minicondaUrl);
		}
	}

	private void reconfigureLogging() {
		if (verbose.length > 1) {
			System.setProperty(SystemProperties.LOG4J2_LEVEL, "trace");
		} else if (verbose.length > 0) {
			System.setProperty(SystemProperties.LOG4J2_LEVEL, "info");
		}
		if (debug) {
			System.setProperty(SystemProperties.LOG4J2_CONFIG_THROWABLE, "%n%throwable");
		}
		LOGGER.warn("{}", terminal.getWidth());
		
		//((LoggerContext) LogManager.getContext(false)).reconfigure();
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
	}

}
