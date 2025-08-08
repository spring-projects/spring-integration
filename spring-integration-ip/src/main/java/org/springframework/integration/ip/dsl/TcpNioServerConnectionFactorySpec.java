/*
 * Copyright © 2023 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2023-present the original author or authors.
 */

package org.springframework.integration.ip.dsl;

import org.springframework.integration.ip.tcp.connection.TcpNioConnectionSupport;
import org.springframework.integration.ip.tcp.connection.TcpNioServerConnectionFactory;

/**
 * {@link TcpServerConnectionFactorySpec} for {@link TcpNioServerConnectionFactory}s.
 *
 * @author Gary Russell
 * @since 6.0.3
 */
public class TcpNioServerConnectionFactorySpec
		extends TcpServerConnectionFactorySpec<TcpNioServerConnectionFactorySpec, TcpNioServerConnectionFactory> {

	protected TcpNioServerConnectionFactorySpec(int port) {
		super(new TcpNioServerConnectionFactory(port));
	}

	/**
	 * True to use direct buffers.
	 * @param usingDirectBuffers true for direct.
	 * @return the spec.
	 * @see TcpNioServerConnectionFactory#setUsingDirectBuffers(boolean)
	 */
	public TcpNioServerConnectionFactorySpec directBuffers(boolean usingDirectBuffers) {
		this.target.setUsingDirectBuffers(usingDirectBuffers);
		return this;
	}

	/**
	 * The {@link TcpNioConnectionSupport} to use.
	 * @param tcpNioSupport the {@link TcpNioConnectionSupport}.
	 * @return the spec.
	 * @see TcpNioServerConnectionFactory#setTcpNioConnectionSupport(TcpNioConnectionSupport)
	 */
	public TcpNioServerConnectionFactorySpec connectionSupport(TcpNioConnectionSupport tcpNioSupport) {
		this.target.setTcpNioConnectionSupport(tcpNioSupport);
		return this;
	}

}
