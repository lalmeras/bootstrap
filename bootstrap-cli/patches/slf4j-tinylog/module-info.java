module org.tinylog.api.slf4j {
	exports org.tinylog.slf4j;

	requires org.slf4j;
	requires org.tinylog.api;
	requires org.tinylog.impl;
}