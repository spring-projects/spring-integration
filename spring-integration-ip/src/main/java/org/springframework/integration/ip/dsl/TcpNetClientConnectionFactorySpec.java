/*
 * Copyright © 2023 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2023-present the original author or authors.
 */

package org.springframework.integration.ip.dsl;

import org.springframework.integration.ip.tcp.connection.TcpNetClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNetConnectionSupport;
import org.springframework.integration.ip.tcp.connection.TcpSocketFactorySupport;

/**
 * {@link TcpClientConnectionFactorySpec} for {@link TcpNetClientConnectionFactory}s.
 *
 * @author Gary Russell
 * @since 6.0.3
 */
public class TcpNetClientConnectionFactorySpec
		extends TcpClientConnectionFactorySpec<TcpNetClientConnectionFactorySpec, TcpNetClientConnectionFactory> {

	protected TcpNetClientConnectionFactorySpec(String host, int port) {
		super(new TcpNetClientConnectionFactory(host, port));
	}

	/**
	 * The {@link TcpNetConnectionSupport} to use to create connection objects.
	 * @param connectionSupport the {@link TcpNetConnectionSupport}.
	 * @return the spec.
	 * @see TcpNetClientConnectionFactory#setTcpNetConnectionSupport(TcpNetConnectionSupport)
	 */
	public TcpNetClientConnectionFactorySpec connectionSupport(TcpNetConnectionSupport connectionSupport) {
		this.target.setTcpNetConnectionSupport(connectionSupport);
		return this;
	}

	/**
	 * Set the {@link TcpSocketFactorySupport} used to create server sockets.
	 * @param tcpSocketFactorySupport the {@link TcpSocketFactorySupport}
	 * @return the spec.
	 * @see TcpNetClientConnectionFactory#setTcpSocketFactorySupport(TcpSocketFactorySupport)
	 */
	public TcpNetClientConnectionFactorySpec socketFactorySupport(TcpSocketFactorySupport tcpSocketFactorySupport) {
		this.target.setTcpSocketFactorySupport(tcpSocketFactorySupport);
		return this;
	}

}
