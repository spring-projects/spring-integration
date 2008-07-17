/*
 * Copyright 2002-2008 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.endpoint.HandlerEndpoint;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.router.AggregatingMessageHandler;
import org.springframework.integration.router.CompletionStrategy;
import org.springframework.integration.router.CompletionStrategyAdapter;
import org.springframework.integration.util.MethodInvoker;

/**
 * @author Marius Bogoevici
 * @author Mark Fisher
 */
public class AggregatorParserTests {

	private ApplicationContext context;


	@Before
	public void setUp() {
		this.context = new ClassPathXmlApplicationContext("aggregatorParserTests.xml", this.getClass());
	}

	@Test
	public void testAggregation() {
		HandlerEndpoint endpoint = (HandlerEndpoint) context.getBean("aggregatorWithReference");
		AggregatingMessageHandler aggregatingHandler = (AggregatingMessageHandler) endpoint.getHandler();
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
		HandlerEndpoint endpoint = (HandlerEndpoint) context.getBean("completelyDefinedAggregator");
		AggregatingMessageHandler completeAggregatingMessageHandler =
				(AggregatingMessageHandler) endpoint.getHandler(); 
		TestAggregator testAggregator = (TestAggregator) context.getBean("aggregatorBean");
		CompletionStrategy completionStrategy = (CompletionStrategy) context.getBean("completionStrategy");
		MessageChannel defaultReplyChannel = (MessageChannel) context.getBean("replyChannel");
		MessageChannel discardChannel = (MessageChannel) context.getBean("discardChannel");
		DirectFieldAccessor messageHandlerFieldAccessor = new DirectFieldAccessor(completeAggregatingMessageHandler);
		Assert.assertEquals("The AggregatingMessageHandler is not injected with the appropriate Aggregator instance",
				testAggregator, messageHandlerFieldAccessor.getPropertyValue("aggregator"));
		Assert.assertEquals(
				"The AggregatingMessageHandler is not injected with the appropriate CompletionStrategy instance",
				completionStrategy, messageHandlerFieldAccessor.getPropertyValue("completionStrategy"));
		Assert.assertEquals("The AggregatingMessageHandler is not injected with the appropriate default reply channel",
				defaultReplyChannel, messageHandlerFieldAccessor.getPropertyValue("defaultReplyChannel"));
		Assert.assertEquals("The AggregatingMessageHandler is not injected with the appropriate discard channel",
				discardChannel, messageHandlerFieldAccessor.getPropertyValue("discardChannel"));
		Assert.assertEquals("The AggregatingMessageHandler is not set with the appropriate timeout value", 86420000l,
				messageHandlerFieldAccessor.getPropertyValue("sendTimeout"));
		Assert.assertEquals(
						"The AggregatingMessageHandler is not configured with the appropriate 'send partial results on timeout' flag",
						true, messageHandlerFieldAccessor.getPropertyValue("sendPartialResultOnTimeout"));
		Assert.assertEquals("The AggregatingMessageHandler is not configured with the appropriate reaper interval",
				135l, messageHandlerFieldAccessor.getPropertyValue("reaperInterval"));
		Assert.assertEquals(
				"The AggregatingMessageHandler is not configured with the appropriate tracked correlationId capacity",
				99, messageHandlerFieldAccessor.getPropertyValue("trackedCorrelationIdCapacity"));
		Assert.assertEquals("The AggregatingMessageHandler is not configured with the appropriate timeout",
				42l, messageHandlerFieldAccessor.getPropertyValue("timeout"));
	}

	@Test
	public void testSimpleJavaBeanAggregator() {
		List<Message<?>> outboundMessages = new ArrayList<Message<?>>();
		HandlerEndpoint endpoint = (HandlerEndpoint) context.getBean("aggregatorWithReferenceAndMethod");
		AggregatingMessageHandler addingAggregator = (AggregatingMessageHandler) endpoint.getHandler();
		outboundMessages.add(createMessage(1l, "id1", 3, 1, null));
		outboundMessages.add(createMessage(2l, "id1", 3, 3, null));
		outboundMessages.add(createMessage(3l, "id1", 3, 2, null));
		for (Message<?> message : outboundMessages) {
			addingAggregator.handle(message);
		}
		MessageChannel replyChannel = (MessageChannel) context.getBean("replyChannel");
		Message<?> response = replyChannel.receive();
		Assert.assertEquals(6l, response.getPayload());
	}

	@Test(expected=BeanCreationException.class)
	public void testMissingMethodOnAggregator() {
		context = new ClassPathXmlApplicationContext("invalidMethodNameAggregator.xml", this.getClass());		
	}

	@Test(expected=BeanCreationException.class)
	public void testDuplicateCompletionStrategyDefinition() {
		context = new ClassPathXmlApplicationContext(
				"completionStrategyMethodWithMissingReference.xml", this.getClass());		
	}

	@Test
	public void testAggregatorWithPojoCompletionStrategy(){
		HandlerEndpoint endpoint = (HandlerEndpoint) context.getBean("aggregatorWithPojoCompletionStrategy");
		AggregatingMessageHandler aggregatorWithPojoCompletionStrategy = (AggregatingMessageHandler) endpoint.getHandler();
		CompletionStrategy completionStrategy = (CompletionStrategy)
				new DirectFieldAccessor(aggregatorWithPojoCompletionStrategy).getPropertyValue("completionStrategy");
		Assert.assertTrue(completionStrategy instanceof CompletionStrategyAdapter);
		DirectFieldAccessor completionStrategyAccessor = new DirectFieldAccessor(completionStrategy);
		MethodInvoker invoker = (MethodInvoker) completionStrategyAccessor.getPropertyValue("invoker");
		Assert.assertTrue(new DirectFieldAccessor(invoker).getPropertyValue("object") instanceof MaxValueCompletionStrategy);
		Assert.assertTrue(((Method)completionStrategyAccessor.getPropertyValue("method")).getName().equals("checkCompleteness"));
		aggregatorWithPojoCompletionStrategy.handle(createMessage(1l, "id1", 0 , 0, null));
		aggregatorWithPojoCompletionStrategy.handle(createMessage(2l, "id1", 0 , 0, null));
		aggregatorWithPojoCompletionStrategy.handle(createMessage(3l, "id1", 0 , 0, null));
		MessageChannel replyChannel = (MessageChannel) context.getBean("replyChannel");
		Message<?> reply = replyChannel.receive(0);
		Assert.assertNull(reply);
		aggregatorWithPojoCompletionStrategy.handle(createMessage(5l, "id1", 0 , 0, null));
		reply = replyChannel.receive(0);
		Assert.assertNotNull(reply);		
		Assert.assertEquals(11l, reply.getPayload());
	}

	@Test(expected=BeanCreationException.class)
	public void testAggregatorWithInvalidCompletionStrategyMethod() {
		context = new ClassPathXmlApplicationContext("invalidCompletionStrategyMethod.xml", this.getClass());		
	}


	private static <T> Message<T> createMessage(T payload, Object correlationId, int sequenceSize, int sequenceNumber,
			MessageChannel replyChannel) {
		return MessageBuilder.fromPayload(payload)
				.setCorrelationId(correlationId)
				.setSequenceSize(sequenceSize)
				.setSequenceNumber(sequenceNumber)
				.setReturnAddress(replyChannel).build();
	}

}
