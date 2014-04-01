/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.annotation.Header;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.transformer.MessageTransformingHandler;
import org.springframework.integration.transformer.MethodInvokingTransformer;
import org.springframework.messaging.Message;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
public class HeaderAnnotationTransformerTests {

	@Test // INT-1082
	public void headerAnnotationWithPrefixedHeader() {
		Object target = new TestTransformer();
		MethodInvokingTransformer transformer = new MethodInvokingTransformer(target, "appendCorrelationId");
		MessageTransformingHandler handler = new MessageTransformingHandler(transformer);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		QueueChannel outputChannel = new QueueChannel();
		handler.setOutputChannel(outputChannel);
		handler.handleMessage(MessageBuilder.withPayload("test").setCorrelationId("abc").build());
		Message<?> result = outputChannel.receive(0);
		assertNotNull(result);
		assertEquals("testabc", result.getPayload());
		assertEquals("abc", new IntegrationMessageHeaderAccessor(result).getCorrelationId());
	}

	@Test // INT-1082
	public void headerAnnotationWithPrefixedHeaderAndRelativeExpression() {
		Object target = new TestTransformer();
		MethodInvokingTransformer transformer = new MethodInvokingTransformer(target, "evalCorrelationId");
		MessageTransformingHandler handler = new MessageTransformingHandler(transformer);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		QueueChannel outputChannel = new QueueChannel();
		handler.setOutputChannel(outputChannel);
		handler.handleMessage(MessageBuilder.withPayload("test").setCorrelationId("abc").build());
		Message<?> result = outputChannel.receive(0);
		assertNotNull(result);
		assertEquals("ABC", result.getPayload());
		assertEquals("abc", new IntegrationMessageHeaderAccessor(result).getCorrelationId());
	}

	@Test
	public void headerAnnotationWithUnprefixedHeader() {
		Object target = new TestTransformer();
		MethodInvokingTransformer transformer = new MethodInvokingTransformer(target, "appendFoo");
		MessageTransformingHandler handler = new MessageTransformingHandler(transformer);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		QueueChannel outputChannel = new QueueChannel();
		handler.setOutputChannel(outputChannel);
		handler.handleMessage(MessageBuilder.withPayload("test").setHeader("foo", "bar").build());
		Message<?> result = outputChannel.receive(0);
		assertNotNull(result);
		assertEquals("testbar", result.getPayload());
		assertEquals("bar", result.getHeaders().get("foo"));
	}

	@Test
	public void headerAnnotationWithUnprefixedHeaderAndRelativeExpression() {
		Object target = new TestTransformer();
		MethodInvokingTransformer transformer = new MethodInvokingTransformer(target, "evalFoo");
		MessageTransformingHandler handler = new MessageTransformingHandler(transformer);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		QueueChannel outputChannel = new QueueChannel();
		handler.setOutputChannel(outputChannel);
		handler.handleMessage(MessageBuilder.withPayload("test").setHeader("foo", "bar").build());
		Message<?> result = outputChannel.receive(0);
		assertNotNull(result);
		assertEquals("BAR", result.getPayload());
		assertEquals("bar", result.getHeaders().get("foo"));
	}


	public static class TestTransformer {

		public String appendCorrelationId(Object payload,
				@Header(value = IntegrationMessageHeaderAccessor.CORRELATION_ID, required = true) Object correlationId) {
			return payload.toString() + correlationId.toString();
		}

		public String appendFoo(Object payload, @Header(value = "foo") Object header) {
			return payload.toString() + header.toString();
		}

		public String evalCorrelationId(@Header(value = IntegrationMessageHeaderAccessor.CORRELATION_ID + ".toUpperCase()") String result) {
			return result.toString();
		}

		public String evalFoo(@Header(value = "foo.toUpperCase()", required = true) String result) {
			return result.toString();
		}
	}

}
