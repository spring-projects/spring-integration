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
package org.springframework.integration.ip.tcp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.integration.Message;
import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpConnection;
import org.springframework.integration.ip.tcp.connection.TcpListener;
import org.springframework.integration.ip.tcp.connection.TcpSender;

/**
 * Inbound Gateway using a server connection factory - threading is controlled by the
 * factory. For java.net connections, each socket can process only one message at a time.
 * For java.nio connections, messages may be multiplexed but the client will need to 
 * provide correlation logic. If the client is a {@link TcpOutboundGateway} multiplexing
 * is not used, but multiple concurrent connections can be used if the connection factory uses
 * single-use connections. For true asynchronous bi-directional communication, a pair of
 * inbound / outbound channel adapters should be used.
 * @author Gary Russell
 * @since 2.0
 *
 */
public class TcpInboundGateway extends MessagingGatewaySupport implements TcpListener, TcpSender {

	private AbstractServerConnectionFactory connectionFactory;
	
	private Map<String, TcpConnection> connections = new ConcurrentHashMap<String, TcpConnection>();

	public boolean onMessage(Message<?> message) {
		Message<?> reply = this.sendAndReceiveMessage(message);
		if (reply == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("null reply received for " + message + " nothing to send");
			}
			return false;
		}
		String connectionId = (String) message.getHeaders().get(IpHeaders.CONNECTION_ID);
		TcpConnection connection = connections.get(connectionId);
		if (connection == null) {
			logger.error("Connection " + connectionId + " not found when processing reply for " + message);
			return false;
		}
		try {
			connection.send(reply);
		} catch (Exception e) {
			logger.error("Failed to send reply", e);
		}
		return false;
	}

	/** 
	 * @return true if the associated connection factory is listening.
	 */
	public boolean isListening() {
		return connectionFactory.isListening();
	
	}

	/**
	 * 
	 * @param connectionFactory the Connection Factory
	 */
	public void setConnectionFactory(AbstractServerConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
		connectionFactory.registerListener(this);
		connectionFactory.registerSender(this);
	}

	public void addNewConnection(TcpConnection connection) {
		connections.put(connection.getConnectionId(), connection);
	}

	public void removeDeadConnection(TcpConnection connection) {
		connections.remove(connection.getConnectionId());
	}
	public String getComponentType(){
		return "ip:tcp-inbound-gateway";
	}

	/**
	 * @return the connectionFactory
	 */
	protected AbstractServerConnectionFactory getConnectionFactory() {
		return connectionFactory;
	}
}
