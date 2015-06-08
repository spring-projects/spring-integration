/*
 * Copyright 2002-2015 the original author or authors.
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
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import javax.net.ssl.SSLSession;

import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.util.Assert;

/**
 * Given a list of connection factories, serves up {@link TcpConnection}s
 * that can iterate over a connection from each factory until the write
 * succeeds or the list is exhausted.
 * @author Gary Russell
 * @since 2.2
 *
 */
public class FailoverClientConnectionFactory extends AbstractClientConnectionFactory {

	private final List<AbstractClientConnectionFactory> factories;

	public FailoverClientConnectionFactory(List<AbstractClientConnectionFactory> factories) {
		super("", 0);
		Assert.notEmpty(factories, "At least one factory is required");
		this.factories = factories;
	}

	@Override
	protected void onInit() throws Exception {
		super.onInit();
		for (AbstractClientConnectionFactory factory : factories) {
			Assert.state(!(this.isSingleUse() ^ factory.isSingleUse()),
				"Inconsistent singleUse - delegate factories must match this one");
		}
	}

	/**
	 * Delegate TCP Client Connection factories that are used to receive
	 * data need a Listener to send the messages to.
	 * This applies to client factories used for outbound gateways
	 * or for a pair of collaborating channel adapters.
	 * <p>
	 * During initialization, if a factory detects it has no listener
	 * it's listening logic (active thread) is terminated.
	 * <p>
	 * The listener registered with a factory is provided to each
	 * connection it creates so it can call the onMessage() method.
	 * <p>
	 * This code satisfies the first requirement in that this
	 * listener signals to the factory that it needs to run
	 * its listening logic.
	 * <p>
	 * When we wrap actual connections with FailoverTcpConnections,
	 * the connection is given the wrapper as a listener, so it
	 * can enhance the headers in onMessage(); the wrapper then invokes
	 * the real listener supplied here, with the modified message.
	 */
	@Override
	public void registerListener(TcpListener listener) {
		super.registerListener(listener);
		for (AbstractClientConnectionFactory factory : this.factories) {
			factory.registerListener(new TcpListener() {
				@Override
				public boolean onMessage(Message<?> message) {
					if (!(message instanceof ErrorMessage)) {
						throw new UnsupportedOperationException("This should never be called");
					}
					return false;
				}
			});
		}
	}

	@Override
	public void enableManualListenerRegistration() {
		for (AbstractClientConnectionFactory factory : this.factories) {
			factory.enableManualListenerRegistration();
		}
	}

	@Override
	public void registerSender(TcpSender sender) {
		for (AbstractClientConnectionFactory factory : this.factories) {
			factory.registerSender(sender);
		}
	}

	@Override
	protected TcpConnectionSupport obtainConnection() throws Exception {
		TcpConnectionSupport connection = this.getTheConnection();
		if (connection != null && connection.isOpen()) {
			((FailoverTcpConnection) connection).incrementEpoch();
			return connection;
		}
		FailoverTcpConnection failoverTcpConnection = new FailoverTcpConnection(this.factories);
		failoverTcpConnection.registerListener(this.getListener());
		failoverTcpConnection.incrementEpoch();
		return failoverTcpConnection;
	}


	@Override
	public void start() {
		for (AbstractClientConnectionFactory factory : this.factories) {
			factory.start();
		}
		this.setActive(true);
		super.start();
	}

	@Override
	public void stop() {
		this.setActive(false);
		for (AbstractClientConnectionFactory factory : this.factories) {
			factory.stop();
		}
	}

	/**
	 * Returns true if all factories are running
	 */
	@Override
	public boolean isRunning() {
		boolean isRunning = true;
		for (AbstractClientConnectionFactory factory : this.factories) {
			isRunning = !isRunning ? false : factory.isRunning();
		}
		return isRunning;
	}

	/**
	 * Wrapper for a list of factories; delegates to a connection from
	 * one of those factories and fails over to another if necessary.
	 * @author Gary Russell
	 * @since 2.2
	 *
	 */
	private class FailoverTcpConnection extends TcpConnectionSupport implements TcpListener {

		private final List<AbstractClientConnectionFactory> factories;

		private final String connectionId;

		private volatile Iterator<AbstractClientConnectionFactory> factoryIterator;

		private volatile AbstractClientConnectionFactory currentFactory;

		private volatile TcpConnectionSupport delegate;

		private volatile boolean open = true;

		private final AtomicLong epoch = new AtomicLong();

		public FailoverTcpConnection(List<AbstractClientConnectionFactory> factories) throws Exception {
			this.factories = factories;
			this.factoryIterator = factories.iterator();
			findAConnection();
			this.connectionId = UUID.randomUUID().toString();
		}

		void incrementEpoch() {
			this.epoch.incrementAndGet();
		}

