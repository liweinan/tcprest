package io.tcprest.server;

import io.tcprest.ssl.SSLParam;

import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class NioTcpRestServer extends SingleThreadTcpRestServer {

	public NioTcpRestServer() throws Exception {
		super();
	}

	public NioTcpRestServer(int port) throws Exception {
		super(port);
	}

	public NioTcpRestServer(int port, SSLParam sslParam) throws Exception {
		super(port, sslParam);
	}

	public NioTcpRestServer(ServerSocket socket) {
		super(socket);
	}

	private ByteBuffer buffer = ByteBuffer.allocateDirect(1024);

	@Override
	public void run() {
		
		try {			
			ServerSocketChannel ssc = serverSocket.getChannel();	
			ssc.configureBlocking(false);
			Selector sel = Selector.open();
			ssc.register(sel, SelectionKey.OP_ACCEPT);

			while (true) {
				int n = sel.select();

				if (n == 0)
					continue;

				Iterator iter = sel.selectedKeys().iterator();
				while (iter.hasNext()) {
					SelectionKey key = (SelectionKey) iter.next();

					if (key.isAcceptable()) {
						ServerSocketChannel _ssc = (ServerSocketChannel) key
								.channel();
						SocketChannel _sc = _ssc.accept();

						_sc.configureBlocking(false);
						_sc.register(sel, SelectionKey.OP_READ);

					}

					if (key.isReadable()) {
						SocketChannel _sc = (SocketChannel) key.channel();

						buffer.clear();

						StringBuffer requestBuf = new StringBuffer();

						while (_sc.read(buffer) > 0) {
							buffer.flip();

							CharBuffer charBuf = buffer.asCharBuffer();

							while (charBuf.hasRemaining()) {
								requestBuf.append(charBuf.get());
							}

							buffer.clear();
						}

						String request = requestBuf.toString();
						String response = processRequest(request);

						_sc.write(ByteBuffer.wrap(response.getBytes()));

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

	@Override
	public void up() {
		status = TcpRestServerStatus.RUNNING;
		this.start();
	}

	@Override
	public void down() {
		status = TcpRestServerStatus.CLOSING;
	}

}
