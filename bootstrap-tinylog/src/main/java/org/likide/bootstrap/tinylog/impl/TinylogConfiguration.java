package org.likide.bootstrap.tinylog.impl;

import java.util.HashMap;
import java.util.Map;

import org.likide.bootstrap.tinylog.ITinylogConfiguration;
import org.slf4j.event.Level;

public class TinylogConfiguration implements ITinylogConfiguration {

	private boolean includeStacktrace = false;
	private Level level;
	private Map<String, String> customProperties;

	private TinylogConfiguration() {}

	@Override
	public ITinylogConfiguration includeStacktrace(boolean includeStacktrace) {
		this.includeStacktrace = includeStacktrace;
		return this;
	}

	@Override
	public ITinylogConfiguration defaultLevel(Level level) {
		this.level = level;
		return this;
	}

	@Override
	public ITinylogConfiguration overrideProperties(Map<String, String> customProperties) {
		if (customProperties == null) {
			customProperties = new HashMap<>();
		}
		customProperties.putAll(customProperties);
		return this;
	}

	@Override
	public Map<String, String> build() {
		Map<String, String> configuration = new HashMap<>();
		configuration.put("writer", "console");
		if (includeStacktrace) {
			configuration.put("writer.format", "{level}: {class}.{method}()\t{message}");
		} else {
			configuration.put("writer.format", "{level}: {class}.{method}()\t{message-only}");
		}
		configuration.put("writer.level", "trace");
		configuration.put("level", "trace");
		if (level == null) {
			configuration.put("level@org", System.getProperty("logging.level", "warn"));
		} else {
			configuration.put("level@org", level.toString());
		}
		
		if (customProperties != null) {
			configuration.putAll(customProperties);
		}
		return configuration;
	}

	public static TinylogConfiguration builder() {
		return new TinylogConfiguration();
	}

}