		/**
		 * Finds a connection from the underlying list of factories. If necessary,
		 * each factory is tried; including the current one if we wrap around.
		 * This allows for the condition where the current connection is closed,
		 * the current factory can serve up a new connection, but all other
		 * factories are down.
		 * @throws Exception
		 */
		private synchronized void findAConnection() throws Exception {
			boolean success = false;
			AbstractClientConnectionFactory lastFactoryToTry = this.currentFactory;
			AbstractClientConnectionFactory nextFactory = null;
			if (!this.factoryIterator.hasNext()) {
				this.factoryIterator = this.factories.iterator();
			}
			boolean retried = false;
			while (!success) {
				try {
					nextFactory = this.factoryIterator.next();
					this.delegate = nextFactory.getConnection();
					this.delegate.registerListener(this);
					this.currentFactory = nextFactory;
					success = this.delegate.isOpen();
				}
				catch (IOException e) {
					if (!this.factoryIterator.hasNext()) {
						if (retried && lastFactoryToTry == null || lastFactoryToTry == nextFactory) {
							/*
							 *  We've tried every factory including the
							 *  one the current connection was on.
							 */
							this.open = false;
							throw e;
						}
						this.factoryIterator = this.factories.iterator();
						retried = true;
					}
				}
			}
		}

		@Override
		public void close() {
			this.delegate.close();
			this.open = false;
		}

		@Override
		public boolean isOpen() {
			return this.open;
		}

		/**
		 * Sends to the current connection; if it fails, attempts to
		 * send to a new connection obtained from {@link #findAConnection()}.
		 * If send fails on a connection from every factory, we give up.
		 */
		@Override
		public synchronized void send(Message<?> message) throws Exception {
			boolean success = false;
			AbstractClientConnectionFactory lastFactoryToTry = this.currentFactory;
			AbstractClientConnectionFactory lastFactoryTried = null;
			boolean retried = false;
			while (!success) {
				try {
					lastFactoryTried = this.currentFactory;
					this.delegate.send(message);
					success = true;
				}
				catch (IOException e) {
					if (retried && lastFactoryTried == lastFactoryToTry) {
						logger.error("All connection factories exhausted", e);
						this.open = false;
						throw e;
					}
					retried = true;
					if (logger.isDebugEnabled()) {
						logger.debug("Send to " + this.delegate.getConnectionId() + " failed; attempting failover", e);
					}
					this.delegate.close();
					findAConnection();
					if (logger.isDebugEnabled()) {
						logger.debug("Failing over to " + this.delegate.getConnectionId());
					}
				}
			}
		}

		@Override
		public Object getPayload() throws Exception {
			return this.delegate.getPayload();
		}

		@Override
		public void run() {
			throw new UnsupportedOperationException("Not supported on FailoverTcpConnection");
		}

		@Override
		public String getHostName() {
			return this.delegate.getHostName();
		}

		@Override
		public String getHostAddress() {
			return this.delegate.getHostAddress();
		}

		@Override
		public int getPort() {
			return this.delegate.getPort();
		}

		@Override
		public Object getDeserializerStateKey() {
			return this.delegate.getDeserializerStateKey();
		}

		@Override
		public void registerSender(TcpSender sender) {
			this.delegate.registerSender(sender);
		}

		@Override
		public String getConnectionId() {
			return this.connectionId + ":" + epoch;
		}

		@Override
		public void setSingleUse(boolean singleUse) {
			this.delegate.setSingleUse(singleUse);
		}

		@Override
		public boolean isSingleUse() {
			return this.delegate.isSingleUse();
		}

		@Override
		public boolean isServer() {
			return this.delegate.isServer();
		}

		@Override
		public void setMapper(TcpMessageMapper mapper) {
			this.delegate.setMapper(mapper);
		}

		@Override
		public Deserializer<?> getDeserializer() {
			return this.delegate.getDeserializer();
		}

		@Override
		public void setDeserializer(Deserializer<?> deserializer) {
			this.delegate.setDeserializer(deserializer);
		}

		@Override
		public Serializer<?> getSerializer() {
			return this.delegate.getSerializer();
		}

		@Override
		public void setSerializer(Serializer<?> serializer) {
			this.delegate.setSerializer(serializer);
		}

		@Override
		public SSLSession getSslSession() {
			return this.delegate.getSslSession();
		}

		/**
		 * We have to intercept the message to replace the connectionId header with
		 * ours so the listener can correlate a response with a request. We supply
		 * the actual connectionId in another header for convenience and tracing
		 * purposes.
		 */
		@Override
		public boolean onMessage(Message<?> message) {
			if (this.delegate.getConnectionId().equals(message.getHeaders().get(IpHeaders.CONNECTION_ID))) {
				AbstractIntegrationMessageBuilder<?> messageBuilder = FailoverClientConnectionFactory.this
						.getMessageBuilderFactory().fromMessage(message)
							.setHeader(IpHeaders.CONNECTION_ID, this.getConnectionId());
				if (message.getHeaders().get(IpHeaders.ACTUAL_CONNECTION_ID) == null) {
					messageBuilder.setHeader(IpHeaders.ACTUAL_CONNECTION_ID,
							message.getHeaders().get(IpHeaders.CONNECTION_ID));
				}
				return this.getListener().onMessage(messageBuilder.build());
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug("Message from defunct connection ignored " + message);
				}
				return false;
			}
		}

	}
}
