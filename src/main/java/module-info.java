module org.likide.bootstrap {
	requires info.picocli;
	
	requires jline.terminal;
	
	requires org.slf4j;
	requires jul.to.slf4j;
	requires org.apache.logging.log4j;
	requires org.apache.logging.log4j.core;
	requires java.logging;

	requires svm;

	requires java.net.http;
	
	exports org.likide.bootstrap;
	opens org.likide.bootstrap;
}