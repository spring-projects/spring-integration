/*
 * Copyright 2002-2014 the original author or authors.
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

import java.util.concurrent.Executor;

import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.util.SimplePool;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;

/**
 * @author Gary Russell
 * @since 2.2
 *
 */
public class CachingClientConnectionFactory extends AbstractClientConnectionFactory {

	private final AbstractClientConnectionFactory targetConnectionFactory;

	private final SimplePool<TcpConnectionSupport> pool;

	public CachingClientConnectionFactory(AbstractClientConnectionFactory target, int poolSize) {
		super("", 0);
		// override single-use to true to force "close" after use
		target.setSingleUse(true);
		this.targetConnectionFactory = target;
		pool = new SimplePool<TcpConnectionSupport>(poolSize, new SimplePool.PoolItemCallback<TcpConnectionSupport>() {

			@Override
			public TcpConnectionSupport createForPool() {
				try {
					return targetConnectionFactory.getConnection();
				} catch (Exception e) {
					throw new MessagingException("Failed to obtain connection", e);
				}
			}

			@Override
			public boolean isStale(TcpConnectionSupport connection) {
				return !connection.isOpen();
			}

			@Override
			public void removedFromPool(TcpConnectionSupport connection) {
				connection.close();
			}
		});
	}

	public void setConnectionWaitTimeout(int connectionWaitTimeout) {
		this.pool.setWaitTimeout(connectionWaitTimeout);
	}

	public synchronized void setPoolSize(int poolSize) {
		this.pool.setPoolSize(poolSize);
	}

	public int getPoolSize() {
		return this.pool.getPoolSize();
	}

	public int getIdleCount() {
		return this.pool.getIdleCount();
	}

	public int getActiveCount() {
		return this.pool.getActiveCount();
	}

	public int getAllocatedCount() {
		return this.pool.getAllocatedCount();
	}

	@Override
	public TcpConnectionSupport obtainConnection() throws Exception {
		CachedConnection cachedConnection = new CachedConnection(this.pool.getItem());
		cachedConnection.registerListener(this.getListener());
		return cachedConnection;
	}

	private class CachedConnection extends TcpConnectionInterceptorSupport {

		private volatile boolean released;

		public CachedConnection(TcpConnectionSupport connection) {
			super.setTheConnection(connection);
			connection.registerListener(this);
		}

		@Override
		public synchronized void close() {
			if(this.released) {
				if (logger.isDebugEnabled()) {
					logger.debug("Connection " + this.getConnectionId() + " has already been released");
				}
			}
			else  {
				/**
				 * If the delegate is stopped, actually close the connection, but still release
				 * it to the pool, it will be discarded/renewed the next time it is retrieved.
				 */
				if (!isRunning()) {
					if (logger.isDebugEnabled()){
						logger.debug("Factory not running - closing " + this.getConnectionId());
					}
					super.close();
				}
				pool.releaseItem(this.getTheConnection());
				this.released = true;
			}
		}

		@Override
		public String getConnectionId() {
			return "Cached:" + super.getConnectionId();
		}

		@Override
		public String toString() {
			return this.getConnectionId();
		}

		/**
		 * We have to intercept the message to replace the connectionId header with
		 * ours so the listener can correlate a response with a request. We supply
		 * the actual connectionId in another header for convenience and tracing
		 * purposes.
		 */
		@Override
		public boolean onMessage(Message<?> message) {
			AbstractIntegrationMessageBuilder<?> messageBuilder = CachingClientConnectionFactory.this
					.getMessageBuilderFactory().fromMessage(message)
						.setHeader(IpHeaders.CONNECTION_ID, this.getConnectionId());
			if (message.getHeaders().get(IpHeaders.ACTUAL_CONNECTION_ID) == null) {
				messageBuilder.setHeader(IpHeaders.ACTUAL_CONNECTION_ID,
						message.getHeaders().get(IpHeaders.CONNECTION_ID));
			}
			this.getListener().onMessage(messageBuilder.build());
			close(); // return to pool after response is received
			return true; // true so the single-use connection doesn't close itself
		}

		private void physicallyClose() {
			this.getTheConnection().close();
		}

	}

///////////////// DELEGATE METHODS ///////////////////////

	@Override
	public boolean isRunning() {
		return targetConnectionFactory.isRunning();
	}

	@Override
	public int hashCode() {
		return targetConnectionFactory.hashCode();
	}

	@Override
	public void setComponentName(String componentName) {
		targetConnectionFactory.setComponentName(componentName);
	}

	@Override
	public String getComponentType() {
		return targetConnectionFactory.getComponentType();
	}

	@Override
	public boolean equals(Object obj) {
		return targetConnectionFactory.equals(obj);
	}

	@Override
	public int getSoTimeout() {
		return targetConnectionFactory.getSoTimeout();
	}

	@Override
	public void setSoTimeout(int soTimeout) {
		targetConnectionFactory.setSoTimeout(soTimeout);
	}

	@Override
	public int getSoReceiveBufferSize() {
		return targetConnectionFactory.getSoReceiveBufferSize();
	}

