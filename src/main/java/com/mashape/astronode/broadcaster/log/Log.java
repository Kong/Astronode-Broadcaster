package com.mashape.astronode.broadcaster.log;

import org.slf4j.Logger;

import com.mashape.astronode.broadcaster.configuration.BroadcasterConfiguration;
import com.mashape.astronode.broadcaster.configuration.LogLevel;

public class Log {

	public static void trace(final Logger logger, String message) {
		Object[] val = null;
		trace(logger, message, val);
	}

	public static void trace(final Logger logger, String message, Object... value) {
		if (LogLevel.TRACE.getValue() >= BroadcasterConfiguration.getLogLevel().getValue()) {
			logger.trace(message, value);
		}
	}

	public static void info(final Logger logger, String message) {
		Object[] val = null;
		info(logger, message, val);
	}

	public static void info(final Logger logger, String message, Object... value) {
		if (LogLevel.INFO.getValue() >= BroadcasterConfiguration.getLogLevel().getValue()) {
			logger.info(message, value);
		}
	}

	public static void error(final Logger logger, String message) {
		error(logger, message, null);
	}

	public static void error(final Logger logger, String message, Object value) {
		if (LogLevel.ERROR.getValue() >= BroadcasterConfiguration.getLogLevel().getValue()) {
			logger.error(message, value);
		}
	}

}
