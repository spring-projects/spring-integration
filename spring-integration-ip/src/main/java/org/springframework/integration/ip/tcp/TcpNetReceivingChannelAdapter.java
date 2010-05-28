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
import java.net.ServerSocket;
import java.net.Socket;

import javax.net.ServerSocketFactory;

import org.springframework.integration.core.Message;
import org.springframework.integration.ip.util.SocketIoUtils;

/**
 * Tcp Receiving Channel adapter that uses a {@link Socket}. Each
 * connected socket uses a dedicated thread so the pool size must be set
 * accordingly.
 * 
 * @author Gary Russell
 * @since 2.0
 *
 */
public class TcpNetReceivingChannelAdapter extends
		AbstractTcpReceivingChannelAdapter {

	protected ServerSocket serverSocket;
	protected Class<NetSocketReader> customSocketReaderClass;
	/**
	 * Constructs a TcpNetReceivingChannelAdapter that listens on the provided port.
	 * @param port the port on which to listen
	 */
	public TcpNetReceivingChannelAdapter(int port) {
		super(port);
	}

	/**
	 * Creates the server socket, listens for incoming connections and schedules
	 * execution of the {@link #handleSocket(Socket)} method for each new
	 * connection.
	 * 
	 * @see org.springframework.integration.ip.tcp.AbstractTcpReceivingChannelAdapter#server()
	 */
	@Override
	protected void server() {
		while (active) {
			try {
				serverSocket = ServerSocketFactory.getDefault()
						.createServerSocket(port, Math.abs(poolSize));
				listening = true;
				while (true) {
					final Socket socket = serverSocket.accept();
					setSocketOptions(socket);
					this.threadPoolTaskScheduler.execute(new Runnable() {
						public void run() {
							handleSocket(socket);
						}});
				}
			} catch (IOException e) {
				if (serverSocket != null) {
					try {
						serverSocket.close();
					} catch (IOException e1) {}
				}
				listening = false;
				serverSocket = null;
				if (active) {
					logger.error("Error on ServerSocket", e);
				}

			}

		}

	}

	/**
	 * Constructs a {@link NetSocketReader} and calls its {@link NetSocketReader#assembledData}
	 * method repeatedly; for each assembled message, calls {@link #sendMessage(Message)} with
	 * the mapped message.
	 */
	protected void handleSocket(Socket socket) {
		NetSocketReader reader = SocketIoUtils.createNetReader(messageFormat, 
				customSocketReaderClass, socket, this.receiveBufferSize,
				this.soReceiveBufferSize);
		while (true) {
			try {
				int messageStatus = reader.assembleData();
				if (messageStatus < 0) {
					return;
				}
				if (messageStatus == SocketReader.MESSAGE_COMPLETE) {
					if (close) {
						logger.debug("Closing socket because close=true");
						try {
							reader.getSocket().close();
						} catch (IOException ioe) {
							logger.error("Error on close", ioe);
						}
					}
					processMessage(reader);
					if (close) {
						break;
					}
				}
			} catch (Exception e) {
				logger.error("processMessage failed", e);
				return;
			}
		}
	}

	protected void processMessage(NetSocketReader reader)
			throws Exception {
		Message<byte[]> message = mapper.toMessage(reader);
		if (message != null) {
			sendMessage(message);
		}
	}
	
	@Override
	protected void doStop() {
		super.doStop();
		try {
			this.serverSocket.close();
		}
		catch (Exception e) {
			// ignore
		}
	}

	/**
	 * @param customSocketReaderClassName the {@link NetSocketReader} class to use
	 * @throws ClassNotFoundException if the named class cannot be loaded
	 */
	@SuppressWarnings("unchecked")
	public void setCustomSocketReaderClassName(
			String customSocketReaderClassName) throws ClassNotFoundException {
		if (customSocketReaderClassName != null) {
			this.customSocketReaderClass = (Class<NetSocketReader>) Class
				.forName(customSocketReaderClassName);
			if (!(NetSocketReader.class.isAssignableFrom(this.customSocketReaderClass))) {
				throw new IllegalArgumentException("Custom socket reader must be of type NetSocketReader");
			}
		}
	}

}
