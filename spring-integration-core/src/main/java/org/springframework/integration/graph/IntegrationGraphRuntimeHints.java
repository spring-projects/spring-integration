/*
 * Copyright 2023 the original author or authors.
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
