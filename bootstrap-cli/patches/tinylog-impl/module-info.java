module org.tinylog.impl {
	requires org.tinylog.api;
	
	exports org.tinylog.core;
	exports org.tinylog.policies;
	exports org.tinylog.throwable;
	exports org.tinylog.writers;
	
	provides org.tinylog.policies.Policy with
		org.tinylog.policies.DailyPolicy,
		org.tinylog.policies.StartupPolicy,
		org.tinylog.policies.SizePolicy;
	
	provides org.tinylog.provider.LoggingProvider with org.tinylog.core.TinylogLoggingProvider;

	provides org.tinylog.throwable.ThrowableFilter with
		org.tinylog.throwable.DropCauseThrowableFilter,
		org.tinylog.throwable.KeepThrowableFilter,
		org.tinylog.throwable.StripThrowableFilter,
		org.tinylog.throwable.UnpackThrowableFilter;
	
	provides org.tinylog.writers.Writer with
		org.tinylog.writers.ConsoleWriter,
		org.tinylog.writers.FileWriter,
		org.tinylog.writers.JdbcWriter,
		org.tinylog.writers.LogcatWriter,
		org.tinylog.writers.RollingFileWriter,
		org.tinylog.writers.SharedFileWriter;
}