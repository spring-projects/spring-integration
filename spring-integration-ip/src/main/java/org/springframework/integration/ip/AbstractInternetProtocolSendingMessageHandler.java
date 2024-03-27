/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.ip;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.support.management.ManageableLifecycle;
import org.springframework.util.Assert;

/**
 * Base class for UDP MessageHandlers.
 *
 * @author Gary Russell
 * @author Christian Tzolov
 *
 * @since 2.0
 */
public abstract class AbstractInternetProtocolSendingMessageHandler extends AbstractMessageHandler
		implements CommonSocketOptions, ManageableLifecycle {

	private final Lock lock = new ReentrantLock();

	private final SocketAddress destinationAddress;

	private final String host;

	private final int port;

	private volatile int soSendBufferSize = -1;

	private volatile int soTimeout = -1;

	private volatile boolean running;

	public AbstractInternetProtocolSendingMessageHandler(String host, int port) {
		Assert.notNull(host, "host must not be null");
		this.destinationAddress = new InetSocketAddress(host, port);
		this.host = host;
		this.port = port;
	}

	/**
	 * @param timeout The timeout.
	 * @see java.net.DatagramSocket#setSoTimeout(int)
	 */
	@Override
	public void setSoTimeout(int timeout) {
		this.soTimeout = timeout;
	}

	/**
	 * @param size The receive buffer size.
	 * @see java.net.DatagramSocket#setReceiveBufferSize(int)
	 */
	@Override
	public void setSoReceiveBufferSize(int size) {
	}

	/**
	 * @param size The send buffer size.
	 * @see java.net.DatagramSocket#setSendBufferSize(int)
	 */
	@Override
	public void setSoSendBufferSize(int size) {
		this.soSendBufferSize = size;
	}

	/**
	 * @return the host
	 */
	public String getHost() {
		return this.host;
	}

	/**
	 * @return the port
	 */
	public int getPort() {
		return this.port;
	}

	/**
	 * @return the destinationAddress
	 */
	public SocketAddress getDestinationAddress() {
		return this.destinationAddress;
	}

	/**
	 * @return the soTimeout
	 */
	public int getSoTimeout() {
		return this.soTimeout;
	}

	/**
	 * @return the soSendBufferSize
	 */
	public int getSoSendBufferSize() {
		return this.soSendBufferSize;
	}

	@Override
	public void start() {
		this.lock.lock();
		try {
			if (!this.running) {
				this.doStart();
				this.running = true;
			}
		}
		finally {
			this.lock.unlock();
		}
	}

	protected abstract void doStart();

	@Override
	public void stop() {
		this.lock.lock();
		try {
			if (this.running) {
				this.doStop();
				this.running = false;
			}
		}
		finally {
			this.lock.unlock();
		}
	}

	protected abstract void doStop();

	@Override
	public boolean isRunning() {
		return this.running;
	}

}
