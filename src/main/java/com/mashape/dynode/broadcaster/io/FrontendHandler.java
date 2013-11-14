package com.mashape.dynode.broadcaster.io;

import com.google.common.base.Function;
import com.mashape.dynode.broadcaster.io.pool.BackendServerManager;
import com.mashape.dynode.broadcaster.io.pool.BackendServerManagerEventListener;
import com.mashape.dynode.broadcaster.log.Log;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class FrontendHandler extends ChannelInboundHandlerAdapter implements BackendServerManagerEventListener {
	
    private static final Logger LOG = LoggerFactory.getLogger(FrontendHandler.class);
    
    private volatile Channel channel;
    private volatile InetSocketAddress clientAddress;
    private final ServerLauncher parent;
    private final Map<InetSocketAddress, BackendHandler> backendHandlers;

    public FrontendHandler(ServerLauncher parent) {
        this.parent = parent;
        this.backendHandlers = new ConcurrentHashMap<>();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        channel = ctx.channel();
        clientAddress = (InetSocketAddress) channel.remoteAddress();
        Log.info(LOG, "Client {}:{} connected", clientAddress.getHostString(), clientAddress.getPort());

        BackendServerManager bsm = parent.getBackendServerManager();
        if (bsm.hasBackendServers()) {
            bsm.traverseAndAddEventListener(new Function<InetSocketAddress, Object>() {
                @Override
                public Object apply(InetSocketAddress input) {
                    backendHandlers.put(input, new BackendHandler(parent, FrontendHandler.this, input));
                    return null;
                }
            }, this);
        } else {
            channel.close();
            Log.info(LOG, "No backend servers in pool. Discarding client {}:{}", clientAddress.getHostString(), clientAddress.getPort());
        }
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            ByteBuf packet = (ByteBuf) msg;
            Collection<BackendHandler> handlers = backendHandlers.values();
            for (BackendHandler backend : handlers) {
                backend.transferPacket(packet.retain());
            }
            Log.trace(LOG, "Packet received from {}:{}. Size: {} byte(s). Broadcasting packet to {} backend server(s)",
                    clientAddress.getHostString(), clientAddress.getPort(), packet.readableBytes(), handlers.size());
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        parent.getBackendServerManager().removeEventListener(this);
        for (BackendHandler backend : backendHandlers.values()) {
            backend.stopCompletely();
        }
        backendHandlers.clear();
        Log.info(LOG, "Client {}:{} disconnected", clientAddress.getHostString(), clientAddress.getPort());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    	Log.error(LOG, "Exception occurred", cause);
        ChannelUtil.closeOnFlush(ctx.channel());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
            	Log.info(LOG, "Disconnecting client {}:{} due to idle timeout", clientAddress.getHostString(), clientAddress.getPort());
                ChannelUtil.closeOnFlush(ctx.channel());
            }
        }
    }

    @Override
    public void serverAdded(InetSocketAddress address) {
    	if (!backendHandlers.containsKey(address)) {
    		backendHandlers.put(address, new BackendHandler(parent, this, address));
    	}
    }

    @Override
    public void serverRemoved(InetSocketAddress address) {
        BackendHandler backendHandler = backendHandlers.remove(address);
        if (backendHandler != null) {
            backendHandler.stopCompletely();
        }
    }

    protected InetSocketAddress getClientAddress() {
        return clientAddress;
    }

    protected Channel getChannel() {
        return channel;
    }
}
