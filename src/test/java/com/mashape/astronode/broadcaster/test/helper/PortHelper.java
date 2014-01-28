package com.mashape.astronode.broadcaster.test.helper;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;

public class PortHelper {

	private static int MIN_PORT_NUMBER = 10000;
	private static int MAX_PORT_NUMBER = 50000;

	public static int getAvailablePort() {
		int port = 0;
		do {
			port = getRandom(MIN_PORT_NUMBER, MAX_PORT_NUMBER);
		} while (!available(port));

		return port;
	}

	private static int getRandom(int min, int max) {
		return min + (int) (Math.random() * ((max - min) + 1));
	}

	public static boolean available(int port) {
		ServerSocket ss = null;
		DatagramSocket ds = null;
		try {
			ss = new ServerSocket(port);
			ss.setReuseAddress(true);
			ds = new DatagramSocket(port);
			ds.setReuseAddress(true);
			return true;
		} catch (IOException e) {
		} finally {
			if (ds != null) {
				ds.close();
			}

			if (ss != null) {
				try {
					ss.close();
				} catch (IOException e) {
					/* should not be thrown */
				}
			}
		}

		return false;
	}

}
