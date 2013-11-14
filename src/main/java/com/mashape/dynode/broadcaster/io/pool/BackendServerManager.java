package com.mashape.dynode.broadcaster.io.pool;

import com.google.common.base.Function;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

public class BackendServerManager {
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

    public synchronized boolean addServer(final InetSocketAddress address) {
        boolean result = backendServers.add(address);
        if (result) {
            for (BackendServerManagerEventListener listener : eventListeners) {
                listener.serverAdded(address);
            }
        }
        return result;
    }

    public synchronized boolean removeServer(final InetSocketAddress address) {
        boolean result = backendServers.remove(address);
        if (result) {
            for (BackendServerManagerEventListener listener : eventListeners) {
                listener.serverRemoved(address);
            }
        }
        return result;
    }

    public synchronized boolean traverseAndAddEventListener(Function<InetSocketAddress, Object> function, BackendServerManagerEventListener listener) {
        for (InetSocketAddress address : backendServers) {
            function.apply(address);
        }
        return this.eventListeners.add(listener);
    }
}