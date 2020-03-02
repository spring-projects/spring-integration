/*
 * Copyright 2017-2020 the original author or authors.
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

import java.util.List;
import java.util.concurrent.Executor;

import javax.net.ssl.SSLSession;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.expression.Expression;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.util.Assert;

/**
 * A client connection factory that binds a connection to a thread. Close operations
 * are ignored; to physically close a connection and release the thread local, invoke
 * {@link #releaseConnection()}.
 *
 * @author Gary Russell
 * @since 5.0
 *
 */
public class ThreadAffinityClientConnectionFactory extends AbstractClientConnectionFactory {

	private final AbstractClientConnectionFactory connectionFactory;

	/*
	 * Not static because we might have several factories with different delegates.
	 */
	private final ThreadLocal<TcpThreadConnection> connections = new ThreadLocal<>();

	public ThreadAffinityClientConnectionFactory(AbstractClientConnectionFactory connectionFactory) {
		super("", 0);
		Assert.isTrue(connectionFactory.isSingleUse(),
				"ConnectionFactory must be single-use to assign a connection per thread");
		this.connectionFactory = connectionFactory;
	}

	@Override
	public TcpConnectionSupport getConnection() throws InterruptedException {
		TcpThreadConnection connection = this.connections.get();
		if (connection == null || !connection.isOpen()) {
			TcpConnectionSupport delegate = this.connectionFactory.getConnection();
			connection = new TcpThreadConnection(delegate);
			this.connections.set(connection);
		}
		return connection;
	}

	public void releaseConnection() {
		TcpThreadConnection connection = this.connections.get();
		if (connection != null) {
			this.connections.remove();
			connection.connection.close();
		}
	}

	/*
	 * The following are all delegate methods.
	 */

	@Override
	public void enableManualListenerRegistration() {
		this.connectionFactory.enableManualListenerRegistration();
	}

	@Override
	public String getComponentName() {
		return this.connectionFactory.getComponentName();
	}

	@Override
	public void setComponentName(String componentName) {
		this.connectionFactory.setComponentName(componentName);
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.connectionFactory.setApplicationEventPublisher(applicationEventPublisher);
	}

	@Override
	public String getComponentType() {
		return this.connectionFactory.getComponentType();
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.connectionFactory.setBeanFactory(beanFactory);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.connectionFactory.setApplicationContext(applicationContext);
	}

	@Override
	@Nullable
	public ApplicationEventPublisher getApplicationEventPublisher() {
		return this.connectionFactory.getApplicationEventPublisher();
	}

	@Override
	public void setChannelResolver(DestinationResolver<MessageChannel> channelResolver) {
		this.connectionFactory.setChannelResolver(channelResolver);
	}

	@Override
	public Expression getExpression() {
		return this.connectionFactory.getExpression();
	}

	@Override
	public void forceClose(TcpConnection connection) {
		this.connectionFactory.forceClose(connection);
	}

	@Override
	public int getSoTimeout() {
		return this.connectionFactory.getSoTimeout();
	}

	@Override
	public void setSoTimeout(int soTimeout) {
		this.connectionFactory.setSoTimeout(soTimeout);
	}

	@Override
	public int getSoReceiveBufferSize() {
		return this.connectionFactory.getSoReceiveBufferSize();
	}

	@Override
	public void setSoReceiveBufferSize(int soReceiveBufferSize) {
		this.connectionFactory.setSoReceiveBufferSize(soReceiveBufferSize);
	}

	@Override
	public int getSoSendBufferSize() {
		return this.connectionFactory.getSoSendBufferSize();
	}

	@Override
	public void setSoSendBufferSize(int soSendBufferSize) {
		this.connectionFactory.setSoSendBufferSize(soSendBufferSize);
	}

	@Override
	public boolean isSoTcpNoDelay() {
		return this.connectionFactory.isSoTcpNoDelay();
	}

	@Override
	public void setSoTcpNoDelay(boolean soTcpNoDelay) {
		this.connectionFactory.setSoTcpNoDelay(soTcpNoDelay);
	}

	@Override
	public int getSoLinger() {
		return this.connectionFactory.getSoLinger();
	}

	@Override
	public void setSoLinger(int soLinger) {
		this.connectionFactory.setSoLinger(soLinger);
	}

	@Override
	public boolean isSoKeepAlive() {
		return this.connectionFactory.isSoKeepAlive();
	}

	@Override
	public void setSoKeepAlive(boolean soKeepAlive) {
		this.connectionFactory.setSoKeepAlive(soKeepAlive);
	}

	@Override
	public ConversionService getConversionService() {
		return this.connectionFactory.getConversionService();
	}

	@Override
	public int getSoTrafficClass() {
		return this.connectionFactory.getSoTrafficClass();
	}

	@Override
	public void setSoTrafficClass(int soTrafficClass) {
		this.connectionFactory.setSoTrafficClass(soTrafficClass);
	}

	@Override
	public void setHost(String host) {
		this.connectionFactory.setHost(host);
	}

	@Override
	public String getHost() {
		return this.connectionFactory.getHost();
	}

	@Override
	public void setPort(int port) {
		this.connectionFactory.setPort(port);
	}

	@Override
	public String getApplicationContextId() {
		return this.connectionFactory.getApplicationContextId();
	}

	@Override
	public int getPort() {
		return this.connectionFactory.getPort();
	}

	@Override
	public TcpListener getListener() {
		return this.connectionFactory.getListener();
	}

	@Override
	public TcpSender getSender() {
		return this.connectionFactory.getSender();
	}

	@Override
	public Serializer<?> getSerializer() {
		return this.connectionFactory.getSerializer();
	}

	@Override
	public Deserializer<?> getDeserializer() {
		return this.connectionFactory.getDeserializer();
	}

