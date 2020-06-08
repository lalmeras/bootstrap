package org.likide.bootstrap.logging.impl;

import org.likide.bootstrap.logging.LoggingConfiguration;
import org.slf4j.event.Level;

public class LoggingConfigurationImpl implements LoggingConfiguration {

	private boolean includeStacktrace = false;
	private Level defaultLevel;

	public LoggingConfigurationImpl() {
		super();
	}

	@Override
	public LoggingConfiguration includeStacktrace(boolean includeStacktrace) {
		this.includeStacktrace = includeStacktrace;
		return this;
	}
	@Override
	public boolean isIncludeStacktrace() {
		return includeStacktrace;
	}

	@Override
	public LoggingConfiguration defaultLevel(Level level) {
		this.defaultLevel = level;
		return this;
	}
	@Override
	public Level getDefaultLevel() {
		return defaultLevel;
	}

}
