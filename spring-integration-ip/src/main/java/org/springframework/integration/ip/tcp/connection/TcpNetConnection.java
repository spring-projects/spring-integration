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
import java.net.SocketTimeoutException;

import org.springframework.core.serializer.Serializer;
import org.springframework.core.serializer.Deserializer;
import org.springframework.integration.Message;
import org.springframework.integration.ip.tcp.serializer.SoftEndOfStreamException;

/**
 * A TcpConnection that uses and underlying {@link Socket}.
 * 
 * @author Gary Russell
 * @since 2.0
 *
 */
public class TcpNetConnection extends AbstractTcpConnection {

	private final Socket socket; 
	
	private boolean noReadErrorOnClose;
	
	/**
	 * Constructs a TcpNetConnection for the socket.
	 * @param socket the socket
	 * @param server if true this connection was created as
	 * a result of an incoming request.
	 */
	public TcpNetConnection(Socket socket, boolean server, boolean lookupHost) {
		super(socket, server, lookupHost);
		this.socket = socket;
	}
	
	/**
	 * Closes this connection.
	 */
	public void close() {
		this.noReadErrorOnClose = true;
		try {
			this.socket.close();
		} catch (Exception e) {}
		super.close();
	}

	public boolean isOpen() {
		return !this.socket.isClosed();
	}

	@SuppressWarnings("unchecked")
	public synchronized void send(Message<?> message) throws Exception {
		Object object = this.getMapper().fromMessage(message);
		((Serializer<Object>) this.getSerializer()).serialize(object, this.socket.getOutputStream());
		this.afterSend(message);
	}

	public Object getPayload() throws Exception {
		return this.getDeserializer().deserialize(this.socket.getInputStream());
	}

	public int getPort() {
		return this.socket.getPort();
	}
	
	/**
	 * If there is no listener, and this connection is not for single use, 
	 * this method exits. When there is a listener, the method runs in a
	 * loop reading input from the connections's stream, data is converted
	 * to an object using the {@link Deserializer} and the listener's
	 * {@link TcpListener#onMessage(Message)} method is called. For single use
	 * connections with no listener, the socket is closed after its timeout
	 * expires. If data is received on a single use socket with no listener, 
	 * a warning is logged.
	 */
	public void run() {
		boolean singleUse = this.isSingleUse();
		TcpListener listener = this.getListener();
		if (listener == null && !singleUse) {
			logger.debug("TcpListener exiting - no listener and not single use");
			return;
		}
		Message<?> message = null;
		boolean okToRun = true;
		logger.debug("Reading...");
		boolean intercepted = false;
		while (okToRun) {
			try {
				message = this.getMapper().toMessage(this);
			} catch (Exception e) {
				this.closeConnection();
				if (!(e instanceof SoftEndOfStreamException)) {
					if (e instanceof SocketTimeoutException && singleUse) {
						logger.debug("Closing single use socket after timeout");
					} else {
						if (this.noReadErrorOnClose) {
							if (logger.isDebugEnabled()) {
								logger.debug("Read exception " +
										 this.getConnectionId() + " " +
										 e.getClass().getSimpleName() + 
									     ":" + e.getCause() + ":" + e.getMessage());
							}
						} else if (logger.isTraceEnabled()) {
							logger.error("Read exception " +
									 this.getConnectionId(), e);
						} else {
							logger.error("Read exception " +
										 this.getConnectionId() + " " +
										 e.getClass().getSimpleName() + 
									     ":" + e.getCause() + ":" + e.getMessage());
						}
					}
				}
				break;
			}
			if (logger.isDebugEnabled())
				logger.debug("Message received " + message);
			try {
				if (listener == null) {
					logger.warn("Unexpected message - no inbound adapter registered with connection " + message);
					continue;
				}
				intercepted = listener.onMessage(message);
			} catch (Exception e) {
				if (e instanceof NoListenerException) {
					if (singleUse) {
						logger.debug("Closing single use socket after inbound message " + this.getConnectionId());
						this.closeConnection();
						okToRun = false;
					} else {
						logger.warn("Unexpected message - no inbound adapter registered with connection " + message);
					}
				} else {
					logger.error("Exception sending meeeage: " + message, e);				
				}
			}
			/*
			 * For single use sockets, we close after receipt if we are on the client
			 * side, and the data was not intercepted, 
			 * or the server side has no outbound adapter registered
			 */
			if (singleUse && ((!this.isServer() && !intercepted) || (this.isServer() && this.getSender() == null))) {
				logger.debug("Closing single use socket after inbound message " + this.getConnectionId());
				this.closeConnection();
				okToRun = false;
			}
		}
	}

}
