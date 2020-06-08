module org.likide.bootstrap.logging {
	requires transitive org.slf4j;
	
	exports org.likide.bootstrap.logging;
	
	uses org.likide.bootstrap.logging.LoggingManager;
}