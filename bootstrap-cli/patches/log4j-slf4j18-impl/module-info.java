module org.apache.logging.log4j.slf4j {
	requires org.slf4j;
	requires org.apache.logging.log4j;
	
	provides org.slf4j.spi.SLF4JServiceProvider with org.apache.logging.slf4j.SLF4JServiceProvider;
}
