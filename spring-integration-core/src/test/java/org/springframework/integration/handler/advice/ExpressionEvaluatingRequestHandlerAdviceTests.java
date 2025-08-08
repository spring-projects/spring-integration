/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.handler.advice;

import java.util.Map;

import org.aopalliance.aop.Advice;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.handler.advice.ExpressionEvaluatingRequestHandlerAdvice.MessageHandlingExpressionEvaluatingAdviceException;
import org.springframework.integration.message.AdviceMessage;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class ExpressionEvaluatingRequestHandlerAdviceTests {

	@Autowired
	@Qualifier("advised.input")
	private MessageChannel in;

	@Autowired
	private EERHAConfig config;

	@Test
	public void test() {
		this.in.send(new GenericMessage<>("good"));
		this.in.send(new GenericMessage<>("junk", Map.of("some_request_header_key", "some_request_header_value")));
		assertThat(config.successful).isInstanceOf(AdviceMessage.class);
		assertThat(config.successful.getPayload()).isEqualTo("good was successful");

		assertThat(config.failed).isInstanceOf(ErrorMessage.class);
		Object evaluationResult = ((MessageHandlingExpressionEvaluatingAdviceException) config.failed.getPayload())
				.getEvaluationResult();
		assertThat((String) evaluationResult).startsWith("junk was bad, with reason:");
		assertThat(config.failed.getHeaders()).containsEntry("some_request_header_key", "some_request_header_value");
	}

	@Configuration
	@EnableIntegration
	public static class EERHAConfig {

		@Bean
		public IntegrationFlow advised() {
			return f -> f
					.<String>handle((payload, headers) -> {
								if (payload.equals("good")) {
									return null;
								}
								else {
									throw new RuntimeException("some failure");
								}
							},
							c -> c.advice(expressionAdvice()));
		}

		@Bean
		public Advice expressionAdvice() {
			ExpressionEvaluatingRequestHandlerAdvice advice = new ExpressionEvaluatingRequestHandlerAdvice();
			advice.setSuccessChannelName("success.input");
			advice.setOnSuccessExpressionString("payload + ' was successful'");
			advice.setFailureChannelName("failure.input");
			advice.setOnFailureExpressionString(
					"payload + ' was bad, with reason: ' + #exception.message");
			advice.setTrapException(true);
			return advice;
		}

		private Message<?> successful;

		@Bean
		public IntegrationFlow success() {
			return f -> f
					.handle(m -> this.successful = m);
		}

		private Message<?> failed;

		@Bean
		public IntegrationFlow failure() {
			return f -> f
					.handle(m -> this.failed = m);
		}

	}

}
