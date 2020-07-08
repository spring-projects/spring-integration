/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.ip.tcp.connection;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.util.SimplePool;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;

/**
 * Connection factory that caches connections from the underlying target factory. The underlying
 * factory will be reconfigured to have {@code singleUse=true} in order for the connection to be
 * returned to the cache after use. Users should not subsequently set the underlying property to
 * false, or cache starvation will result.
 *
 * @author Gary Russell
 * @since 2.2
 *
 */
public class CachingClientConnectionFactory extends AbstractClientConnectionFactory implements DisposableBean {

	private final AbstractClientConnectionFactory targetConnectionFactory;

	private final SimplePool<TcpConnectionSupport> pool;

	/**
	 * Construct a caching connection factory that delegates to the provided factory, with
	 * the provided pool size.
	 * @param target the target factory.
	 * @param poolSize the number of connections to allow.
	 */
	public CachingClientConnectionFactory(AbstractClientConnectionFactory target, int poolSize) {
		super("", 0);
		// override single-use to true so the target creates multiple connections
		target.setSingleUse(true);
		this.targetConnectionFactory = target;
		class Callback implements SimplePool.PoolItemCallback<TcpConnectionSupport> {

			@Override
			public TcpConnectionSupport createForPool() {
				try {
					return CachingClientConnectionFactory.this.targetConnectionFactory.getConnection();
				}
				catch (Exception e) {
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

		}
		this.pool = new SimplePool<TcpConnectionSupport>(poolSize, new Callback());
	}

	/**
	 * @param connectionWaitTimeout the new timeout.
	 * @see SimplePool#setWaitTimeout(long)
	 */
	public void setConnectionWaitTimeout(int connectionWaitTimeout) {
		this.pool.setWaitTimeout(connectionWaitTimeout);
	}

	/**
	 * @param poolSize the new pool size.
	 * @see SimplePool#setPoolSize(int)
	 */
	public void setPoolSize(int poolSize) {
		this.pool.setPoolSize(poolSize);
	}

	/**
	 * @see SimplePool#getPoolSize()
	 * @return the pool size.
	 */
	public int getPoolSize() {
		return this.pool.getPoolSize();
	}

	/**
	 * @see SimplePool#getIdleCount()
	 * @return the idle count.
	 */
	public int getIdleCount() {
		return this.pool.getIdleCount();
	}

	/**
	 * @see SimplePool#getActiveCount()
	 * @return the active count.
	 */
	public int getActiveCount() {
		return this.pool.getActiveCount();
	}

	/**
	 * @see SimplePool#getAllocatedCount()
	 * @return the allocated count.
	 */
	public int getAllocatedCount() {
		return this.pool.getAllocatedCount();
	}

	@Override
	public TcpConnectionSupport obtainConnection() {
		return new CachedConnection(this.pool.getItem(), getListener());
	}

///////////////// DELEGATE METHODS ///////////////////////

	@Override
	public boolean isRunning() {
		return this.targetConnectionFactory.isRunning();
	}

	@Override
	public int hashCode() {
		return this.targetConnectionFactory.hashCode();
	}

	@Override
	public void setComponentName(String componentName) {
		this.targetConnectionFactory.setComponentName(componentName);
	}

	@Override
	public String getComponentType() {
		return this.targetConnectionFactory.getComponentType();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		CachingClientConnectionFactory that = (CachingClientConnectionFactory) o;

		return this.targetConnectionFactory.equals(that.targetConnectionFactory);

	}

	@Override
	public int getSoTimeout() {
		return this.targetConnectionFactory.getSoTimeout();
	}

	@Override
	public void setSoTimeout(int soTimeout) {
		this.targetConnectionFactory.setSoTimeout(soTimeout);
	}

	@Override
	public int getSoReceiveBufferSize() {
		return this.targetConnectionFactory.getSoReceiveBufferSize();
	}

	@Override
	public void setSoReceiveBufferSize(int soReceiveBufferSize) {
		this.targetConnectionFactory.setSoReceiveBufferSize(soReceiveBufferSize);
	}

	@Override
	public int getSoSendBufferSize() {
		return this.targetConnectionFactory.getSoSendBufferSize();
	}

	@Override
	public void setSoSendBufferSize(int soSendBufferSize) {
		this.targetConnectionFactory.setSoSendBufferSize(soSendBufferSize);
	}

	@Override
	public boolean isSoTcpNoDelay() {
		return this.targetConnectionFactory.isSoTcpNoDelay();
	}

	@Override
	public void setSoTcpNoDelay(boolean soTcpNoDelay) {
		this.targetConnectionFactory.setSoTcpNoDelay(soTcpNoDelay);
	}

	@Override
	public int getSoLinger() {
		return this.targetConnectionFactory.getSoLinger();
	}

	@Override
	public void setSoLinger(int soLinger) {
		this.targetConnectionFactory.setSoLinger(soLinger);
	}

	@Override
	public boolean isSoKeepAlive() {
		return this.targetConnectionFactory.isSoKeepAlive();
	}

	@Override
	public void setSoKeepAlive(boolean soKeepAlive) {
		this.targetConnectionFactory.setSoKeepAlive(soKeepAlive);
	}

	@Override
	public int getSoTrafficClass() {
		return this.targetConnectionFactory.getSoTrafficClass();
	}

	@Override
	public void setSoTrafficClass(int soTrafficClass) {
		this.targetConnectionFactory.setSoTrafficClass(soTrafficClass);
	}

	@Override
	public String getHost() {
		return this.targetConnectionFactory.getHost();
	}

	@Override
	public int getPort() {
		return this.targetConnectionFactory.getPort();
	}

	@Override
	public TcpSender getSender() {
		return this.targetConnectionFactory.getSender();
	}

	@Override
	public Serializer<?> getSerializer() {
		return this.targetConnectionFactory.getSerializer();
	}

	@Override
	public Deserializer<?> getDeserializer() {
		return this.targetConnectionFactory.getDeserializer();
	}

	@Override
	public TcpMessageMapper getMapper() {
		return this.targetConnectionFactory.getMapper();
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
		this.targetConnectionFactory.enableManualListenerRegistration();
	}

	@Override
	public void registerSender(TcpSender sender) {
		this.targetConnectionFactory.registerSender(sender);
	}

	@Override
	public void setTaskExecutor(Executor taskExecutor) {
		this.targetConnectionFactory.setTaskExecutor(taskExecutor);
	}

	@Override
	public void setDeserializer(Deserializer<?> deserializer) {
		this.targetConnectionFactory.setDeserializer(deserializer);
	}

	@Override
	public void setSerializer(Serializer<?> serializer) {
		this.targetConnectionFactory.setSerializer(serializer);
	}

	@Override
	public void setMapper(TcpMessageMapper mapper) {
		this.targetConnectionFactory.setMapper(mapper);
	}

	@Override
	public boolean isSingleUse() {
		return true;
	}

	/**
	 * Ignored on this factory; connections are always cached in the pool. The underlying
	 * connection factory will have its singleUse property coerced to true (causing the
	 * connection to be returned). Setting it to false on the underlying factory after initialization
	 * will cause cache starvation.
	 * @param singleUse the singleUse.
	 */
	@Override
	public void setSingleUse(boolean singleUse) {
		if (!singleUse && logger.isDebugEnabled()) {
			logger.debug("singleUse=false is not supported; cached connections are never closed");
		}
	}

	@Override
	public void setInterceptorFactoryChain(TcpConnectionInterceptorFactoryChain interceptorFactoryChain) {
		this.targetConnectionFactory.setInterceptorFactoryChain(interceptorFactoryChain);
	}

	@Override
	public void setLookupHost(boolean lookupHost) {
		this.targetConnectionFactory.setLookupHost(lookupHost);
	}

	@Override
	public boolean isLookupHost() {
		return this.targetConnectionFactory.isLookupHost();
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
	public void enableManualListenerRegistration() {
		super.enableManualListenerRegistration();
		this.targetConnectionFactory.enableManualListenerRegistration();
	}

	@Override
	public void start() {
		setActive(true);
		this.targetConnectionFactory.start();
		super.start();
	}

	@Override
	public synchronized void stop() {
		this.targetConnectionFactory.stop();
		this.pool.removeAllIdleItems();
	}

	@Override
	public void destroy() throws Exception {
		this.pool.close();
	}

	private final class CachedConnection extends TcpConnectionInterceptorSupport {

		private final AtomicBoolean released = new AtomicBoolean();

		CachedConnection(TcpConnectionSupport connection,
				@Nullable TcpListener tcpListener) {  // NOSONAR false positive, connection not marked @Nullable

			super.setTheConnection(connection);
			super.registerListener(tcpListener);
		}

		@Override
		public void close() {
			if (!this.released.compareAndSet(false, true)) {
				if (logger.isDebugEnabled()) {
					logger.debug("Connection " + getConnectionId() + " has already been released");
				}
			}
			else {
				/*
				 * If the delegate is stopped, actually close the connection, but still release
				 * it to the pool, it will be discarded/renewed the next time it is retrieved.
				 */
				if (!isRunning()) {
					if (logger.isDebugEnabled()) {
						logger.debug("Factory not running - closing " + getConnectionId());
					}
					super.close();
				}
				CachingClientConnectionFactory.this.pool.releaseItem(getTheConnection());
			}
		}

		@Override
		public String getConnectionId() {
			return "Cached:" + super.getConnectionId();
		}

		@Override
		public String toString() {
			return getConnectionId();
		}

		/**
		 * We have to intercept the message to replace the connectionId header with
		 * ours so the listener can correlate a response with a request. We supply
		 * the actual connectionId in another header for convenience and tracing
		 * purposes.
		 */
		@Override
		public boolean onMessage(Message<?> message) {
			Message<?> modifiedMessage;
			if (message instanceof ErrorMessage) {
				Map<String, Object> headers = new HashMap<String, Object>(message.getHeaders());
				headers.put(IpHeaders.CONNECTION_ID, getConnectionId());
				if (headers.get(IpHeaders.ACTUAL_CONNECTION_ID) == null) {
					headers.put(IpHeaders.ACTUAL_CONNECTION_ID,
							message.getHeaders().get(IpHeaders.CONNECTION_ID));
				}
				modifiedMessage = new ErrorMessage((Throwable) message.getPayload(), headers);
			}
			else {
				AbstractIntegrationMessageBuilder<?> messageBuilder =
						CachingClientConnectionFactory.this.getMessageBuilderFactory()
								.fromMessage(message)
								.setHeader(IpHeaders.CONNECTION_ID, getConnectionId());
				if (message.getHeaders().get(IpHeaders.ACTUAL_CONNECTION_ID) == null) {
					messageBuilder.setHeader(IpHeaders.ACTUAL_CONNECTION_ID,
							message.getHeaders().get(IpHeaders.CONNECTION_ID));
				}
				modifiedMessage = messageBuilder.build();
			}
			TcpListener listener = getListener();
			if (listener != null) {
				listener.onMessage(modifiedMessage);
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug("Message discarded; no listener: " + message);
				}
			}
			return true;
		}

		private void physicallyClose() {
			getTheConnection().close();
		}

	}

}
