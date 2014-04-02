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

package org.springframework.integration.transformer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

import java.util.UUID;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
public class HeaderFilterTests {

	@Test
	public void testFilterDirectly() {
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader("x", 1).setHeader("y", 2).setHeader("z", 3)
				.build();
		HeaderFilter filter = new HeaderFilter("x", "z");
		Message<?> result = filter.transform(message);
		assertNotNull(result);
		assertNotNull(result.getHeaders().get("y"));
		assertNull(result.getHeaders().get("x"));
		assertNull(result.getHeaders().get("z"));
	}

	@Test
	public void testFilterWithinHandler() {
		UUID correlationId = UUID.randomUUID();
		QueueChannel replyChannel = new QueueChannel();
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader("x", 1).setHeader("y", 2).setHeader("z", 3)
				.setCorrelationId(correlationId)
				.setReplyChannel(replyChannel)
				.setErrorChannelName("testErrorChannel")
				.build();
		HeaderFilter filter = new HeaderFilter("x", "z");
		MessageTransformingHandler handler = new MessageTransformingHandler(filter);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		handler.handleMessage(message);
		Message<?> result = replyChannel.receive(0);
		assertNotNull(result);
		assertNotNull(result.getHeaders().get("y"));
		assertNull(result.getHeaders().get("x"));
		assertNull(result.getHeaders().get("z"));
		assertEquals("testErrorChannel", result.getHeaders().getErrorChannel());
		assertEquals(replyChannel, result.getHeaders().getReplyChannel());
		assertEquals(correlationId, new IntegrationMessageHeaderAccessor(result).getCorrelationId());
	}

}
