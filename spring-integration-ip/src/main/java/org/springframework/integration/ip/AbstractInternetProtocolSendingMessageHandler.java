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

package org.springframework.integration.ip;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.core.MessageHandler;
import org.springframework.util.Assert;

/**
 * Base class for all TCP/UDP MessageHandlers.
 *
 * @author Gary Russell
 * @since 2.0
 */
public abstract class AbstractInternetProtocolSendingMessageHandler implements MessageHandler, CommonSocketOptions {

	protected final Log logger = LogFactory.getLog(getClass());

	protected final SocketAddress destinationAddress;

	protected final String host;

	protected final int port;

	protected volatile int soSendBufferSize = -1;

	protected volatile int soTimeout = -1;

	public AbstractInternetProtocolSendingMessageHandler(String host, int port) {
		Assert.notNull(host, "host must not be null");
		this.destinationAddress = new InetSocketAddress(host, port);
		this.host = host;
		this.port = port;
	}


	/**
	 * @see Socket#setSoTimeout(int)
	 * @see DatagramSocket#setSoTimeout(int)
	 * @param timeout
	 */
	public void setSoTimeout(int timeout) {
		this.soTimeout = timeout;
	}

	/**
	 * @see Socket#setReceiveBufferSize(int)
	 * @see DatagramSocket#setReceiveBufferSize(int)
	 * @param size
	 */
	public void setSoReceiveBufferSize(int size) {
	}

	/**
	 * @see Socket#setSendBufferSize(int)
	 * @see DatagramSocket#setSendBufferSize(int)
	 * @param size
	 */
	public void setSoSendBufferSize(int size) {
		this.soSendBufferSize = size;
	}

	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

}
