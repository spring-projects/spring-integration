/*
 * Copyright © 2023 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2023-present the original author or authors.
 */

package org.springframework.integration.ip.dsl;

import org.springframework.integration.ip.tcp.connection.TcpNetConnectionSupport;
import org.springframework.integration.ip.tcp.connection.TcpNetServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpSocketFactorySupport;

/**
 * {@link TcpServerConnectionFactorySpec} for {@link TcpNetServerConnectionFactory}s.
 *
 * @author Gary Russell
 * @since 6.0.3
 */
public class TcpNetServerConnectionFactorySpec
		extends TcpServerConnectionFactorySpec<TcpNetServerConnectionFactorySpec, TcpNetServerConnectionFactory> {

	protected TcpNetServerConnectionFactorySpec(int port) {
		super(new TcpNetServerConnectionFactory(port));
	}

	/**
	 * The {@link TcpNetConnectionSupport} to use to create connection objects.
	 * @param connectionSupport the {@link TcpNetConnectionSupport}.
	 * @return the spec.
	 * @see TcpNetServerConnectionFactory#setTcpNetConnectionSupport(TcpNetConnectionSupport)
	 */
	public TcpNetServerConnectionFactorySpec connectionSupport(TcpNetConnectionSupport connectionSupport) {
		this.target.setTcpNetConnectionSupport(connectionSupport);
		return this;
	}

	/**
	 * Set the {@link TcpSocketFactorySupport} used to create server sockets.
	 * @param tcpSocketFactorySupport the {@link TcpSocketFactorySupport}
	 * @return the spec.
	 * @see TcpNetServerConnectionFactory#setTcpSocketFactorySupport(TcpSocketFactorySupport)
	 */
	public TcpNetServerConnectionFactorySpec socketFactorySupport(TcpSocketFactorySupport tcpSocketFactorySupport) {
		this.target.setTcpSocketFactorySupport(tcpSocketFactorySupport);
		return this;
	}

}
