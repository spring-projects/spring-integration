/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jpa.outbound;

import org.springframework.integration.config.AbstractSimpleMessageHandlerFactoryBean;
import org.springframework.integration.jpa.core.JpaExecutor;
import org.springframework.integration.jpa.support.OutboundGatewayType;
import org.springframework.lang.Nullable;

/**
 * The {@link JpaOutboundGatewayFactoryBean} creates instances of the
 * {@link JpaOutboundGateway}. Optionally this
 * {@link org.springframework.beans.factory.FactoryBean} will add Aop Advices (e.g.
 * {@link org.springframework.transaction.interceptor.TransactionInterceptor} to the
 * {@link JpaOutboundGateway} instance.
 *
 * @author Amol Nayak
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.2
 *
 */
public class JpaOutboundGatewayFactoryBean extends AbstractSimpleMessageHandlerFactoryBean<JpaOutboundGateway> {

	private JpaExecutor jpaExecutor;

	private OutboundGatewayType gatewayType = OutboundGatewayType.UPDATING;

	private boolean producesReply = true;

	@Nullable
	private Long replyTimeout;

	private boolean requiresReply = false;

	public void setJpaExecutor(JpaExecutor jpaExecutor) {
		this.jpaExecutor = jpaExecutor;
	}

	public void setGatewayType(OutboundGatewayType gatewayType) {
		this.gatewayType = gatewayType;
	}

	public void setProducesReply(boolean producesReply) {
		this.producesReply = producesReply;
	}

	/**
	 * Specify the time the gateway will wait to send the result to the reply channel.
	 * Only applies when the reply channel itself might block the 'send' operation
	 * (for example a bounded QueueChannel that is currently full).
	 * @param replyTimeout The timeout in milliseconds
	 */
	public void setReplyTimeout(long replyTimeout) {
		this.replyTimeout = replyTimeout;
	}

	public void setRequiresReply(boolean requiresReply) {
		this.requiresReply = requiresReply;
	}

	@Override
	protected JpaOutboundGateway createHandler() {
		JpaOutboundGateway jpaOutboundGateway = new JpaOutboundGateway(this.jpaExecutor);
		jpaOutboundGateway.setGatewayType(this.gatewayType);
		jpaOutboundGateway.setProducesReply(this.producesReply);
		if (this.replyTimeout != null) {
			jpaOutboundGateway.setSendTimeout(this.replyTimeout);
		}
		jpaOutboundGateway.setRequiresReply(this.requiresReply);
		return jpaOutboundGateway;
	}

}
