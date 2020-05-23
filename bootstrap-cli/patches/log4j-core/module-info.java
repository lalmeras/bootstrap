module org.apache.logging.log4j.core {
	requires java.desktop;
	requires java.management;
	requires org.apache.logging.log4j;
	
	uses org.apache.logging.log4j.core.util.ContextDataProvider;
	uses org.apache.logging.log4j.core.util.WatchEventService;
	
	exports org.apache.logging.log4j.core;
	exports org.apache.logging.log4j.core.impl to org.apache.logging.log4j;
	provides org.apache.logging.log4j.spi.Provider with org.apache.logging.log4j.core.impl.Log4jProvider;
	provides org.apache.logging.log4j.core.util.ContextDataProvider with org.apache.logging.log4j.core.impl.ThreadContextDataProvider;
}