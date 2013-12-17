package com.mashape.astronode.broadcaster.io.pool;

import java.net.InetSocketAddress;

public interface BackendServerManagerEventListener {
	void serverAdded(InetSocketAddress address);

	void serverRemoved(InetSocketAddress address);
}
