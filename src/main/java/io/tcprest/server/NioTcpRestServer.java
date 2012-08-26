package io.tcprest.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Iterator;

/**
 * NioTcpRestServer does not support SSL
 *
 * @author Weinan Li
 * @created 08 26 2012
 */
public class NioTcpRestServer extends AbstractTcpRestServer {

    private ServerSocketChannel ssc;

    public NioTcpRestServer() throws Exception {
        this(TcpRestServerConfig.DEFAULT_PORT);
    }

    public NioTcpRestServer(int port) throws Exception {
        ssc = ServerSocketChannel.open();
        ServerSocket sc = ssc.socket();
        sc.bind(new InetSocketAddress(port));
        ssc.configureBlocking(false);

        logger.info("NioServerSocket initialized: " + ssc.socket());
    }

    private ByteBuffer buffer = ByteBuffer.allocateDirect(1024);

    public int getServerPort() {
        return ssc.socket().getLocalPort();
    }

    public void up() {
        status = TcpRestServerStatus.RUNNING;
        new Thread() {
            public void run() {
                try {
                    Selector sel = Selector.open();
                    ssc.register(sel, SelectionKey.OP_ACCEPT);

                    while (true) {
                        int readyCount = sel.select();

                        if (readyCount == 0)
                            continue;

                        Iterator iter = sel.selectedKeys().iterator();
                        while (iter.hasNext()) {
                            SelectionKey key = (SelectionKey) iter.next();

                            if (key.isAcceptable()) {
                                ServerSocketChannel _ssc = (ServerSocketChannel) key.channel();
                                SocketChannel _sc = _ssc.accept();

                                _sc.configureBlocking(false);
                                _sc.register(sel, SelectionKey.OP_READ);

                            }

                            if (key.isReadable()) {
                                SocketChannel _sc = (SocketChannel) key.channel();

                                buffer.clear();

                                StringBuffer requestBuf = new StringBuffer();

                                int count = 0;
                                while ((count = _sc.read(buffer)) > 0) {
                                    buffer.flip();
                                    Charset charset = Charset.forName("UTF-8");
                                    CharsetDecoder decoder = charset.newDecoder();
                                    CharBuffer charBuffer = decoder.decode(buffer);
                                    requestBuf.append(charBuffer);
                                    buffer.clear();
                                }
                                logger.debug("incoming request: " + requestBuf.toString());
                                String request = requestBuf.toString();
                                String response = processRequest(request.trim());

                                // TODO put into seperate channel
                                _sc.write(ByteBuffer.wrap(response.getBytes()));

                                // TODO check logic
                                if (count == 0)
                                    _sc.close();

                            }

                            iter.remove();
                        }
                    }
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        }.start();
    }

    // TODO check logic
    public void down() {
        status = TcpRestServerStatus.CLOSING;
        try {
            ssc.close();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

}
