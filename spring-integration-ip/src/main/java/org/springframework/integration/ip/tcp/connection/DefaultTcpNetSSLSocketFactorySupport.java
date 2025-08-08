/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ip.tcp.connection;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;

import org.springframework.util.Assert;

/**
 * Implementation of {@link TcpSocketFactorySupport}
 * for SSL sockets {@link javax.net.ssl.SSLServerSocket} and {@link javax.net.ssl.SSLSocket}.
 *
 * @author Gary Russell
 * @since 2.2
 *
 */
public class DefaultTcpNetSSLSocketFactorySupport implements TcpSocketFactorySupport {

	private final SSLContext sslContext;

	public DefaultTcpNetSSLSocketFactorySupport(TcpSSLContextSupport sslContextSupport) {
		Assert.notNull(sslContextSupport, "TcpSSLContextSupport must not be null");
		try {
			this.sslContext = sslContextSupport.getSSLContext();
		}
		catch (GeneralSecurityException | IOException e) {
			throw new IllegalArgumentException("Invalid TcpSSLContextSupport - it failed to provide an SSLContext", e);
		}
		Assert.notNull(this.sslContext, "SSLContext retrieved from context support must not be null");
	}

	public ServerSocketFactory getServerSocketFactory() {
		return this.sslContext.getServerSocketFactory();
	}

	public SocketFactory getSocketFactory() {
		return this.sslContext.getSocketFactory();
	}

}
