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

package org.springframework.integration.config;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.aggregator.AggregatingMessageHandler;
import org.springframework.integration.aggregator.CorrelationStrategy;
import org.springframework.integration.aggregator.ExpressionEvaluatingCorrelationStrategy;
import org.springframework.integration.aggregator.ExpressionEvaluatingReleaseStrategy;
import org.springframework.integration.aggregator.MethodInvokingMessageGroupProcessor;
import org.springframework.integration.aggregator.MethodInvokingReleaseStrategy;
import org.springframework.integration.aggregator.ReleaseStrategy;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;

/**
 * @author Marius Bogoevici
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gunnar Hillert
 * @author Gary Russell
 */
public class AggregatorParserTests {

	private ApplicationContext context;

	@Before
	public void setUp() {
		this.context = new ClassPathXmlApplicationContext("aggregatorParserTests.xml", this.getClass());
	}

	@Test
	public void testAggregation() {
		MessageChannel input = (MessageChannel) context.getBean("aggregatorWithReferenceInput");
		TestAggregatorBean aggregatorBean = (TestAggregatorBean) context.getBean("aggregatorBean");
		List<Message<?>> outboundMessages = new ArrayList<Message<?>>();
		outboundMessages.add(createMessage("123", "id1", 3, 1, null));
		outboundMessages.add(createMessage("789", "id1", 3, 3, null));
		outboundMessages.add(createMessage("456", "id1", 3, 2, null));
		for (Message<?> message : outboundMessages) {
			input.send(message);
		}
		assertEquals("One and only one message must have been aggregated", 1, aggregatorBean.getAggregatedMessages()
				.size());
		Message<?> aggregatedMessage = aggregatorBean.getAggregatedMessages().get("id1");
		assertEquals("The aggregated message payload is not correct", "123456789", aggregatedMessage.getPayload());
		Object mbf = context.getBean(IntegrationUtils.INTEGRATION_MESSAGE_BUILDER_FACTORY_BEAN_NAME);
		Object handler = context.getBean("aggregatorWithReference.handler");
		assertSame(mbf, TestUtils.getPropertyValue(handler, "outputProcessor.messageBuilderFactory"));
		assertSame(mbf, TestUtils.getPropertyValue(handler, "outputProcessor.processor.messageBuilderFactory"));
	}

