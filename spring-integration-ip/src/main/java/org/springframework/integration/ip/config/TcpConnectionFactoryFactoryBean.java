/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.ip.config;

import java.util.concurrent.Executor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.Lifecycle;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.DefaultTcpNetConnectionSupport;
import org.springframework.integration.ip.tcp.connection.DefaultTcpNetSSLSocketFactorySupport;
import org.springframework.integration.ip.tcp.connection.DefaultTcpNetSocketFactorySupport;
import org.springframework.integration.ip.tcp.connection.DefaultTcpNioConnectionSupport;
import org.springframework.integration.ip.tcp.connection.DefaultTcpNioSSLConnectionSupport;
import org.springframework.integration.ip.tcp.connection.DefaultTcpSocketSupport;
import org.springframework.integration.ip.tcp.connection.TcpConnectionInterceptorFactoryChain;
import org.springframework.integration.ip.tcp.connection.TcpMessageMapper;
import org.springframework.integration.ip.tcp.connection.TcpNetClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNetConnectionSupport;
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
public class TcpConnectionFactoryFactoryBean extends AbstractFactoryBean<AbstractConnectionFactory>
		implements Lifecycle, BeanNameAware, ApplicationEventPublisherAware, ApplicationContextAware {

	private AbstractConnectionFactory connectionFactory;

	private String type;

	private String host;

	private int port;

	private int soTimeout;

	private int soSendBufferSize;

	private int soReceiveBufferSize;

	private boolean soTcpNoDelay;

	private int soLinger = -1; // don't set by default

	private boolean soKeepAlive;

	private int soTrafficClass = -1; // don't set by default

	private Executor taskExecutor;

	private Deserializer<?> deserializer = new ByteArrayCrLfSerializer();

	private Serializer<?> serializer = new ByteArrayCrLfSerializer();

	private TcpMessageMapper mapper = new TcpMessageMapper();

	private boolean mapperSet;

	private boolean singleUse;

	private int backlog = 5; // NOSONAR magic number

	private TcpConnectionInterceptorFactoryChain interceptorFactoryChain;

	private boolean lookupHost = true;

	private String localAddress;

	private boolean usingNio;

	private boolean usingDirectBuffers;

	private String beanName;

	private boolean applySequence;

	private Long readDelay;

	private TcpSSLContextSupport sslContextSupport;

	private Integer sslHandshakeTimeout;

	private TcpSocketSupport socketSupport = new DefaultTcpSocketSupport();

	private TcpNioConnectionSupport nioConnectionSupport;

	private TcpNetConnectionSupport netConnectionSupport;

	private TcpSocketFactorySupport socketFactorySupport;

	private ApplicationEventPublisher applicationEventPublisher;

	private Integer connectTimeout;

	private ApplicationContext applicationContext;

	public TcpConnectionFactoryFactoryBean() {
	}

	public TcpConnectionFactoryFactoryBean(String type) {
		setType(type);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public Class<?> getObjectType() {
		return this.connectionFactory != null ? this.connectionFactory.getClass() :
				this.type == null ? AbstractConnectionFactory.class :
						isServer() ? AbstractServerConnectionFactory.class :
								isClient() ? AbstractClientConnectionFactory.class :
										AbstractConnectionFactory.class;
	}

	@Override
	protected AbstractConnectionFactory createInstance() {
		BeanFactory beanFactory = getBeanFactory();
		if (!this.mapperSet && beanFactory != null) {
			this.mapper.setBeanFactory(beanFactory);
		}
		if (this.usingNio) {
			if (isServer()) {
				TcpNioServerConnectionFactory factory = new TcpNioServerConnectionFactory(this.port);
				this.setCommonAttributes(factory);
				this.setServerAttributes(factory);
				factory.setUsingDirectBuffers(this.usingDirectBuffers);
				factory.setTcpNioConnectionSupport(this.obtainNioConnectionSupport());
				this.connectionFactory = factory;
			}
			else {
				TcpNioClientConnectionFactory factory = new TcpNioClientConnectionFactory(this.host, this.port);
				this.setCommonAttributes(factory);
				factory.setUsingDirectBuffers(this.usingDirectBuffers);
				factory.setTcpNioConnectionSupport(this.obtainNioConnectionSupport());
				this.connectionFactory = factory;
			}
			if (this.sslHandshakeTimeout != null) {
				this.connectionFactory.setSslHandshakeTimeout(this.sslHandshakeTimeout);
			}
		}
		else {
			if (isServer()) {
				TcpNetServerConnectionFactory factory = new TcpNetServerConnectionFactory(this.port);
				this.setCommonAttributes(factory);
				this.setServerAttributes(factory);
				factory.setTcpSocketFactorySupport(this.obtainSocketFactorySupport());
				factory.setTcpNetConnectionSupport(this.obtainNetConnectionSupport());
				this.connectionFactory = factory;
			}
			else {
				TcpNetClientConnectionFactory factory = new TcpNetClientConnectionFactory(
						this.host, this.port);
				this.setCommonAttributes(factory);
				factory.setTcpSocketFactorySupport(this.obtainSocketFactorySupport());
				factory.setTcpNetConnectionSupport(this.obtainNetConnectionSupport());
				if (this.connectTimeout != null) {
					factory.setConnectTimeout(this.connectTimeout);
				}
				this.connectionFactory = factory;
			}
		}
		if (beanFactory != null) {
			this.connectionFactory.setBeanFactory(beanFactory);
		}
		if (this.applicationContext != null) {
			this.connectionFactory.setApplicationContext(this.applicationContext);
		}
		this.connectionFactory.afterPropertiesSet();
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
		if (this.readDelay != null) {
			factory.setReadDelay(this.readDelay);
		}
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
			return new DefaultTcpNetSSLSocketFactorySupport(this.sslContextSupport);
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
			return new DefaultTcpNioSSLConnectionSupport(this.sslContextSupport);
		}
	}

	private TcpNetConnectionSupport obtainNetConnectionSupport() {
		if (this.netConnectionSupport != null) {
			return this.netConnectionSupport;
		}
		else {
			return new DefaultTcpNetConnectionSupport();
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
	public final void setType(String type) {
		this.type = type;
		Assert.isTrue(isServer() || isClient(), "type must be 'server' or 'client'");
	}

	/**
	 * @param localAddress The local address.
	 * @see AbstractServerConnectionFactory#setLocalAddress(java.lang.String)
	 */
	public void setLocalAddress(String localAddress) {
		Assert.notNull(localAddress, "LocalAddress may not be null");
		this.localAddress = localAddress;
	}

	/**
	 * @param soTimeout The timeout, in milliseconds.
	 * @see AbstractConnectionFactory#setSoTimeout(int)
	 */
	public void setSoTimeout(int soTimeout) {
		this.soTimeout = soTimeout;
	}

	/**
	 * @param soReceiveBufferSize The receive buffer size.
	 * @see AbstractConnectionFactory#setSoReceiveBufferSize(int)
	 */
	public void setSoReceiveBufferSize(int soReceiveBufferSize) {
		this.soReceiveBufferSize = soReceiveBufferSize;
	}

	/**
	 * @param soSendBufferSize The send buffer size.
	 * @see AbstractConnectionFactory#setSoSendBufferSize(int)
	 */
	public void setSoSendBufferSize(int soSendBufferSize) {
		this.soSendBufferSize = soSendBufferSize;
	}

	/**
	 * @param soTcpNoDelay The TCP no delay to set.
	 * @see AbstractConnectionFactory#setSoTcpNoDelay(boolean)
	 */
	public void setSoTcpNoDelay(boolean soTcpNoDelay) {
		this.soTcpNoDelay = soTcpNoDelay;
	}

	/**
	 * @param soLinger The SO Linger to set.
	 * @see AbstractConnectionFactory#setSoLinger(int)
	 */
	public void setSoLinger(int soLinger) {
		this.soLinger = soLinger;
	}

	/**
	 * @param soKeepAlive The SO keepalive to set.
	 * @see AbstractConnectionFactory#setSoKeepAlive(boolean)
	 */
	public void setSoKeepAlive(boolean soKeepAlive) {
		this.soKeepAlive = soKeepAlive;
	}

	/**
	 * @param soTrafficClass The SO traffic class to set.
	 * @see AbstractConnectionFactory#setSoTrafficClass(int)
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
	 * @see TcpNioServerConnectionFactory#setUsingDirectBuffers(boolean)
	 */
	public void setUsingDirectBuffers(boolean usingDirectBuffers) {
		this.usingDirectBuffers = usingDirectBuffers;
	}

	/**
	 * @param taskExecutor The task executor.
	 * @see AbstractConnectionFactory#setTaskExecutor(java.util.concurrent.Executor)
	 */
	public void setTaskExecutor(Executor taskExecutor) {
		Assert.notNull(taskExecutor, "Executor may not be null");
		this.taskExecutor = taskExecutor;
	}

	/**
	 * @param deserializer The deserializer.
	 * @see AbstractConnectionFactory#setDeserializer(org.springframework.core.serializer.Deserializer)
	 */
	public void setDeserializer(Deserializer<?> deserializer) {
		Assert.notNull(deserializer, "Deserializer may not be null");
		this.deserializer = deserializer;
	}

	/**
	 * @param serializer The serializer.
	 * @see AbstractConnectionFactory#setSerializer(org.springframework.core.serializer.Serializer)
	 */
	public void setSerializer(Serializer<?> serializer) {
		Assert.notNull(serializer, "Serializer may not be null");
		this.serializer = serializer;
	}

	/**
	 * @param mapper The mapper.
	 * @see AbstractConnectionFactory#setMapper(TcpMessageMapper)
	 */
	public void setMapper(TcpMessageMapper mapper) {
		Assert.notNull(mapper, "TcpMessageMapper may not be null");
		this.mapper = mapper;
		this.mapperSet = true;
	}

	/**
	 * @param singleUse The singleUse to set.
	 * @see AbstractConnectionFactory#setSingleUse(boolean)
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
	 * @see AbstractConnectionFactory#setInterceptorFactoryChain(TcpConnectionInterceptorFactoryChain)
	 */
	public void setInterceptorFactoryChain(
			TcpConnectionInterceptorFactoryChain interceptorFactoryChain) {
		Assert.notNull(interceptorFactoryChain, "InterceptorFactoryChain may not be null");
		this.interceptorFactoryChain = interceptorFactoryChain;
	}

	/**
	 * @param lookupHost The lookupHost to set.
	 * @see AbstractConnectionFactory#setLookupHost(boolean)
	 */
	public void setLookupHost(boolean lookupHost) {
		this.lookupHost = lookupHost;
	}

	/**
	 *
	 * @see AbstractConnectionFactory#start()
	 */
	@Override
	public void start() {
		this.connectionFactory.start();
	}

	/**
	 *
	 * @see AbstractConnectionFactory#stop()
	 */
	@Override
	public void stop() {
		this.connectionFactory.stop();
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
		Assert.notNull(sslContextSupport, "TcpSSLContextSupport may not be null");
		this.sslContextSupport = sslContextSupport;
	}

	public void setSocketSupport(TcpSocketSupport tcpSocketSupport) {
		Assert.notNull(tcpSocketSupport, "TcpSocketSupport may not be null");
		this.socketSupport = tcpSocketSupport;
	}

	public void setNioConnectionSupport(TcpNioConnectionSupport tcpNioSupport) {
		Assert.notNull(tcpNioSupport, "TcpNioConnectionSupport may not be null");
		this.nioConnectionSupport = tcpNioSupport;
	}

	public void setNetConnectionSupport(TcpNetConnectionSupport tcpNetSupport) {
		Assert.notNull(tcpNetSupport, "TcpNetConnectionSupport may not be null");
		this.netConnectionSupport = tcpNetSupport;
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

	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	/**
	 * Set the SSL handshake timeout (only used with SSL and NIO).
	 * @param sslHandshakeTimeout the timeout.
	 * @since 4.3.6
	 */
	public void setSslHandshakeTimeout(Integer sslHandshakeTimeout) {
		this.sslHandshakeTimeout = sslHandshakeTimeout;
	}

	private boolean isClient() {
		return "client".equals(this.type);
	}

	private boolean isServer() {
		return "server".equals(this.type);
	}

}

