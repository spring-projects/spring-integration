/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
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
