package cn.huiwings.tcprest.server;

import java.util.logging.Logger;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.util.CharsetUtil;

/**
 * Protocol handler for NettyTcpRestServer using Netty 4.x API.
 *
 * <p>Processes incoming TcpRest protocol messages and writes responses.
 * The channel is closed after each response to maintain request-response semantics.</p>
 *
 * @author Weinan Li
 * @date 2012-11-05
 * @updated 2026-02-17 - Upgraded to Netty 4.x API
 */
public class NettyTcpRestProtocolHandler extends SimpleChannelInboundHandler<String> {
    private static final Logger logger = Logger.getLogger(NettyTcpRestProtocolHandler.class.getName());
    private final NettyTcpRestServer serverInstance;

    public NettyTcpRestProtocolHandler(NettyTcpRestServer serverInstance) {
        this.serverInstance = serverInstance;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String request) throws Exception {
        try {
            logger.fine("Received request: " + request);
            String response = serverInstance.processRequest(request);
            logger.fine("Sending response: " + response);
            // Manually create ByteBuf with response + newline for BufferedReader.readLine()
            ByteBuf buf = Unpooled.copiedBuffer(response + "\n", CharsetUtil.UTF_8);
            ctx.writeAndFlush(buf).addListener(ChannelFutureListener.CLOSE);
        } catch (Exception e) {
            logger.severe("Error processing request: " + e.getMessage());
            e.printStackTrace();
            ctx.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.severe("Exception caught in channel: " + cause.getMessage());
        cause.printStackTrace();
        ctx.close();
    }
}
