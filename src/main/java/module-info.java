module bootstrap {
	requires org.slf4j;
	requires info.picocli;
	requires java.net.http;
	exports org.likide.bootstrap;
	opens org.likide.bootstrap;
}