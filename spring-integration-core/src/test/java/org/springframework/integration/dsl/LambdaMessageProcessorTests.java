/*
 * Copyright 2016-2021 the original author or authors.
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

package org.springframework.integration.dsl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.Objects;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.config.IntegrationConverter;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.integration.handler.LambdaMessageProcessor;
import org.springframework.integration.transformer.GenericTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;


/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0
 */
@SpringJUnitConfig
public class LambdaMessageProcessorTests {

	@Autowired
	private BeanFactory beanFactory;

	@Test
	@SuppressWarnings("divzero")
	public void testException() {
		assertThatExceptionOfType(ArithmeticException.class)
				.isThrownBy(() -> handle((m, h) -> 1 / 0));
	}

	@Test
	public void testMessageAsArgument() {
		LambdaMessageProcessor lmp = new LambdaMessageProcessor(
				new GenericTransformer<Message<?>, Message<?>>() { // Must not be lambda

					@Override
					public Message<?> transform(Message<?> source) {
						return messageTransformer(source);
					}

				}, null);
		lmp.setBeanFactory(this.beanFactory);
		GenericMessage<String> testMessage = new GenericMessage<>("foo");
		Object result = lmp.processMessage(testMessage);
		assertThat(result).isSameAs(testMessage);
	}

	@Test
	public void testMessageAsArgumentLambda() {
		LambdaMessageProcessor lmp = new LambdaMessageProcessor(
				(GenericTransformer<Message<?>, Message<?>>) this::messageTransformer, null);
		lmp.setBeanFactory(this.beanFactory);
		GenericMessage<String> testMessage = new GenericMessage<>("foo");
		assertThatExceptionOfType(ClassCastException.class)
				.isThrownBy(() -> lmp.processMessage(testMessage));
	}

	@Test
	public void testCustomConverter() {
		LambdaMessageProcessor lmp = new LambdaMessageProcessor(Function.identity(), TestPojo.class);
		lmp.setBeanFactory(this.beanFactory);
		Object result = lmp.processMessage(new GenericMessage<>("foo"));
		assertThat(result).isEqualTo(new TestPojo("foo"));
	}

	private void handle(GenericHandler<?> h) {
		LambdaMessageProcessor lmp = new LambdaMessageProcessor(h, String.class);
		lmp.setBeanFactory(this.beanFactory);

		lmp.processMessage(new GenericMessage<>("foo"));
	}

	private Message<?> messageTransformer(Message<?> message) {
		return message;
	}


	@Configuration
	@EnableIntegration
	public static class TestConfiguration {

		@Bean
		@IntegrationConverter
		public Converter<String, TestPojo> testPojoConverter() {
			return new Converter<String, TestPojo>() { // Cannot be lambda for explicit generic types

				@Override
				public TestPojo convert(String source) {
					return new TestPojo(source);
				}

			};
		}

	}

	private static class TestPojo {

		private final String value;

		TestPojo(String value) {
			this.value = value;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof TestPojo)) {
				return false;
			}
			TestPojo testPojo = (TestPojo) o;
			return Objects.equals(this.value, testPojo.value);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.value);
		}

	}

}
