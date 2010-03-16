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
import java.lang.reflect.Constructor;
import java.net.ServerSocket;
import java.net.Socket;

import javax.net.ServerSocketFactory;

import org.springframework.beans.BeanUtils;
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
	protected Class<NetSocketReader> customSocketReader;
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
				serverSocket = ServerSocketFactory.getDefault()
						.createServerSocket(port, Math.abs(poolSize));
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
	 * 
	 * @param socket
	 */
	protected void handleSocket(Socket socket) {
		NetSocketReader reader = null;
		if (messageFormat == MessageFormats.FORMAT_CUSTOM) {
			try {
				Constructor<NetSocketReader> ctor = 
					customSocketReader.getConstructor(Socket.class);
				reader = BeanUtils.instantiateClass(ctor, socket);
			} catch (Exception e) {
				throw new MessageMappingException("Failed to instantiate custom reader", e);
			}
		}
		else {
			reader = new NetSocketReader(socket);
		}
		reader.setMessageFormat(messageFormat);
		reader.setMaxMessageSize(receiveBufferSize);
		while (true) {
			try {
				if (reader.assembleData()) {
					Message<byte[]> message = mapper.toMessage(reader);
					if (message != null) {
						sendMessage(message);
					}
				}
			} catch (Exception e) {
				return;
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

	/**
	 * @param customSocketReader the customSocketReader to set
	 * @throws ClassNotFoundException 
	 */
	@SuppressWarnings("unchecked")
	public void setCustomSocketReaderClassName(
			String customSocketReaderClassName) throws ClassNotFoundException {
		this.customSocketReader = (Class<NetSocketReader>) Class
				.forName(customSocketReaderClassName);
	}

}
