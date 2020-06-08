package org.likide.bootstrap.logging;

import java.util.ServiceLoader;

import org.likide.bootstrap.logging.impl.LoggingConfigurationImpl;

public interface LoggingManager {

	public static LoggingManager getInstance() {
		ServiceLoader<LoggingManager> loader = ServiceLoader.load(LoggingManager.class);
		if (loader.findFirst().isPresent()) {
			return loader.findFirst().get();
		} else {
			throw new IllegalStateException("No LoggingManager implementation found.");
		}
	}

	default LoggingConfiguration newConfiguration() {
		return new LoggingConfigurationImpl();
	}

	void init();

	void reconfigure(LoggingConfiguration configuration);

}
