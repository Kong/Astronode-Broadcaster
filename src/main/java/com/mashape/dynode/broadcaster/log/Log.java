package com.mashape.dynode.broadcaster.log;

import org.slf4j.Logger;

import com.mashape.dynode.broadcaster.configuration.DynodeConfiguration;
import com.mashape.dynode.broadcaster.configuration.LogLevel;

public class Log {

	public static void trace(final Logger logger, String message) {
		Object[] val = null;
		trace(logger, message, val);
	}

	public static void trace(final Logger logger, String message, Object... value) {
		if (LogLevel.TRACE.getValue() >= DynodeConfiguration.getLogLevel().getValue()) {
			logger.trace(message, value);
		}
	}

	public static void info(final Logger logger, String message) {
		Object[] val = null;
		info(logger, message, val);
	}

	public static void info(final Logger logger, String message, Object... value) {
		if (LogLevel.INFO.getValue() >= DynodeConfiguration.getLogLevel().getValue()) {
			logger.info(message, value);
		}
	}

	public static void error(final Logger logger, String message) {
		error(logger, message, null);
	}

	public static void error(final Logger logger, String message, Object value) {
		if (LogLevel.ERROR.getValue() >= DynodeConfiguration.getLogLevel().getValue()) {
			logger.error(message, value);
		}
	}

}
