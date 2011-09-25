/*
 * Copyright 2001-2011 the original author or authors.
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
import java.net.SocketException;

/** 
 * Base class for all server connection factories. Server connection factories
 * listen on a port for incoming connections and create new TcpConnection objects
 * for each new connection.
 *  
 * @author Gary Russell
 * @since 2.0
 */
public abstract class AbstractServerConnectionFactory extends AbstractConnectionFactory {

	protected boolean listening;
	
	protected String localAddress;


	/**
	 * The port on which the factory will listen.
	 * @param port
	 */
	public AbstractServerConnectionFactory(int port) {
		super(port);
	}


	/**
	 * Not supported because the factory manages multiple connections and this
	 * method cannot discriminate.
	 */
	public TcpConnection getConnection() throws Exception {
		throw new UnsupportedOperationException("Getting a connection from a server factory is not supported");
	}

	/**
	 * 
	 * @return true if the server is listening on the port.
	 */
	public boolean isListening() {
		return listening;
	}
	
	/**
	 * Transfers attributes such as (de)serializer, singleUse etc to a new connection.
	 * For single use sockets, enforces a socket timeout (default 10 seconds).
	 * @param connection The new connection.
	 * @param socket The new socket. 
	 */
	protected void initializeConnection(TcpConnection connection, Socket socket) {
		TcpListener listener = this.getListener();
		if (listener != null) {
			connection.registerListener(listener);
		}
		connection.registerSender(this.getSender());
		connection.setMapper(this.getMapper());
		connection.setDeserializer(this.getDeserializer());
		connection.setSerializer(this.getSerializer());
		connection.setSingleUse(this.isSingleUse());
		/*
		 * If we have a collaborating outbound channel adapter and we are configured
		 * for single use; need to enforce a timeout on the socket so we will close
		 * it some period after the response was sent (timeout on the next read).
		 */
		if (this.isSingleUse() && this.getSoTimeout() <= 0 && listener != null) {
			try {
				socket.setSoTimeout(DEFAULT_REPLY_TIMEOUT);
			} catch (SocketException e) {
				logger.error("Error setting default reply timeout", e);
			}
		}

	}
	
	/**
	 * 
	 * @return the localAddress
	 */
	public String getLocalAddress() {
		return localAddress;
	}

	/**
	 * Used on multi-homed systems to enforce the server to listen 
	 * on a specfic network address instead of all network adapters.
	 * @param localAddress the ip address of the required adapter.
	 */
	public void setLocalAddress(String localAddress) {
		this.localAddress = localAddress;
	}
}
