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
public class TcpSendingMessageHandler extends AbstractMessageHandler implements TcpSender {
	
	protected Log logger = LogFactory.getLog(this.getClass());	
	
	protected TcpConnection connection;
	
	protected ConnectionFactory clientConnectionFactory;
	
	protected ConnectionFactory serverConnectionFactory;
	
	protected Map<String, TcpConnection> connections = new ConcurrentHashMap<String, TcpConnection>();
	
	protected synchronized TcpConnection getConnection() {
		try {
			this.connection = clientConnectionFactory.getConnection();
		} catch (Exception e) {
			logger.error("Error creating SocketWriter", e);
		}
		return this.connection;
	}

	/**
	 * Close the underlying socket and prepare to establish a new socket on
	 * the next write.
	 */
	protected void close() {
		this.connection.close();
		this.connection = null;
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
		try {
			TcpConnection connection = getConnection();
			if (connection == null) {
				throw new MessageMappingException(message, "Failed to create connection");
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Got Connection " + connection.getConnectionId());
			}
			connection.send(message);
		} catch (Exception e) {
			String connectionId = null; 
			if (this.connection != null) {
				connectionId = this.connection.getConnectionId();
			}
			this.connection = null;
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
}
