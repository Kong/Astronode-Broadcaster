package com.mashape.astronode.broadcaster;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.mashape.astronode.broadcaster.configuration.BroadcasterConfiguration;
import com.mashape.astronode.broadcaster.io.ServerLauncher;
import com.mashape.astronode.broadcaster.log.Log;

public class Main {

	private static final Logger LOG = LoggerFactory.getLogger(Main.class);

	private static CommandLine parseArguments(Options options, String[] args) throws ParseException {
		options.addOption("c", "configuration", true, "The configuration file path");
		options.addOption("h", false, "Show help");

		CommandLineParser parser = new BasicParser();
		CommandLine line = parser.parse(options, args);
		return line;
	}

	public static void main(String[] args) throws ParseException, UnknownHostException {
		Options options = new Options();
		CommandLine line = parseArguments(options, args);

		if (line.hasOption("h") || !line.hasOption("c")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("dynode", options, true);
			System.exit(0);
		}

		init(line.getOptionValue("c"));
	}

	public static void init(String configurationPath) throws UnknownHostException {
		// Load the configuration
		BroadcasterConfiguration.init(configurationPath);

		Log.info(LOG, "Starting Astronode Broadcaster");
		
		Set<InetSocketAddress> bindAddresses = new HashSet<>();
		bindAddresses.add(new InetSocketAddress(BroadcasterConfiguration.getHost(), BroadcasterConfiguration.getPort()));
		bindAddresses.add(new InetSocketAddress(InetAddress.getLocalHost().getHostName(), BroadcasterConfiguration.getPort()));
		bindAddresses.add(new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(), BroadcasterConfiguration.getPort()));
		bindAddresses.add(new InetSocketAddress(InetAddress.getLoopbackAddress().getHostAddress(), BroadcasterConfiguration.getPort()));
		
		final ServerLauncher serverLauncher = new ServerLauncher(bindAddresses, BroadcasterConfiguration.getDiscardResponses());

		List<InetSocketAddress> servers = BroadcasterConfiguration.getServers();
		for (InetSocketAddress server : servers) {
			serverLauncher.getBackendServerManager().addServer(server);
		}

		final ScheduledExecutorService autoUpdateExecutor = Executors.newScheduledThreadPool(1);
		if (BroadcasterConfiguration.isAutoupdate()) {
			int refresh = BroadcasterConfiguration.getServersAutoupdateRefresh();
			AutoUpdateTask autoUpdateTask = new AutoUpdateTask(serverLauncher);
			if (refresh > 0) {
				autoUpdateExecutor.scheduleAtFixedRate(autoUpdateTask, 0, refresh, TimeUnit.SECONDS);
			} else {
				autoUpdateExecutor.schedule(autoUpdateTask, 0, TimeUnit.SECONDS);
			}
		}

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				Log.info(LOG, "Shutting Astronode Broadcaster");
				autoUpdateExecutor.shutdown();
				serverLauncher.stop();
			}
		});

		try {
			serverLauncher.start();
		} catch (InterruptedException e) {
			Log.error(LOG, "Failed to start a server");
		}
	}
}
