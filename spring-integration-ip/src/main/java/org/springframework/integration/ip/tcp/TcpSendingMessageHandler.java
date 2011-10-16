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

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

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
import org.springframework.integration.ip.tcp.connection.ClientModeCapable;
import org.springframework.integration.ip.tcp.connection.ClientModeConnectionManager;
import org.springframework.integration.ip.tcp.connection.ConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpConnection;
import org.springframework.integration.ip.tcp.connection.TcpSender;
import org.springframework.integration.mapping.MessageMappingException;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.Assert;

/**
 * Tcp outbound channel adapter using a TcpConnection to
 * send data - if the connection factory is a server
 * factory, the TcpListener owns the connections. If it is
 * a client factory, this object owns the connection.
 * @author Gary Russell
 * @since 2.0
 *
 */
public class TcpSendingMessageHandler extends AbstractMessageHandler implements
		TcpSender, SmartLifecycle, ClientModeCapable {

	protected final Log logger = LogFactory.getLog(this.getClass());

	private volatile AbstractConnectionFactory clientConnectionFactory;

	private volatile AbstractConnectionFactory serverConnectionFactory;

	private Map<String, TcpConnection> connections = new ConcurrentHashMap<String, TcpConnection>();

	private volatile boolean autoStartup = true;

	private volatile int phase;

	private volatile boolean isClientMode;

	private volatile TaskScheduler scheduler;

	private volatile long retryInterval = 60000;

	private volatile ScheduledFuture<?> scheduledFuture;

	private volatile ClientModeConnectionManager clientModeConnectionManager;

	protected final Object lifecycleMonitor = new Object();

	private volatile boolean active;

	protected TcpConnection getConnection() {
		TcpConnection connection = null;
		if (this.clientConnectionFactory == null) {
			return null;
		}
		try {
			connection = this.clientConnectionFactory.getConnection();
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
			TcpConnection connection = null;
			if (connectionId != null) {
				connection = connections.get(connectionId);
			}
			if (connection != null) {
				try {
					connection.send(message);
				} catch (Exception e) {
					logger.error("Error sending message", e);
					connection.close();
				}
			} else {
				logger.error("Unable to find outbound socket for " + message);
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

	@Override
	protected void onInit() throws Exception {
		super.onInit();
		if (this.isClientMode) {
			Assert.notNull(this.clientConnectionFactory,
					"For client-mode, connection factory must be type='client'");
			Assert.isTrue(!this.clientConnectionFactory.isSingleUse(),
					"For client-mode, connection factory must have single-use='false'");
		}
	}

	public void start() {
		synchronized (this.lifecycleMonitor) {
			if (!this.active) {
				this.active = true;
				if (this.clientConnectionFactory != null) {
					this.clientConnectionFactory.start();
				}
				if (this.serverConnectionFactory != null) {
					this.serverConnectionFactory.start();
				}
				if (this.isClientMode) {
					ClientModeConnectionManager manager = new ClientModeConnectionManager(
							this.clientConnectionFactory);
					this.clientModeConnectionManager = manager;
					this.scheduledFuture = this.getScheduler().scheduleAtFixedRate(manager, this.retryInterval);
				}
			}
		}
	}

	public void stop() {
		synchronized (this.lifecycleMonitor) {
			if (this.active) {
				this.active = false;
				if (this.scheduledFuture != null) {
					this.scheduledFuture.cancel(true);
				}
				if (this.clientConnectionFactory != null) {
					this.clientConnectionFactory.stop();
				}
				if (this.serverConnectionFactory != null) {
					this.serverConnectionFactory.stop();
				}
			}
		}
	}

	public boolean isRunning() {
		return this.active;
	}

	public int getPhase() {
		return this.phase;
	}

	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	public void stop(Runnable callback) {
		synchronized (this.lifecycleMonitor) {
			if (this.active) {
				this.active = false;
				if (this.scheduledFuture != null) {
					this.scheduledFuture.cancel(true);
				}
				this.clientModeConnectionManager = null;
				if (this.clientConnectionFactory != null) {
					this.clientConnectionFactory.stop(callback);
				}
				if (this.serverConnectionFactory != null) {
					this.serverConnectionFactory.stop(callback);
				}
			}
		}
	}

	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	public void setPhase(int phase) {
		this.phase = phase;
	}

	/**
	 * @return the clientConnectionFactory
	 */
	protected ConnectionFactory getClientConnectionFactory() {
		return clientConnectionFactory;
	}

	/**
	 * @return the serverConnectionFactory
	 */
	protected ConnectionFactory getServerConnectionFactory() {
		return serverConnectionFactory;
	}

	/**
	 * @return the connections
	 */
	protected Map<String, TcpConnection> getConnections() {
		return connections;
	}

	/**
	 * @return the isClientMode
	 */
	public boolean isClientMode() {
		return this.isClientMode;
	}

	/**
	 * @param isClientMode
	 *            the isClientMode to set
	 */
	public void setClientMode(boolean isClientMode) {
		this.isClientMode = isClientMode;
	}

	/**
	 * @return the scheduler
	 */
	protected TaskScheduler getScheduler() {
		if (this.scheduler == null) {
			ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
			scheduler.initialize();
			this.scheduler = scheduler;
		}
		return this.scheduler;
	}

	/**
	 * @param scheduler
	 *            the scheduler to set
	 */
	public void setScheduler(TaskScheduler scheduler) {
		this.scheduler = scheduler;
	}

	/**
	 * @return the retryInterval
	 */
	public long getRetryInterval() {
		return this.retryInterval;
	}

	/**
	 * @param retryInterval
	 *            the retryInterval to set
	 */
	public void setRetryInterval(long retryInterval) {
		this.retryInterval = retryInterval;
	}

	public boolean isClientModeConnected() {
		if (this.isClientMode && this.clientModeConnectionManager != null) {
			return this.clientModeConnectionManager.isConnected();
		} else {
			return false;
		}
	}

	public void retryConnection() {
		if (this.active && this.isClientMode && this.clientModeConnectionManager != null) {
			this.clientModeConnectionManager.run();
		}
	}

}