	@Test
	public void testAggregationByExpression() {
		MessageChannel input = (MessageChannel) context.getBean("aggregatorWithExpressionsInput");
		SubscribableChannel outputChannel = (SubscribableChannel) context.getBean("aggregatorWithExpressionsOutput");
		final AtomicReference<Message<?>> aggregatedMessage = new AtomicReference<Message<?>>();
		outputChannel.subscribe(new MessageHandler() {
			@Override
			public void handleMessage(Message<?> message) throws MessageRejectedException, MessageHandlingException,
					MessageDeliveryException {
				aggregatedMessage.set(message);
			}
		});
		List<Message<?>> outboundMessages = new ArrayList<Message<?>>();
		outboundMessages.add(MessageBuilder.withPayload("123").setHeader("foo", "1").build());
		outboundMessages.add(MessageBuilder.withPayload("456").setHeader("foo", "1").build());
		outboundMessages.add(MessageBuilder.withPayload("789").setHeader("foo", "1").build());
		for (Message<?> message : outboundMessages) {
			input.send(message);
		}
		assertEquals("The aggregated message payload is not correct", "[123]", aggregatedMessage.get().getPayload()
				.toString());
		Object mbf = context.getBean(IntegrationUtils.INTEGRATION_MESSAGE_BUILDER_FACTORY_BEAN_NAME);
		Object handler = context.getBean("aggregatorWithExpressions.handler");
		assertSame(mbf, TestUtils.getPropertyValue(handler, "outputProcessor.messageBuilderFactory"));
		assertSame(mbf, TestUtils.getPropertyValue(handler, "outputProcessor.processor.messageBuilderFactory"));
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
		Object handlerMethods =  new DirectFieldAccessor(new DirectFieldAccessor(new DirectFieldAccessor(accessor
				.getPropertyValue("outputProcessor")).getPropertyValue("processor")).getPropertyValue("delegate"))
				.getPropertyValue("handlerMethods");
		assertNull(handlerMethods);
		Object handlerMethod =  new DirectFieldAccessor(new DirectFieldAccessor(new DirectFieldAccessor(accessor
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
		assertEquals("The AggregatorEndpoint is not set with the appropriate timeout value", 86420000l,
				TestUtils.getPropertyValue(consumer, "messagingTemplate.sendTimeout"));
		assertEquals(
				"The AggregatorEndpoint is not configured with the appropriate 'send partial results on timeout' flag",
				true, accessor.getPropertyValue("sendPartialResultOnExpiry"));
		assertFalse(TestUtils.getPropertyValue(consumer, "expireGroupsUponTimeout", Boolean.class));
	}

	@Test
	public void testSimpleJavaBeanAggregator() {
		List<Message<?>> outboundMessages = new ArrayList<Message<?>>();
		MessageChannel input = (MessageChannel) context.getBean("aggregatorWithReferenceAndMethodInput");
		outboundMessages.add(createMessage(1l, "id1", 3, 1, null));
		outboundMessages.add(createMessage(2l, "id1", 3, 3, null));
		outboundMessages.add(createMessage(3l, "id1", 3, 2, null));
		for (Message<?> message : outboundMessages) {
			input.send(message);
		}
		PollableChannel outputChannel = (PollableChannel) context.getBean("outputChannel");
		Message<?> response = outputChannel.receive(10);
		Assert.assertEquals(6l, response.getPayload());
		Object mbf = context.getBean(IntegrationUtils.INTEGRATION_MESSAGE_BUILDER_FACTORY_BEAN_NAME);
		Object handler = context.getBean("aggregatorWithReferenceAndMethod.handler");
		assertSame(mbf, TestUtils.getPropertyValue(handler, "outputProcessor.messageBuilderFactory"));
		assertSame(mbf, TestUtils.getPropertyValue(handler, "outputProcessor.processor.messageBuilderFactory"));
	}

	@Test(expected = BeanCreationException.class)
	public void testMissingMethodOnAggregator() {
		context = new ClassPathXmlApplicationContext("invalidMethodNameAggregator.xml", this.getClass());
	}

	@Test(expected = BeanCreationException.class)
	public void testDuplicateReleaseStrategyDefinition() {
		context = new ClassPathXmlApplicationContext("ReleaseStrategyMethodWithMissingReference.xml", this.getClass());
	}

	@Test
	public void testAggregatorWithPojoReleaseStrategy() {
		MessageChannel input = (MessageChannel) context.getBean("aggregatorWithPojoReleaseStrategyInput");
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("aggregatorWithPojoReleaseStrategy");
		ReleaseStrategy releaseStrategy = (ReleaseStrategy) new DirectFieldAccessor(new DirectFieldAccessor(endpoint)
				.getPropertyValue("handler")).getPropertyValue("releaseStrategy");
		Assert.assertTrue(releaseStrategy instanceof MethodInvokingReleaseStrategy);
		DirectFieldAccessor releaseStrategyAccessor = new DirectFieldAccessor(new DirectFieldAccessor(new DirectFieldAccessor(releaseStrategy)
				.getPropertyValue("adapter")).getPropertyValue("delegate"));
		Object handlerMethods = releaseStrategyAccessor.getPropertyValue("handlerMethods");
		assertNull(handlerMethods);
		Object handlerMethod = releaseStrategyAccessor.getPropertyValue("handlerMethod");
		assertTrue(handlerMethod.toString().contains("checkCompleteness"));
		input.send(createMessage(1l, "correllationId", 4, 0, null));
		input.send(createMessage(2l, "correllationId", 4, 1, null));
		input.send(createMessage(3l, "correllationId", 4, 2, null));
		PollableChannel outputChannel = (PollableChannel) context.getBean("outputChannel");
		Message<?> reply = outputChannel.receive(0);
		Assert.assertNull(reply);
		input.send(createMessage(5l, "correllationId", 4, 3, null));
		reply = outputChannel.receive(0);
		Assert.assertNotNull(reply);
		assertEquals(11l, reply.getPayload());
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
		input.send(createMessage(1l, "correllationId", 4, 0, null));
		input.send(createMessage(2l, "correllationId", 4, 1, null));
		input.send(createMessage(3l, "correllationId", 4, 2, null));
		PollableChannel outputChannel = (PollableChannel) context.getBean("outputChannel");
		Message<?> reply = outputChannel.receive(0);
		Assert.assertNull(reply);
		input.send(createMessage(5l, "correllationId", 4, 3, null));
		reply = outputChannel.receive(0);
		Assert.assertNotNull(reply);
		assertEquals(11l, reply.getPayload());
	}

	@Test(expected = BeanCreationException.class)
	public void testAggregatorWithInvalidReleaseStrategyMethod() {
		context = new ClassPathXmlApplicationContext("invalidReleaseStrategyMethod.xml", this.getClass());
	}

	@Test
	public void testAggregationWithExpressionsAndPojoAggregator() {
		EventDrivenConsumer aggregatorConsumer = (EventDrivenConsumer) context.getBean("aggregatorWithExpressionsAndPojoAggregator");
		AggregatingMessageHandler aggregatingMessageHandler = (AggregatingMessageHandler) TestUtils.getPropertyValue(aggregatorConsumer, "handler");
		MethodInvokingMessageGroupProcessor messageGroupProcessor = (MethodInvokingMessageGroupProcessor) TestUtils.getPropertyValue(aggregatingMessageHandler, "outputProcessor");
		Object mbf = context.getBean(IntegrationUtils.INTEGRATION_MESSAGE_BUILDER_FACTORY_BEAN_NAME);
		assertSame(mbf, TestUtils.getPropertyValue(messageGroupProcessor, "messageBuilderFactory"));
		Object messageGroupProcessorTargetObject = TestUtils.getPropertyValue(messageGroupProcessor, "processor.delegate.targetObject");
		assertSame(context.getBean("aggregatorBean"), messageGroupProcessorTargetObject);
		ReleaseStrategy releaseStrategy = (ReleaseStrategy) TestUtils.getPropertyValue(aggregatingMessageHandler, "releaseStrategy");
		CorrelationStrategy correlationStrategy = (CorrelationStrategy) TestUtils.getPropertyValue(aggregatingMessageHandler, "correlationStrategy");
		Long minimumTimeoutForEmptyGroups = TestUtils.getPropertyValue(aggregatingMessageHandler, "minimumTimeoutForEmptyGroups", Long.class);

		assertTrue(ExpressionEvaluatingReleaseStrategy.class.equals(releaseStrategy.getClass()));
		assertTrue(ExpressionEvaluatingCorrelationStrategy.class.equals(correlationStrategy.getClass()));
		assertEquals(60000L, minimumTimeoutForEmptyGroups.longValue());
	}

	@Test(expected=BeanDefinitionParsingException.class)
	public void testAggregatorFailureIfMutuallyExclusivityPresent() {
		this.context = new ClassPathXmlApplicationContext("aggregatorParserFailTests.xml", this.getClass());
	}

	private static <T> Message<T> createMessage(T payload, Object correlationId, int sequenceSize, int sequenceNumber,
			MessageChannel outputChannel) {
		return MessageBuilder.withPayload(payload).setCorrelationId(correlationId).setSequenceSize(sequenceSize)
				.setSequenceNumber(sequenceNumber).setReplyChannel(outputChannel).build();
	}
}
