/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.amqp.dsl;

import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.integration.dsl.IntegrationComponentSpec;

/**
 * Base class for container specs.
 *
 * @param <S> the current spec extension type
 * @param <C> the listener container type
 *
 * @author Gary Russell
 *
 * @since 6.0
 *
 */
public abstract class MessageListenerContainerSpec<S extends MessageListenerContainerSpec<S, C>,
		C extends MessageListenerContainer>
		extends IntegrationComponentSpec<S, C> {

	/**
	 * Set the queue names.
	 * @param queueNames the queue names.
	 * @return this spec.
	 */
	public S queueName(String... queueNames) {
		this.target.setQueueNames(queueNames);
		return _this();
	}

}
