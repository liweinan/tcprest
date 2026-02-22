package cn.huiwings.tcprest.server;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.net.InetSocketAddress;

/**
 * UDP transport server using Netty NioDatagramChannel.
 *
 * <p>One datagram = one request; one datagram = one response. Reuses Protocol V2 and
 * {@link AbstractTcpRestServer#processRequest(String)}. No SSL/DTLS.</p>
 *
 * <p><b>Payload size:</b> Request and response should fit in a single UDP packet.
 * Default max payload is 1472 bytes (safe for typical MTU). Oversized packets are dropped.</p>
 *
 * @see NettyUdpRestProtocolHandler
 */
public class NettyUdpRestServer extends AbstractTcpRestServer {

    /** Default max datagram payload (bytes) to avoid IP fragmentation. */
    public static final int DEFAULT_MAX_DATAGRAM_PAYLOAD = 1472;

    private EventLoopGroup group;
    private Channel channel;
    private final int port;
    private final String bindAddress;
    private final int maxPayloadSize;

    public NettyUdpRestServer(int port) {
        this(port, null, DEFAULT_MAX_DATAGRAM_PAYLOAD);
    }

    public NettyUdpRestServer(int port, String bindAddress) {
        this(port, bindAddress, DEFAULT_MAX_DATAGRAM_PAYLOAD);
    }

    public NettyUdpRestServer(int port, String bindAddress, int maxPayloadSize) {
        this.port = port;
        this.bindAddress = bindAddress;
        this.maxPayloadSize = maxPayloadSize > 0 ? maxPayloadSize : DEFAULT_MAX_DATAGRAM_PAYLOAD;
    }

    @Override
    public void up() {
        up(false);
    }

    @Override
    public void up(boolean setDaemon) {
        group = new NioEventLoopGroup();
        initializeProtocolComponents();

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_BROADCAST, false)
                    .handler(new io.netty.channel.ChannelInitializer<io.netty.channel.socket.DatagramChannel>() {
                        @Override
                        protected void initChannel(io.netty.channel.socket.DatagramChannel ch) {
                            ch.pipeline().addLast(new NettyUdpRestProtocolHandler(NettyUdpRestServer.this, maxPayloadSize));
                        }
                    });

            InetSocketAddress address = (bindAddress == null || bindAddress.isEmpty())
                    ? new InetSocketAddress(port)
                    : new InetSocketAddress(bindAddress, port);
            channel = bootstrap.bind(address).sync().channel();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start NettyUdpRestServer", e);
        }
    }

    @Override
    public void down() {
        try {
            if (channel != null) {
                channel.close().sync();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (group != null) {
                group.shutdownGracefully();
            }
        }
    }

    @Override
    public int getServerPort() {
        return port;
    }
}
