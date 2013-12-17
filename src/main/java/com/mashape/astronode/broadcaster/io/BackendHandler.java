package com.mashape.astronode.broadcaster.io;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.ReferenceCountUtil;

import java.net.InetSocketAddress;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mashape.astronode.broadcaster.configuration.BroadcasterConfiguration;
import com.mashape.astronode.broadcaster.log.Log;

@ChannelHandler.Sharable
class BackendHandler extends ChannelInboundHandlerAdapter implements ChannelFutureListener, Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(BackendHandler.class);

	private final String id;
	private final ServerLauncher parent;
	private final FrontendHandler frontendHandler;
	private final InetSocketAddress clientAddress;
	private final InetSocketAddress serverAddress;
	private final Queue<ByteBuf> packetQueue;
	private final AtomicBoolean keepWorking;
	private volatile Channel channel;
	private volatile boolean transferInProgress = false;
	private final Bootstrap bootstrap;

	public BackendHandler(ServerLauncher parent, FrontendHandler frontendHandler, InetSocketAddress serverAddress) {
		this.parent = parent;
		this.frontendHandler = frontendHandler;
		this.serverAddress = serverAddress;
		this.packetQueue = new ConcurrentLinkedQueue<>();
		this.keepWorking = new AtomicBoolean(true);
		this.clientAddress = frontendHandler.getClientAddress();
		this.id = clientAddress.getHostString() + ':' + clientAddress.getPort() + "<>" + serverAddress.getHostString() + ':'
				+ serverAddress.getPort();
		this.bootstrap = new Bootstrap();
		bootstrap.group(parent.getWorkerGroup()).channel(NioSocketChannel.class)
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, BroadcasterConfiguration.getConnectTimeoutSeconds() * 1000)
				.handler(this);
		Log.trace(LOG, "{} backend handler initialized", id);
	}

	@Override
	public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {
		try {
			if (!parent.isDiscardBackendResponses()) {
				ByteBuf packet = (ByteBuf) msg;
				Channel clientChannel = frontendHandler.getChannel();
				if (clientChannel.isActive() && keepWorking.get()) {
					clientChannel.writeAndFlush(packet.retain());
					Log.trace(LOG, "Transferring received packet from backend server {}:{} to client {}:{}. Size: {} byte(s)",
							serverAddress.getHostString(), serverAddress.getPort(), clientAddress.getHostString(),
							clientAddress.getPort(), packet.readableBytes());
				} else {
					Log.trace(LOG, "Client socket is not active anymore or handler was stopped: {}. Ignoring received packet", id);
				}
			}
		} finally {
			ReferenceCountUtil.release(msg);
		}
	}

	public synchronized void transferPacket(ByteBuf packet) {
		if (keepWorking.get()) {
			packetQueue.add(packet);
			if (!transferInProgress) {
				transferInProgress = true;
				doTransferPendingPacket();
			}
		} else {
			packet.release();
			Log.trace(LOG, "Handler was stopped: {}. Ignoring request to transfer a packet", id);
		}
	}

	private void doTransferPendingPacket() {
		if (keepWorking.get()) {
			if (channel != null && channel.isActive()) {
				transferFirstPacketInQueue();
			} else {
				establishConnection();
			}
		}
	}

	private void establishConnection() {
		if (!keepWorking.get()) {
			return;
		}
		if (channel != null && channel.isOpen()) {
			channel.close();
		}

		ChannelFuture future = bootstrap.connect(serverAddress);
		channel = future.channel();
		future.addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (future.isSuccess()) {
					Log.info(LOG, "Connection with backend server established: {}", id);
					transferFirstPacketInQueue();
				} else {
					channel.close();
					Log.error(LOG,
							"Connection establishment failed: " + serverAddress.getHostString() + ':' + serverAddress.getPort(),
							future.cause());
					if (keepWorking.get()) {
						scheduleConnectionEstablishment();
					}
				}
			}
		});
	}

	private void scheduleConnectionEstablishment() {
		Log.info(LOG, "New attempt to establish a connection with backend " + "server {}:{} will be made {} seconds later",
				serverAddress.getHostString(), serverAddress.getPort(), BroadcasterConfiguration.getReconnectDelaySeconds());
		parent.getWorkerGroup().schedule(BackendHandler.this, BroadcasterConfiguration.getReconnectDelaySeconds(), TimeUnit.SECONDS);
	}

	private void transferFirstPacketInQueue() {
		if (keepWorking.get()) {
			ByteBuf packet = packetQueue.peek();
			if (packet != null) {
				channel.writeAndFlush(packet.duplicate().retain()).addListener(this);
			} else {
				throw new IllegalStateException("Illegal state. Packet queue is empty: " + id);
			}
		}
	}

	@Override
	public void run() {
		establishConnection();
	}

	@Override
	public synchronized void operationComplete(ChannelFuture future) throws Exception {
		if (future.isSuccess()) {
			Log.trace(LOG, "Packet transferred successfully: {}", id);
			ByteBuf packet = packetQueue.poll();
			packet.release();
			if (packetQueue.isEmpty()) {
				transferInProgress = false;
				Log.trace(LOG, "No more packets in queue: {}. Waiting for next packet", id);
			} else if (keepWorking.get()) {
				Log.trace(LOG, "There are packets in queue: {}. Transferring next", id);
				doTransferPendingPacket();
			}
		} else {
			Log.error(LOG, "Packet transferring failed: " + id, future.cause());
			if (keepWorking.get()) {
				channel.close();
				doTransferPendingPacket();
			}
		}
	}

	public void stopCompletely() {
		if (keepWorking.compareAndSet(true, false)) {
			if (channel != null && channel.isActive()) {
				channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(new ChannelFutureListener() {
					@Override
					public void operationComplete(ChannelFuture future) throws Exception {
						channel.close();
						doStopCompletely();
					}
				});
			} else {
				doStopCompletely();
			}
		}
	}

	private void doStopCompletely() {
		for (ByteBuf packet : packetQueue) {
			packet.release();
		}
		packetQueue.clear();
		Log.trace(LOG, "{} backend handler stopped", id);
	}
}
