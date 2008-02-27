/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.config;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.router.AggregatingMessageHandler;
import org.springframework.integration.router.Aggregator;
import org.springframework.integration.router.CompletionStrategy;
import org.springframework.util.ReflectionUtils;

/**
 * @author Marius Bogoevici
 */
public class AggregatorParserTests {

	private ApplicationContext context;


	@Before
	public void setUp() {
		context = new ClassPathXmlApplicationContext("aggregatorParserTests.xml", this.getClass());
	}

	@Test
	public void testAggregation() {
		AggregatingMessageHandler aggregatingHandler = (AggregatingMessageHandler) context
				.getBean("aggregatorWithReference");
		TestAggregator aggregatorBean = (TestAggregator) context.getBean("aggregatorBean");
		List<Message<?>> outboundMessages = new ArrayList<Message<?>>();
		outboundMessages.add(createMessage("123", "id1", 3, 1, null));
		outboundMessages.add(createMessage("789", "id1", 3, 3, null));
		outboundMessages.add(createMessage("456", "id1", 3, 2, null));
		for (Message<?> message : outboundMessages) {
			aggregatingHandler.handle(message);
		}
		Assert.assertEquals("One and only one message must have been aggregated", 1, aggregatorBean
				.getAggregatedMessages().size());
		Message<?> aggregatedMessage = aggregatorBean.getAggregatedMessages().get("id1");
		Assert.assertEquals("The aggreggated message payload is not correct", "123456789", aggregatedMessage
				.getPayload());
	}

	@Test
	public void testPropertyAssignment() throws Exception {
		AggregatingMessageHandler completeAggregatingMessageHandler = (AggregatingMessageHandler) context
				.getBean("completelyDefinedAggregator");
		TestAggregator testAggregator = (TestAggregator) context.getBean("aggregatorBean");
		CompletionStrategy completionStrategy = (CompletionStrategy) context.getBean("completionStrategy");
		MessageChannel defaultReplyChannel = (MessageChannel) context.getBean("replyChannel");
		MessageChannel discardChannel = (MessageChannel) context.getBean("discardChannel");

		Assert.assertEquals("The AggregatingMessageHandler is not injected with the appropriate Aggregator instance",
				testAggregator, getPropertyValue(completeAggregatingMessageHandler, "aggregator", Aggregator.class));
		Assert.assertEquals(
				"The AggregatingMessageHandler is not injected with the appropriate CompletionStrategy instance",
				completionStrategy, getPropertyValue(completeAggregatingMessageHandler, "completionStrategy",
						CompletionStrategy.class));
		Assert.assertEquals("The AggregatingMessageHandler is not injected with the appropriate default reply channel",
				defaultReplyChannel, getPropertyValue(completeAggregatingMessageHandler, "defaultReplyChannel",
						MessageChannel.class));
		Assert.assertEquals("The AggregatingMessageHandler is not injected with the appropriate discard channel",
				discardChannel, getPropertyValue(completeAggregatingMessageHandler, "discardChannel",
						MessageChannel.class));
		Assert.assertEquals("The AggregatingMessageHandler is not set with the appropriate timeout value", 86420000l,
				getPropertyValue(completeAggregatingMessageHandler, "sendTimeout", long.class));
		Assert.assertEquals(
						"The AggregatingMessageHandler is not configured with the appropriate 'send partial results on timeout' flag",
						true, getPropertyValue(completeAggregatingMessageHandler, "sendPartialResultOnTimeout",
								boolean.class));
		Assert.assertEquals("The AggregatingMessageHandler is not configured with the appropriate reaper interval",
				135l, getPropertyValue(completeAggregatingMessageHandler, "reaperInterval", long.class));
		Assert.assertEquals(
				"The AggregatingMessageHandler is not configured with the appropriate tracked correlationId capacity",
				99, getPropertyValue(completeAggregatingMessageHandler, "trackedCorrelationIdCapacity", int.class));
	}


	private static Message<?> createMessage(String payload, Object correlationId, int sequenceSize, int sequenceNumber,
			MessageChannel replyChannel) {
		StringMessage message = new StringMessage(payload);
		message.getHeader().setCorrelationId(correlationId);
		message.getHeader().setSequenceSize(sequenceSize);
		message.getHeader().setSequenceNumber(sequenceNumber);
		message.getHeader().setReturnAddress(replyChannel);
		return message;
	}

	/**
	 * Reading private fields through reflection, since they don't have setters
	 * @param beanUnderTest
	 * @param fieldName
	 * @return the value of the field
	 * @throws Exception
	 */
	private static Object getPropertyValue(Object beanUnderTest, String fieldName, Class<?> type) throws Exception {
		Field field = ReflectionUtils.findField(beanUnderTest.getClass(), fieldName, type);
		ReflectionUtils.makeAccessible(field);
		return field.get(beanUnderTest);
	}

}