	@Override
	public TcpMessageMapper getMapper() {
		return this.connectionFactory.getMapper();
	}

	@Override
	public void registerListener(TcpListener listener) {
		this.connectionFactory.registerListener(listener);
	}

	@Override
	public void setMessageBuilderFactory(MessageBuilderFactory messageBuilderFactory) {
		this.connectionFactory.setMessageBuilderFactory(messageBuilderFactory);
	}

	@Override
	public void registerSender(TcpSender sender) {
		this.connectionFactory.registerSender(sender);
	}

	@Override
	public void setTaskExecutor(Executor taskExecutor) {
		this.connectionFactory.setTaskExecutor(taskExecutor);
	}

	@Override
	public void setDeserializer(Deserializer<?> deserializer) {
		this.connectionFactory.setDeserializer(deserializer);
	}

	@Override
	public void setSerializer(Serializer<?> serializer) {
		this.connectionFactory.setSerializer(serializer);
	}

	@Override
	public void setMapper(TcpMessageMapper mapper) {
		this.connectionFactory.setMapper(mapper);
	}

	@Override
	public void setInterceptorFactoryChain(TcpConnectionInterceptorFactoryChain interceptorFactoryChain) {
		this.connectionFactory.setInterceptorFactoryChain(interceptorFactoryChain);
	}

	@Override
	public void setLookupHost(boolean lookupHost) {
		this.connectionFactory.setLookupHost(lookupHost);
	}

	@Override
	public boolean isLookupHost() {
		return this.connectionFactory.isLookupHost();
	}

	@Override
	public void setNioHarvestInterval(int nioHarvestInterval) {
		this.connectionFactory.setNioHarvestInterval(nioHarvestInterval);
	}

	@Override
	public void setSslHandshakeTimeout(int sslHandshakeTimeout) {
		this.connectionFactory.setSslHandshakeTimeout(sslHandshakeTimeout);
	}

	@Override
	public void setReadDelay(long readDelay) {
		this.connectionFactory.setReadDelay(readDelay);
	}

	@Override
	public void start() {
		this.connectionFactory.start();
	}

	@Override
	public void stop() {
		this.connectionFactory.stop();
	}

	@Override
	public boolean isRunning() {
		return this.connectionFactory.isRunning();
	}

	@Override
	public void setTcpSocketSupport(TcpSocketSupport tcpSocketSupport) {
		this.connectionFactory.setTcpSocketSupport(tcpSocketSupport);
	}

	@Override
	public List<String> getOpenConnectionIds() {
		return this.connectionFactory.getOpenConnectionIds();
	}

	@Override
	public boolean closeConnection(String connectionId) {
		return false;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ":" + this.connectionFactory.toString();
	}

	/**
	 * Delegates all calls except {@link #close()} to the wrapped connection.
	 */
	private static class TcpThreadConnection extends TcpConnectionSupport {

		private final TcpConnectionSupport connection;

		TcpThreadConnection(TcpConnectionSupport connection) {
			this.connection = connection;
		}

		@Override
		public boolean isOpen() {
			return this.connection.isOpen();
		}

		@Override
		public void send(Message<?> message) {
			this.connection.send(message);
		}

		@Override
		public int hashCode() {
			return this.connection.hashCode();
		}

		@Override
		public Object getPayload() {
			return this.connection.getPayload();
		}

		@Override
		public int getPort() {
			return this.connection.getPort();
		}

		@Override
		public Object getDeserializerStateKey() {
			return this.connection.getDeserializerStateKey();
		}

		@Override
		public SSLSession getSslSession() {
			return this.connection.getSslSession();
		}

		@Override
		public boolean equals(Object obj) {
			return this.connection.equals(obj);
		}

		@Override
		public void close() {
			// empty
		}

		@Override
		public TcpMessageMapper getMapper() {
			return this.connection.getMapper();
		}

		@Override
		public void setMapper(TcpMessageMapper mapper) {
			this.connection.setMapper(mapper);
		}

		@Override
		public Deserializer<?> getDeserializer() {
			return this.connection.getDeserializer();
		}

		@Override
		public void setDeserializer(Deserializer<?> deserializer) {
			this.connection.setDeserializer(deserializer);
		}

		@Override
		public Serializer<?> getSerializer() {
			return this.connection.getSerializer();
		}

		@Override
		public void setSerializer(Serializer<?> serializer) {
			this.connection.setSerializer(serializer);
		}

		@Override
		public void registerListener(TcpListener listener) {
			this.connection.registerListener(listener);
		}

		@Override
		public void enableManualListenerRegistration() {
			this.connection.enableManualListenerRegistration();
		}

		@Override
		public void registerSender(TcpSender sender) {
			this.connection.registerSender(sender);
		}

		@Override
		public TcpListener getListener() {
			return this.connection.getListener();
		}

		@Override
		public TcpSender getSender() {
			return this.connection.getSender();
		}

		@Override
		public boolean isServer() {
			return this.connection.isServer();
		}

		@Override
		public long incrementAndGetConnectionSequence() {
			return this.connection.incrementAndGetConnectionSequence();
		}

		@Override
		public String getHostAddress() {
			return this.connection.getHostAddress();
		}

		@Override
		public String getHostName() {
			return this.connection.getHostName();
		}

		@Override
		public String getConnectionId() {
			return this.connection.getConnectionId();
		}

		@Override
		public String toString() {
			return "TcpThreadConnection:" + this.connection.getConnectionId();
		}

		@Override
		public SocketInfo getSocketInfo() {
			return this.connection.getSocketInfo();
		}

		@Override
		public void publishEvent(TcpConnectionEvent event) {
			this.connection.publishEvent(event);
		}

		@Override
		public void run() {
		}

	}

}
