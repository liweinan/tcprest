package cn.huiwings.tcprest.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * NioTcpRestServer uses Java NIO for non-blocking I/O.
 *
 * <p><b>SSL Support:</b> This server does NOT support SSL/TLS.</p>
 *
 * <p><b>Rationale:</b> Java NIO's SocketChannel doesn't support SSL directly.
 * Implementing SSL with NIO requires using SSLEngine, which adds significant
 * complexity for handshaking, buffer management, and error handling.</p>
 *
 * <p><b>Alternatives for SSL:</b></p>
 * <ul>
 *   <li>For low-traffic SSL: Use {@link SingleThreadTcpRestServer} with {@link cn.huiwings.tcprest.ssl.SSLParam}</li>
 *   <li>For high-traffic SSL: Use {@code NettyTcpRestServer} (in tcprest-netty module)</li>
 * </ul>
 *
 * <p><b>Best use case:</b> High-throughput applications without encryption requirements,
 * or when using external SSL termination (e.g., nginx, HAProxy).</p>
 *
 * @author Weinan Li
 * @created 08 26 2012
 */
public class NioTcpRestServer extends AbstractTcpRestServer {

    private ServerSocketChannel ssc;
    private final List<SocketChannel> runningChannels = new ArrayList<SocketChannel>();
    private Thread worker = null;

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

    public int getServerPort() {
        return ssc.socket().getLocalPort();
    }

    private static final Executor workers = Executors.newCachedThreadPool();

    private class ReadChannelWorker implements Runnable {

        private SelectionKey key;

        public ReadChannelWorker(SelectionKey key) {
            this.key = key;
        }

        public void run() {
            SocketChannel _sc = null;
            try {
                _sc = (SocketChannel) key.channel();
                synchronized (runningChannels) {
                    runningChannels.add(_sc);
                }
                StringBuilder requestBuf = new StringBuilder();
                decodeChannel(_sc, requestBuf, Charset.forName("UTF-8"));

                logger.debug("incoming request: " + requestBuf.toString());
                String request = requestBuf.toString();
                String response = processRequest(request.trim());

                key.attach(response);
                key.interestOps(SelectionKey.OP_WRITE); // ready for writing response
                key.selector().wakeup();
            } catch (Exception e) {
                try {
                    if (_sc != null) {
                        synchronized (runningChannels) {
                            runningChannels.remove(_sc);
                            _sc.close();
                        }
                    }
                } catch (Exception e2) {
                }
            }
        }
    }

    private class WriteChannelWorker implements Runnable {

        private SelectionKey key;

        public WriteChannelWorker(SelectionKey key) {
            this.key = key;
        }

        public void run() {
            SocketChannel sc = null;
            try {
                sc = (SocketChannel) key.channel();
                sc.write(ByteBuffer.wrap(((String) key.attachment()).getBytes()));
            } catch (Exception e) {
            } finally {
                if (sc != null) {
                    try {
                        sc.close(); // response sent, close channel
                        synchronized (runningChannels) {
                            runningChannels.remove(sc);
                        }
                    } catch (Exception e2) {
                    }
                }
            }
        }
    }

