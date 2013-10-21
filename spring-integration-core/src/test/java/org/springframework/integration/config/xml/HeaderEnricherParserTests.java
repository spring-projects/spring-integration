/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.config.xml;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.transformer.MessageTransformationException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Mark Fisher
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class HeaderEnricherParserTests {

	@Autowired
	private ApplicationContext context;


	@Test // INT-1154
	public void sendTimeoutDefault() {
		Object endpoint = context.getBean("headerEnricherWithDefaults");
		long sendTimeout = TestUtils.getPropertyValue(endpoint, "handler.messagingTemplate.sendTimeout", Long.class).longValue();
		assertEquals(-1L, sendTimeout);
	}

	@Test // INT-1154
	public void sendTimeoutConfigured() {
		Object endpoint = context.getBean("headerEnricherWithSendTimeout");
		long sendTimeout = TestUtils.getPropertyValue(endpoint, "handler.messagingTemplate.sendTimeout", Long.class).longValue();
		assertEquals(1234L, sendTimeout);
	}

	@Test // INT-1167
	public void shouldSkipNullsDefault() {
		Object endpoint = context.getBean("headerEnricherWithDefaults");
		Boolean shouldSkipNulls = TestUtils.getPropertyValue(endpoint, "handler.transformer.shouldSkipNulls", Boolean.class);
		assertEquals(Boolean.TRUE, shouldSkipNulls);
	}

	@Test // INT-1167
	public void shouldSkipNullsFalseConfigured() {
		Object endpoint = context.getBean("headerEnricherWithShouldSkipNullsFalse");
		Boolean shouldSkipNulls = TestUtils.getPropertyValue(endpoint, "handler.transformer.shouldSkipNulls", Boolean.class);
		assertEquals(Boolean.FALSE, shouldSkipNulls);
	}

	@Test // INT-1167
	public void shouldSkipNullsTrueConfigured() {
		Object endpoint = context.getBean("headerEnricherWithShouldSkipNullsTrue");
		Boolean shouldSkipNulls = TestUtils.getPropertyValue(endpoint, "handler.transformer.shouldSkipNulls", Boolean.class);
		assertEquals(Boolean.TRUE, shouldSkipNulls);
	}
	
	@Test(expected=MessageTransformationException.class) 
	public void testStringPriorityHeader() {
		MessageHandler messageHandler = 
				TestUtils.getPropertyValue(context.getBean("headerEnricherWithPriorityAsString"), "handler", MessageHandler.class);
		Message<?> message = new GenericMessage<String>("hello");
		messageHandler.handleMessage(message);
	}
	@Test
	public void testStringPriorityHeaderWithType() {
        MessageHandler messageHandler =
                        TestUtils.getPropertyValue(context.getBean("headerEnricherWithPriorityAsStringAndType"), "handler", MessageHandler.class);
        QueueChannel replyChannel = new QueueChannel();
        Message<?> message = MessageBuilder.withPayload("foo").setReplyChannel(replyChannel).build();
        messageHandler.handleMessage(message);
        Message<?> transformed = replyChannel.receive(1000);
        assertNotNull(transformed);
        Object priority = transformed.getHeaders().get("priority");
        assertNotNull(priority);
        assertTrue(priority instanceof Integer);
	}

}
