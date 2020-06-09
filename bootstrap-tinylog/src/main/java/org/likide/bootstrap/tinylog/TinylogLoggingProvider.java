package org.likide.bootstrap.tinylog;

import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.likide.bootstrap.logging.LoggingConfiguration;
import org.likide.bootstrap.logging.LoggingManager;
import org.tinylog.Level;
import org.tinylog.Supplier;
import org.tinylog.configuration.Configuration;
import org.tinylog.core.ConfigurationParser;
import org.tinylog.core.LogEntry;
import org.tinylog.core.LogEntryValue;
import org.tinylog.core.TinylogContextProvider;
import org.tinylog.format.MessageFormatter;
import org.tinylog.provider.ContextProvider;
import org.tinylog.provider.InternalLogger;
import org.tinylog.provider.LoggingProvider;
import org.tinylog.runtime.RuntimeProvider;
import org.tinylog.runtime.Timestamp;
import org.tinylog.writers.Writer;

public class TinylogLoggingProvider implements LoggingProvider {

	private final TinylogContextProvider context;
	private Level globalLevel;
	private final Map<String, Level> customLevels;
	private final List<String> knownTags;
	private Collection<Writer>[][] writers;
	private Collection<LogEntryValue>[][] requiredLogEntryValues;
	private BitSet fullStackTraceRequired;

