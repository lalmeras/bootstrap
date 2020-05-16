package org.likide.bootstrap;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine.IDefaultValueProvider;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.OptionSpec;

public class DefaultProvider implements IDefaultValueProvider {

	public static final Logger LOGGER = LoggerFactory.getLogger(DefaultProvider.class);

	@Override
	public String defaultValue(ArgSpec argSpec) throws Exception {
		if (argSpec.isOption() && ((OptionSpec) argSpec).longestName().equals("--name")) {
			return defaultEnvironmentName(Paths.get("").toAbsolutePath());
		}
		return null;
	}

	public static String defaultEnvironmentName(Path path) {
		// we go up the path to find the first dirname that is not 'bootstrap'
		while (path.getFileName() != null
				&& path.getFileName().toString().equals("bootstrap")
				&& ! path.getParent().equals(path)) {
			path = path.getParent();
		}
		if (path.getFileName() == null || path.getFileName().toString().equals("bootstrap")) {
			return "default";
		}
		return cleanBootstrapName(path.getFileName().toString(), false);
	}

	public static String cleanBootstrapName(String candidate, boolean warn) {
		String cleanedName = candidate.replaceAll("[^0-9a-zA-Z-]", "_");
		if ( ! cleanedName.equals(candidate) && warn) {
			LOGGER.warn("Environment renamed from {} to {} to remove special characters",
					candidate, cleanedName);
		}
		return cleanedName;
	}

}
