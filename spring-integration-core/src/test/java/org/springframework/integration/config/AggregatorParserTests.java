/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Marius Bogoevici
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Glenn Renfro
 */
@SpringJUnitConfig
@DirtiesContext
public class AggregatorParserTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void testAggregation() {
		MessageChannel input = (MessageChannel) context.getBean("aggregatorWithReferenceInput");
		TestAggregatorBean aggregatorBean = (TestAggregatorBean) context.getBean("aggregatorBean");
		List<Message<?>> outboundMessages = new ArrayList<>();
		outboundMessages.add(createMessage("123", "id1", 3, 1, null));
		outboundMessages.add(createMessage("789", "id1", 3, 3, null));
		outboundMessages.add(createMessage("456", "id1", 3, 2, null));

		outboundMessages.forEach(input::send);

		assertThat(aggregatorBean.getAggregatedMessages().size())
				.as("One and only one message must have been aggregated").isEqualTo(1);
		Message<?> aggregatedMessage = aggregatorBean.getAggregatedMessages().get("id1");
		assertThat(aggregatedMessage.getPayload()).as("The aggregated message payload is not correct")
				.isEqualTo("123456789");
		Object mbf = context.getBean(IntegrationUtils.INTEGRATION_MESSAGE_BUILDER_FACTORY_BEAN_NAME);
		Object handler = context.getBean("aggregatorWithReference.handler");
		assertThat(TestUtils.<Object>getPropertyValue(handler, "outputProcessor.messageBuilderFactory")).isSameAs(mbf);
		assertThat(TestUtils.<Boolean>getPropertyValue(handler, "releaseLockBeforeSend")).isTrue();
	}

	@Test
	public void testAggregationWithMessageGroupProcessor() {
		QueueChannel output = this.context.getBean("outputChannel", QueueChannel.class);
		output.purge(null);
		MessageChannel input = (MessageChannel) context.getBean("aggregatorWithMGPReferenceInput");
		List<Message<?>> outboundMessages = new ArrayList<>();
		outboundMessages.add(createMessage("123", "id1", 3, 1, null));
		outboundMessages.add(createMessage("789", "id1", 3, 3, null));
		outboundMessages.add(createMessage("456", "id1", 3, 2, null));

		outboundMessages.forEach(input::send);

		assertThat(output.getQueueSize()).isEqualTo(3);
		output.purge(null);
		assertThat(TestUtils.<Boolean>getPropertyValue(context.getBean("aggregatorWithMGPReference.handler"),
				"releaseLockBeforeSend")).isFalse();
	}

	@Test
	public void testAggregationWithMessageGroupProcessorAndStrategies() {
		QueueChannel output = this.context.getBean("outputChannel", QueueChannel.class);
		output.purge(null);
		MessageChannel input = (MessageChannel) context.getBean("aggregatorWithCustomMGPReferenceInput");
		List<Message<?>> outboundMessages = new ArrayList<>();
		outboundMessages.add(createMessage("123", "id1", 3, 1, null));
		outboundMessages.add(createMessage("789", "id1", 3, 3, null));
		outboundMessages.add(createMessage("456", "id1", 3, 2, null));

		outboundMessages.forEach(input::send);

		assertThat(output.getQueueSize()).isEqualTo(3);
		output.purge(null);
	}

	@Test
	public void testAggregationByExpression() {
		MessageChannel input = (MessageChannel) context.getBean("aggregatorWithExpressionsInput");
		SubscribableChannel outputChannel = (SubscribableChannel) context.getBean("aggregatorWithExpressionsOutput");
		final AtomicReference<Message<?>> aggregatedMessage = new AtomicReference<>();
		outputChannel.subscribe(aggregatedMessage::set);
		List<Message<?>> outboundMessages = new ArrayList<>();
		outboundMessages.add(MessageBuilder.withPayload("123").setHeader("foo", "1").build());
		outboundMessages.add(MessageBuilder.withPayload("456").setHeader("foo", "1").build());
		outboundMessages.add(MessageBuilder.withPayload("789").setHeader("foo", "1").build());

		outboundMessages.forEach(input::send);

		assertThat(aggregatedMessage.get().getPayload()
				.toString()).as("The aggregated message payload is not correct").isEqualTo("[123]");
		Object mbf = context.getBean(IntegrationUtils.INTEGRATION_MESSAGE_BUILDER_FACTORY_BEAN_NAME);
		Object handler = context.getBean("aggregatorWithExpressions.handler");
		assertThat(TestUtils.<Object>getPropertyValue(handler, "outputProcessor.messageBuilderFactory")).isSameAs(mbf);
		assertThat(TestUtils.<Boolean>getPropertyValue(handler, "expireGroupsUponTimeout")).isTrue();
	}

	@Test
	public void testPropertyAssignment() {
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("completelyDefinedAggregator");
		ReleaseStrategy releaseStrategy = (ReleaseStrategy) context.getBean("releaseStrategy");
		CorrelationStrategy correlationStrategy = (CorrelationStrategy) context.getBean("correlationStrategy");
		Object consumer = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertThat(consumer).isInstanceOf(AggregatingMessageHandler.class);
		DirectFieldAccessor accessor = new DirectFieldAccessor(consumer);
		Object handlerMethods = new DirectFieldAccessor(new DirectFieldAccessor(new DirectFieldAccessor(accessor
				.getPropertyValue("outputProcessor")).getPropertyValue("processor")).getPropertyValue("delegate"))
				.getPropertyValue("handlerMethods");
		assertThat(handlerMethods).isNotNull();
		Object handlerMethod = new DirectFieldAccessor(new DirectFieldAccessor(new DirectFieldAccessor(accessor
				.getPropertyValue("outputProcessor")).getPropertyValue("processor")).getPropertyValue("delegate"))
				.getPropertyValue("handlerMethod");
		assertThat(handlerMethod.toString().contains("createSingleMessageFromGroup")).isTrue();
		assertThat(accessor.getPropertyValue("releaseStrategy"))
				.as("The AggregatorEndpoint is not injected with the appropriate ReleaseStrategy instance")
				.isEqualTo(releaseStrategy);
		assertThat(accessor.getPropertyValue("correlationStrategy"))
				.as("The AggregatorEndpoint is not injected with the appropriate CorrelationStrategy instance")
				.isEqualTo(correlationStrategy);
		assertThat(accessor.getPropertyValue("outputChannelName"))
				.as("The AggregatorEndpoint is not injected with the appropriate output channel")
				.isEqualTo("outputChannel");
		assertThat(accessor.getPropertyValue("discardChannelName"))
				.as("The AggregatorEndpoint is not injected with the appropriate discard channel")
				.isEqualTo("discardChannel");
		assertThat(TestUtils.<Long>getPropertyValue(consumer, "messagingTemplate.sendTimeout"))
				.as("The AggregatorEndpoint is not set with the appropriate timeout value").isEqualTo(86420000L);
		assertThat(accessor.getPropertyValue("sendPartialResultOnExpiry"))
				.as("The AggregatorEndpoint is not configured with the appropriate 'send partial results on timeout' " +
						"flag")
				.isEqualTo(true);
		assertThat(TestUtils.<Boolean>getPropertyValue(consumer, "expireGroupsUponTimeout")).isFalse();
		assertThat(TestUtils.<Boolean>getPropertyValue(consumer, "expireGroupsUponCompletion")).isTrue();
		assertThat(TestUtils.<Long>getPropertyValue(consumer, "minimumTimeoutForEmptyGroups")).isEqualTo(123L);
		assertThat(TestUtils.<Expression>getPropertyValue(consumer, "groupTimeoutExpression")
				.getExpressionString()).isEqualTo("456");
		assertThat(TestUtils.<Object>getPropertyValue(consumer, "lockRegistry"))
				.isSameAs(this.context.getBean(LockRegistry.class));
		assertThat(TestUtils.<Object>getPropertyValue(consumer, "taskScheduler"))
				.isSameAs(this.context.getBean("scheduler"));
		assertThat(TestUtils.<Object>getPropertyValue(consumer, "messageStore")).isSameAs(this.context.getBean("store"));
		assertThat(TestUtils.<Integer>getPropertyValue(consumer, "order")).isEqualTo(5);
		assertThat(TestUtils.<Object>getPropertyValue(consumer, "forceReleaseAdviceChain")).isNotNull();
		assertThat(TestUtils.<Boolean>getPropertyValue(consumer, "popSequence")).isFalse();
		assertThat(TestUtils.<Long>getPropertyValue(consumer, "expireTimeout")).isEqualTo(250L);
		assertThat(TestUtils.<Duration>getPropertyValue(consumer, "expireDuration"))
				.isEqualTo(Duration.ofSeconds(10));
	}

	@Test
	public void testSimpleJavaBeanAggregator() {
		List<Message<?>> outboundMessages = new ArrayList<>();
		MessageChannel input = (MessageChannel) context.getBean("aggregatorWithReferenceAndMethodInput");
		outboundMessages.add(createMessage(1L, "id1", 3, 1, null));
		outboundMessages.add(createMessage(2L, "id1", 3, 3, null));
		outboundMessages.add(createMessage(3L, "id1", 3, 2, null));
		outboundMessages.forEach(input::send);
		PollableChannel outputChannel = (PollableChannel) context.getBean("outputChannel");
		Message<?> response = outputChannel.receive(10);
		assertThat(response.getPayload()).isEqualTo(6L);
		Object mbf = context.getBean(IntegrationUtils.INTEGRATION_MESSAGE_BUILDER_FACTORY_BEAN_NAME);
		Object handler = context.getBean("aggregatorWithReferenceAndMethod.handler");
		assertThat(TestUtils.<Object>getPropertyValue(handler, "outputProcessor.messageBuilderFactory"))
				.isSameAs(mbf);
	}

	@Test
	public void testMissingMethodOnAggregator() {
		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() -> new ClassPathXmlApplicationContext("invalidMethodNameAggregator.xml", getClass()))
				.withStackTraceContaining("Adder] has no eligible methods");
	}

	@Test
	public void testMissingReleaseStrategyDefinition() {
		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() -> new ClassPathXmlApplicationContext("ReleaseStrategyMethodWithMissingReference.xml",
						getClass()))
				.withStackTraceContaining("No bean named 'testReleaseStrategy' available");
	}

	@Test
	public void testAggregatorWithPojoReleaseStrategy() {
		MessageChannel input = this.context.getBean("aggregatorWithPojoReleaseStrategyInput", MessageChannel.class);
		EventDrivenConsumer endpoint =
				this.context.getBean("aggregatorWithPojoReleaseStrategy", EventDrivenConsumer.class);
		ReleaseStrategy releaseStrategy = TestUtils.getPropertyValue(endpoint, "handler.releaseStrategy");
		assertThat(releaseStrategy instanceof MethodInvokingReleaseStrategy).isTrue();
		MessagingMethodInvokerHelper methodInvokerHelper =
				TestUtils.getPropertyValue(releaseStrategy, "adapter.delegate");
		Object handlerMethods = TestUtils.getPropertyValue(methodInvokerHelper, "handlerMethods");
		assertThat(handlerMethods).isNotNull();
		Object handlerMethod = TestUtils.getPropertyValue(methodInvokerHelper, "handlerMethod");
		assertThat(handlerMethod.toString().contains("checkCompleteness")).isTrue();
		input.send(createMessage(1L, "correlationId", 4, 0, null));
		input.send(createMessage(2L, "correlationId", 4, 1, null));
		input.send(createMessage(3L, "correlationId", 4, 2, null));
		PollableChannel outputChannel = (PollableChannel) context.getBean("outputChannel");
		Message<?> reply = outputChannel.receive(0);
		assertThat(reply).isNull();
		input.send(createMessage(5L, "correlationId", 4, 3, null));
		reply = outputChannel.receive(0);
		assertThat(reply).isNotNull();
		assertThat(reply.getPayload()).isEqualTo(11L);
	}

	@Test // see INT-2011
	public void testAggregatorWithPojoReleaseStrategyAsCollection() {
		MessageChannel input = (MessageChannel) context.getBean("aggregatorWithPojoReleaseStrategyInputAsCollection");
		EventDrivenConsumer endpoint = (EventDrivenConsumer)
				context.getBean("aggregatorWithPojoReleaseStrategyAsCollection");
		ReleaseStrategy releaseStrategy = (ReleaseStrategy) new DirectFieldAccessor(new DirectFieldAccessor(endpoint)
				.getPropertyValue("handler")).getPropertyValue("releaseStrategy");
		assertThat(releaseStrategy instanceof MethodInvokingReleaseStrategy).isTrue();
		DirectFieldAccessor releaseStrategyAccessor =
				new DirectFieldAccessor(new DirectFieldAccessor(new DirectFieldAccessor(releaseStrategy)
						.getPropertyValue("adapter")).getPropertyValue("delegate"));
		Object handlerMethods = releaseStrategyAccessor.getPropertyValue("handlerMethods");
		assertThat(handlerMethods).isNotNull();
		Object handlerMethod = releaseStrategyAccessor.getPropertyValue("handlerMethod");
		assertThat(handlerMethod.toString().contains("checkCompleteness")).isTrue();
		input.send(createMessage(1L, "correlationId", 4, 0, null));
		input.send(createMessage(2L, "correlationId", 4, 1, null));
		input.send(createMessage(3L, "correlationId", 4, 2, null));
		PollableChannel outputChannel = (PollableChannel) context.getBean("outputChannel");
		Message<?> reply = outputChannel.receive(0);
		assertThat(reply).isNull();
		input.send(createMessage(5L, "correlationId", 4, 3, null));
		reply = outputChannel.receive(0);
		assertThat(reply).isNotNull();
		assertThat(reply.getPayload()).isEqualTo(11L);
	}

	@Test
	public void testAggregatorWithInvalidReleaseStrategyMethod() {
		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() -> new ClassPathXmlApplicationContext("invalidReleaseStrategyMethod.xml", getClass()))
				.withStackTraceContaining("TestReleaseStrategy] has no eligible methods");
	}

	@Test
	public void testAggregationWithExpressionsAndPojoAggregator() {
		EventDrivenConsumer aggregatorConsumer = context.getBean("aggregatorWithExpressionsAndPojoAggregator",
				EventDrivenConsumer.class);
		AggregatingMessageHandler aggregatingMessageHandler =
				TestUtils.getPropertyValue(aggregatorConsumer, "handler");
		MethodInvokingMessageGroupProcessor messageGroupProcessor =
				TestUtils.getPropertyValue(aggregatingMessageHandler, "outputProcessor");
		Object messageGroupProcessorTargetObject = TestUtils.getPropertyValue(messageGroupProcessor, "processor" +
				".delegate.targetObject");
		assertThat(messageGroupProcessorTargetObject).isSameAs(context.getBean("aggregatorBean"));
		ReleaseStrategy releaseStrategy = TestUtils.getPropertyValue(aggregatingMessageHandler, "releaseStrategy");
		CorrelationStrategy correlationStrategy =
				TestUtils.getPropertyValue(aggregatingMessageHandler, "correlationStrategy");
		Long minimumTimeoutForEmptyGroups =
				TestUtils.getPropertyValue(aggregatingMessageHandler, "minimumTimeoutForEmptyGroups");

		assertThat(ExpressionEvaluatingReleaseStrategy.class.equals(releaseStrategy.getClass())).isTrue();
		assertThat(ExpressionEvaluatingCorrelationStrategy.class.equals(correlationStrategy.getClass())).isTrue();
		assertThat(minimumTimeoutForEmptyGroups.longValue()).isEqualTo(60000L);
	}

	@Test
	public void testAggregatorFailureIfMutuallyExclusivityPresent() {
		try {
			new ClassPathXmlApplicationContext("aggregatorParserFailTests.xml", this.getClass()).close();
		}
		catch (BeanDefinitionParsingException e) {
			assertThat(e.getMessage())
					.contains("Exactly one of the 'release-strategy' or 'release-strategy-expression' attribute is allowed.");
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
