/*
 * Copyright 2016-2019 the original author or authors.
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

package org.springframework.integration.ip.tcp.connection;

import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;

import org.springframework.util.Assert;

/**
 * Simple wrapper around {@link Socket} providing access to getters (except
 * input/output streams).
 *
 * @author Gary Russell
 * @since 4.2.5
 *
 */
public class SocketInfo {

	private final Socket socket;

	public SocketInfo(Socket socket) {
		Assert.notNull(socket, "'socket' cannot be null");
		this.socket = socket;
	}

	public InetAddress getInetAddress() {
		return this.socket.getInetAddress();
	}

	public InetAddress getLocalAddress() {
		return this.socket.getLocalAddress();
	}

	public int getPort() {
		return this.socket.getPort();
	}

	public int getLocalPort() {
		return this.socket.getLocalPort();
	}

	public SocketAddress getRemoteSocketAddress() {
		return this.socket.getRemoteSocketAddress();
	}

	public SocketAddress getLocalSocketAddress() {
		return this.socket.getLocalSocketAddress();
	}

	public SocketChannel getChannel() {
		return this.socket.getChannel();
	}

	public boolean getTcpNoDelay() throws SocketException {
		return this.socket.getTcpNoDelay();
	}

	public int getSoLinger() throws SocketException {
		return this.socket.getSoLinger();
	}

	public boolean getOOBInline() throws SocketException {
		return this.socket.getOOBInline();
	}

	public int getSoTimeout() throws SocketException {
		return this.socket.getSoTimeout();
	}

	public int getSendBufferSize() throws SocketException {
		return this.socket.getSendBufferSize();
	}

	public int getReceiveBufferSize() throws SocketException {
		return this.socket.getReceiveBufferSize();
	}

	public boolean getKeepAlive() throws SocketException {
		return this.socket.getKeepAlive();
	}

	public int getTrafficClass() throws SocketException {
		return this.socket.getTrafficClass();
	}

	public boolean getReuseAddress() throws SocketException {
		return this.socket.getReuseAddress();
	}

	@Override
	public String toString() {
		return this.socket.toString();
	}

	public boolean isConnected() {
		return this.socket.isConnected();
	}

	public boolean isClosed() {
		return this.socket.isClosed();
	}

	public boolean isInputShutdown() {
		return this.socket.isInputShutdown();
	}

	public boolean isOutputShutdown() {
		return this.socket.isOutputShutdown();
	}

}
