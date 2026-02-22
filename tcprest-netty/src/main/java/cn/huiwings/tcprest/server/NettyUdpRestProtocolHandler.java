package cn.huiwings.tcprest.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;
import java.util.logging.Logger;

/**
 * Protocol handler for NettyUdpRestServer. Receives one datagram (one request),
 * processes via V2 protocol, sends one datagram back to the sender.
 *
 * <p>No SSL/DTLS. Request and response must fit in a single UDP packet (see max payload).</p>
 */
public class NettyUdpRestProtocolHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private static final Logger logger = Logger.getLogger(NettyUdpRestProtocolHandler.class.getName());

    private final NettyUdpRestServer serverInstance;
    private final int maxPayloadSize;

    public NettyUdpRestProtocolHandler(NettyUdpRestServer serverInstance, int maxPayloadSize) {
        this.serverInstance = serverInstance;
        this.maxPayloadSize = maxPayloadSize;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
        InetSocketAddress sender = msg.sender();
        ByteBuf content = msg.content();
        if (content.readableBytes() > maxPayloadSize) {
            logger.warning("Dropping oversized datagram: " + content.readableBytes() + " > " + maxPayloadSize);
            return;
        }
        String request = content.toString(CharsetUtil.UTF_8);
        try {
            logger.fine("Received request from " + sender + ": " + sanitizeForLog(request));
            String response = serverInstance.processRequest(request);
            logger.fine("Sending response to " + sender + ": " + sanitizeForLog(response));
            ByteBuf buf = Unpooled.copiedBuffer(response, CharsetUtil.UTF_8);
            ctx.writeAndFlush(new DatagramPacket(buf, sender));
        } catch (Exception e) {
            logger.severe("Error processing request: " + e.getMessage());
            String errorResponse = serverInstance.encodeErrorResponse(e, cn.huiwings.tcprest.protocol.v2.StatusCode.SERVER_ERROR);
            ByteBuf buf = Unpooled.copiedBuffer(errorResponse, CharsetUtil.UTF_8);
            ctx.writeAndFlush(new DatagramPacket(buf, sender));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.severe("Exception caught in UDP channel: " + cause.getMessage());
        cause.printStackTrace();
        ctx.close();
    }

    private static String sanitizeForLog(String s) {
        if (s == null) return "null";
        String t = s.replace("\r", " ").replace("\n", " ");
        return t.length() > 500 ? t.substring(0, 500) + "..." : t;
    }
}
