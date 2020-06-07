package org.likide.bootstrap.tinylog;

import java.util.Map;

import org.slf4j.event.Level;

public interface ITinylogConfiguration {

	ITinylogConfiguration includeStacktrace(boolean includeStacktrace);

	ITinylogConfiguration defaultLevel(Level level);

	ITinylogConfiguration overrideProperties(Map<String, String> customProperties);

	Map<String, String> build();

}