package com.mashape.astronode.broadcaster.io.pool;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.mashape.astronode.broadcaster.log.Log;

public class BackendServerManager {
	
	private static final Logger LOG = LoggerFactory.getLogger(BackendServerManager.class);
	
	private Set<InetSocketAddress> backendServers;
	private Set<BackendServerManagerEventListener> eventListeners;

	public BackendServerManager() {
		this.backendServers = new HashSet<>();
		this.eventListeners = new HashSet<>();
	}

	public synchronized boolean hasBackendServers() {
		return !backendServers.isEmpty();
	}

	public synchronized boolean removeEventListener(BackendServerManagerEventListener listener) {
		return eventListeners.remove(listener);
	}

	public synchronized void setServers(final Set<InetSocketAddress> addresses) {
		for (InetSocketAddress address : addresses) {
			addServer(address);
		}
		ImmutableSet<InetSocketAddress> difference = Sets.symmetricDifference(backendServers, addresses).immutableCopy();
		for (InetSocketAddress address : difference) {
			removeServer(address);
		}
	}

	public synchronized boolean addServer(final InetSocketAddress address) {
		boolean result = false;
		if (!backendServers.contains(address)) {
			Log.info(LOG, "* Adding " + address.toString());
			result = backendServers.add(address);
			if (result) {
				for (BackendServerManagerEventListener listener : eventListeners) {
					listener.serverAdded(address);
				}
			}
		}
		return result;
	}

	public synchronized boolean removeServer(final InetSocketAddress address) {
		boolean result = false;
		if (backendServers.contains(address)) {
			Log.info(LOG, "* Removing " + address.toString());
			result = backendServers.remove(address);
			if (result) {
				for (BackendServerManagerEventListener listener : eventListeners) {
					listener.serverRemoved(address);
				}
			}
		}
		return result;
	}

	public synchronized boolean traverseAndAddEventListener(Function<InetSocketAddress, Object> function,
			BackendServerManagerEventListener listener) {
		for (InetSocketAddress address : backendServers) {
			function.apply(address);
		}
		return this.eventListeners.add(listener);
	}
}