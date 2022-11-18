/*
 * Copyright 2022 the original author or authors.
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

package org.springframework.integration.kafka.aot;

import java.util.stream.Stream;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.integration.kafka.inbound.KafkaInboundGateway;
import org.springframework.integration.kafka.inbound.KafkaMessageDrivenChannelAdapter;
import org.springframework.integration.kafka.inbound.KafkaMessageSource;
import org.springframework.util.ReflectionUtils;

/**
 * {@link RuntimeHintsRegistrar} for Spring Integration Kafka module.
 *
 * @author Artem Bilan
 *
 * @since 6.0
 */
class KafkaRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		ReflectionHints reflectionHints = hints.reflection();

		// Java DSL does not register beans during AOT phase, so @Reflective is not reachable from Pausable
		Stream.of("pause", "resume", "isPaused")
				.flatMap((name) ->
						Stream.of(KafkaMessageDrivenChannelAdapter.class,
										KafkaInboundGateway.class,
										KafkaMessageSource.class)
								.filter((type) -> reflectionHints.getTypeHint(type) == null)
								.flatMap((type) -> Stream.ofNullable(ReflectionUtils.findMethod(type, name))))
				.forEach(method -> reflectionHints.registerMethod(method, ExecutableMode.INVOKE));
	}

}
