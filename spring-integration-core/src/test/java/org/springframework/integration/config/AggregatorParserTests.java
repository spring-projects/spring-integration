/*
 * Copyright 2002-2016 the original author or authors.
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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.aggregator.AggregatingMessageHandler;
import org.springframework.integration.aggregator.CorrelationStrategy;
import org.springframework.integration.aggregator.ExpressionEvaluatingCorrelationStrategy;
import org.springframework.integration.aggregator.ExpressionEvaluatingReleaseStrategy;
import org.springframework.integration.aggregator.MethodInvokingMessageGroupProcessor;
import org.springframework.integration.aggregator.MethodInvokingReleaseStrategy;
import org.springframework.integration.aggregator.ReleaseStrategy;
import org.springframework.integration.aggregator.SimpleMessageGroupProcessor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.support.MessagingMethodInvokerHelper;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Marius Bogoevici
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gunnar Hillert
 * @author Gary Russell
 */
@RunWith(SpringRunner.class)
public class AggregatorParserTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void testAggregation() {
		MessageChannel input = (MessageChannel) context.getBean("aggregatorWithReferenceInput");
		TestAggregatorBean aggregatorBean = (TestAggregatorBean) context.getBean("aggregatorBean");
		List<Message<?>> outboundMessages = new ArrayList<Message<?>>();
		outboundMessages.add(createMessage("123", "id1", 3, 1, null));
		outboundMessages.add(createMessage("789", "id1", 3, 3, null));
		outboundMessages.add(createMessage("456", "id1", 3, 2, null));

		outboundMessages.forEach(input::send);

