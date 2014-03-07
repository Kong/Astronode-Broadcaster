package com.mashape.astronode.broadcaster.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mashape.astronode.broadcaster.log.Log;
import com.mashape.unirest.http.HttpMethod;

public class BroadcasterConfiguration {

	final static Logger LOG = LoggerFactory.getLogger(BroadcasterConfiguration.class);

	private static String host;
	private static int port;
	private static LogLevel logLevel;
	private static int backlogSize;
	private static boolean reuseAddr;
	private static boolean discardResponses;
	private static int readIdleSeconds;
	private static int reconnectDelaySeconds;
	private static int connectTimeoutSeconds;

	private static List<InetSocketAddress> servers = new LinkedList<>();

	private static boolean autoupdate;
	private static HttpMethod serversAutoupdateMethod;
	private static String serversAutoupdateUrl;
	private static String serversAutoupdateParameters;
	private static int serversAutoupdateRefresh;

	public static void init(String path) {
		Properties properties = new Properties();
		try {
			FileInputStream inputStream = new FileInputStream(new File(path));
			try {
				properties.load(inputStream);
			} catch (IOException e) {
				LOG.error("Invalid configuration file");
			}
		} catch (FileNotFoundException e1) {
			LOG.error("Error loading configuration file");
			throw new RuntimeException("Can't load configuration file at: " + path);
		}

		logLevel = LogLevel.valueOf(loadEntry(properties, "log_level", true, true).toUpperCase());

		host = loadEntry(properties, "host");
		port = Integer.parseInt(loadEntry(properties, "port"));

		backlogSize = Integer.parseInt(loadEntry(properties, "backlog_size"));
		reuseAddr = Boolean.parseBoolean(loadEntry(properties, "reuse_addr"));
		discardResponses = Boolean.parseBoolean(loadEntry(properties, "discard_responses"));
		readIdleSeconds = Integer.parseInt(loadEntry(properties, "read_idle_timeout"));
		reconnectDelaySeconds = Integer.parseInt(loadEntry(properties, "reconnect_delay_timeout"));
		connectTimeoutSeconds = Integer.parseInt(loadEntry(properties, "connect_timeout"));

		String serversList = loadEntry(properties, "servers", false);
		if (StringUtils.isNotBlank(serversList)) {
			String[] confServers = serversList.split(",");
			for (String server : confServers) {
				InetSocketAddress address = getAddress(server);
				if (address != null) {
					servers.add(address);
				}
			}
		}

		String confAutoupdate = loadEntry(properties, "servers_autoupdate", false);
		if (StringUtils.isNotBlank(confAutoupdate) && Boolean.parseBoolean(confAutoupdate)) {
			autoupdate = true;

			// Load method
			String autoupdateMethod = loadEntry(properties, "servers_autoupdate_method");
			serversAutoupdateMethod = HttpMethod.valueOf(autoupdateMethod.toUpperCase());

			serversAutoupdateUrl = loadEntry(properties, "servers_autoupdate_url");
			serversAutoupdateParameters = loadEntry(properties, "servers_autoupdate_parameters", false);
			serversAutoupdateRefresh = Integer.parseInt(loadEntry(properties, "servers_autoupdate_refresh"));
		}

	}

	public static InetSocketAddress getAddress(String server) {
		String[] parts = server.trim().split(":");
		if (parts.length == 2) {
			return new InetSocketAddress(parts[0], Integer.parseInt(parts[1]));
		}
		return null;
	}

	private static String loadEntry(Properties properties, String key, boolean required, boolean skipLog) {
		String value = properties.getProperty(key);
		if (StringUtils.isBlank(value) && required) {
			throw new RuntimeException("Invalid value for key: " + key);
		} else {
			value = value.trim();
		}
		if (!skipLog)
			Log.info(LOG, "> " + key + ": " + value);
		return value;
	}

	private static String loadEntry(Properties properties, String key, boolean required) {
		return loadEntry(properties, key, required, false);
	}

	private static String loadEntry(Properties properties, String key) {
		return loadEntry(properties, key, true, false);
	}

	public static int getPort() {
		return port;
	}

	public static int getBacklogSize() {
		return backlogSize;
	}

	public static boolean getReuseAddr() {
		return reuseAddr;
	}

	public static boolean getDiscardResponses() {
		return discardResponses;
	}

	public static int getReadIdleSeconds() {
		return readIdleSeconds;
	}

	public static int getReconnectDelaySeconds() {
		return reconnectDelaySeconds;
	}

	public static int getConnectTimeoutSeconds() {
		return connectTimeoutSeconds;
	}

	public static String getHost() {
		return host;
	}

	public static List<InetSocketAddress> getServers() {
		return servers;
	}

	public static boolean isAutoupdate() {
		return autoupdate;
	}

	public static HttpMethod getServersAutoupdateMethod() {
		return serversAutoupdateMethod;
	}

	public static String getServersAutoupdateUrl() {
		return serversAutoupdateUrl;
	}

	public static String getServersAutoupdateParameters() {
		return serversAutoupdateParameters;
	}

	public static int getServersAutoupdateRefresh() {
		return serversAutoupdateRefresh;
	}

	public static LogLevel getLogLevel() {
		return logLevel;
	}
	
	public static void setLogLevel(LogLevel logLevelInfo) {
		logLevel = logLevelInfo;
	}

}
