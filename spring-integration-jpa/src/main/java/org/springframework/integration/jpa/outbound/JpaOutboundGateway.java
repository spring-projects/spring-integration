/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.jpa.outbound;

import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.jpa.core.JpaExecutor;
import org.springframework.integration.jpa.support.OutboundGatewayType;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * The Jpa Outbound Gateway will allow you to make outbound operations to either:
 *
 * <ul>
 *   <li>submit (insert, delete) data to a database using JPA</li>
 *   <li>retrieve (select) data from a database</li>
 * </ul>
 *
 * Depending on the selected {@link OutboundGatewayType}, the outbound gateway
 * will use either the {@link JpaExecutor}'s poll method or its
 * executeOutboundJpaOperation method.
 *
 * In order to initialize the adapter, you must provide a {@link JpaExecutor} as
 * constructor.
 *
 * @author Gunnar Hillert
 * @author Amol Nayak
 *
 * @since 2.2
 *
 */
public class JpaOutboundGateway extends AbstractReplyProducingMessageHandler {

	private final JpaExecutor   jpaExecutor;
	private OutboundGatewayType gatewayType = OutboundGatewayType.UPDATING;
	private boolean producesReply = true;	//false for outbound-channel-adapter, true for outbound-gateway

	/**
	 * Constructor taking an {@link JpaExecutor} that wraps all JPA Operations.
	 *
	 * @param jpaExecutor Must not be null
	 *
	 */
	public JpaOutboundGateway(JpaExecutor jpaExecutor) {
		Assert.notNull(jpaExecutor, "jpaExecutor must not be null.");
		this.jpaExecutor = jpaExecutor;

	}

	@Override
	public String getComponentType() {
		return "jpa:outbound-gateway";
	}

	@Override
	protected void doInit() {
		this.jpaExecutor.setBeanFactory(this.getBeanFactory());
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		final Object result;
		if (OutboundGatewayType.RETRIEVING.equals(this.gatewayType)) {
			result = this.jpaExecutor.poll(requestMessage);
		} else if (OutboundGatewayType.UPDATING.equals(this.gatewayType)) {
			result = this.jpaExecutor.executeOutboundJpaOperation(requestMessage);
		} else {
			throw new IllegalArgumentException(String.format("GatewayType  '%s' is not supported.", this.gatewayType));
		}

		if (result == null || !producesReply) {
			return null;
		}

		return this.getMessageBuilderFactory().withPayload(result).copyHeaders(requestMessage.getHeaders()).build();

	}

	/**
	 *
	 * @param gatewayType The gateway type.
	 */
	public void setGatewayType(OutboundGatewayType gatewayType) {
		Assert.notNull(gatewayType, "gatewayType must not be null.");
		this.gatewayType = gatewayType;
	}

	/**
	 * If set to 'false', this component will act as an Outbound Channel Adapter.
	 * If not explicitly set this property will default to 'true'.
	 *
	 * @param producesReply Defaults to 'true'.
	 *
	 */
	public void setProducesReply(boolean producesReply) {
		this.producesReply = producesReply;
	}
}
