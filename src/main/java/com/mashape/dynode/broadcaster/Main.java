package com.mashape.dynode.broadcaster;

import java.net.InetSocketAddress;
import java.util.List;
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
import com.mashape.dynode.broadcaster.configuration.DynodeConfiguration;
import com.mashape.dynode.broadcaster.io.ServerLauncher;
import com.mashape.dynode.broadcaster.log.Log;

public class Main {
	
	private static final Logger LOG = LoggerFactory.getLogger(Main.class);

	private static CommandLine parseArguments(Options options, String[] args) throws ParseException {
		options.addOption("c", "configuration", true, "The configuration file path");
		options.addOption("h", false, "Show help");
		
		CommandLineParser parser = new BasicParser();
		CommandLine line = parser.parse(options, args);
		return line;
	}
	
	public static void main(String[] args) throws ParseException {
		Options options = new Options();
		CommandLine line = parseArguments(options, args);
		
		if (line.hasOption("h") || !line.hasOption("c")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("dynode", options, true);
			System.exit(0);
		}
		
		// Load the configuration
		DynodeConfiguration.init(line.getOptionValue("c"));
		
		Log.info(LOG, "Starting Dynode Broadcaster");
		
		final ServerLauncher serverLauncher = new ServerLauncher(
				Sets.newHashSet(new InetSocketAddress(DynodeConfiguration
						.getHost(), DynodeConfiguration.getPort())),
				DynodeConfiguration.getDiscardResponses());

		List<InetSocketAddress> servers = DynodeConfiguration.getServers();
		for(InetSocketAddress server : servers) {
			serverLauncher.getBackendServerManager().addServer(server);
		}
		
		if (DynodeConfiguration.isAutoupdate()) {
			ScheduledExecutorService autoUpdateExecutor = Executors.newSingleThreadScheduledExecutor();
			int refresh = DynodeConfiguration.getServersAutoupdateRefresh();
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
				Log.info(LOG, "Shutting Dynode Broadcaster");
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
