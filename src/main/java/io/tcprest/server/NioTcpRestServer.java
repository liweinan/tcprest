package io.tcprest.server;

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

/**
 * NioTcpRestServer does not support SSL
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

    public void up(boolean setDaemon) {
        status = TcpRestServerStatus.RUNNING;
        worker = new Thread() {
            public void run() {
                while (worker == Thread.currentThread()) {
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
                                    synchronized (runningChannels) {
                                        runningChannels.add(_sc);
                                    }
                                    StringBuilder requestBuf = new StringBuilder();
                                    decodeChannel(_sc, requestBuf, Charset.forName("UTF-8"));

                                    logger.debug("incoming request: " + requestBuf.toString());
                                    String request = requestBuf.toString();
                                    String response = processRequest(request.trim());


                                    key.interestOps(SelectionKey.OP_WRITE);
                                    key.attach(response);

                                }

                                if (key.isWritable()) {
                                    SocketChannel sc = (SocketChannel) key.channel();
                                    sc.write(ByteBuffer.wrap(((String) key.attachment()).getBytes()));
                                    sc.close();
                                    synchronized (runningChannels) {
                                        runningChannels.remove(sc);
                                    }
                                }

                                iter.remove();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        for (SocketChannel sc : runningChannels) {
                            try {
                                sc.close();
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        }
                    }
                }
            }
        };
        worker.start();
        if (setDaemon) {
            try {
                worker.join();
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

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
        // todo All the already opened channels are not closed properly
        status = TcpRestServerStatus.CLOSING;
        try {
            ssc.close();
            for (SocketChannel sc : runningChannels) {
                sc.close();
            }
            worker = null;
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    /**
     * Use following command to start:
     * <pre>
     * mvn -q exec:java -Dexec.mainClass="io.tcprest.server.NioTcpRestServer"
     * </pre>
     */
    public static void main(String args[]) {
        // todo add config processing code that could load resources
        NioTcpRestServer server = null;
        try {
            // fixme
            server = new NioTcpRestServer();
            server.up(true);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

}
