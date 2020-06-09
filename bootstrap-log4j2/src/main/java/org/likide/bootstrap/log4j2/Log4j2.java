package org.likide.bootstrap.log4j2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.likide.bootstrap.logging.LoggingConfiguration;
import org.likide.bootstrap.logging.LoggingManager;

public class Log4j2 implements LoggingManager {

	@Override
	public void init() {
		System.setProperty("log4j2.level", System.getProperty("logging.level", "warn"));
		System.setProperty("log4j2.disable.jmx", Boolean.toString(true));
		System.setProperty("log4j2.config.throwable", "%notEmpty{ -%throwable{short.message}{separator()}}");
	}

	@Override
	public void reconfigure(LoggingConfiguration configuration) {
		if (configuration.isIncludeStacktrace()) {
			System.setProperty("log4j2.config.throwable", "%throwable");
		} else {
			System.setProperty("log4j2.config.throwable", "%notEmpty{ -%throwable{short.message}{separator()}}");
		}
		if (configuration.getDefaultLevel() == null) {
			System.setProperty("log4j2.level", System.getProperty("logging.level", "warn"));
		} else {
			System.setProperty("log4j2.level", configuration.getDefaultLevel().toString());
		}
		
		((LoggerContext) LogManager.getContext(false)).reconfigure();
	}

}
