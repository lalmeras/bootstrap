package org.likide.bootstrap.tinylog;

import java.util.Map;

import org.tinylog.provider.ProviderRegistry;

public class TinylogConfiguration {

	private TinylogConfiguration() {};

	public static void reconfigure(Map<String, String> properties) {
		((TinylogLoggingProvider) ProviderRegistry.getLoggingProvider()).reload(properties);
	}
}