	@Override
	public void setSoReceiveBufferSize(int soReceiveBufferSize) {
		targetConnectionFactory.setSoReceiveBufferSize(soReceiveBufferSize);
	}

	@Override
	public int getSoSendBufferSize() {
		return targetConnectionFactory.getSoSendBufferSize();
	}

	@Override
	public void setSoSendBufferSize(int soSendBufferSize) {
		targetConnectionFactory.setSoSendBufferSize(soSendBufferSize);
	}

	@Override
	public boolean isSoTcpNoDelay() {
		return targetConnectionFactory.isSoTcpNoDelay();
	}

	@Override
	public void setSoTcpNoDelay(boolean soTcpNoDelay) {
		targetConnectionFactory.setSoTcpNoDelay(soTcpNoDelay);
	}

	@Override
	public int getSoLinger() {
		return targetConnectionFactory.getSoLinger();
	}

	@Override
	public void setSoLinger(int soLinger) {
		targetConnectionFactory.setSoLinger(soLinger);
	}

	@Override
	public boolean isSoKeepAlive() {
		return targetConnectionFactory.isSoKeepAlive();
	}

	@Override
	public void setSoKeepAlive(boolean soKeepAlive) {
		targetConnectionFactory.setSoKeepAlive(soKeepAlive);
	}

	@Override
	public int getSoTrafficClass() {
		return targetConnectionFactory.getSoTrafficClass();
	}

	@Override
	public void setSoTrafficClass(int soTrafficClass) {
		targetConnectionFactory.setSoTrafficClass(soTrafficClass);
	}

	@Override
	public String getHost() {
		return targetConnectionFactory.getHost();
	}

	@Override
	public int getPort() {
		return targetConnectionFactory.getPort();
	}

	@Override
	public TcpSender getSender() {
		return targetConnectionFactory.getSender();
	}

	@Override
	public Serializer<?> getSerializer() {
		return targetConnectionFactory.getSerializer();
	}

	@Override
	public Deserializer<?> getDeserializer() {
		return targetConnectionFactory.getDeserializer();
	}

	@Override
	public TcpMessageMapper getMapper() {
		return targetConnectionFactory.getMapper();
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
	 * When we wrap actual connections with CachedConnections,
	 * the connection is given the wrapper as a listener, so it
	 * can enhance the headers in onMessage(); the wrapper then invokes
	 * the real listener supplied here, with the modified message.
	 */
	@Override
	public void registerListener(TcpListener listener) {
		super.registerListener(listener);
		targetConnectionFactory.registerListener(new TcpListener() {
			@Override
			public boolean onMessage(Message<?> message) {
				if (!(message instanceof ErrorMessage)) {
					throw new UnsupportedOperationException("This should never be called");
				}
				return false;
			}
		});
	}

	@Override
	public void registerSender(TcpSender sender) {
		targetConnectionFactory.registerSender(sender);
	}

	@Override
	public void setTaskExecutor(Executor taskExecutor) {
		targetConnectionFactory.setTaskExecutor(taskExecutor);
	}

	@Override
	public void setDeserializer(Deserializer<?> deserializer) {
		targetConnectionFactory.setDeserializer(deserializer);
	}

	@Override
	public void setSerializer(Serializer<?> serializer) {
		targetConnectionFactory.setSerializer(serializer);
	}

	@Override
	public void setMapper(TcpMessageMapper mapper) {
		targetConnectionFactory.setMapper(mapper);
	}

	@Override
	public boolean isSingleUse() {
		return targetConnectionFactory.isSingleUse();
	}

	@Override
	public void setSingleUse(boolean singleUse) {
		targetConnectionFactory.setSingleUse(singleUse);
	}

	@Override
	public void setInterceptorFactoryChain(
			TcpConnectionInterceptorFactoryChain interceptorFactoryChain) {
		targetConnectionFactory
				.setInterceptorFactoryChain(interceptorFactoryChain);
	}

	@Override
	public void setLookupHost(boolean lookupHost) {
		targetConnectionFactory.setLookupHost(lookupHost);
	}

	@Override
	public boolean isLookupHost() {
		return targetConnectionFactory.isLookupHost();
	}


	@Override
	public void forceClose(TcpConnection connection) {
		if (connection instanceof CachedConnection) {
			((CachedConnection) connection).physicallyClose();
		}
		// will be returned to pool but stale, so will be re-established
		super.forceClose(connection);
	}

	@Override
	public void start() {
		this.setActive(true);
		targetConnectionFactory.start();
		super.start();
	}

	@Override
	public synchronized void stop() {
		targetConnectionFactory.stop();
		this.pool.removeAllIdleItems();
	}

	@Override
	public int getPhase() {
		return targetConnectionFactory.getPhase();
	}

	@Override
	public boolean isAutoStartup() {
		return targetConnectionFactory.isAutoStartup();
	}

	@Override
	public void stop(Runnable callback) {
		targetConnectionFactory.stop(callback);
	}

}
