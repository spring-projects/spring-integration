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

package org.springframework.integration.ip.config;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.DefaultTcpNetSSLSocketFactorySupport;
import org.springframework.integration.ip.tcp.connection.DefaultTcpNetSocketFactorySupport;
import org.springframework.integration.ip.tcp.connection.DefaultTcpNioConnectionSupport;
import org.springframework.integration.ip.tcp.connection.DefaultTcpNioSSLConnectionSupport;
import org.springframework.integration.ip.tcp.connection.DefaultTcpSocketSupport;
import org.springframework.integration.ip.tcp.connection.TcpConnectionInterceptorFactoryChain;
import org.springframework.integration.ip.tcp.connection.TcpMessageMapper;
import org.springframework.integration.ip.tcp.connection.TcpNetClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNetServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNioClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNioConnectionSupport;
import org.springframework.integration.ip.tcp.connection.TcpNioServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpSSLContextSupport;
import org.springframework.integration.ip.tcp.connection.TcpSocketFactorySupport;
import org.springframework.integration.ip.tcp.connection.TcpSocketSupport;
import org.springframework.integration.ip.tcp.serializer.ByteArrayCrLfSerializer;
import org.springframework.util.Assert;

/**
 * Instantiates a TcpN(et|io)(Server|Client)ConnectionFactory, depending
 * on type and using-nio attributes.
 *
 * @author Gary Russell
 * @since 2.0.5
 */
