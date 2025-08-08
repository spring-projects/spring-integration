/*
 * Copyright © 2019 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2019-present the original author or authors.
 */

package org.springframework.integration.rsocket;

import org.springframework.messaging.ReactiveMessageHandler;

/**
 * A marker {@link ReactiveMessageHandler} extension interface for Spring Integration
 * inbound endpoints.
 * It is used as mapping predicate in the internal RSocket acceptor of the
 * {@link AbstractRSocketConnector}.
 *
 * @author Artem Bilan
 *
 * @since 5.2
 *
 * @see AbstractRSocketConnector
 * @see org.springframework.integration.rsocket.inbound.RSocketInboundGateway
 */
public interface IntegrationRSocketEndpoint extends ReactiveMessageHandler {

	/**
	 * Obtain path patterns this {@link ReactiveMessageHandler} is going to be mapped onto.
	 * @return the path patterns for mapping.
	 */
	String[] getPath();

	/**
	 * Obtain {@link RSocketInteractionModel}s
	 * this {@link ReactiveMessageHandler} is going to be mapped onto.
	 * Defaults to all the {@link RSocketInteractionModel}s.
	 * @return the interaction models for mapping.
	 * @since 5.2.2
	 */
	default RSocketInteractionModel[] getInteractionModels() {
		return RSocketInteractionModel.values();
	}

}
