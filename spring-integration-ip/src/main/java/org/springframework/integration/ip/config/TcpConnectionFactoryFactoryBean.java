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
package org.springframework.integration.ip.config;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpConnectionInterceptorFactoryChain;
import org.springframework.integration.ip.tcp.connection.TcpListener;
import org.springframework.integration.ip.tcp.connection.TcpMessageMapper;
import org.springframework.integration.ip.tcp.connection.TcpNetClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNetServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNioClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNioServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpSender;
import org.springframework.integration.ip.tcp.serializer.ByteArrayCrLfSerializer;

/**
 * Instantiates a TcpN(et|io)(Server|Client)ConnectionFactory, depending
 * on type and using-nio attributes.
 * 
 * @author Gary Russell
 * @since 2.0.5
 *
 */
public class TcpConnectionFactoryFactoryBean extends AbstractFactoryBean<AbstractConnectionFactory> 
		implements SmartLifecycle {

	private AbstractConnectionFactory connectionFactory;
	
	private String type;

	protected String host;
	
	protected int port;
	
	protected TcpListener listener;

	protected TcpSender sender;

	protected int soTimeout;

	private int soSendBufferSize;

	private int soReceiveBufferSize;
	
	private boolean soTcpNoDelay;

	private int soLinger  = -1; // don't set by default

	private boolean soKeepAlive;

	private int soTrafficClass = -1; // don't set by default
	
	private Executor taskExecutor;
	
	protected Deserializer<?> deserializer = new ByteArrayCrLfSerializer();
	
	protected Serializer<?> serializer = new ByteArrayCrLfSerializer();
	
	protected TcpMessageMapper mapper = new TcpMessageMapper();

	protected boolean singleUse;

	protected int poolSize = 5;

	protected volatile boolean active;

	protected TcpConnectionInterceptorFactoryChain interceptorFactoryChain;
	
	private boolean lookupHost = true;
	
	private String localAddress;

	private boolean usingNio;
	
	private boolean usingDirectBuffers;
	
	@Override
	public Class<?> getObjectType() {
		return this.connectionFactory != null ? this.connectionFactory.getClass() 
											  : AbstractConnectionFactory.class;
	}

	@Override
	protected AbstractConnectionFactory createInstance() throws Exception {
		if (this.usingNio) {
			if ("server".equals(this.type)) {
				TcpNioServerConnectionFactory connectionFactory = new TcpNioServerConnectionFactory(this.port);
				this.setCommonAttributes(connectionFactory);
				this.setServerAttributes(connectionFactory);
				connectionFactory.setUsingDirectBuffers(this.usingDirectBuffers);
				this.connectionFactory = connectionFactory;
			} else {
				TcpNioClientConnectionFactory connectionFactory = new TcpNioClientConnectionFactory(
						this.host, this.port);
				this.setCommonAttributes(connectionFactory);
				connectionFactory.setUsingDirectBuffers(this.usingDirectBuffers);
				this.connectionFactory = connectionFactory;
			}
		} else {
			if ("server".equals(this.type)) {
				TcpNetServerConnectionFactory connectionFactory = new TcpNetServerConnectionFactory(this.port);
				this.setCommonAttributes(connectionFactory);
				this.setServerAttributes(connectionFactory);
				this.connectionFactory = connectionFactory;
			} else {
				TcpNetClientConnectionFactory connectionFactory = new TcpNetClientConnectionFactory(
						this.host, this.port);
				this.setCommonAttributes(connectionFactory);
				this.connectionFactory = connectionFactory;
			}
		}
		return this.connectionFactory;
	}

	private void setCommonAttributes(AbstractConnectionFactory factory) {
		factory.setDeserializer(this.deserializer);
		factory.setInterceptorFactoryChain(this.interceptorFactoryChain);
		factory.setLookupHost(this.lookupHost);
		factory.setMapper(this.mapper);
		factory.setPoolSize(this.poolSize);
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
	}

	private void setServerAttributes(AbstractServerConnectionFactory factory) {
		factory.setLocalAddress(this.localAddress);
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
		this.host = host;
	}

	/**
	 * @param server the server to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @param localAddress
	 * @see org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory#setLocalAddress(java.lang.String)
	 */
	public void setLocalAddress(String localAddress) {
		this.localAddress = localAddress;
	}

	/**
	 * @param soTimeout
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#setSoTimeout(int)
	 */
	public void setSoTimeout(int soTimeout) {
		this.soTimeout = soTimeout;
	}

	/**
	 * @param soReceiveBufferSize
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#setSoReceiveBufferSize(int)
	 */
	public void setSoReceiveBufferSize(int soReceiveBufferSize) {
		this.soReceiveBufferSize = soReceiveBufferSize;
	}

	/**
	 * @param soSendBufferSize
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#setSoSendBufferSize(int)
	 */
	public void setSoSendBufferSize(int soSendBufferSize) {
		this.soSendBufferSize = soSendBufferSize;
	}

	/**
	 * @param soTcpNoDelay
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#setSoTcpNoDelay(boolean)
	 */
	public void setSoTcpNoDelay(boolean soTcpNoDelay) {
		this.soTcpNoDelay = soTcpNoDelay;
	}

	/**
	 * @param soLinger
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#setSoLinger(int)
	 */
	public void setSoLinger(int soLinger) {
		this.soLinger = soLinger;
	}

	/**
	 * @param soKeepAlive
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#setSoKeepAlive(boolean)
	 */
	public void setSoKeepAlive(boolean soKeepAlive) {
		this.soKeepAlive = soKeepAlive;
	}

	/**
	 * @param soTrafficClass
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
	 * @param usingDirectBuffers
	 * @see org.springframework.integration.ip.tcp.connection.TcpNioServerConnectionFactory#setUsingDirectBuffers(boolean)
	 */
	public void setUsingDirectBuffers(boolean usingDirectBuffers) {
		this.usingDirectBuffers = usingDirectBuffers;
	}

	/**
	 * @param taskExecutor
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#setTaskExecutor(java.util.concurrent.Executor)
	 */
	public void setTaskExecutor(Executor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * @param deserializer
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#setDeserializer(org.springframework.core.serializer.Deserializer)
	 */
	public void setDeserializer(Deserializer<?> deserializer) {
		this.deserializer = deserializer;
	}

	/**
	 * @param serializer
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#setSerializer(org.springframework.core.serializer.Serializer)
	 */
	public void setSerializer(Serializer<?> serializer) {
		this.serializer = serializer;
	}

	/**
	 * @param mapper
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#setMapper(org.springframework.integration.ip.tcp.connection.TcpMessageMapper)
	 */
	public void setMapper(TcpMessageMapper mapper) {
		this.mapper = mapper; 
	}

	/**
	 * @param singleUse
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#setSingleUse(boolean)
	 */
	public void setSingleUse(boolean singleUse) {
		this.singleUse = singleUse;
	}

	/**
	 * @param poolSize
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#setPoolSize(int)
	 */
	public void setPoolSize(int poolSize) {
		this.poolSize = poolSize;
	}

	/**
	 * @param interceptorFactoryChain
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#setInterceptorFactoryChain(org.springframework.integration.ip.tcp.connection.TcpConnectionInterceptorFactoryChain)
	 */
	public void setInterceptorFactoryChain(
			TcpConnectionInterceptorFactoryChain interceptorFactoryChain) {
		this.interceptorFactoryChain = interceptorFactoryChain;
	}

	/**
	 * @param lookupHost
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#setLookupHost(boolean)
	 */
	public void setLookupHost(boolean lookupHost) {
		this.lookupHost = lookupHost;
	}

	/**
	 * 
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#start()
	 */
	public void start() {
		this.connectionFactory.start();
	}

	/**
	 * 
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#stop()
	 */
	public void stop() {
		this.connectionFactory.stop();
	}

	/**
	 * @return
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#getPhase()
	 */
	public int getPhase() {
		return this.connectionFactory.getPhase();
	}

	/**
	 * @return
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#isAutoStartup()
	 */
	public boolean isAutoStartup() {
		return this.connectionFactory.isAutoStartup();
	}

	/**
	 * @param callback
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#stop(java.lang.Runnable)
	 */
	public void stop(Runnable callback) {
		this.connectionFactory.stop(callback);
	}

	public boolean isRunning() {
		return this.connectionFactory.isRunning();
	}

}

