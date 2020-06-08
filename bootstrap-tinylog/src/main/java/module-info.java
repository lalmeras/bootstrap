module org.likide.bootstrap.tinylog {
	requires transitive org.slf4j;
	
	requires transitive org.likide.bootstrap.logging;
	requires org.tinylog.api;
	requires org.tinylog.impl;
	requires org.tinylog.api.slf4j;
	
	exports org.likide.bootstrap.tinylog;
	
	provides org.slf4j.spi.SLF4JServiceProvider with org.likide.bootstrap.tinylog.impl.TinylogSLF4JServiceProvider;
	provides org.likide.bootstrap.logging.LoggingManager with org.likide.bootstrap.tinylog.Tinylog;
}
