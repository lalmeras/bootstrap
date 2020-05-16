module org.likide.bootstrap {
	requires org.slf4j;
	requires org.apache.logging.log4j;
	requires org.apache.logging.log4j.core;
	requires info.picocli;
	requires java.net.http;
	exports org.likide.bootstrap;
	opens org.likide.bootstrap;
}