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

import org.springframework.integration.adapter.MessageMappingException;
import org.springframework.integration.core.Message;

/**
 * Tcp Receiving Channel adapter that uses a {@link java.net.Socket}. Each
 * connected socket uses a dedicated thread so the pool size must be set
 * accordingly.
 * 
 * @author Gary Russell
 *
 */
public class TcpNetReceivingChannelAdapter extends
		AbstractTcpReceivingChannelAdapter {

	protected ServerSocket serverSocket;
	/**
	 * Constructs a TcpNetReceivingChannelAdapter that listens on the port.
	 * @param port The port.
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
				serverSocket = ServerSocketFactory.getDefault().createServerSocket(port);
				while (true) {
					final Socket socket = serverSocket.accept();
					setSocketOptions(socket);
					this.threadPoolTaskScheduler.execute(new Runnable() {
						public void run() {
							handleSocket(socket);
						}});
				}
			} catch (IOException e) {
				if (!active) {
					if (serverSocket != null) {
						try {
							serverSocket.close();
						} catch (IOException e1) {}
					}
					serverSocket = null;
					return;
				}
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}

	/**
	 * Constructs a {@link NetSocketReader} and calls its {@link NetSocketReader#assembledData}
	 * method repeatedly; for each assembled message, calls {@link #sendMessage(Message)} with
	 * the mapped message.
	 * 
	 * @param socket
	 */
	protected void handleSocket(Socket socket) {
		NetSocketReader reader = null;
		if (messageFormat == MessageFormats.FORMAT_CUSTOM) {
			try {
				reader = (NetSocketReader) customSocketReader.newInstance();
				reader.setSocket(socket);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new MessageMappingException("Failed to instantiate custom reader", e);
			}
		}
		else {
			reader = new NetSocketReader(socket);
		}
		reader.setMessageFormat(messageFormat);
		while (true) {
			try {
				if (reader.assembleData()) {
					Message<byte[]> message = mapper.toMessage(reader);
					if (message != null) {
						sendMessage(message);
					}
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				try {
					socket.close();
					return;
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
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

}
