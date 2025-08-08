/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.ip.dsl;

import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;

/**
 * An {@link AbstractConnectionFactorySpec} for {@link AbstractServerConnectionFactory}s.
 *
 * @param <S> the target {@link TcpServerConnectionFactorySpec} implementation type.
 * @param <C> the target {@link AbstractServerConnectionFactory} implementation type.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0
 *
 */
public abstract class TcpServerConnectionFactorySpec
		<S extends TcpServerConnectionFactorySpec<S, C>, C extends AbstractServerConnectionFactory>
		extends AbstractConnectionFactorySpec<S, C> {

	/**
	 * Create an instance.
	 * @param cf the connection factory.
	 * @since 6.0.3
	 */
	protected TcpServerConnectionFactorySpec(C cf) {
		super(cf);
	}

	/**
	 * @param localAddress the local address.
	 * @return the spec.
	 * @see AbstractServerConnectionFactory#setLocalAddress(String)
	 */
	public S localAddress(String localAddress) {
		this.target.setLocalAddress(localAddress);
		return _this();
	}

	/**
	 * @param backlog the backlog.
	 * @return the spec.
	 * @see AbstractServerConnectionFactory#setBacklog(int)
	 */
	public S backlog(int backlog) {
		this.target.setBacklog(backlog);
		return _this();
	}

}
