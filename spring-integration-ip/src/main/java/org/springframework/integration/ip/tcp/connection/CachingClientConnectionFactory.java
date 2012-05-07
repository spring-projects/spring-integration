/*
 * Copyright 2002-2012 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.integration.MessagingException;
import org.springframework.integration.util.SimplePool;

/**
 * @author Gary Russell
 * @since 2.2
 *
 */
public class CachingClientConnectionFactory extends AbstractClientConnectionFactory {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final AbstractClientConnectionFactory targetConnectionFactory;

	private final SimplePool<TcpConnection> pool;

	public CachingClientConnectionFactory(AbstractClientConnectionFactory target, int poolSize) {
		super("", 0);
		// override single-use to true to force "close" after use
		target.setSingleUse(true);
		this.targetConnectionFactory = target;
		pool = new SimplePool<TcpConnection>(poolSize, new SimplePool.PoolItemCallback<TcpConnection>() {

			public TcpConnection createForPool() {
				try {
					return targetConnectionFactory.getConnection();
				} catch (Exception e) {
					throw new MessagingException("Failed to obtain connection", e);
				}
			}

			public boolean isStale(TcpConnection connection) {
				return !connection.isOpen();
			}

			public void removedFromPool(TcpConnection connection) {
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

	public TcpConnection getOrMakeConnection() throws Exception {
		return new CachedConnection(this.pool.getItem());
	}

	private class CachedConnection extends AbstractTcpConnectionInterceptor {

		private volatile boolean released;

		public CachedConnection(TcpConnection connection) {
			super.setTheConnection(connection);
			if (connection instanceof AbstractTcpConnection) {
				((AbstractTcpConnection) connection).registerListener(this);
			}
		}

		@Override
		public synchronized void close() {
			/**
			 * If the delegate is stopped, actually close
			 * the connection.
			 */
			if (!isRunning()) {
				if (logger.isDebugEnabled()){
					logger.debug("Factory not running - closing " + this.getConnectionId());
				}
				pool.releaseItem(null); // just open up a permit
				super.close();
			}
			else if(this.released) {
				if (logger.isDebugEnabled()) {
					logger.debug("Connection " + this.getConnectionId() + " has already been released");
				}
			}
			else  {
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

	}

///////////////// DELEGATE METHODS ///////////////////////

	public void run() {

	}

	public boolean isRunning() {
		return targetConnectionFactory.isRunning();
	}

	@Override
	public void close() {
		targetConnectionFactory.close();
	}

	public int hashCode() {
		return targetConnectionFactory.hashCode();
	}

	public void setComponentName(String componentName) {
		targetConnectionFactory.setComponentName(componentName);
	}

	public String getComponentType() {
		return targetConnectionFactory.getComponentType();
	}

	public boolean equals(Object obj) {
		return targetConnectionFactory.equals(obj);
	}

	public int getSoTimeout() {
		return targetConnectionFactory.getSoTimeout();
	}

	public void setSoTimeout(int soTimeout) {
		targetConnectionFactory.setSoTimeout(soTimeout);
	}

	public int getSoReceiveBufferSize() {
		return targetConnectionFactory.getSoReceiveBufferSize();
	}

	public void setSoReceiveBufferSize(int soReceiveBufferSize) {
		targetConnectionFactory.setSoReceiveBufferSize(soReceiveBufferSize);
	}

	public int getSoSendBufferSize() {
		return targetConnectionFactory.getSoSendBufferSize();
	}

	public void setSoSendBufferSize(int soSendBufferSize) {
		targetConnectionFactory.setSoSendBufferSize(soSendBufferSize);
	}

	public boolean isSoTcpNoDelay() {
		return targetConnectionFactory.isSoTcpNoDelay();
	}

	public void setSoTcpNoDelay(boolean soTcpNoDelay) {
		targetConnectionFactory.setSoTcpNoDelay(soTcpNoDelay);
	}

	public int getSoLinger() {
		return targetConnectionFactory.getSoLinger();
	}

	public void setSoLinger(int soLinger) {
		targetConnectionFactory.setSoLinger(soLinger);
	}

	public boolean isSoKeepAlive() {
		return targetConnectionFactory.isSoKeepAlive();
	}

	public void setSoKeepAlive(boolean soKeepAlive) {
		targetConnectionFactory.setSoKeepAlive(soKeepAlive);
	}

	public int getSoTrafficClass() {
		return targetConnectionFactory.getSoTrafficClass();
	}

	public void setSoTrafficClass(int soTrafficClass) {
		targetConnectionFactory.setSoTrafficClass(soTrafficClass);
	}

	public String getHost() {
		return targetConnectionFactory.getHost();
	}

	public int getPort() {
		return targetConnectionFactory.getPort();
	}

	public TcpListener getListener() {
		return targetConnectionFactory.getListener();
	}

	public TcpSender getSender() {
		return targetConnectionFactory.getSender();
	}

	public Serializer<?> getSerializer() {
		return targetConnectionFactory.getSerializer();
	}

	public Deserializer<?> getDeserializer() {
		return targetConnectionFactory.getDeserializer();
	}

	public TcpMessageMapper getMapper() {
		return targetConnectionFactory.getMapper();
	}

	public void registerListener(TcpListener listener) {
		targetConnectionFactory.registerListener(listener);
	}

	public void registerSender(TcpSender sender) {
		targetConnectionFactory.registerSender(sender);
	}

	public void setTaskExecutor(Executor taskExecutor) {
		targetConnectionFactory.setTaskExecutor(taskExecutor);
	}

	public void setDeserializer(Deserializer<?> deserializer) {
		targetConnectionFactory.setDeserializer(deserializer);
	}

	public void setSerializer(Serializer<?> serializer) {
		targetConnectionFactory.setSerializer(serializer);
	}

	public void setMapper(TcpMessageMapper mapper) {
		targetConnectionFactory.setMapper(mapper);
	}

	public boolean isSingleUse() {
		return targetConnectionFactory.isSingleUse();
	}

	public void setSingleUse(boolean singleUse) {
		targetConnectionFactory.setSingleUse(singleUse);
	}

	public void setInterceptorFactoryChain(
			TcpConnectionInterceptorFactoryChain interceptorFactoryChain) {
		targetConnectionFactory
				.setInterceptorFactoryChain(interceptorFactoryChain);
	}

	public void setLookupHost(boolean lookupHost) {
		targetConnectionFactory.setLookupHost(lookupHost);
	}

	public boolean isLookupHost() {
		return targetConnectionFactory.isLookupHost();
	}

	public void start() {
		this.setActive(true);
		targetConnectionFactory.start();
	}

	public synchronized void stop() {
		targetConnectionFactory.stop();
		this.pool.removeAllIdleItems();
	}

	public int getPhase() {
		return targetConnectionFactory.getPhase();
	}

	public boolean isAutoStartup() {
		return targetConnectionFactory.isAutoStartup();
	}

	public void stop(Runnable callback) {
		targetConnectionFactory.stop(callback);
	}

}
