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

import java.net.Socket;

import javax.net.SocketFactory;

import org.springframework.integration.ip.util.SocketIoUtils;


/**
 * TCP Sending Channel Adapter that that uses a {@link java.net.Socket}.
 * @author Gary Russell
 * @since 2.0
 *
 */
public class TcpNetSendingMessageHandler extends
		AbstractTcpSendingMessageHandler {

	protected Class<NetSocketWriter> customSocketWriterClass;

	/**
	 * Constructs a TcpNetSendingMessageHandler that sends data to the 
	 * specified host and port.
	 * @param host The host.
	 * @param port The port.
	 */
	public TcpNetSendingMessageHandler(String host, int port) {
		super(host, port);
	}
	
	protected volatile Socket socket;
	
	/**
	 * @return the socket
	 */
	protected Socket getSocket() {
		return socket;
	}

	/**
	 * @return the writer
	 */
	protected synchronized SocketWriter getWriter() {
		if (this.writer == null) {
			try {
				logger.debug("Opening new socket connection");
				this.socket = SocketFactory.getDefault().createSocket(this.host, this.port);
				this.setSocketAttributes(socket);
				NetSocketWriter writer = SocketIoUtils.createNetWriter(messageFormat,
						customSocketWriterClass, socket);
				this.writer = writer;
			} catch (Exception e) {
				logger.error("Error creating SocketWriter", e);
			}
		}
		return this.writer;
	}

	/**
	 * @param customSocketWriterClassName the customSocketWriterClassName to set
	 * @throws ClassNotFoundException 
	 */
	@SuppressWarnings("unchecked")
	public void setCustomSocketWriterClassName(
			String customSocketWriterClassName) throws ClassNotFoundException {
		if (customSocketWriterClassName != null) {
			this.customSocketWriterClass = (Class<NetSocketWriter>) Class
					.forName(customSocketWriterClassName);
			if (!(NetSocketWriter.class.isAssignableFrom(this.customSocketWriterClass))) {
				throw new IllegalArgumentException("Custom socket writer must be of type NetSocketWriter");
			}
		}
	}

	/**
	 * Close the underlying socket and prepare to establish a new socket on
	 * the next write.
	 */
	protected void close() {
		this.writer.doClose();
		this.writer = null;
	}

	public void setLocalAddress(String localAddress) {
		logger.warn("localAddress not used on tcp outbound endpoints");
	}
}
