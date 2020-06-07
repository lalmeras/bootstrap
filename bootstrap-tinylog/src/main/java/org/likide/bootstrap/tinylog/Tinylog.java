package org.likide.bootstrap.tinylog;

import org.likide.bootstrap.tinylog.impl.TinylogConfiguration;
import org.tinylog.provider.ProviderRegistry;

public class Tinylog {

	private Tinylog() {}

	public static void init() {
		System.setProperty("tinylog.provider", TinylogLoggingProvider.class.getName());
	}

	public static void reconfigure(ITinylogConfiguration configuration) {
		((TinylogLoggingProvider) ProviderRegistry.getLoggingProvider()).reload(configuration);
	}

	public static ITinylogConfiguration newConfiguration() {
		return TinylogConfiguration.builder();
	}

}
