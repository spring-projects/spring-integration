/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.ip.dsl;

import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;

/**
 * An {@link AbstractConnectionFactorySpec} for {@link AbstractClientConnectionFactory}s.
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
public abstract class TcpClientConnectionFactorySpec
		<S extends TcpClientConnectionFactorySpec<S, C>, C extends AbstractClientConnectionFactory>
		extends AbstractConnectionFactorySpec<S, C> {

	/**
	 * Create an instance.
	 * @param cf the connection factory.
	 * @since 6.0.3
	 */
	protected TcpClientConnectionFactorySpec(C cf) {
		super(cf);
	}

	/**
	 * Set the connection timeout in seconds. Defaults to 60.
	 * @param connectTimeout the timeout.
	 * @return the spec.
	 * @since 5.2
	 */
	public S connectTimeout(int connectTimeout) {
		this.target.setConnectTimeout(connectTimeout);
		return _this();
	}

}
