module org.likide.bootstrap.tinylog {
	requires org.slf4j;
	
	requires org.tinylog.api;
	requires org.tinylog.impl;
	requires org.tinylog.api.slf4j;
	
	exports org.likide.bootstrap.tinylog;
	
	provides org.slf4j.spi.SLF4JServiceProvider with org.likide.bootstrap.tinylog.impl.TinylogSLF4JServiceProvider;
}
