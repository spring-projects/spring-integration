/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.jpa.outbound;

import org.springframework.integration.config.AbstractSimpleMessageHandlerFactoryBean;
import org.springframework.integration.jpa.core.JpaExecutor;
import org.springframework.integration.jpa.support.OutboundGatewayType;

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

	private long replyTimeout;

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
	 * Specifies the time the gateway will wait to send the result to the reply channel.
	 * Only applies when the reply channel itself might block the send
	 * (for example a bounded QueueChannel that is currently full).
	 * By default the Gateway will wait indefinitely.
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
		jpaOutboundGateway.setSendTimeout(this.replyTimeout);
		jpaOutboundGateway.setRequiresReply(this.requiresReply);
		return jpaOutboundGateway;
	}

}
