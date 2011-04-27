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

package org.springframework.integration.ip.tcp.connection;

import java.net.Socket;

import javax.net.SocketFactory;

/**
 * A client connection factory that creates {@link TcpNetConnection}s. 
 * @author Gary Russell
 * @since 2.0
 *
 */
public class TcpNetClientConnectionFactory extends
		AbstractClientConnectionFactory {

	/**
	 * Creates a TcpNetClientConnectionFactory for connections to the host and port.
	 * @param host the host
	 * @param port the port
	 */
	public TcpNetClientConnectionFactory(String host, int port) {
		super(host, port);
	}

	/**
	 * Obtains a connection - if {@link #setSingleUse(boolean)} was called with
	 * true, a new connection is returned; otherwise a single connection is
	 * reused for all requests while the connection remains open.
	 */
	public TcpConnection getConnection() throws Exception {
		if (this.theConnection != null && this.theConnection.isOpen()) {
			return this.theConnection;
		}
		logger.debug("Opening new socket connection to " + this.host + ":" + this.port);
		Socket socket = SocketFactory.getDefault().createSocket(this.host, this.port);
		setSocketAttributes(socket);
		TcpConnection connection = new TcpNetConnection(socket, false, this.isLookupHost());
		connection = wrapConnection(connection);
		initializeConnection(connection, socket);
		this.getTaskExecutor().execute(connection);
		if (!this.singleUse) {
			this.theConnection = connection;
		}
		this.harvestClosedConnections();
		return connection;
	}

	public void close() {
	}

	public void run() {
	}

	public boolean isRunning() {
		return this.active;
	}

}