		assertEquals("One and only one message must have been aggregated", 1,
				aggregatorBean.getAggregatedMessages().size());
		Message<?> aggregatedMessage = aggregatorBean.getAggregatedMessages().get("id1");
		assertEquals("The aggregated message payload is not correct", "123456789", aggregatedMessage.getPayload());
		Object mbf = context.getBean(IntegrationUtils.INTEGRATION_MESSAGE_BUILDER_FACTORY_BEAN_NAME);
		Object handler = context.getBean("aggregatorWithReference.handler");
		assertSame(mbf, TestUtils.getPropertyValue(handler, "outputProcessor.messageBuilderFactory"));
	}

	@Test
	public void testAggregationWithMessageGroupProcessor() {
		QueueChannel output = this.context.getBean("outputChannel", QueueChannel.class);
		output.purge(null);
		MessageChannel input = (MessageChannel) context.getBean("aggregatorWithMGPReferenceInput");
		List<Message<?>> outboundMessages = new ArrayList<Message<?>>();
		outboundMessages.add(createMessage("123", "id1", 3, 1, null));
		outboundMessages.add(createMessage("789", "id1", 3, 3, null));
		outboundMessages.add(createMessage("456", "id1", 3, 2, null));

		outboundMessages.forEach(input::send);

		assertEquals(3, output.getQueueSize());
		output.purge(null);
	}

	@Test
	public void testAggregationWithMessageGroupProcessorAndStrategies() {
		QueueChannel output = this.context.getBean("outputChannel", QueueChannel.class);
		output.purge(null);
		MessageChannel input = (MessageChannel) context.getBean("aggregatorWithCustomMGPReferenceInput");
		List<Message<?>> outboundMessages = new ArrayList<Message<?>>();
		outboundMessages.add(createMessage("123", "id1", 3, 1, null));
		outboundMessages.add(createMessage("789", "id1", 3, 3, null));
		outboundMessages.add(createMessage("456", "id1", 3, 2, null));

		outboundMessages.forEach(input::send);

		assertEquals(3, output.getQueueSize());
		output.purge(null);
	}

	@Test
	public void testAggregationByExpression() {
		MessageChannel input = (MessageChannel) context.getBean("aggregatorWithExpressionsInput");
		SubscribableChannel outputChannel = (SubscribableChannel) context.getBean("aggregatorWithExpressionsOutput");
		final AtomicReference<Message<?>> aggregatedMessage = new AtomicReference<Message<?>>();
		outputChannel.subscribe(aggregatedMessage::set);
		List<Message<?>> outboundMessages = new ArrayList<Message<?>>();
		outboundMessages.add(MessageBuilder.withPayload("123").setHeader("foo", "1").build());
		outboundMessages.add(MessageBuilder.withPayload("456").setHeader("foo", "1").build());
		outboundMessages.add(MessageBuilder.withPayload("789").setHeader("foo", "1").build());

		outboundMessages.forEach(input::send);

		assertEquals("The aggregated message payload is not correct", "[123]", aggregatedMessage.get().getPayload()
				.toString());
		Object mbf = context.getBean(IntegrationUtils.INTEGRATION_MESSAGE_BUILDER_FACTORY_BEAN_NAME);
		Object handler = context.getBean("aggregatorWithExpressions.handler");
		assertSame(mbf, TestUtils.getPropertyValue(handler, "outputProcessor.messageBuilderFactory"));
		assertTrue(TestUtils.getPropertyValue(handler, "expireGroupsUponTimeout", Boolean.class));
	}

	@Test
	public void testPropertyAssignment() throws Exception {
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("completelyDefinedAggregator");
		ReleaseStrategy releaseStrategy = (ReleaseStrategy) context.getBean("releaseStrategy");
		CorrelationStrategy correlationStrategy = (CorrelationStrategy) context.getBean("correlationStrategy");
		MessageChannel outputChannel = (MessageChannel) context.getBean("outputChannel");
		MessageChannel discardChannel = (MessageChannel) context.getBean("discardChannel");
		Object consumer = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertThat(consumer, is(instanceOf(AggregatingMessageHandler.class)));
		DirectFieldAccessor accessor = new DirectFieldAccessor(consumer);
		Object handlerMethods = new DirectFieldAccessor(new DirectFieldAccessor(new DirectFieldAccessor(accessor
				.getPropertyValue("outputProcessor")).getPropertyValue("processor")).getPropertyValue("delegate"))
				.getPropertyValue("handlerMethods");
		assertNull(handlerMethods);
		Object handlerMethod = new DirectFieldAccessor(new DirectFieldAccessor(new DirectFieldAccessor(accessor
				.getPropertyValue("outputProcessor")).getPropertyValue("processor")).getPropertyValue("delegate"))
				.getPropertyValue("handlerMethod");
		assertTrue(handlerMethod.toString().contains("createSingleMessageFromGroup"));
		assertEquals("The AggregatorEndpoint is not injected with the appropriate ReleaseStrategy instance",
				releaseStrategy, accessor.getPropertyValue("releaseStrategy"));
		assertEquals("The AggregatorEndpoint is not injected with the appropriate CorrelationStrategy instance",
				correlationStrategy, accessor.getPropertyValue("correlationStrategy"));
		assertEquals("The AggregatorEndpoint is not injected with the appropriate output channel",
				outputChannel, accessor.getPropertyValue("outputChannel"));
		assertEquals("The AggregatorEndpoint is not injected with the appropriate discard channel",
				discardChannel, accessor.getPropertyValue("discardChannel"));
		assertEquals("The AggregatorEndpoint is not set with the appropriate timeout value", 86420000L,
				TestUtils.getPropertyValue(consumer, "messagingTemplate.sendTimeout"));
		assertEquals(
				"The AggregatorEndpoint is not configured with the appropriate 'send partial results on timeout' flag",
				true, accessor.getPropertyValue("sendPartialResultOnExpiry"));
		assertFalse(TestUtils.getPropertyValue(consumer, "expireGroupsUponTimeout", Boolean.class));
		assertTrue(TestUtils.getPropertyValue(consumer, "expireGroupsUponCompletion", Boolean.class));
		assertEquals(123L, TestUtils.getPropertyValue(consumer, "minimumTimeoutForEmptyGroups"));
		assertEquals("456", TestUtils.getPropertyValue(consumer, "groupTimeoutExpression", Expression.class)
				.getExpressionString());
		assertSame(this.context.getBean(LockRegistry.class), TestUtils.getPropertyValue(consumer, "lockRegistry"));
		assertSame(this.context.getBean("scheduler"), TestUtils.getPropertyValue(consumer, "taskScheduler"));
		assertSame(this.context.getBean("store"), TestUtils.getPropertyValue(consumer, "messageStore"));
		assertEquals(5, TestUtils.getPropertyValue(consumer, "order"));
		assertNotNull(TestUtils.getPropertyValue(consumer, "forceReleaseAdviceChain"));

	}

	@Test
	public void testSimpleJavaBeanAggregator() {
		List<Message<?>> outboundMessages = new ArrayList<Message<?>>();
		MessageChannel input = (MessageChannel) context.getBean("aggregatorWithReferenceAndMethodInput");
		outboundMessages.add(createMessage(1L, "id1", 3, 1, null));
		outboundMessages.add(createMessage(2L, "id1", 3, 3, null));
		outboundMessages.add(createMessage(3L, "id1", 3, 2, null));
		outboundMessages.forEach(input::send);
		PollableChannel outputChannel = (PollableChannel) context.getBean("outputChannel");
		Message<?> response = outputChannel.receive(10);
		Assert.assertEquals(6L, response.getPayload());
		Object mbf = context.getBean(IntegrationUtils.INTEGRATION_MESSAGE_BUILDER_FACTORY_BEAN_NAME);
		Object handler = context.getBean("aggregatorWithReferenceAndMethod.handler");
		assertSame(mbf, TestUtils.getPropertyValue(handler, "outputProcessor.messageBuilderFactory"));
	}

	@Test
	public void testMissingMethodOnAggregator() {
		try {
			new ClassPathXmlApplicationContext("invalidMethodNameAggregator.xml", this.getClass()).close();
			fail("Expected exception");
		}
		catch (BeanCreationException e) {
			assertThat(e.getMessage(), containsString("Adder] has no eligible methods"));
		}
	}

	@Test
	public void testMissingReleaseStrategyDefinition() {
		try {
			new ClassPathXmlApplicationContext("ReleaseStrategyMethodWithMissingReference.xml", this.getClass()).close();
			fail("Expected exception");
		}
		catch (BeanCreationException e) {
			assertThat(e.getMessage(), containsString("No bean named 'testReleaseStrategy' available"));
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testAggregatorWithPojoReleaseStrategy() {
		MessageChannel input = this.context.getBean("aggregatorWithPojoReleaseStrategyInput", MessageChannel.class);
		EventDrivenConsumer endpoint = this.context.getBean("aggregatorWithPojoReleaseStrategy", EventDrivenConsumer.class);
		ReleaseStrategy releaseStrategy =
				TestUtils.getPropertyValue(endpoint, "handler.releaseStrategy", ReleaseStrategy.class);
		Assert.assertTrue(releaseStrategy instanceof MethodInvokingReleaseStrategy);
		MessagingMethodInvokerHelper<Long> methodInvokerHelper =
				TestUtils.getPropertyValue(releaseStrategy, "adapter.delegate", MessagingMethodInvokerHelper.class);
		Object handlerMethods = TestUtils.getPropertyValue(methodInvokerHelper, "handlerMethods");
		assertNull(handlerMethods);
		Object handlerMethod = TestUtils.getPropertyValue(methodInvokerHelper, "handlerMethod");
		assertTrue(handlerMethod.toString().contains("checkCompleteness"));
		input.send(createMessage(1L, "correlationId", 4, 0, null));
		input.send(createMessage(2L, "correlationId", 4, 1, null));
		input.send(createMessage(3L, "correlationId", 4, 2, null));
		PollableChannel outputChannel = (PollableChannel) context.getBean("outputChannel");
		Message<?> reply = outputChannel.receive(0);
		Assert.assertNull(reply);
		input.send(createMessage(5L, "correlationId", 4, 3, null));
		reply = outputChannel.receive(0);
		Assert.assertNotNull(reply);
		assertEquals(11L, reply.getPayload());
	}

	@Test // see INT-2011
	public void testAggregatorWithPojoReleaseStrategyAsCollection() {
		MessageChannel input = (MessageChannel) context.getBean("aggregatorWithPojoReleaseStrategyInputAsCollection");
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("aggregatorWithPojoReleaseStrategyAsCollection");
		ReleaseStrategy releaseStrategy = (ReleaseStrategy) new DirectFieldAccessor(new DirectFieldAccessor(endpoint)
				.getPropertyValue("handler")).getPropertyValue("releaseStrategy");
		Assert.assertTrue(releaseStrategy instanceof MethodInvokingReleaseStrategy);
		DirectFieldAccessor releaseStrategyAccessor = new DirectFieldAccessor(new DirectFieldAccessor(new DirectFieldAccessor(releaseStrategy)
				.getPropertyValue("adapter")).getPropertyValue("delegate"));
		Object handlerMethods = releaseStrategyAccessor.getPropertyValue("handlerMethods");
		assertNull(handlerMethods);
		Object handlerMethod = releaseStrategyAccessor.getPropertyValue("handlerMethod");
		assertTrue(handlerMethod.toString().contains("checkCompleteness"));
		input.send(createMessage(1L, "correlationId", 4, 0, null));
		input.send(createMessage(2L, "correlationId", 4, 1, null));
		input.send(createMessage(3L, "correlationId", 4, 2, null));
		PollableChannel outputChannel = (PollableChannel) context.getBean("outputChannel");
		Message<?> reply = outputChannel.receive(0);
		Assert.assertNull(reply);
		input.send(createMessage(5L, "correlationId", 4, 3, null));
		reply = outputChannel.receive(0);
		Assert.assertNotNull(reply);
		assertEquals(11L, reply.getPayload());
	}

	@Test
	public void testAggregatorWithInvalidReleaseStrategyMethod() {
		try {
			new ClassPathXmlApplicationContext("invalidReleaseStrategyMethod.xml", this.getClass()).close();
			fail("Expected exception");
		}
		catch (BeanCreationException e) {
			assertThat(e.getMessage(), containsString("TestReleaseStrategy] has no eligible methods"));
		}
	}

	@Test
	public void testAggregationWithExpressionsAndPojoAggregator() {
		EventDrivenConsumer aggregatorConsumer = (EventDrivenConsumer) context.getBean("aggregatorWithExpressionsAndPojoAggregator");
		AggregatingMessageHandler aggregatingMessageHandler = (AggregatingMessageHandler) TestUtils.getPropertyValue(aggregatorConsumer, "handler");
		MethodInvokingMessageGroupProcessor messageGroupProcessor = (MethodInvokingMessageGroupProcessor) TestUtils.getPropertyValue(aggregatingMessageHandler, "outputProcessor");
		Object messageGroupProcessorTargetObject = TestUtils.getPropertyValue(messageGroupProcessor, "processor.delegate.targetObject");
		assertSame(context.getBean("aggregatorBean"), messageGroupProcessorTargetObject);
		ReleaseStrategy releaseStrategy = (ReleaseStrategy) TestUtils.getPropertyValue(aggregatingMessageHandler, "releaseStrategy");
		CorrelationStrategy correlationStrategy = (CorrelationStrategy) TestUtils.getPropertyValue(aggregatingMessageHandler, "correlationStrategy");
		Long minimumTimeoutForEmptyGroups = TestUtils.getPropertyValue(aggregatingMessageHandler, "minimumTimeoutForEmptyGroups", Long.class);

		assertTrue(ExpressionEvaluatingReleaseStrategy.class.equals(releaseStrategy.getClass()));
		assertTrue(ExpressionEvaluatingCorrelationStrategy.class.equals(correlationStrategy.getClass()));
		assertEquals(60000L, minimumTimeoutForEmptyGroups.longValue());
	}

	@Test
	public void testAggregatorFailureIfMutuallyExclusivityPresent() {
		try {
			new ClassPathXmlApplicationContext("aggregatorParserFailTests.xml", this.getClass()).close();
		}
		catch (BeanDefinitionParsingException e) {
			assertThat(e.getMessage(), containsString(
					"Exactly one of the 'release-strategy' or 'release-strategy-expression' attribute is allowed."));
		}
	}

	private static <T> Message<T> createMessage(T payload, Object correlationId, int sequenceSize, int sequenceNumber,
			MessageChannel outputChannel) {
		return MessageBuilder.withPayload(payload).setCorrelationId(correlationId).setSequenceSize(sequenceSize)
				.setSequenceNumber(sequenceNumber).setReplyChannel(outputChannel).build();
	}

	public static class MyMGP extends SimpleMessageGroupProcessor {

		@org.springframework.integration.annotation.ReleaseStrategy
		public boolean release(Collection<Message<?>> messages) {
			return messages.size() >= 3;
		}

		@org.springframework.integration.annotation.CorrelationStrategy
		public Object correlate(Message<?> m) {
			return new IntegrationMessageHeaderAccessor(m).getCorrelationId();
		}

	}

}
