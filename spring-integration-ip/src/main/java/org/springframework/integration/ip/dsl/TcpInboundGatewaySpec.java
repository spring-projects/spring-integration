/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.ip.dsl;

import java.util.Collections;
import java.util.Map;

import org.springframework.integration.dsl.ComponentsRegistration;
import org.springframework.integration.dsl.MessagingGatewaySpec;
import org.springframework.integration.ip.tcp.TcpInboundGateway;
import org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory;
import org.springframework.scheduling.TaskScheduler;

/**
 * A {@link MessagingGatewaySpec} for {@link TcpInboundGateway}s.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0
 *
 */
public class TcpInboundGatewaySpec extends MessagingGatewaySpec<TcpInboundGatewaySpec, TcpInboundGateway>
		implements ComponentsRegistration {

	protected final AbstractConnectionFactory connectionFactory; // NOSONAR - final

	/**
	 * Construct an instance using an existing spring-managed connection factory.
	 * @param connectionFactoryBean the spring-managed bean.
	 */
	protected TcpInboundGatewaySpec(AbstractConnectionFactory connectionFactoryBean) {
		super(new TcpInboundGateway());
		this.connectionFactory = null;
		this.target.setConnectionFactory(connectionFactoryBean);
	}

	/**
	 * Construct an instance using a connection factory spec.
	 * @param connectionFactorySpec the spec.
	 */
	protected TcpInboundGatewaySpec(AbstractConnectionFactorySpec<?, ?> connectionFactorySpec) {
		super(new TcpInboundGateway());
		this.connectionFactory = connectionFactorySpec.getObject();
		this.target.setConnectionFactory(this.connectionFactory);
	}

	/**
	 * @param isClientMode true to connect in client mode
	 * @return the spec.
	 * @see TcpInboundGateway#setClientMode(boolean)
	 */
	public TcpInboundGatewaySpec clientMode(boolean isClientMode) {
		this.target.setClientMode(isClientMode);
		return _this();
	}

	/**
	 * @param retryInterval the client mode retry interval to set.
	 * @return the spec.
	 * @see TcpInboundGateway#setRetryInterval(long)
	 */
	public TcpInboundGatewaySpec retryInterval(long retryInterval) {
		this.target.setRetryInterval(retryInterval);
		return _this();
	}

	/**
	 * @param taskScheduler the scheduler for connecting in client mode.
	 * @return the spec.
	 * @see TcpInboundGateway#setTaskScheduler(TaskScheduler)
	 */
	public TcpInboundGatewaySpec taskScheduler(TaskScheduler taskScheduler) {
		this.target.setTaskScheduler(taskScheduler);
		return _this();
	}

	@Override
	public Map<Object, String> getComponentsToRegister() {
		return this.connectionFactory != null
				? Collections.singletonMap(this.connectionFactory, this.connectionFactory.getComponentName())
				: Collections.emptyMap();
	}

}
