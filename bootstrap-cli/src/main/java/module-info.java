module org.likide.bootstrap {
	requires java.net.http;
	
	requires info.picocli;
	
	requires jline.terminal;
	
	requires org.slf4j;
	requires jul.to.slf4j;
	requires java.logging;
	
	requires org.tinylog.api;
	requires org.tinylog.impl;
	requires org.tinylog.api.slf4j;
	
	exports org.likide.bootstrap;
	opens org.likide.bootstrap;
	
	provides org.slf4j.spi.SLF4JServiceProvider with org.likide.bootstrap.TinylogSLF4JServiceProvider;
	provides org.tinylog.provider.LoggingProvider with org.likide.bootstrap.TinylogLoggingProvider;
	uses org.tinylog.provider.LoggingProvider;
}
