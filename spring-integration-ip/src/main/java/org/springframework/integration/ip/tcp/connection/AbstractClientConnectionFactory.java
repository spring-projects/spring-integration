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

import java.net.Socket;
import java.net.SocketException;

import org.springframework.util.Assert;

/**
 * Abstract class for client connection factories; client connection factories
 * establish outgoing connections.
 * @author Gary Russell
 * @since 2.0
 *
 */
public abstract class AbstractClientConnectionFactory extends AbstractConnectionFactory {

	protected TcpConnection theConnection;

	/**
	 * Constructs a factory that will established connections to the host and port.
	 * @param host The host.
	 * @param port The port.
	 */
	public AbstractClientConnectionFactory(String host, int port) {
		Assert.notNull(host, "host must not be null");
		this.host = host;
		this.port = port;
	}
	
	/**
	 * Transfers attributes such as (de)serializers, singleUse etc to a new connection.
	 * When the connection factory has a reference to a TCPListener (to read 
	 * responses), or for single use connections, the connection is executed.
	 * Single use connections need to read from the connection in order to 
	 * close it after the socket timeout.
	 * @param connection The new connection.
	 * @param socket The new socket. 
	 */
	protected void initializeConnection(TcpConnection connection, Socket socket) {
		if (this.listener != null) {
			connection.registerListener(this.listener);
		}
		if (this.listener != null || this.singleUse) {
			if (this.soTimeout <= 0) {
				try {
					socket.setSoTimeout(DEFAULT_REPLY_TIMEOUT);
				} catch (SocketException e) {
					logger.error("Error setting default reply timeout", e);
				}
			}
		}
		connection.setMapper(this.mapper);
		connection.setDeserializer(this.deserializer);
		connection.setSerializer(this.serializer);
		connection.setSingleUse(this.singleUse);
	}

}