	/** */
	public TinylogLoggingProvider() {
		Configuration.replace(buildProperties(LoggingManager.getInstance().newConfiguration()));
		context = new TinylogContextProvider();
		globalLevel = ConfigurationParser.getGlobalLevel();
		customLevels = ConfigurationParser.getCustomLevels();
		knownTags = ConfigurationParser.getTags();

		Level minimumLevel = calculateMinimumLevel(globalLevel, customLevels);
		boolean hasWritingThread = ConfigurationParser.isWritingThreadEnabled();

		writers = ConfigurationParser.createWriters(knownTags, minimumLevel, hasWritingThread);
		requiredLogEntryValues = calculateRequiredLogEntryValues(writers);
		fullStackTraceRequired = calculateFullStackTraceRequirements(requiredLogEntryValues);

		if (ConfigurationParser.isAutoShutdownEnabled()) {
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					try {
						shutdown();
					} catch (InterruptedException ex) {
						InternalLogger.log(Level.ERROR, ex, "Interrupted while waiting for shutdown");
					}
				}
			});
		}
	}

	public void reload(LoggingConfiguration configuration) {
		Configuration.replace(buildProperties(configuration));
		globalLevel = ConfigurationParser.getGlobalLevel();
		customLevels.clear();
		customLevels.putAll(ConfigurationParser.getCustomLevels());
		knownTags.clear();
		knownTags.addAll(ConfigurationParser.getTags());

		Level minimumLevel = calculateMinimumLevel(globalLevel, customLevels);
		boolean hasWritingThread = ConfigurationParser.isWritingThreadEnabled();

		writers = ConfigurationParser.createWriters(knownTags, minimumLevel, hasWritingThread);
		requiredLogEntryValues = calculateRequiredLogEntryValues(writers);
		fullStackTraceRequired = calculateFullStackTraceRequirements(requiredLogEntryValues);
	}

	public Map<String, String> buildProperties(LoggingConfiguration configuration) {
		Map<String, String> properties = new HashMap<>();
		properties.put("writer", "console");
		if (configuration.isIncludeStacktrace()) {
			properties.put("writer.format", "{level}: {class}.{method}()\t{message}");
		} else {
			properties.put("writer.format", "{level}: {class}.{method}()\t{message-only}");
		}
		properties.put("writer.level", "trace");
		properties.put("level", "trace");
		if (configuration.getDefaultLevel() == null) {
			properties.put("level@org", System.getProperty("logging.level", "warn"));
		} else {
			properties.put("level@org", configuration.getDefaultLevel().toString());
		}
		return properties;
	}

	@Override
	public ContextProvider getContextProvider() {
		return context;
	}

	@Override
	public Level getMinimumLevel() {
		Level level = Level.OFF;
		for (int tagIndex = 0; tagIndex < writers.length; ++tagIndex) {
			for (int levelIndex = Level.TRACE.ordinal(); levelIndex < level.ordinal(); ++levelIndex) {
				if (writers[tagIndex][levelIndex].size() > 0) {
					level = Level.values()[levelIndex];
				}
			}
		}
		return level;
	}

	@Override
	public Level getMinimumLevel(final String tag) {
		int tagIndex = getTagIndex(tag);
		for (int levelIndex = Level.TRACE.ordinal(); levelIndex < Level.OFF.ordinal(); ++levelIndex) {
			if (writers[tagIndex][levelIndex].size() > 0) {
				return Level.values()[levelIndex];
			}
		}
		return Level.OFF;
	}

	@Override
	public boolean isEnabled(final int depth, final String tag, final Level level) {
		Level activeLevel;

		if (customLevels.isEmpty()) {
			activeLevel = globalLevel;
		} else {
			String className = RuntimeProvider.getCallerClassName(depth + 1);
			activeLevel = getLevel(className);
		}

		return activeLevel.ordinal() <= level.ordinal() && writers[getTagIndex(tag)][level.ordinal()].size() > 0;
	}

	@Override
	public void log(final int depth, final String tag, final Level level, final Throwable exception, final MessageFormatter formatter,
		final Object obj, final Object... arguments) {
		int tagIndex = getTagIndex(tag);

		StackTraceElement stackTraceElement;
		if (fullStackTraceRequired.get(tagIndex)) {
			stackTraceElement = RuntimeProvider.getCallerStackTraceElement(depth + 1);
		} else {
			stackTraceElement = null;
		}

		Level activeLevel;
		if (customLevels.isEmpty()) {
			if (stackTraceElement == null && requiredLogEntryValues[tagIndex][level.ordinal()].contains(LogEntryValue.CLASS)) {
				stackTraceElement = new StackTraceElement(RuntimeProvider.getCallerClassName(depth + 1), "<unknown>", null, -1);
			}
			activeLevel = globalLevel;
		} else {
			if (stackTraceElement == null) {
				stackTraceElement = new StackTraceElement(RuntimeProvider.getCallerClassName(depth + 1), "<unknown>", null, -1);
			}
			activeLevel = getLevel(stackTraceElement.getClassName());
		}

		if (activeLevel.ordinal() <= level.ordinal()) {
			LogEntry logEntry = createLogEntry(stackTraceElement, tag, tagIndex, level, exception, formatter, obj, arguments);
			output(logEntry, writers[tagIndex][logEntry.getLevel().ordinal()]);
		}
	}

	@Override
	public void log(final String loggerClassName, final String tag, final Level level, final Throwable exception,
		final MessageFormatter formatter, final Object obj, final Object... arguments) {
		int tagIndex = getTagIndex(tag);

		StackTraceElement stackTraceElement;
		if (fullStackTraceRequired.get(tagIndex)) {
			stackTraceElement = RuntimeProvider.getCallerStackTraceElement(loggerClassName);
		} else {
			stackTraceElement = null;
		}

		Level activeLevel;
		if (customLevels.isEmpty()) {
			if (stackTraceElement == null && requiredLogEntryValues[tagIndex][level.ordinal()].contains(LogEntryValue.CLASS)) {
				stackTraceElement = new StackTraceElement(RuntimeProvider.getCallerClassName(loggerClassName), "<unknown>", null, -1);
			}
			activeLevel = globalLevel;
		} else {
			if (stackTraceElement == null) {
				stackTraceElement = new StackTraceElement(RuntimeProvider.getCallerClassName(loggerClassName), "<unknown>", null, -1);
			}
			activeLevel = getLevel(stackTraceElement.getClassName());
		}

		if (activeLevel.ordinal() <= level.ordinal()) {
			LogEntry logEntry = createLogEntry(stackTraceElement, tag, tagIndex, level, exception, formatter, obj, arguments);
			output(logEntry, writers[tagIndex][logEntry.getLevel().ordinal()]);
		}
	}

	@Override
	public void shutdown() throws InterruptedException {
		for (Writer writer : getAllWriters(writers)) {
			try {
				writer.close();
			} catch (Exception ex) {
				InternalLogger.log(Level.ERROR, ex, "Failed to close writer");
			}
		}
	}

	/**
	 * Calculates the minimum severity level that can output any log entries.
	 *
	 * @param globalLevel
	 *            Global severity level
	 * @param customLevels
	 *            Custom severity levels for packages and classes
	 * @return Minimum severity level
	 */
	private static Level calculateMinimumLevel(final Level globalLevel, final Map<String, Level> customLevels) {
		Level minimumLevel = globalLevel;
		for (Level level : customLevels.values()) {
			if (level.ordinal() < minimumLevel.ordinal()) {
				minimumLevel = level;
			}
		}
		return minimumLevel;
	}

	/**
	 * Creates a matrix with all required log entry values for each tag and severity level.
	 *
	 * @param writers
	 *            Matrix with registered writers
	 * @return Matrix with all required log entry values
	 */
	@SuppressWarnings("unchecked")
	private static Collection<LogEntryValue>[][] calculateRequiredLogEntryValues(final Collection<Writer>[][] writers) {
		Collection<LogEntryValue>[][] logEntryValues = new Collection[writers.length][Level.values().length - 1];

		for (int tagIndex = 0; tagIndex < writers.length; ++tagIndex) {
			for (int levelIndex = 0; levelIndex < Level.OFF.ordinal(); ++levelIndex) {
				Set<LogEntryValue> values = EnumSet.noneOf(LogEntryValue.class);
				for (Writer writer : writers[tagIndex][levelIndex]) {
					values.addAll(writer.getRequiredLogEntryValues());
				}
				logEntryValues[tagIndex][levelIndex] = values;
			}
		}

		return logEntryValues;
	}

	/**
	 * Calculates for which tag a full stack trace element with method name, file name and line number is required.
	 *
	 * @param logEntryValues
	 *            Matrix with required log entry values
	 * @return Each set bit represents a tag that requires a full stack trace element
	 */
	private static BitSet calculateFullStackTraceRequirements(final Collection<LogEntryValue>[][] logEntryValues) {
		BitSet result = new BitSet(logEntryValues.length);
		for (int i = 0; i < logEntryValues.length; ++i) {
			Collection<LogEntryValue> values = logEntryValues[i][Level.ERROR.ordinal()];
			if (values.contains(LogEntryValue.METHOD) || values.contains(LogEntryValue.FILE) || values.contains(LogEntryValue.LINE)) {
				result.set(i);
			}
		}
		return result;
	}

	/**
	 * Collects all writer instances from a matrix of writers.
	 *
	 * @param matrix
	 *            All writers
	 * @return Collection that contains each writer only once
	 */
	private static Collection<Writer> getAllWriters(final Collection<Writer>[][] matrix) {
		Collection<Writer> writers = Collections.newSetFromMap(new IdentityHashMap<Writer, Boolean>());
		for (int i = 0; i < matrix.length; ++i) {
			for (int j = 0; j < matrix[i].length; ++j) {
				writers.addAll(matrix[i][j]);
			}
		}
		return writers;
	}

	/**
	 * Gets the index of a tag.
	 *
	 * @param tag
	 *            Name of tag
	 * @return Index of tag
	 */
	private int getTagIndex(final String tag) {
		if (tag == null) {
			return 0;
		} else {
			int index = knownTags.indexOf(tag);
			return index == -1 ? knownTags.size() + 1 : index + 1;
		}
	}

	/**
	 * Gets the severity level for a class. If there is no custom severity level for the class or one of it's
	 * (sub-)packages, the global severity level will be returned.
	 *
	 * @param className
	 *            Fully-qualified class name
	 * @return Severity level for given class
	 */
	private Level getLevel(final String className) {
		String key = className;
		while (true) {
			Level customLevel = customLevels.get(key);
			if (customLevel == null) {
				int index = key.lastIndexOf('.');
				if (index == -1) {
					return globalLevel;
				} else {
					key = key.substring(0, index);
				}
			} else {
				return customLevel;
			}
		}
	}

	/**
	 * Creates a new log entry.
	 *
	 * @param stackTraceElement
	 *            Optional stack trace element of caller
	 * @param tag
	 *            Tag name if issued from a tagged logger
	 * @param tagIndex
	 *            Index of tag
	 * @param level
	 *            Severity level
	 * @param exception
	 *            Caught exception or throwable to log
	 * @param formatter
	 *            Formatter for text message
	 * @param obj
	 *            Message to log
	 * @param arguments
	 *            Arguments for message
	 * @return Filled log entry
	 */
	private LogEntry createLogEntry(final StackTraceElement stackTraceElement, final String tag, final int tagIndex, final Level level,
		final Throwable exception, final MessageFormatter formatter, final Object obj, final Object[] arguments) {
		Collection<LogEntryValue> required = requiredLogEntryValues[tagIndex][level.ordinal()];

		Timestamp timestamp = RuntimeProvider.createTimestamp();
		Thread thread = required.contains(LogEntryValue.THREAD) ? Thread.currentThread() : null;
		Map<String, String> context = required.contains(LogEntryValue.CONTEXT) ? this.context.getMapping() : null;

		String className;
		String methodName;
		String fileName;
		int lineNumber;
		if (stackTraceElement == null) {
			className = null;
			methodName = null;
			fileName = null;
			lineNumber = -1;
		} else {
			className = stackTraceElement.getClassName();
			methodName = stackTraceElement.getMethodName();
			fileName = stackTraceElement.getFileName();
			lineNumber = stackTraceElement.getLineNumber();
		}

		String message;
		if (arguments == null || arguments.length == 0) {
			Object evaluatedObject = obj instanceof Supplier<?> ? ((Supplier<?>) obj).get() : obj;
			message = evaluatedObject == null ? null : evaluatedObject.toString();
		} else {
			message = formatter.format((String) obj, arguments);
		}

		return new LogEntry(timestamp, thread, context, className, methodName, fileName, lineNumber, tag, level, message, exception);
	}

	/**
	 * Outputs a log entry to all passed writers.
	 * 
	 * @param logEntry
	 *            Log entry to be output
	 * @param writers
	 *            All writers for outputting the passed log entry
	 */
	private void output(final LogEntry logEntry, final Iterable<Writer> writers) {
		for (Writer writer : writers) {
			try {
				writer.write(logEntry);
			} catch (Exception ex) {
				InternalLogger.log(Level.ERROR, ex, "Failed to write log entry '" + logEntry.getMessage() + "'");
			}
		}
	}
}
