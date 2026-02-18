package cn.huiwings.tcprest.server;

import java.io.IOException;
import java.net.InetAddress;
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
 * <p><b>Bind Address Support:</b> Supports binding to specific IP addresses for security and multi-homing.</p>
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

    /**
     * Create NIO server on default port (8000) binding to all interfaces.
     *
     * @throws Exception if server creation fails
     */
    public NioTcpRestServer() throws Exception {
        this(TcpRestServerConfig.DEFAULT_PORT);
    }

    /**
     * Create NIO server on specified port binding to all interfaces.
     *
     * @param port the port to bind to
     * @throws Exception if server creation fails
     */
    public NioTcpRestServer(int port) throws Exception {
        this(port, null);
    }

    /**
     * Create NIO server on specified port and bind address.
     *
     * @param port the port to bind to
     * @param bindAddress the IP address to bind to (null = all interfaces, "127.0.0.1" = localhost only)
     * @throws Exception if server creation fails or address is invalid
     */
    public NioTcpRestServer(int port, String bindAddress) throws Exception {
        ssc = ServerSocketChannel.open();
        ServerSocket sc = ssc.socket();

        InetAddress addr = (bindAddress == null || bindAddress.isEmpty())
                ? null
                : InetAddress.getByName(bindAddress);

        sc.bind(new InetSocketAddress(addr, port));
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

                // Read until we get a complete line (non-blocking with short timeout)
                StringBuilder requestBuf = new StringBuilder();
                ByteBuffer bb = ByteBuffer.allocate(1024);
                long deadline = System.currentTimeMillis() + 2000; // 2 second timeout
                boolean lineComplete = false;

                while (!lineComplete && System.currentTimeMillis() < deadline) {
                    int bytesRead = _sc.read(bb);

                    if (bytesRead == -1) {
                        // EOF
                        synchronized (runningChannels) {
                            runningChannels.remove(_sc);
                            _sc.close();
                        }
                        return;
                    }

                    if (bytesRead > 0) {
                        bb.flip();
                        CharBuffer cb = Charset.forName("UTF-8").decode(bb);
                        requestBuf.append(cb.toString());
                        bb.clear();

                        // Check for complete line
                        if (requestBuf.indexOf("\n") >= 0 || requestBuf.indexOf("\r") >= 0) {
                            lineComplete = true;
                        }
                    } else {
                        // No data available, brief pause
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }

                if (lineComplete) {
                    // Process the request
                    String request = requestBuf.toString();
                    int newlineIndex = Math.max(request.indexOf('\n'), request.indexOf('\r'));
                    if (newlineIndex >= 0) {
                        request = request.substring(0, newlineIndex);
                    }

                    logger.debug("incoming request: " + request);
                    String response = processRequest(request.trim());

                    key.attach(response);
                    // Enable write, selector will pick it up on next iteration
                    key.interestOps(SelectionKey.OP_WRITE);
                    key.selector().wakeup();
                }
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
        boolean lineComplete = false;
        long startTime = System.currentTimeMillis();
        final long TIMEOUT_MS = 5000; // 5 second timeout for reading complete line
        int consecutiveEmptyReads = 0;
        final int MAX_CONSECUTIVE_EMPTY = 50; // Allow some retries

        // Read until we get a complete line (ends with \n or \r) or EOF or timeout
        while (!eof && !lineComplete) {
            // Check timeout
            if (System.currentTimeMillis() - startTime > TIMEOUT_MS) {
                break; // Timeout - use whatever we have
            }

            // Input buffer underflow; decoder wants more input
            if (result == CoderResult.UNDERFLOW) {
                // decoder consumed all input, prepare to refill
                bb.clear();
                // Fill the input buffer (non-blocking read)
                int bytesRead = source.read(bb);

                if (bytesRead == -1) {
                    // -1 means EOF (connection closed)
                    eof = true;
                    consecutiveEmptyReads = 0;
                } else if (bytesRead == 0) {
                    // No data available right now in non-blocking mode
                    // Check if we already have a complete line
                    if (builder.indexOf("\n") >= 0 || builder.indexOf("\r") >= 0) {
                        lineComplete = true;
                    } else {
                        // Increment empty read counter
                        consecutiveEmptyReads++;
                        if (consecutiveEmptyReads >= MAX_CONSECUTIVE_EMPTY) {
                            // Too many consecutive empty reads, yield briefly
                            try {
                                Thread.sleep(1);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                            consecutiveEmptyReads = 0;
                        }
                    }
                } else {
                    // Successfully read some data, reset counter
                    consecutiveEmptyReads = 0;
                }

                // Prepare the buffer for reading by decoder
                bb.flip();

                // If buffer is empty and we haven't reached EOF, continue loop
                if (bb.remaining() == 0 && !eof) {
                    continue;
                }
            }

            // Decode input bytes to output chars; pass EOF flag
            result = decoder.decode(bb, cb, eof);

            // If output buffer has content, drain it and check for newline
            if (cb.position() > 0) {
                drainCharBuf(cb, builder);
                // Check if we've received a complete line (ends with \n or \r)
                if (builder.indexOf("\n") >= 0 || builder.indexOf("\r") >= 0) {
                    lineComplete = true;
                }
            }

            // If output buffer is full, drain it
            if (result == CoderResult.OVERFLOW) {
                drainCharBuf(cb, builder);
            }
        }

        // Flush any remaining state from the decoder
        while (decoder.flush(cb) == CoderResult.OVERFLOW) {
            drainCharBuf(cb, builder);
        }

        // Drain any chars remaining in the output buffer
        if (cb.position() > 0) {
            drainCharBuf(cb, builder);
        }
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