    public void up(boolean setDaemon) {
        status = TcpRestServerStatus.RUNNING;
        worker = new Thread() {
            public void run() {
                while (status.equals(TcpRestServerStatus.RUNNING) && !Thread.currentThread().isInterrupted()) {
                    try {
                        Selector sel = Selector.open();
                        ssc.register(sel, SelectionKey.OP_ACCEPT);

                        while (status.equals(TcpRestServerStatus.RUNNING) && !Thread.currentThread().isInterrupted()) {
                            int readyCount = sel.select(1000); // 1 second timeout

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
                                    key.interestOps(key.interestOps() & (~SelectionKey.OP_READ));
                                    workers.execute(new ReadChannelWorker(key));
                                }

                                if (key.isWritable()) {
                                    key.interestOps(key.interestOps() & (~SelectionKey.OP_WRITE));
                                    workers.execute(new WriteChannelWorker(key));
                                }

                                iter.remove();
                            }
                        }
                    } catch (ClosedChannelException e) {
                        // Expected during shutdown when ssc.close() is called
                        logger.debug("Server channel closed: " + e.getMessage());
                        break;
                    } catch (Exception e) {
                        logger.error("Error in NIO server: " + e.getMessage());
                        for (SocketChannel sc : runningChannels) {
                            try {
                                sc.close();
                            } catch (IOException e1) {
                                logger.error("Error closing channel: " + e1.getMessage());
                            }
                        }
                    }
                }
            }
        };
        worker.setDaemon(setDaemon);
        worker.start();
    }

    public void up() {
        up(false);
    }

    /**
     * Borrowed from Java NIO by Ron Hitchens (ron@ronsoft.com) with modifications.
     * <p/>
     * General purpose static method which reads bytes from a Channel,
     * decodes them according
     *
     * @param source  A ReadableByteChannel object which will be read to
     *                EOF as a source of encoded bytes.
     * @param builder A StringBuilder object to which decoded chars will be written.
     * @param charset A Charset object, whose CharsetDecoder will be used * to do the character set decoding.
     */
    private static void decodeChannel(ReadableByteChannel source, StringBuilder builder, Charset charset)
            throws UnsupportedCharsetException, IOException {
        // Get a decoder instance from the Charset
        CharsetDecoder decoder = charset.newDecoder();

        // Tell decoder to replace bad chars with default mark
        // decoder.onMalformedInput(CodingErrorAction.REPLACE);
        // decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);

        ByteBuffer bb = ByteBuffer.allocateDirect(1024);
        CharBuffer cb = CharBuffer.allocate(1024);

        // Buffer starts empty; indicate input is needed
        CoderResult result = CoderResult.UNDERFLOW;
        boolean eof = false;
        while (!eof) {
            // Input buffer underflow; decoder wants more input
            if (result == CoderResult.UNDERFLOW) {
                // decoder consumed all input, prepare to refill
                bb.clear();
                // Fill the input buffer; watch for EOF
                eof = (source.read(bb) == 0);
                // Prepare the buffer for reading by decoder
                bb.flip();
            }
            // Decode input bytes to output chars; pass EOF flag
            result = decoder.decode(bb, cb, eof);
            // If output buffer is full, drain output
            if (result == CoderResult.OVERFLOW) {
                drainCharBuf(cb, builder);
            }
        }
        // Flush any remaining state from the decoder, being careful
        // to detect output buffer overflow(s)
        while (decoder.flush(cb) == CoderResult.OVERFLOW) {
            drainCharBuf(cb, builder);
        }

        // Drain any chars remaining in the output buffer
        drainCharBuf(cb, builder);
    }

    /**
     * Borrowed from Java NIO by Ron Hitchens (ron@ronsoft.com) with modifications.
     * <p/>
     * Helper method to drain the char buffer and write its content to
     * the given Writer object. Upon return, the buffer is empty and
     * ready to be refilled.
     *
     * @param cb      A CharBuffer containing chars to be written.
     * @param builder A StringBuilder object to consume the chars in cb.
     */
    private static void drainCharBuf(CharBuffer cb, StringBuilder builder)
            throws IOException {
        // Prepare buffer for draining
        cb.flip();

        // This writes the chars contained in the CharBuffer but
        // doesn't actually modify the state of the buffer.
        // If the char buffer was being drained by calls to get(),
        // a loop might be needed here.
        if (cb.hasRemaining()) {
            builder.append(cb.toString());
        }
        cb.clear(); // Prepare buffer to be filled again
    }

    public void down() {
        status = TcpRestServerStatus.CLOSING;

        // Interrupt worker thread
        if (worker != null) {
            worker.interrupt();
        }

        // Close server channel and all client channels
        try {
            if (ssc != null && ssc.isOpen()) {
                ssc.close();
            }
            synchronized (runningChannels) {
                for (SocketChannel sc : runningChannels) {
                    try {
                        sc.close();
                    } catch (IOException e) {
                        logger.error("Error closing client channel: " + e.getMessage());
                    }
                }
                runningChannels.clear();
            }
        } catch (IOException e) {
            logger.error("Error closing server channel: " + e.getMessage());
        }

        // Wait for worker thread termination (5 second timeout)
        if (worker != null) {
            try {
                worker.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

}
