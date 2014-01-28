package com.mashape.astronode.broadcaster.test.helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TcpServer extends Thread {
	
	final static Logger LOG = LoggerFactory.getLogger(TcpServer.class);
	
	private boolean done;
	private int port;
	private List<String> output;

	public TcpServer(List<String> output, int port) {
		this.output = output;
		this.port = port;
	}

	@Override
	public void run() {
		try {
			ServerSocket server = new ServerSocket(port);
			LOG.info("TCP server started on port: " + port);
			Socket socket = server.accept();
			BufferedReader dataInputStream = new BufferedReader(
					new InputStreamReader(socket.getInputStream()));
			while (!done) {
				String line = dataInputStream.readLine();
				if (line != null) {
					LOG.info("[" + port + "] Received: " + line);
					output.add(line);
				}
			}
			if (socket != null) {
				socket.close();
			}
			if (dataInputStream != null) {
				dataInputStream.close();
			}
			server.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void close() throws IOException {
		done = true;
	}

}
