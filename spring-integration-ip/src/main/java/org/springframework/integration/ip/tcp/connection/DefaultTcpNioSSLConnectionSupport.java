/*
 * Copyright 2002-2016 the original author or authors.
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

import java.nio.channels.SocketChannel;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.util.Assert;

/**
 * Implementation of {@link TcpNioConnectionSupport} for SSL
 * NIO connections.
 * @author Gary Russell
 * @since 2.2
 *
 */
public class DefaultTcpNioSSLConnectionSupport implements TcpNioConnectionSupport, InitializingBean {

	private volatile SSLContext sslContext;

	private final TcpSSLContextSupport sslContextSupport;

	public DefaultTcpNioSSLConnectionSupport(TcpSSLContextSupport sslContextSupport) {
		Assert.notNull(sslContextSupport, "TcpSSLContextSupport must not be null");
		this.sslContextSupport = sslContextSupport;
	}

	/**
	 * Creates a {@link TcpNioSSLConnection}.
	 */
	public TcpNioConnection createNewConnection(SocketChannel socketChannel, boolean server, boolean lookupHost,
			ApplicationEventPublisher applicationEventPublisher, String connectionFactoryName) throws Exception {
		SSLEngine sslEngine = this.sslContext.createSSLEngine();
		TcpNioSSLConnection tcpNioSSLConnection = new TcpNioSSLConnection(socketChannel, server, lookupHost,
				applicationEventPublisher, connectionFactoryName, sslEngine);
		tcpNioSSLConnection.init();
		return tcpNioSSLConnection;
	}

	public void afterPropertiesSet() throws Exception {
		this.sslContext = this.sslContextSupport.getSSLContext();
		Assert.notNull(this.sslContext, "SSLContext must not be null");
	}

}
