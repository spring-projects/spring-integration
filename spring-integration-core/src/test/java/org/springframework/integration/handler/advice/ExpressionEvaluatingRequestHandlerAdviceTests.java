/*
 * Copyright 2017-2021 the original author or authors.
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

package org.springframework.integration.handler.advice;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0
 *
 */
@SpringJUnitConfig
public class ExpressionEvaluatingRequestHandlerAdviceTests {

	@Autowired
	@Qualifier("advised.input")
	private MessageChannel in;

	@Autowired
	private EERHAConfig config;

	@Test
	public void test() {
		this.in.send(new GenericMessage<>("good"));
		this.in.send(new GenericMessage<>("junk"));
		assertThat(config.successful).isInstanceOf(AdviceMessage.class);
		assertThat(config.successful.getPayload()).isEqualTo("good was successful");
		assertThat(config.failed).isInstanceOf(ErrorMessage.class);
		Object evaluationResult = ((MessageHandlingExpressionEvaluatingAdviceException) config.failed.getPayload())
				.getEvaluationResult();
		assertThat((String) evaluationResult).startsWith("junk was bad, with reason:");
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
