/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.dsl;

import org.reactivestreams.Publisher;

import org.springframework.messaging.Message;

/**
 * @author Artem Bilan
 *
 * @since 5.0
 */
public final class IntegrationFlowBuilder extends IntegrationFlowDefinition<IntegrationFlowBuilder> {

	IntegrationFlowBuilder() {
	}

	@Override
	public StandardIntegrationFlow get() { // NOSONAR - not useless, increases visibility
		return super.get();
	}

	@Override
	public <T> Publisher<Message<T>> toReactivePublisher() { // NOSONAR - not useless, increases visibility
		return super.toReactivePublisher();
	}

	@Override
	public <T> Publisher<Message<T>> toReactivePublisher(boolean autoStartOnSubscribe) { // NOSONAR
		return super.toReactivePublisher(autoStartOnSubscribe);
	}

}
