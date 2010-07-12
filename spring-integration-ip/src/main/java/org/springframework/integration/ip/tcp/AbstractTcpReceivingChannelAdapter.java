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
import java.net.SocketException;

import org.springframework.integration.ip.AbstractInternetProtocolReceivingChannelAdapter;

/**
 * Abstract class for tcp/ip incoming channel adapters. Implementations
 * for {@link java.net.Socket} and {@link java.nio.channels.SocketChannel}
 * are provided.
 * 
 * @author Gary Russell
 * @since 2.0
 *
 */
public abstract class AbstractTcpReceivingChannelAdapter extends
		AbstractInternetProtocolReceivingChannelAdapter {

	protected volatile SocketMessageMapper mapper = new SocketMessageMapper();

	protected volatile boolean soKeepAlive;

	protected volatile int messageFormat = MessageFormats.FORMAT_LENGTH_HEADER;
	
	protected volatile boolean close;
	
	/**
	 * Constructs a receiving channel adapter that listens on the port.
	 * @param port The port to listen on.
	 */
	public AbstractTcpReceivingChannelAdapter(int port) {
		super(port);
	}

	/**
	 * Checks that we have a task executor and calls 
	 * {@link #server()}.
	 */
	public void run() {
		if (logger.isDebugEnabled()) {
			logger.debug(this.getClass().getSimpleName() + " running on port: " + port);
		}
		checkTaskExecutor("TCP-Incoming-Msg-Handler");
		server();
	}

	/**
	 * Establishes the server.
	 */
	protected abstract void server();

	/**
	 * Sets soTimeout, soKeepAlive and tcpNoDelay according to the configured
	 * properties.
	 * @param socket The socket.
	 * @throws SocketException
	 */
	protected void setSocketOptions(Socket socket) throws SocketException {
		socket.setSoTimeout(this.soTimeout);
		if (this.soReceiveBufferSize > 0) {
			socket.setReceiveBufferSize(this.soReceiveBufferSize);
		}
		socket.setKeepAlive(this.soKeepAlive);
		
	}

	/**
	 * @see Socket#setKeepAlive(boolean)
	 * @param soKeepAlive the soKeepAlive to set
	 */
	public void setSoKeepAlive(boolean soKeepAlive) {
		this.soKeepAlive = soKeepAlive;
	}

	/**
	 * @see MessageFormats
	 * @param messageFormat the messageFormat to set
	 */
	public void setMessageFormat(int messageFormat) {
		this.messageFormat = messageFormat;
		mapper.setMessageFormat(messageFormat);
	}

	/**
	 * @param close the close to set
	 */
	public void setClose(boolean close) {
		this.close = close;
	}

}
