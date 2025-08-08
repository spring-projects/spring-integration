/*
 * Copyright © 2023 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2023-present the original author or authors.
 */

package org.springframework.integration.ip.dsl;

import org.springframework.integration.ip.tcp.connection.TcpNioClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNioConnectionSupport;

/**
 * {@link TcpClientConnectionFactorySpec} for {@link TcpNioClientConnectionFactory}s.
 *
 * @author Gary Russell
 * @since 6.0.3
 */
public class TcpNioClientConnectionFactorySpec
		extends TcpClientConnectionFactorySpec<TcpNioClientConnectionFactorySpec, TcpNioClientConnectionFactory> {

	protected TcpNioClientConnectionFactorySpec(String host, int port) {
		super(new TcpNioClientConnectionFactory(host, port));
	}

	/**
	 * True to use direct buffers.
	 * @param usingDirectBuffers true for direct.
	 * @return the spec.
	 * @see TcpNioClientConnectionFactory#setUsingDirectBuffers(boolean)
	 */
	public TcpNioClientConnectionFactorySpec directBuffers(boolean usingDirectBuffers) {
		this.target.setUsingDirectBuffers(usingDirectBuffers);
		return this;
	}

	/**
	 * The {@link TcpNioConnectionSupport} to use.
	 * @param tcpNioSupport the {@link TcpNioConnectionSupport}.
	 * @return the spec.
	 * @see TcpNioClientConnectionFactory#setTcpNioConnectionSupport(TcpNioConnectionSupport)
	 */
	public TcpNioClientConnectionFactorySpec connectionSupport(TcpNioConnectionSupport tcpNioSupport) {
		this.target.setTcpNioConnectionSupport(tcpNioSupport);
		return this;
	}

}
