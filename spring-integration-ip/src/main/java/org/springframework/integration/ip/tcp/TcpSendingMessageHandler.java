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

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.Message;
import org.springframework.integration.MessageDeliveryException;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory;
import org.springframework.integration.ip.tcp.connection.ConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpConnection;
import org.springframework.integration.ip.tcp.connection.TcpSender;
import org.springframework.integration.mapping.MessageMappingException;

/**
 * Tcp outbound channel adapter using a TcpConnection to
 * send data - if the connection factory is a server
 * factory, the TcpListener owns the connections. If it is
 * a client factory, this object owns the connection.
 * @author Gary Russell
 * @since 2.0
 *
 */
public class TcpSendingMessageHandler extends AbstractMessageHandler implements TcpSender, SmartLifecycle {

	protected final Log logger = LogFactory.getLog(this.getClass());

	protected volatile ConnectionFactory clientConnectionFactory;

	protected volatile ConnectionFactory serverConnectionFactory;

	protected Map<String, TcpConnection> connections = new ConcurrentHashMap<String, TcpConnection>();

	private volatile boolean autoStartup;

	private volatile int phase;

	protected synchronized TcpConnection getConnection() {
		TcpConnection connection = null;
		try {
			connection = clientConnectionFactory.getConnection();
		} catch (Exception e) {
			logger.error("Error creating SocketWriter", e);
		}
		return connection;
	}

	/**
	 * Writes the message payload to the underlying socket, using the specified
	 * message format.
	 * @see org.springframework.integration.core.MessageHandler#handleMessage(org.springframework.integration.Message)
	 */
	public void handleMessageInternal(final Message<?> message) throws MessageRejectedException,
			MessageHandlingException, MessageDeliveryException {
		if (this.serverConnectionFactory != null) {
			// We don't own the connection, we are asynchronously replying
			Object connectionId = message.getHeaders().get(IpHeaders.CONNECTION_ID);
			TcpConnection connection = connections.get(connectionId);
			if (connection != null) {
				try {
					connection.send(message);
				} catch (Exception e) {
					logger.error("Error sending message", e);
					connection.close();
				}
			} else {
				logger.error("Unable to find incoming socket for " + message);
			}
			return;
		}

		// we own the connection
		try {
			doWrite(message);
		} catch (MessageMappingException e) {
			// retry - socket may have closed
			if (e.getCause() instanceof IOException) {
				logger.debug("Fail on first write attempt", e);
				doWrite(message);
			} else {
				throw e;
			}
		}
	}

	/**
	 * Method that actually does the write.
	 * @param message The message to write.
	 */
	protected void doWrite(Message<?> message) {
		TcpConnection connection = null;
		try {
			connection = getConnection();
			if (connection == null) {
				throw new MessageMappingException(message, "Failed to create connection");
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Got Connection " + connection.getConnectionId());
			}
			connection.send(message);
		} catch (Exception e) {
			String connectionId = null;
			if (connection != null) {
				connectionId = connection.getConnectionId();
			}
			if (e instanceof MessageMappingException) {
				throw (MessageMappingException) e;
			}
			throw new MessageMappingException(message, "Failed to map message using " + connectionId, e);
		}
	}

	/**
	 * Sets the client or server connection factory; for this (an outbound adapter), if
	 * the factory is a server connection factory, the sockets are owned by a receiving
	 * channel adapter and this adapter is used to send replies.
	 *
	 * @param connectionFactory the connectionFactory to set
	 */
	public void setConnectionFactory(AbstractConnectionFactory connectionFactory) {
		if (connectionFactory instanceof AbstractClientConnectionFactory) {
			this.clientConnectionFactory = connectionFactory;
		} else {
			this.serverConnectionFactory = connectionFactory;
			connectionFactory.registerSender(this);
		}
	}

	public void addNewConnection(TcpConnection connection) {
		connections.put(connection.getConnectionId(), connection);
	}

	public void removeDeadConnection(TcpConnection connection) {
		connections.remove(connection.getConnectionId());
	}
	public String getComponentType(){
		return "ip:tcp-outbound-channel-adapter";
	}

	public void start() {
		if (this.clientConnectionFactory != null) {
			this.clientConnectionFactory.start();
		}
		if (this.serverConnectionFactory != null) {
			this.serverConnectionFactory.start();
		}
	}

	public void stop() {
		if (this.clientConnectionFactory != null) {
			this.clientConnectionFactory.stop();
		}
		if (this.serverConnectionFactory != null) {
			this.serverConnectionFactory.stop();
		}
	}

	public boolean isRunning() {
		boolean cfRunning = this.clientConnectionFactory != null ? this.clientConnectionFactory.isRunning() : false;
		boolean sfRunning = this.serverConnectionFactory != null ? this.serverConnectionFactory.isRunning() : false;
		return cfRunning | sfRunning;
	}

	public int getPhase() {
		return this.phase;
	}

	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	public void stop(Runnable callback) {
		if (this.clientConnectionFactory != null) {
			this.clientConnectionFactory.stop(callback);
		}
		if (this.serverConnectionFactory != null) {
			this.serverConnectionFactory.stop(callback);
		}
	}

	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	public void setPhase(int phase) {
		this.phase = phase;
	}

}
