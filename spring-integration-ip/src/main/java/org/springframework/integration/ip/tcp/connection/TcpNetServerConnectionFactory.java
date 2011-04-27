/*
 * Copyright 2002-2011 the original author or authors.
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

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import javax.net.ServerSocketFactory;

/**
 * Implements a server connection factory that produces {@link TcpNetConnection}s using
 * a {@link ServerSocket}. Must have a {@link TcpListener} registered.
 * @author Gary Russell
 * @since 2.0
 *
 */
public class TcpNetServerConnectionFactory extends AbstractServerConnectionFactory {

	protected ServerSocket serverSocket;

	/**
	 * Listens for incoming connections on the port.
	 * @param port The port.
	 */
	public TcpNetServerConnectionFactory(int port) {
		super(port);
	}

	/**
	 * If no listener registers, exits.
	 * Accepts incoming connections and creates TcpConnections for each new connection. 
	 * Invokes {{@link #initializeConnection(TcpConnection, Socket)} and executes the 
	 * connection {@link TcpConnection#run()} using the task executor.
	 * I/O errors on the server socket/channel are logged and the factory is stopped.
	 */
	public void run() {
		ServerSocket theServerSocket = null;
		if (this.listener == null) {
			logger.info("No listener bound to server connection factory; will not read; exiting...");
			return;
		}
		try {
			if (this.localAddress == null) {
				this.serverSocket = ServerSocketFactory.getDefault()
						.createServerSocket(this.port, Math.abs(this.poolSize));
			} else {
				InetAddress whichNic = InetAddress.getByName(this.localAddress);
				this.serverSocket = ServerSocketFactory.getDefault()
						.createServerSocket(port, Math.abs(poolSize), whichNic);
			}
			theServerSocket = this.serverSocket;
			this.listening = true;
			logger.info("Listening on port " + this.port);
			while (true) {
				final Socket socket = serverSocket.accept();
				logger.debug("Accepted connection from " + socket.getInetAddress().getHostAddress());
				setSocketAttributes(socket);
				TcpConnection connection = new TcpNetConnection(socket, true, this.isLookupHost());
				connection = wrapConnection(connection);
				this.initializeConnection(connection, socket);
				this.getTaskExecutor().execute(connection);
				this.harvestClosedConnections();
			}
		} catch (Exception e) {
			this.listening = false;
			// don't log an error if we had a good socket once and now it's closed
			if (e instanceof SocketException && theServerSocket != null) {
				logger.warn("Server Socket closed");
			} else if (this.active) {
				logger.error("Error on ServerSocket", e);
			}
			this.active = false;
		}
	}

	public boolean isRunning() {
		return this.active;
	}

	public void close() {
		if (this.serverSocket == null) {
			return;
		}
		try {
			this.serverSocket.close();
		} catch (IOException e) {}
		this.serverSocket = null;
	}
	

}
