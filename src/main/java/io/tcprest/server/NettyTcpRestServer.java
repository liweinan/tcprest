package io.tcprest.server;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;
import org.jboss.netty.util.CharsetUtil;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * 11 05 2012
 *
 * @author <a href="mailto:l.weinan@gmail.com">Weinan Li</a>
 */
public class NettyTcpRestServer extends AbstractTcpRestServer {

    private ChannelFactory factory;
    private int port;
    static final ChannelGroup allChannels = new DefaultChannelGroup("NettyTcpRestServer");

    public void up() {
        up(false);
    }

    public static NettyTcpRestServer getInstance() {
        return new NettyTcpRestServer();
    }

    public NettyTcpRestServer() {
        this.port = TcpRestServerConfig.DEFAULT_PORT;
    }


    public NettyTcpRestServer(int port) {
        this.port = port;
    }


    public void up(boolean setDaemon) {
        factory = new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool());

        ServerBootstrap bootstrap = new ServerBootstrap(factory);

        final NettyTcpRestProtocolHandler handler = new NettyTcpRestProtocolHandler(this);

        final ChannelPipeline pipeline = Channels.pipeline();
        pipeline.addLast("stringDecoder", new StringDecoder((CharsetUtil.UTF_8)));
        pipeline.addLast("tcpRestProtocolHandler", handler);
        pipeline.addLast("stringEncoder", new StringEncoder(CharsetUtil.UTF_8));

        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            public ChannelPipeline getPipeline() {
                return pipeline;
            }
        });

        bootstrap.setOption("child.tcpNoDelay", true);
        bootstrap.setOption("child.keepAlive", true);
        allChannels.add(bootstrap.bind(new InetSocketAddress(port)));
    }

    public void down() {
        ChannelGroupFuture future = allChannels.close();
        future.awaitUninterruptibly();
    }

    public int getServerPort() {
        return port;
    }
}
