package com.mashape.astronode.broadcaster.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.ServerSocket;

import org.junit.Test;

import com.mashape.astronode.broadcaster.test.helper.PortHelper;

public class PortHelperTest {

	@SuppressWarnings("resource")
	@Test
	public void testPortAvailable() throws IOException {
		int availablePort = PortHelper.getAvailablePort();
		assertTrue(PortHelper.available(availablePort));
		new ServerSocket(availablePort);
		assertFalse(PortHelper.available(availablePort));
	}
	
}
