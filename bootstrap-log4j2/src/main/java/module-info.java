module org.likide.bootstrap.log4j2 {
	requires org.likide.bootstrap.logging;
	requires org.apache.logging.log4j;
	requires org.apache.logging.log4j.core;
	requires org.apache.logging.log4j.slf4j;
	
	provides org.likide.bootstrap.logging.LoggingManager with org.likide.bootstrap.log4j2.Log4j2;
}