package com.mashape.dynode.broadcaster.io;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mashape.dynode.broadcaster.configuration.DynodeConfiguration;
import com.mashape.dynode.broadcaster.io.pool.BackendServerManager;
import com.mashape.dynode.broadcaster.log.Log;

public class ServerLauncher {

	private static final Logger LOG = LoggerFactory.getLogger(ServerLauncher.class);

	private final Set<InetSocketAddress> bindAddresses;
	private final boolean discardBackendResponses;
	private final EventLoopGroup bossGroup;
	private final EventLoopGroup workerGroup;
	private final AtomicBoolean started;
	private final AtomicBoolean stopped;
	private final ChannelGroup allChannels;
	private final BackendServerManager backendServerManager;

	public ServerLauncher(Set<InetSocketAddress> bindAddresses, boolean discardBackendResponses) {
		this.bindAddresses = bindAddresses;
		this.discardBackendResponses = discardBackendResponses;
		this.bossGroup = new NioEventLoopGroup();
		this.workerGroup = new NioEventLoopGroup();
		this.started = new AtomicBoolean(false);
		this.stopped = new AtomicBoolean(true);
		this.allChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
		this.backendServerManager = new BackendServerManager();
	}

	public void start() throws InterruptedException {
		if (started.compareAndSet(false, true)) {
			stopped.set(false);

			Log.info(LOG, "Initializing event loop groups and server socket");
			try {
				doStart();
			} finally {
				stop();
			}
		} else {
			throw new IllegalStateException("Call of start() multiply times on a single ServerLauncher instance is not allowed");
		}
	}

	private void doStart() throws InterruptedException {
		ServerBootstrap bootstrap = new ServerBootstrap();
		bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
				.childHandler(new ChannelInitializer<SocketChannel>() {
					@Override
					public void initChannel(SocketChannel ch) throws Exception {
						allChannels.add(ch);
						ChannelPipeline pipeline = ch.pipeline();
						pipeline.addLast(new IdleStateHandler(DynodeConfiguration.getReadIdleSeconds(), 0, 0));
						pipeline.addLast(new FrontendHandler(ServerLauncher.this));
					}
				}).option(ChannelOption.SO_BACKLOG, DynodeConfiguration.getBacklogSize())
				.option(ChannelOption.SO_REUSEADDR, DynodeConfiguration.getReuseAddr());

		List<Channel> serverChannels = new ArrayList<>(bindAddresses.size());
		for (InetSocketAddress addr : bindAddresses) {
			Channel channel = bootstrap.bind(addr).sync().channel();
			allChannels.add(channel);
			serverChannels.add(channel);
			Log.info(LOG, "Server socket bound to {}:{}", addr.getHostString(), addr.getPort());
		}

		for (Channel channel : serverChannels) {
			channel.closeFuture().awaitUninterruptibly();
		}
	}

	public void stop() {
		if (stopped.compareAndSet(false, true)) {
			Log.info(LOG, "Stopping server. Closing all opened sockets");
			allChannels.close().awaitUninterruptibly();

			Log.info(LOG, "Shutdown event loop groups");
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}
	}

	public boolean isDiscardBackendResponses() {
		return discardBackendResponses;
	}

	public BackendServerManager getBackendServerManager() {
		return backendServerManager;
	}

	protected EventLoopGroup getWorkerGroup() {
		return workerGroup;
	}
}
