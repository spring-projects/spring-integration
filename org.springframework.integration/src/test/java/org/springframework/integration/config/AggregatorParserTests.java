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
import org.springframework.integration.aggregator.AggregatorEndpoint;
import org.springframework.integration.aggregator.CompletionStrategy;
import org.springframework.integration.aggregator.CompletionStrategyAdapter;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;
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
		AggregatorEndpoint endpoint =
				(AggregatorEndpoint) context.getBean("aggregatorWithReference");
		TestAggregator aggregatorBean = (TestAggregator) context.getBean("aggregatorBean");
		List<Message<?>> outboundMessages = new ArrayList<Message<?>>();
		outboundMessages.add(createMessage("123", "id1", 3, 1, null));
		outboundMessages.add(createMessage("789", "id1", 3, 3, null));
		outboundMessages.add(createMessage("456", "id1", 3, 2, null));
		for (Message<?> message : outboundMessages) {
			endpoint.send(message);
		}
		Assert.assertEquals("One and only one message must have been aggregated", 1, aggregatorBean
				.getAggregatedMessages().size());
		Message<?> aggregatedMessage = aggregatorBean.getAggregatedMessages().get("id1");
		Assert.assertEquals("The aggreggated message payload is not correct", "123456789", aggregatedMessage
				.getPayload());
	}

	@Test
	public void testPropertyAssignment() throws Exception {
		AggregatorEndpoint endpoint =
					(AggregatorEndpoint) context.getBean("completelyDefinedAggregator");
		TestAggregator testAggregator = (TestAggregator) context.getBean("aggregatorBean");
		CompletionStrategy completionStrategy = (CompletionStrategy) context.getBean("completionStrategy");
		MessageChannel outputChannel = (MessageChannel) context.getBean("outputChannel");
		MessageChannel discardChannel = (MessageChannel) context.getBean("discardChannel");
		DirectFieldAccessor accessor = new DirectFieldAccessor(endpoint);
		Assert.assertEquals("The AggregatorEndpoint is not injected with the appropriate Aggregator instance",
				testAggregator, accessor.getPropertyValue("aggregator"));
		Assert.assertEquals(
				"The AggregatorEndpoint is not injected with the appropriate CompletionStrategy instance",
				completionStrategy, accessor.getPropertyValue("completionStrategy"));
		Assert.assertEquals("The AggregatorEndpoint is not injected with the appropriate output channel",
				outputChannel, accessor.getPropertyValue("outputChannel"));
		Assert.assertEquals("The AggregatorEndpoint is not injected with the appropriate discard channel",
				discardChannel, accessor.getPropertyValue("discardChannel"));
		Assert.assertEquals("The AggregatorEndpoint is not set with the appropriate timeout value",
				86420000l, accessor.getPropertyValue("sendTimeout"));
		Assert.assertEquals(
				"The AggregatorEndpoint is not configured with the appropriate 'send partial results on timeout' flag",
				true, accessor.getPropertyValue("sendPartialResultOnTimeout"));
		Assert.assertEquals("The AggregatorEndpoint is not configured with the appropriate reaper interval",
				135l, accessor.getPropertyValue("reaperInterval"));
		Assert.assertEquals(
				"The AggregatorEndpoint is not configured with the appropriate tracked correlationId capacity",
				99, accessor.getPropertyValue("trackedCorrelationIdCapacity"));
		Assert.assertEquals("The AggregatorEndpoint is not configured with the appropriate timeout",
				42l, accessor.getPropertyValue("timeout"));
	}

	@Test
	public void testSimpleJavaBeanAggregator() {
		List<Message<?>> outboundMessages = new ArrayList<Message<?>>();
		AggregatorEndpoint addingAggregator =
				(AggregatorEndpoint) context.getBean("aggregatorWithReferenceAndMethod");
		outboundMessages.add(createMessage(1l, "id1", 3, 1, null));
		outboundMessages.add(createMessage(2l, "id1", 3, 3, null));
		outboundMessages.add(createMessage(3l, "id1", 3, 2, null));
		for (Message<?> message : outboundMessages) {
			addingAggregator.send(message);
		}
		PollableChannel outputChannel = (PollableChannel) context.getBean("outputChannel");
		Message<?> response = outputChannel.receive();
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
		AggregatorEndpoint aggregatorWithPojoCompletionStrategy =
				(AggregatorEndpoint) context.getBean("aggregatorWithPojoCompletionStrategy");
		CompletionStrategy completionStrategy = (CompletionStrategy)
				new DirectFieldAccessor(aggregatorWithPojoCompletionStrategy).getPropertyValue("completionStrategy");
		Assert.assertTrue(completionStrategy instanceof CompletionStrategyAdapter);
		DirectFieldAccessor completionStrategyAccessor = new DirectFieldAccessor(completionStrategy);
		MethodInvoker invoker = (MethodInvoker) completionStrategyAccessor.getPropertyValue("invoker");
		Assert.assertTrue(new DirectFieldAccessor(invoker).getPropertyValue("object") instanceof MaxValueCompletionStrategy);
		Assert.assertTrue(((Method)completionStrategyAccessor.getPropertyValue("method")).getName().equals("checkCompleteness"));
		aggregatorWithPojoCompletionStrategy.send(createMessage(1l, "id1", 0 , 0, null));
		aggregatorWithPojoCompletionStrategy.send(createMessage(2l, "id1", 0 , 0, null));
		aggregatorWithPojoCompletionStrategy.send(createMessage(3l, "id1", 0 , 0, null));
		PollableChannel outputChannel = (PollableChannel) context.getBean("outputChannel");
		Message<?> reply = outputChannel.receive(0);
		Assert.assertNull(reply);
		aggregatorWithPojoCompletionStrategy.send(createMessage(5l, "id1", 0 , 0, null));
		reply = outputChannel.receive(0);
		Assert.assertNotNull(reply);		
		Assert.assertEquals(11l, reply.getPayload());
	}

	@Test(expected=BeanCreationException.class)
	public void testAggregatorWithInvalidCompletionStrategyMethod() {
		context = new ClassPathXmlApplicationContext("invalidCompletionStrategyMethod.xml", this.getClass());		
	}


	private static <T> Message<T> createMessage(T payload, Object correlationId, int sequenceSize, int sequenceNumber,
			MessageChannel outputChannel) {
		return MessageBuilder.fromPayload(payload)
				.setCorrelationId(correlationId)
				.setSequenceSize(sequenceSize)
				.setSequenceNumber(sequenceNumber)
				.setReturnAddress(outputChannel).build();
	}

}
