package com.mashape.astronode.broadcaster.test;

import static org.junit.Assert.assertEquals;

import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mashape.astronode.broadcaster.Main;
import com.mashape.astronode.broadcaster.configuration.BroadcasterConfiguration;
import com.mashape.astronode.broadcaster.configuration.LogLevel;
import com.mashape.astronode.broadcaster.test.helper.PortHelper;
import com.mashape.astronode.broadcaster.test.helper.TcpServer;

public class ServerTest {

	final static Logger LOG = LoggerFactory.getLogger(ServerTest.class);
	
	private static final String LOCALHOST = "127.0.0.1";

	private static int tcpPort1;
	private static int tcpPort2;
	private static int broadcasterPort;

	private static List<String> output1;
	private static List<String> output2;

	@Before
	public void setUp() {
		tcpPort1 = PortHelper.getAvailablePort();
		tcpPort2 = PortHelper.getAvailablePort();
		broadcasterPort = PortHelper.getAvailablePort();
		output1 = new LinkedList<>();
		output2 = new LinkedList<>();
	}

	@Test
	public void testBroadcast() throws Throwable {
		// Start the two servers
		new TcpServer(output1, tcpPort1).start();
		new TcpServer(output2, tcpPort2).start();

		Executors.newSingleThreadExecutor().execute(new BroadcasterThread());

		LOG.info("Waiting 5 seconds for broadcaster server to start..");
		Thread.sleep(5000);
		LOG.info("Making client call to broadcaster..");
		try {
			Socket clientSocket = new Socket(LOCALHOST, broadcasterPort);
			DataOutputStream outToServer = new DataOutputStream(
					clientSocket.getOutputStream());
			outToServer.writeBytes("hello\n");
			outToServer.writeBytes("World\n");
			clientSocket.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		LOG.info("Waiting 2 seconds for data to propagate to backend servers");
		Thread.sleep(2000);

		assertEquals(2, output1.size());
		assertEquals(2, output2.size());
		assertEquals("hello", output1.get(0));
		assertEquals("hello", output2.get(0));
		assertEquals("World", output1.get(1));
		assertEquals("World", output2.get(1));
	}

	class BroadcasterThread implements Runnable {
		@Override
		public void run() {
			List<InetSocketAddress> backendServers = new LinkedList<>();
			backendServers.add(new InetSocketAddress(LOCALHOST, tcpPort1));
			backendServers.add(new InetSocketAddress(LOCALHOST, tcpPort2));
			BroadcasterConfiguration.setLogLevel(LogLevel.INFO);
			try {
				Main.start(LOCALHOST, broadcasterPort, backendServers, true,
						false, 0);
			} catch (UnknownHostException e) {
				throw new RuntimeException(e);
			}
		}
	}

}