public class TcpConnectionFactoryFactoryBean extends AbstractFactoryBean<AbstractConnectionFactory> implements SmartLifecycle, BeanNameAware,
		BeanFactoryAware, ApplicationEventPublisherAware {

	private volatile AbstractConnectionFactory connectionFactory;

	private volatile String type;

	private volatile String host;

	private volatile int port;

	private volatile int soTimeout;

	private volatile int soSendBufferSize;

	private volatile int soReceiveBufferSize;

	private volatile boolean soTcpNoDelay;

	private volatile int soLinger  = -1; // don't set by default

	private volatile boolean soKeepAlive;

	private volatile int soTrafficClass = -1; // don't set by default

	private volatile Executor taskExecutor;

	private volatile Deserializer<?> deserializer = new ByteArrayCrLfSerializer();

	private volatile Serializer<?> serializer = new ByteArrayCrLfSerializer();

	private volatile TcpMessageMapper mapper = new TcpMessageMapper();

	private volatile boolean mapperSet;

	private volatile boolean singleUse;

	private volatile int backlog = 5;

	private volatile TcpConnectionInterceptorFactoryChain interceptorFactoryChain;

	private volatile boolean lookupHost = true;

	private volatile String localAddress;

	private volatile boolean usingNio;

	private volatile boolean usingDirectBuffers;

	private volatile String beanName;

	private volatile boolean applySequence;

	private volatile long readDelay;

	private volatile TcpSSLContextSupport sslContextSupport;

	private volatile TcpSocketSupport socketSupport = new DefaultTcpSocketSupport();

	private volatile TcpNioConnectionSupport nioConnectionSupport;

	private volatile TcpSocketFactorySupport socketFactorySupport;

	private volatile ApplicationEventPublisher applicationEventPublisher;

	private volatile BeanFactory beanFactory;

	@Override
	public final void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	public Class<?> getObjectType() {
		return this.connectionFactory != null ? this.connectionFactory.getClass()
											  : AbstractConnectionFactory.class;
	}

	@Override
	protected AbstractConnectionFactory createInstance() throws Exception {
		if (!this.mapperSet) {
			mapper.setBeanFactory(this.beanFactory);
		}
		if (this.usingNio) {
			if ("server".equals(this.type)) {
				TcpNioServerConnectionFactory connectionFactory = new TcpNioServerConnectionFactory(this.port);
				this.setCommonAttributes(connectionFactory);
				this.setServerAttributes(connectionFactory);
				connectionFactory.setUsingDirectBuffers(this.usingDirectBuffers);
				connectionFactory.setTcpNioConnectionSupport(this.obtainNioConnectionSupport());
				this.connectionFactory = connectionFactory;
			} else {
				TcpNioClientConnectionFactory connectionFactory = new TcpNioClientConnectionFactory(
						this.host, this.port);
				this.setCommonAttributes(connectionFactory);
				connectionFactory.setUsingDirectBuffers(this.usingDirectBuffers);
				connectionFactory.setTcpNioConnectionSupport(this.obtainNioConnectionSupport());
				this.connectionFactory = connectionFactory;
			}
		}
		else {
			if ("server".equals(this.type)) {
				TcpNetServerConnectionFactory connectionFactory = new TcpNetServerConnectionFactory(this.port);
				this.setCommonAttributes(connectionFactory);
				this.setServerAttributes(connectionFactory);
				connectionFactory.setTcpSocketFactorySupport(this.obtainSocketFactorySupport());
				this.connectionFactory = connectionFactory;
			} else {
				TcpNetClientConnectionFactory connectionFactory = new TcpNetClientConnectionFactory(
						this.host, this.port);
				this.setCommonAttributes(connectionFactory);
				connectionFactory.setTcpSocketFactorySupport(this.obtainSocketFactorySupport());
				this.connectionFactory = connectionFactory;
			}
		}
		return this.connectionFactory;
	}

	private void setCommonAttributes(AbstractConnectionFactory factory) {
		factory.setDeserializer(this.deserializer);
		factory.setInterceptorFactoryChain(this.interceptorFactoryChain);
		factory.setLookupHost(this.lookupHost);
		this.mapper.setApplySequence(this.applySequence);
		factory.setMapper(this.mapper);
		factory.setSerializer(this.serializer);
		factory.setSingleUse(this.singleUse);
		factory.setSoKeepAlive(this.soKeepAlive);
		factory.setSoLinger(this.soLinger);
		factory.setSoReceiveBufferSize(this.soReceiveBufferSize);
		factory.setSoSendBufferSize(this.soSendBufferSize);
		factory.setSoTcpNoDelay(this.soTcpNoDelay);
		factory.setSoTimeout(this.soTimeout);
		factory.setSoTrafficClass(this.soTrafficClass);
		factory.setTaskExecutor(this.taskExecutor);
		factory.setBeanName(this.beanName);
		factory.setTcpSocketSupport(this.socketSupport);
		factory.setApplicationEventPublisher(this.applicationEventPublisher);
		factory.setReadDelay(this.readDelay);
	}

	private void setServerAttributes(AbstractServerConnectionFactory factory) {
		factory.setLocalAddress(this.localAddress);
		factory.setBacklog(this.backlog);
	}

	private TcpSocketFactorySupport obtainSocketFactorySupport() {
		if (this.socketFactorySupport != null) {
			return this.socketFactorySupport;
		}
		if (this.sslContextSupport == null) {
			return new DefaultTcpNetSocketFactorySupport();
		}
		else {
			DefaultTcpNetSSLSocketFactorySupport socketFactorySupport = new DefaultTcpNetSSLSocketFactorySupport(this.sslContextSupport);
			try {
				socketFactorySupport.afterPropertiesSet();
			}
			catch (Exception e) {
				throw new IllegalStateException("Failed to set up TcpSocketFactorySupport", e);
			}
			return socketFactorySupport;
		}
	}

	private TcpNioConnectionSupport obtainNioConnectionSupport() {
		if (this.nioConnectionSupport != null) {
			return this.nioConnectionSupport;
		}
		if (this.sslContextSupport == null) {
			return new DefaultTcpNioConnectionSupport();
		}
		else {
			DefaultTcpNioSSLConnectionSupport connectionSupport = new DefaultTcpNioSSLConnectionSupport(this.sslContextSupport);
			try {
				connectionSupport.afterPropertiesSet();
			}
			catch (Exception e) {
				throw new IllegalStateException("Failed to set up TcpConnectionSupport", e);
			}
			return connectionSupport;
		}

	}

	/**
	 * @param port the port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * @param host the host to set
	 */
	public void setHost(String host) {
		Assert.notNull(host, "Host may not be null");
		this.host = host;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @param localAddress The local addres..
	 * @see org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory#setLocalAddress(java.lang.String)
	 */
	public void setLocalAddress(String localAddress) {
		Assert.notNull(localAddress, "LocalAddress may not be null");
		this.localAddress = localAddress;
	}

	/**
	 * @param soTimeout The timeout.
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#setSoTimeout(int)
	 */
	public void setSoTimeout(int soTimeout) {
		this.soTimeout = soTimeout;
	}

	/**
	 * @param soReceiveBufferSize The receive buffer size.
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#setSoReceiveBufferSize(int)
	 */
	public void setSoReceiveBufferSize(int soReceiveBufferSize) {
		this.soReceiveBufferSize = soReceiveBufferSize;
	}

	/**
	 * @param soSendBufferSize The send buffer size.
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#setSoSendBufferSize(int)
	 */
	public void setSoSendBufferSize(int soSendBufferSize) {
		this.soSendBufferSize = soSendBufferSize;
	}

	/**
	 * @param soTcpNoDelay The TCP no delay to set.
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#setSoTcpNoDelay(boolean)
	 */
	public void setSoTcpNoDelay(boolean soTcpNoDelay) {
		this.soTcpNoDelay = soTcpNoDelay;
	}

	/**
	 * @param soLinger The SO Linger to set.
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#setSoLinger(int)
	 */
	public void setSoLinger(int soLinger) {
		this.soLinger = soLinger;
	}

	/**
	 * @param soKeepAlive The SO keepalive to set.
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#setSoKeepAlive(boolean)
	 */
	public void setSoKeepAlive(boolean soKeepAlive) {
		this.soKeepAlive = soKeepAlive;
	}

	/**
	 * @param soTrafficClass The SO traffic class to set.
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#setSoTrafficClass(int)
	 */
	public void setSoTrafficClass(int soTrafficClass) {
		this.soTrafficClass = soTrafficClass;
	}

	/**
	 * @param usingNio the usingNio to set
	 */
	public void setUsingNio(boolean usingNio) {
		this.usingNio = usingNio;
	}

	/**
	 * @param usingDirectBuffers the usingDirectBuffers to set.
	 * @see org.springframework.integration.ip.tcp.connection.TcpNioServerConnectionFactory#setUsingDirectBuffers(boolean)
	 */
	public void setUsingDirectBuffers(boolean usingDirectBuffers) {
		this.usingDirectBuffers = usingDirectBuffers;
	}

	/**
	 * @param taskExecutor The task executor.
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#setTaskExecutor(java.util.concurrent.Executor)
	 */
	public void setTaskExecutor(Executor taskExecutor) {
		Assert.notNull(taskExecutor, "Executor may not be null");
		this.taskExecutor = taskExecutor;
	}

	/**
	 * @param deserializer The deserializer.
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#setDeserializer(org.springframework.core.serializer.Deserializer)
	 */
	public void setDeserializer(Deserializer<?> deserializer) {
		Assert.notNull(deserializer, "Deserializer may not be null");
		this.deserializer = deserializer;
	}

	/**
	 * @param serializer The serializer.
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#setSerializer(org.springframework.core.serializer.Serializer)
	 */
	public void setSerializer(Serializer<?> serializer) {
		Assert.notNull(serializer, "Serializer may not be null");
		this.serializer = serializer;
	}

	/**
	 * @param mapper The mapper.
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#setMapper(org.springframework.integration.ip.tcp.connection.TcpMessageMapper)
	 */
	public void setMapper(TcpMessageMapper mapper) {
		Assert.notNull(mapper, "TcpMessageMapper may not be null");
		this.mapper = mapper;
		this.mapperSet = true;
	}

	/**
	 * @param singleUse The singleUse to set.
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#setSingleUse(boolean)
	 */
	public void setSingleUse(boolean singleUse) {
		this.singleUse = singleUse;
	}

	/**
	 * @param backlog The backlog.
	 * @see AbstractServerConnectionFactory#setBacklog(int)
	 */
	public void setBacklog(int backlog) {
		this.backlog = backlog;
	}

	/**
	 * @param interceptorFactoryChain The interceptor factory chain.
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#setInterceptorFactoryChain(org.springframework.integration.ip.tcp.connection.TcpConnectionInterceptorFactoryChain)
	 */
	public void setInterceptorFactoryChain(
			TcpConnectionInterceptorFactoryChain interceptorFactoryChain) {
		Assert.notNull(interceptorFactoryChain, "InterceptorFactoryChain may not be null");
		this.interceptorFactoryChain = interceptorFactoryChain;
	}

	/**
	 * @param lookupHost The lookupHost to set.
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#setLookupHost(boolean)
	 */
	public void setLookupHost(boolean lookupHost) {
		this.lookupHost = lookupHost;
	}

	/**
	 *
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#start()
	 */
	@Override
	public void start() {
		this.connectionFactory.start();
	}

	/**
	 *
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#stop()
	 */
	@Override
	public void stop() {
		this.connectionFactory.stop();
	}

	/**
	 * @return phase
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#getPhase()
	 */
	@Override
	public int getPhase() {
		return this.connectionFactory.getPhase();
	}

	/**
	 * @return isAutoStartup
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#isAutoStartup()
	 */
	@Override
	public boolean isAutoStartup() {
		return this.connectionFactory.isAutoStartup();
	}

	/**
	 * @param callback The Runnable to invoke.
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#stop(java.lang.Runnable)
	 */
	@Override
	public void stop(Runnable callback) {
		this.connectionFactory.stop(callback);
	}

	@Override
	public boolean isRunning() {
		return this.connectionFactory.isRunning();
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

	/**
	 * @param applySequence the applySequence to set
	 */
	public void setApplySequence(boolean applySequence) {
		this.applySequence = applySequence;
	}

	public void setReadDelay(long readDelay) {
		this.readDelay = readDelay;
	}

	public void setSslContextSupport(TcpSSLContextSupport sslContextSupport) {
		Assert.notNull(sslContextSupport, "TcpSSLConstextSupport may not be null");
		this.sslContextSupport = sslContextSupport;
	}

	public void setSocketSupport(TcpSocketSupport tcpSocketSupport) {
		Assert.notNull(tcpSocketSupport, "TcpSocketSupport may not be null");
		this.socketSupport = tcpSocketSupport;
	}

	/**
	 * Rare property - not exposed through namespace
	 * @param tcpNioSupport The tcpNioSupport to set.
	 */
	public void setNioConnectionSupport(TcpNioConnectionSupport tcpNioSupport) {
		Assert.notNull(tcpNioSupport, "TcpNioConnectionSupport may not be null");
		this.nioConnectionSupport = tcpNioSupport;
	}

	public void setSocketFactorySupport(
			TcpSocketFactorySupport tcpSocketFactorySupport) {
		Assert.notNull(tcpSocketFactorySupport, "TcpSocketFactorySupport may not be null");
		this.socketFactorySupport = tcpSocketFactorySupport;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

}

