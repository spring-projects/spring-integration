/*
 * Copyright © 2023 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2023-present the original author or authors.
 */

package org.springframework.integration.graph;

import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * The {@link RuntimeHintsRegistrar} implementation for {@link Graph}
 * (and related types) reflection hints registration.
 *
 * @author Artem Bilan
 *
 * @since 6.0.3
 */
class IntegrationGraphRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		new BindingReflectionHintsRegistrar()
				.registerReflectionHints(hints.reflection(),
						Graph.class,
						ErrorCapableCompositeMessageHandlerNode.class,
						ErrorCapableDiscardingMessageHandlerNode.class,
						ErrorCapableMessageHandlerNode.class,
						ErrorCapableRoutingNode.class,
						MessageGatewayNode.class,
						MessageProducerNode.class,
						MessageSourceNode.class,
						PollableChannelNode.class);
	}

}
