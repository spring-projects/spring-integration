/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.dsl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.integration.handler.LambdaMessageProcessor;
import org.springframework.integration.support.converter.ConfigurableCompositeMessageConverter;
import org.springframework.integration.transformer.GenericTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.support.GenericMessage;


/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class LambdaMessageProcessorTests {

	@Test
	@SuppressWarnings("divzero")
	public void testException() {
		try {
			handle((m, h) -> 1 / 0);
			fail("Expected exception");
		}
		catch (Exception e) {
			assertThat(e.getCause(), instanceOf(ArithmeticException.class));
		}
	}

	@Test
	public void testMessageAsArgument() {
		LambdaMessageProcessor lmp = new LambdaMessageProcessor(new GenericTransformer<Message<?>, Message<?>>() {

			@Override
			public Message<?> transform(Message<?> source) {
				return messageTransformer(source);
			}

		}, null);
		lmp.setBeanFactory(mock(BeanFactory.class));
		GenericMessage<String> testMessage = new GenericMessage<>("foo");
		Object result = lmp.processMessage(testMessage);
		assertSame(testMessage, result);
	}

	@Test
	public void testMessageAsArgumentLambda() {
		LambdaMessageProcessor lmp = new LambdaMessageProcessor(
				(GenericTransformer<Message<?>, Message<?>>) source -> messageTransformer(source), null);
		lmp.setBeanFactory(mock(BeanFactory.class));
		GenericMessage<String> testMessage = new GenericMessage<>("foo");
		assertThatThrownBy(() -> lmp.processMessage(testMessage)).hasCauseExactlyInstanceOf(ClassCastException.class);
	}

	private void handle(GenericHandler<?> h) {
		LambdaMessageProcessor lmp = new LambdaMessageProcessor(h, String.class);
		lmp.setBeanFactory(getBeanFactory());

		lmp.processMessage(new GenericMessage<>("foo"));
	}

	private Message<?> messageTransformer(Message<?> message) {
		return message;
	}


	private BeanFactory getBeanFactory() {
		BeanFactory mockBeanFactory = mock(BeanFactory.class);
		given(mockBeanFactory.getBean(IntegrationContextUtils.ARGUMENT_RESOLVER_MESSAGE_CONVERTER_BEAN_NAME,
				MessageConverter.class))
				.willReturn(new ConfigurableCompositeMessageConverter());
		return mockBeanFactory;
	}

}
