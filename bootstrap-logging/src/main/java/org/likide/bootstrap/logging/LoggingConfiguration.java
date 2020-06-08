package org.likide.bootstrap.logging;

import org.slf4j.event.Level;

public interface LoggingConfiguration {

	LoggingConfiguration defaultLevel(Level level);
	Level getDefaultLevel();

	LoggingConfiguration includeStacktrace(boolean includeStacktrace);
	boolean isIncludeStacktrace();

}