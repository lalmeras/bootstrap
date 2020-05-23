module org.likide.bootstrap {
	requires java.net.http;
	
	requires info.picocli;
	
	requires jline.terminal;
	
	requires org.slf4j;
	requires org.apache.logging.log4j;
	requires org.apache.logging.log4j.core;
	requires jul.to.slf4j;
	requires java.logging;
	
	exports org.likide.bootstrap;
	opens org.likide.bootstrap;
}
