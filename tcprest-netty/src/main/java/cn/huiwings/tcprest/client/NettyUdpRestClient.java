package cn.huiwings.tcprest.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * UDP transport client using Netty. Sends one datagram per request and waits for one response.
 * Implements {@link TcpRestClient} for use with {@link TcpRestClientProxy}.
 *
 * <p>No SSL/DTLS. Request and response must fit in a single UDP packet.</p>
 */
public class NettyUdpRestClient implements TcpRestClient {

    private final String delegatedClassName;
    private final String host;
    private final int port;
    private final NioEventLoopGroup group;
    private final io.netty.channel.Channel channel;
    private final BlockingQueue<String> responseQueue = new LinkedBlockingQueue<>(1);

    public NettyUdpRestClient(String delegatedClassName, String host, int port) throws InterruptedException {
        this.delegatedClassName = delegatedClassName;
        this.host = host;
        this.port = port;
        this.group = new NioEventLoopGroup(1);
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioDatagramChannel.class)
                .handler(new ChannelInitializer<NioDatagramChannel>() {
                    @Override
                    protected void initChannel(NioDatagramChannel ch) {
                        ch.pipeline().addLast(new SimpleChannelInboundHandler<DatagramPacket>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) {
                                String s = msg.content().toString(CharsetUtil.UTF_8);
                                if (!responseQueue.offer(s)) {
                                    responseQueue.clear();
                                    responseQueue.offer(s);
                                }
                            }
                        });
                    }
                });
        channel = bootstrap.bind(0).sync().channel();
    }

    @Override
    public synchronized String sendRequest(String request, int timeout) throws Exception {
        responseQueue.clear();
        InetSocketAddress target = new InetSocketAddress(host, port);
        ByteBuf buf = Unpooled.copiedBuffer(request, CharsetUtil.UTF_8);
        channel.writeAndFlush(new DatagramPacket(buf, target)).sync();
        int waitSeconds = timeout > 0 ? timeout : 30;
        String response = responseQueue.poll(waitSeconds, TimeUnit.SECONDS);
        if (response == null) {
            throw new java.net.SocketTimeoutException("UDP response timeout after " + waitSeconds + " seconds");
        }
        return response;
    }

    @Override
    public String getDeletgatedClassName() {
        return delegatedClassName;
    }

    /**
     * Release the event loop group. Call when the client is no longer needed.
     */
    public void shutdown() {
        if (group != null) {
            group.shutdownGracefully();
        }
    }
}
