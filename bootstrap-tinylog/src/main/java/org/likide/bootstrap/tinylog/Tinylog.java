package org.likide.bootstrap.tinylog;

import org.likide.bootstrap.logging.LoggingConfiguration;
import org.likide.bootstrap.logging.LoggingManager;
import org.tinylog.provider.ProviderRegistry;

public class Tinylog implements LoggingManager {

	public Tinylog() {
		super();
	}

	@Override
	public void init() {
		System.setProperty("tinylog.provider", TinylogLoggingProvider.class.getName());
	}

	@Override
	public void reconfigure(LoggingConfiguration configuration) {
		((TinylogLoggingProvider) ProviderRegistry.getLoggingProvider()).reload(configuration);
	}

}
