module bootstrap {
	requires org.slf4j;
	requires info.picocli;
	requires java.net.http;
	//requires org.tinylog.api;
	//requires org.tinylog.impl;
//	requires org.apache.logging.log4j.slf4j;
//	requires org.apache.logging.log4j.core;
	exports org.likide.bootstrap;
	opens org.likide.bootstrap;
}