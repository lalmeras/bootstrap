module org.likide.bootstrap {
	requires org.likide.bootstrap.logging;
	
	requires java.net.http;
	
	requires info.picocli;
	
	requires jline.terminal;
	
	requires org.slf4j;
	requires jul.to.slf4j;
	requires java.logging;
	
	exports org.likide.bootstrap;
	opens org.likide.bootstrap;
}
