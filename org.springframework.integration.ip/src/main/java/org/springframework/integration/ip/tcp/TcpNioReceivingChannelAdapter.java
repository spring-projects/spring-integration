/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.integration.ip.tcp;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import org.springframework.integration.core.Message;

/**
 * Tcp Receiving Channel adapter that uses a {@link java.nio.channels.SocketChannel}.
 * Sockets are multiplexed across the pooled threads. More than one thread will
 * be required with large numbers of connections and incoming traffic.
 * 
 * @author Gary Russell
 *
 */
public class TcpNioReceivingChannelAdapter extends
		AbstractTcpReceivingChannelAdapter {

	protected ServerSocketChannel serverChannel;
	
	/**
	 * Constructs a TcpNioReceivingChannelAdapter to listen on the port.
	 * @param port The port.
	 */
	public TcpNioReceivingChannelAdapter(int port) {
		super(port);
	}

	/**
	 * Opens a non-blocking {@link ServerSocketChannel}, registers it with a 
	 * {@link Selector} and calls {@link #doSelect(ServerSocketChannel, Selector)}.
	 * 
	 * @see org.springframework.integration.ip.tcp.AbstractTcpReceivingChannelAdapter#server()
	 */
	@Override
	protected void server() {
		try {
			serverChannel = ServerSocketChannel.open();
			serverChannel.configureBlocking(false);
			serverChannel.socket().bind(new InetSocketAddress(port));
			final Selector selector = Selector.open();
			serverChannel.register(selector, SelectionKey.OP_ACCEPT);
			doSelect(serverChannel, selector);

		} catch (IOException e) {
			if (!active) {
				try {
					serverChannel.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				serverChannel = null;
				return;
			}
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Listens for incoming connections and for notifications that a connected
	 * socket is ready for reading.
	 * Accepts incoming connections, registers the new socket with the 
	 * selector for reading.
	 * When a socket is ready for reading, unregisters the read interest and
	 * schedules a call to doRead which reads all available data. When the read
	 * is complete, the socket is again registered for read interest. 
	 * @param server
	 * @param selector
	 * @throws IOException
	 * @throws ClosedChannelException
	 * @throws SocketException
	 */
	private void doSelect(ServerSocketChannel server, final Selector selector)
			throws IOException, ClosedChannelException, SocketException {
		while (active) {
			int selectionCount = selector.select();
			if (logger.isDebugEnabled())
				logger.debug("SelectionCount: " + selectionCount);
			if (selectionCount > 0) {
				Set<SelectionKey> keys = selector.selectedKeys();
				Iterator<SelectionKey> iterator = keys.iterator();
				SocketChannel channel = null;
				while (iterator.hasNext()) {
					final SelectionKey key = iterator.next();
					iterator.remove();
					if (key.isAcceptable()) {
						channel = server.accept();
						channel.configureBlocking(false);
						channel.register(selector, SelectionKey.OP_READ);
						Socket socket = channel.socket();
						setSocketOptions(socket);
					}
					else if (key.isReadable()) {
						key.interestOps(key.interestOps() - key.readyOps());
						if (key.attachment() == null) {
							NioSocketReader reader = createSocketReader(key);
							key.attach(reader);
						}
						this.threadPoolTaskScheduler.execute(new Runnable() {
							public void run() {
								doRead(key);
								if (key.channel().isOpen()) {
									key.interestOps(SelectionKey.OP_READ);
									selector.wakeup();
								}
							}});
					}
					else {
						logger.error("Unexpected key: " + key);
					}
				}
			}			
		}
	}

	/**
	 * Creates an NioSocketReader, either directly,or 
	 * from the supplied class if {@link MessageFormats#FORMAT_CUSTOM}
	 * is used. 
	 * @param key The selection key.
	 * @return The NioSocketReader.
	 */
	private NioSocketReader createSocketReader(final SelectionKey key) {
		NioSocketReader reader = null;
		SocketChannel channel = (SocketChannel) key.channel();
		if (messageFormat == MessageFormats.FORMAT_CUSTOM) {
			try {
				reader = (NioSocketReader) customSocketReader.newInstance();
				reader.setChannel(channel);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			reader = new NioSocketReader(channel);
		}
		reader.setUsingDirectBuffers(usingDirectBuffers);
		reader.setMessageFormat(messageFormat);
		return reader;
	}

	/**
	 * Obtains the {@link NetSocketReader} associated with the channel 
	 * and calls its {@link NetSocketReader#assembledData}
	 * method; if a message is fully assembled,  calls {@link #sendMessage(Message)} with the
	 * mapped message.
	 * 
	 * @param channel
	 */
	private void doRead(SelectionKey key) {
		NioSocketReader reader = (NioSocketReader) key.attachment();
		try {
			if (reader.assembleData()) {
				Message<byte[]> message;
					message = mapper.toMessage(reader);
					if (message != null) {
						sendMessage(message);
					}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			try {
				key.channel().close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

	@Override
	protected void doStop() {
		super.doStop();
		try {
			this.serverChannel.close();
		}
		catch (Exception e) {
			// ignore
		}
	}

}
 