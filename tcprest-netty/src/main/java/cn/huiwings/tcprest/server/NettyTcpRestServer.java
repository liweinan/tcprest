package cn.huiwings.tcprest.server;

import cn.huiwings.tcprest.ssl.SSLParam;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.CharsetUtil;

import javax.net.ssl.KeyManagerFactory;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;

/**
 * NettyTcpRestServer uses Netty 4.x framework for high-performance async I/O.
 *
 * <p><b>SSL Support:</b> This server supports SSL/TLS via {@link cn.huiwings.tcprest.ssl.SSLParam}.</p>
 *
 * <p><b>Bind Address Support:</b> Supports binding to specific IP addresses for security and multi-homing.</p>
 *
 * <p><b>Features:</b></p>
 * <ul>
 *   <li>High-performance async I/O using Netty 4.x</li>
 *   <li>Line-based frame decoding (handles large payloads correctly)</li>
 *   <li>Optional SSL/TLS support</li>
 *   <li>Optional bind address configuration</li>
 *   <li>Boss/Worker thread pool model</li>
 * </ul>
 *
 * <p><b>Performance:</b> Suitable for high-concurrency production scenarios.
 * Uses NIO event loops for efficient connection handling.</p>
 *
 * @author Weinan Li
 * @date 2012-11-05
 * @updated 2026-02-17 - Upgraded to Netty 4.1.131.Final with SSL support
 */
public class NettyTcpRestServer extends AbstractTcpRestServer {

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private final int port;
    private final String bindAddress;
    private final SSLParam sslParam;

    /**
     * Creates a NettyTcpRestServer with default port (8000) and no SSL, binding to all interfaces.
     */
    public NettyTcpRestServer() {
        this(TcpRestServerConfig.DEFAULT_PORT, null, null);
    }

    /**
     * Creates a NettyTcpRestServer with specified port and no SSL, binding to all interfaces.
     *
     * @param port the port to listen on
     */
    public NettyTcpRestServer(int port) {
        this(port, null, null);
    }

    /**
     * Creates a NettyTcpRestServer with specified port and bind address, no SSL.
     *
     * @param port        the port to listen on
     * @param bindAddress the IP address to bind to (null = all interfaces, "127.0.0.1" = localhost only)
     */
    public NettyTcpRestServer(int port, String bindAddress) {
        this(port, bindAddress, null);
    }

    /**
     * Creates a NettyTcpRestServer with specified port and SSL configuration, binding to all interfaces.
     *
     * @param port     the port to listen on
     * @param sslParam SSL configuration (null for no SSL)
     */
    public NettyTcpRestServer(int port, SSLParam sslParam) {
        this(port, null, sslParam);
    }

    /**
     * Creates a NettyTcpRestServer with specified port, bind address, and SSL configuration.
     *
     * @param port        the port to listen on
     * @param bindAddress the IP address to bind to (null = all interfaces, "127.0.0.1" = localhost only)
     * @param sslParam    SSL configuration (null for no SSL)
     */
    public NettyTcpRestServer(int port, String bindAddress, SSLParam sslParam) {
        this.port = port;
        this.bindAddress = bindAddress;
        this.sslParam = sslParam;
    }

    @Override
    public void up() {
        up(false);
    }

    /**
     * Starts the server.
     *
     * @param setDaemon whether to use daemon threads (deprecated, ignored)
     */
    public void up(boolean setDaemon) {
        bossGroup = new NioEventLoopGroup(1); // Accepts incoming connections
        workerGroup = new NioEventLoopGroup(); // Handles I/O operations

        try {
            final SslContext sslContext = createSslContext();

            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();

                            // Add SSL handler first if SSL is enabled
                            if (sslContext != null) {
                                pipeline.addLast("ssl", sslContext.newHandler(ch.alloc()));
                            }

                            // Inbound pipeline: SSL -> LineFramer -> StringDecoder -> Handler
                            // LineBasedFrameDecoder handles large payloads by reading complete lines
                            pipeline.addLast("lineFramer", new LineBasedFrameDecoder(1024 * 1024)); // 1MB max
                            pipeline.addLast("stringDecoder", new StringDecoder(CharsetUtil.UTF_8));
                            // Create a new handler instance for each channel
                            pipeline.addLast("tcpRestProtocolHandler", new NettyTcpRestProtocolHandler(NettyTcpRestServer.this));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true);

            // Bind and start to accept incoming connections
            InetSocketAddress address = (bindAddress == null || bindAddress.isEmpty())
                    ? new InetSocketAddress(port)
                    : new InetSocketAddress(bindAddress, port);
            ChannelFuture future = bootstrap.bind(address).sync();
            serverChannel = future.channel();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start NettyTcpRestServer", e);
        }
    }

    @Override
    public void down() {
        try {
            if (serverChannel != null) {
                serverChannel.close().sync();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (workerGroup != null) {
                workerGroup.shutdownGracefully();
            }
            if (bossGroup != null) {
                bossGroup.shutdownGracefully();
            }
        }
    }

    @Override
    public int getServerPort() {
        return port;
    }

    /**
     * Creates SSL context if SSL is enabled.
     *
     * @return SslContext or null if SSL is disabled
     * @throws Exception if SSL configuration fails
     */
    private SslContext createSslContext() throws Exception {
        if (sslParam == null) {
            return null;
        }

        // Use "jceks" keystore type to match other server implementations
        KeyStore keyStore = KeyStore.getInstance("jceks");
        InputStream keyStoreStream;

        if (sslParam.getKeyStorePath().startsWith("classpath:")) {
            String path = sslParam.getKeyStorePath().substring("classpath:".length());
            keyStoreStream = getClass().getClassLoader().getResourceAsStream(path);
            if (keyStoreStream == null) {
                throw new IllegalArgumentException("Keystore not found in classpath: " + path);
            }
        } else {
            keyStoreStream = new FileInputStream(sslParam.getKeyStorePath());
        }

        try {
            // Load keystore with null password (tcprest keystores don't have store passwords)
            keyStore.load(keyStoreStream, null);
        } finally {
            keyStoreStream.close();
        }

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(keyStore, sslParam.getKeyStoreKeyPass().toCharArray());

        return SslContextBuilder.forServer(keyManagerFactory).build();
    }
}
