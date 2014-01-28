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
		assertTrue(PortHelper.available(20000));
		new ServerSocket(50000);
		assertFalse(PortHelper.available(50000));
	}
	
}
