package io.tcprest.server;

import org.jboss.netty.channel.*;

/**
 * 11 05 2012
 *
 * @author <a href="mailto:l.weinan@gmail.com">Weinan Li</a>
 */
public class NettyTcpRestProtocolHandler extends SimpleChannelHandler {
    private NettyTcpRestServer serverInstance;

    public NettyTcpRestProtocolHandler(NettyTcpRestServer serverInstance) {
        this.serverInstance = serverInstance;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent event) {
        Channel ch = event.getChannel();
        try {
            ChannelFuture future = ch.write(serverInstance.processRequest((String) event.getMessage()));
            future.addListener(new ChannelFutureListener() {
                public void operationComplete(ChannelFuture future) {
                    Channel ch = future.getChannel();
                    ch.close();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        e.getCause().printStackTrace();
        e.getChannel().close();
    }
}
