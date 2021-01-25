/*
 * Copyright 2016-2021 the original author or authors.
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

package org.springframework.integration.dsl.routers;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.annotation.Router;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.config.EnableMessageHistory;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowDefinition;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.core.DestinationResolutionException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Artem Bilan
 * @author Gary Russell
 * @author Jayadev Sirimamilla
 *
 * @since 5.0
 */
@SpringJUnitConfig
@DirtiesContext
public class RouterTests {

	@Autowired
	private ListableBeanFactory beanFactory;

	@Autowired
	@Qualifier("routerInput")
	private MessageChannel routerInput;

	@Autowired
	@Qualifier("oddChannel")
	private PollableChannel oddChannel;

	@Autowired
	@Qualifier("evenChannel")
	private PollableChannel evenChannel;


	@Test
	public void testRouter() {
		this.beanFactory.containsBean("routeFlow.subFlow#0.channel#0");

		int[] payloads = { 1, 2, 3, 4, 5, 6 };

		for (int payload : payloads) {
			this.routerInput.send(new GenericMessage<>(payload));
		}

		for (int i = 0; i < 3; i++) {
			Message<?> receive = this.oddChannel.receive(2000);
			assertThat(receive).isNotNull();
			assertThat(receive.getPayload()).isEqualTo(payloads[i * 2] * 3);

			receive = this.evenChannel.receive(2000);
			assertThat(receive).isNotNull();
			assertThat(receive.getPayload()).isEqualTo(payloads[i * 2 + 1]);
		}
	}

	@Autowired
	@Qualifier("routerTwoSubFlows.input")
	private MessageChannel routerTwoSubFlowsInput;

	@Autowired
	@Qualifier("routerTwoSubFlowsOutput")
	private PollableChannel routerTwoSubFlowsOutput;

	@Test
	public void testRouterWithTwoSubflows() {
		this.routerTwoSubFlowsInput.send(new GenericMessage<Object>(Arrays.asList(1, 2, 3, 4, 5, 6)));
		Message<?> receive = this.routerTwoSubFlowsOutput.receive(5000);
		assertThat(receive).isNotNull();
		Object payload = receive.getPayload();
		assertThat(payload).isInstanceOf(List.class);
		@SuppressWarnings("unchecked")
		List<Integer> results = (List<Integer>) payload;

		assertThat(results).containsExactly(3, 4, 9, 8, 15, 12);
	}

	@Autowired
	@Qualifier("routeSubflowToReplyChannelFlow.input")
	private MessageChannel routeSubflowToReplyChannelFlowInput;

	@Test
	public void testRouterSubflowWithReplyChannelHeader() {
		PollableChannel replyChannel = new QueueChannel();
		this.routeSubflowToReplyChannelFlowInput.send(
				MessageBuilder.withPayload("baz")
						.setReplyChannel(replyChannel)
						.build());

		Message<?> receive = replyChannel.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("BAZ");
	}


	@Autowired
	@Qualifier("routeSubflowWithoutReplyToMainFlow.input")
	private MessageChannel routeSubflowWithoutReplyToMainFlowInput;

	@Autowired
	@Qualifier("routerSubflowResult")
	private PollableChannel routerSubflowResult;

	@Test
	public void testRouterSubflowWithoutReplyToMainFlow() {
		this.routeSubflowWithoutReplyToMainFlowInput.send(new GenericMessage<>("BOO"));

		Message<?> receive = routerSubflowResult.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("boo");
		assertThat(this.defaultOutputChannel.receive(1)).isNull();
		this.routeSubflowWithoutReplyToMainFlowInput.send(new GenericMessage<>("foo"));
		assertThat(this.defaultOutputChannel.receive(10000)).isNotNull();
	}

	@Autowired
	@Qualifier("recipientListInput")
	private MessageChannel recipientListInput;

	@Autowired
	@Qualifier("foo-channel")
	private PollableChannel fooChannel;

	@Autowired
	@Qualifier("bar-channel")
	private PollableChannel barChannel;


	@Autowired
	@Qualifier("recipientListSubFlow1Result")
	private PollableChannel recipientListSubFlow1Result;

	@Autowired
	@Qualifier("recipientListSubFlow2Result")
	private PollableChannel recipientListSubFlow2Result;

	@Autowired
	@Qualifier("recipientListSubFlow3Result")
	private PollableChannel recipientListSubFlow3Result;

	@Autowired
	@Qualifier("defaultOutputChannel")
	private PollableChannel defaultOutputChannel;

	@Test
	public void testRecipientListRouter() {
		Message<String> fooMessage = MessageBuilder.withPayload("fooPayload").setHeader("recipient", true).build();
		Message<String> barMessage = MessageBuilder.withPayload("barPayload").setHeader("recipient", true).build();
		Message<String> bazMessage = new GenericMessage<>("baz");
		Message<String> badMessage = new GenericMessage<>("badPayload");

		this.recipientListInput.send(fooMessage);
		Message<?> result1a = this.fooChannel.receive(10000);
		assertThat(result1a).isNotNull();
		assertThat(result1a.getPayload()).isEqualTo("foo");
		Message<?> result1b = this.barChannel.receive(10000);
		assertThat(result1b).isNotNull();
		assertThat(result1b.getPayload()).isEqualTo("foo");
		Message<?> result1c = this.recipientListSubFlow1Result.receive(10000);
		assertThat(result1c).isNotNull();
		assertThat(result1c.getPayload()).isEqualTo("FOO");
		assertThat(this.recipientListSubFlow2Result.receive(0)).isNull();

		this.recipientListInput.send(barMessage);
		assertThat(this.fooChannel.receive(0)).isNull();
		assertThat(this.recipientListSubFlow2Result.receive(0)).isNull();
		Message<?> result2b = this.barChannel.receive(10000);
		assertThat(result2b).isNotNull();
		assertThat(result2b.getPayload()).isEqualTo("bar");
		Message<?> result2c = this.recipientListSubFlow1Result.receive(10000);
		assertThat(result1c).isNotNull();
		assertThat(result2c.getPayload()).isEqualTo("BAR");

		this.recipientListInput.send(bazMessage);
		assertThat(this.fooChannel.receive(0)).isNull();
		assertThat(this.barChannel.receive(0)).isNull();
		Message<?> result3c = this.recipientListSubFlow1Result.receive(10000);
		assertThat(result3c).isNotNull();
		assertThat(result3c.getPayload()).isEqualTo("BAZ");
		Message<?> result4c = this.recipientListSubFlow2Result.receive(10000);
		assertThat(result4c).isNotNull();
		assertThat(result4c.getPayload()).isEqualTo("Hello baz");

		this.recipientListInput.send(badMessage);
		assertThat(this.fooChannel.receive(0)).isNull();
		assertThat(this.barChannel.receive(0)).isNull();
		assertThat(this.recipientListSubFlow1Result.receive(0)).isNull();
		assertThat(this.recipientListSubFlow2Result.receive(0)).isNull();
		Message<?> resultD = this.defaultOutputChannel.receive(10000);
		assertThat(resultD).isNotNull();
		assertThat(resultD.getPayload()).isEqualTo("bad");

		this.recipientListInput.send(new GenericMessage<>("bax"));
		Message<?> result5c = this.recipientListSubFlow3Result.receive(10000);
		assertThat(result5c).isNotNull();
		assertThat(result5c.getPayload()).isEqualTo("bax");
		assertThat(this.fooChannel.receive(0)).isNull();
		assertThat(this.barChannel.receive(0)).isNull();
		assertThat(this.recipientListSubFlow1Result.receive(0)).isNull();
		assertThat(this.recipientListSubFlow2Result.receive(0)).isNull();
	}

	@Autowired
	@Qualifier("routerMethodInput")
	private MessageChannel routerMethodInput;

	@Autowired
	@Qualifier("routerMethod2Input")
	private MessageChannel routerMethod2Input;

	@Autowired
	@Qualifier("routeMethodInvocationFlow3.input")
	private MessageChannel routerMethod3Input;

	@Autowired
	@Qualifier("routerMultiInput")
	private MessageChannel routerMultiInput;

	@Test
	public void testMethodInvokingRouter() {
		Message<String> fooMessage = new GenericMessage<>("foo");
		Message<String> barMessage = new GenericMessage<>("bar");
		Message<String> badMessage = new GenericMessage<>("bad");

		this.routerMethodInput.send(fooMessage);

		Message<?> result1a = this.fooChannel.receive(2000);
		assertThat(result1a).isNotNull();
		assertThat(result1a.getPayload()).isEqualTo("foo");
		assertThat(this.barChannel.receive(0)).isNull();

		this.routerMethodInput.send(barMessage);
		assertThat(this.fooChannel.receive(0)).isNull();
		Message<?> result2b = this.barChannel.receive(2000);
		assertThat(result2b).isNotNull();
		assertThat(result2b.getPayload()).isEqualTo("bar");

		assertThatExceptionOfType(MessageDeliveryException.class)
				.isThrownBy(() -> this.routerMethodInput.send(badMessage))
				.withMessageContaining("No channel resolved by router");
	}

	@Test
	public void testMethodInvokingRouter2() {
		Message<String> fooMessage = MessageBuilder.withPayload("foo").setHeader("targetChannel", "foo").build();
		Message<String> barMessage = MessageBuilder.withPayload("bar").setHeader("targetChannel", "bar").build();
		Message<String> badMessage = MessageBuilder.withPayload("bad").setHeader("targetChannel", "bad").build();

		this.routerMethod2Input.send(fooMessage);

		Message<?> result1a = this.fooChannel.receive(2000);
		assertThat(result1a).isNotNull();
		assertThat(result1a.getPayload()).isEqualTo("foo");
		assertThat(this.barChannel.receive(0)).isNull();

		this.routerMethod2Input.send(barMessage);
		assertThat(this.fooChannel.receive(0)).isNull();
		Message<?> result2b = this.barChannel.receive(2000);
		assertThat(result2b).isNotNull();
		assertThat(result2b.getPayload()).isEqualTo("bar");

		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> this.routerMethod2Input.send(badMessage))
				.withCauseInstanceOf(DestinationResolutionException.class)
				.withStackTraceContaining("failed to look up MessageChannel with name 'bad-channel'");
	}

	@Test
	public void testMethodInvokingRouter3() {
		Message<String> fooMessage = new GenericMessage<>("foo");
		Message<String> barMessage = new GenericMessage<>("bar");
		Message<String> badMessage = new GenericMessage<>("bad");

		this.routerMethod3Input.send(fooMessage);

		Message<?> result1a = this.fooChannel.receive(2000);
		assertThat(result1a).isNotNull();
		assertThat(result1a.getPayload()).isEqualTo("foo");
		assertThat(this.barChannel.receive(0)).isNull();

		this.routerMethod3Input.send(barMessage);
		assertThat(this.fooChannel.receive(0)).isNull();
		Message<?> result2b = this.barChannel.receive(2000);
		assertThat(result2b).isNotNull();
		assertThat(result2b.getPayload()).isEqualTo("bar");

		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> this.routerMethod3Input.send(badMessage))
				.withCauseInstanceOf(DestinationResolutionException.class)
				.withStackTraceContaining("failed to look up MessageChannel with name 'bad-channel'");
	}

	@Test
	public void testMultiRouter() {
		Message<String> fooMessage = new GenericMessage<>("foo");
		Message<String> barMessage = new GenericMessage<>("bar");
		Message<String> badMessage = new GenericMessage<>("bad");

		this.routerMultiInput.send(fooMessage);
		Message<?> result1a = this.fooChannel.receive(2000);
		assertThat(result1a).isNotNull();
		assertThat(result1a.getPayload()).isEqualTo("foo");
		Message<?> result1b = this.barChannel.receive(2000);
		assertThat(result1b).isNotNull();
		assertThat(result1b.getPayload()).isEqualTo("foo");

		this.routerMultiInput.send(barMessage);
		Message<?> result2a = this.fooChannel.receive(2000);
		assertThat(result2a).isNotNull();
		assertThat(result2a.getPayload()).isEqualTo("bar");
		Message<?> result2b = this.barChannel.receive(2000);
		assertThat(result2b).isNotNull();
		assertThat(result2b.getPayload()).isEqualTo("bar");

		assertThatExceptionOfType(MessageDeliveryException.class)
				.isThrownBy(() -> this.routerMultiInput.send(badMessage))
				.withMessageContaining("No channel resolved by router");
	}

	@Autowired
	@Qualifier("payloadTypeRouteFlow.input")
	private MessageChannel payloadTypeRouteFlowInput;

	@Autowired
	@Qualifier("stringsChannel")
	private PollableChannel stringsChannel;

	@Autowired
	@Qualifier("integersChannel")
	private PollableChannel integersChannel;

	@Test
	public void testPayloadTypeRouteFlow() {
		this.payloadTypeRouteFlowInput.send(new GenericMessage<>("foo"));
		this.payloadTypeRouteFlowInput.send(new GenericMessage<>(22));
		this.payloadTypeRouteFlowInput.send(new GenericMessage<>(33));
		this.payloadTypeRouteFlowInput.send(new GenericMessage<>("BAR"));

		Message<?> receive = this.stringsChannel.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("foo");

		receive = this.stringsChannel.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("BAR");

		assertThat(this.stringsChannel.receive(10)).isNull();

		receive = this.integersChannel.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo(22);

		receive = this.integersChannel.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo(33);

		assertThat(this.integersChannel.receive(10)).isNull();
	}

	@Autowired
	@Qualifier("recipientListOrderFlow.input")
	private MessageChannel recipientListOrderFlowInput;

	@Autowired
	@Qualifier("recipientListOrderResult")
	private PollableChannel recipientListOrderResult;

	@Autowired
	@Qualifier("alwaysRecipient")
	private QueueChannel alwaysRecipient;

	@Test
	@SuppressWarnings("unchecked")
	public void testRecipientListRouterOrder() {
		this.recipientListOrderFlowInput.send(new GenericMessage<>(new AtomicReference<>("")));
		Message<?> receive = this.recipientListOrderResult.receive(10000);
		assertThat(receive).isNotNull();

		AtomicReference<String> result = (AtomicReference<String>) receive.getPayload();
		assertThat(result.get()).isEqualTo("Hello World");

		receive = this.recipientListOrderResult.receive(10000);
		assertThat(receive).isNotNull();
		result = (AtomicReference<String>) receive.getPayload();
		assertThat(result.get()).isEqualTo("Hello World");

		assertThat(this.alwaysRecipient.getQueueSize()).isEqualTo(1);
	}

	@Autowired
	@Qualifier("routerAsNonLastFlow.input")
	private MessageChannel routerAsNonLastFlowChannel;

	@Autowired
	@Qualifier("routerAsNonLastDefaultOutputChannel")
	private PollableChannel routerAsNonLastDefaultOutputChannel;

	@Test
	public void testRouterAsNonLastComponent() {
		this.routerAsNonLastFlowChannel.send(new GenericMessage<>("Hello World"));
		Message<?> receive = this.routerAsNonLastDefaultOutputChannel.receive(1000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("Hello World");
	}

	@Autowired
	@Qualifier("scatterGatherFlow.input")
	private MessageChannel scatterGatherFlowInput;

	@Test
	public void testScatterGather() {
		QueueChannel replyChannel = new QueueChannel();
		Message<String> request = MessageBuilder.withPayload("foo")
				.setReplyChannel(replyChannel)
				.build();
		this.scatterGatherFlowInput.send(request);
		Message<?> bestQuoteMessage = replyChannel.receive(10000);
		assertThat(bestQuoteMessage).isNotNull();
		Object payload = bestQuoteMessage.getPayload();
		assertThat(payload).isInstanceOf(List.class);
		assertThat(((List<?>) payload).size()).isGreaterThanOrEqualTo(1);
	}


	@Autowired
	@Qualifier("exceptionTypeRouteFlow.input")
	private MessageChannel exceptionTypeRouteFlowInput;

	@Autowired
	private PollableChannel illegalArgumentChannel;

	@Autowired
	private PollableChannel runtimeExceptionChannel;

	@Autowired
	private PollableChannel messageHandlingExceptionChannel;

	@Autowired
	private PollableChannel exceptionRouterDefaultChannel;

	@Test
	public void testExceptionTypeRouteFlow() {
		Message<?> failedMessage = new GenericMessage<>("foo");
		IllegalArgumentException rootCause = new IllegalArgumentException("bad argument");
		RuntimeException middleCause = new RuntimeException(rootCause);
		MessageHandlingException error = new MessageHandlingException(failedMessage, "failed", middleCause);
		ErrorMessage message = new ErrorMessage(error);

		this.exceptionTypeRouteFlowInput.send(message);

		assertThat(this.illegalArgumentChannel.receive(1000)).isNotNull();
		assertThat(this.exceptionRouterDefaultChannel.receive(0)).isNull();
		assertThat(this.runtimeExceptionChannel.receive(0)).isNull();
		assertThat(this.messageHandlingExceptionChannel.receive(0)).isNull();
	}

	@Autowired
	@Qualifier("nestedScatterGatherFlow.input")
	private MessageChannel nestedScatterGatherFlowInput;

	@Test
	public void testNestedScatterGather() {
		QueueChannel replyChannel = new QueueChannel();
		Message<String> request = MessageBuilder.withPayload("this is a test")
				.setReplyChannel(replyChannel)
				.build();
		this.nestedScatterGatherFlowInput.send(request);
		Message<?> bestQuoteMessage = replyChannel.receive(10000);
		assertThat(bestQuoteMessage).isNotNull();
		Object payload = bestQuoteMessage.getPayload();
		assertThat(payload).isInstanceOf(String.class);
		List<?> topSequenceDetails =
				(List<?>) bestQuoteMessage.getHeaders()
						.get(IntegrationMessageHeaderAccessor.SEQUENCE_DETAILS, List.class)
						.get(0);

		assertThat(bestQuoteMessage.getHeaders().get(IntegrationMessageHeaderAccessor.CORRELATION_ID))
				.isEqualTo(request.getHeaders().getId());

		assertThat(topSequenceDetails.get(0))
				.isEqualTo(bestQuoteMessage.getHeaders().get(IntegrationMessageHeaderAccessor.CORRELATION_ID));

		assertThat(topSequenceDetails.get(1))
				.isEqualTo(bestQuoteMessage.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER));

		assertThat(topSequenceDetails.get(2))
				.isEqualTo(bestQuoteMessage.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE));
	}

	@Autowired
	@Qualifier("scatterGatherAndExecutorChannelSubFlow.input")
	private MessageChannel scatterGatherAndExecutorChannelSubFlowInput;

	@Test
	public void testScatterGatherWithExecutorChannelSubFlow() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> testMessage =
				MessageBuilder.withPayload("test")
						.setReplyChannel(replyChannel)
						.build();

		this.scatterGatherAndExecutorChannelSubFlowInput.send(testMessage);

		Message<?> receive = replyChannel.receive(10_000);
		assertThat(receive).isNotNull();
		Object payload = receive.getPayload();
		assertThat(payload).isInstanceOf(List.class);
		assertThat(((List<?>) payload).get(1)).isInstanceOf(RuntimeException.class);
	}

	@Autowired
	@Qualifier("propagateErrorFromGatherer.gateway")
	private Function<Object, ?> propagateErrorFromGathererGateway;

	@Test
	public void propagateErrorFromGatherer() {
		assertThatExceptionOfType(RuntimeException.class)
				.isThrownBy(() -> propagateErrorFromGathererGateway.apply("bar"))
				.withMessage("intentional");
	}

	@Autowired
	@Qualifier("scatterGatherInSubFlow.input")
	MessageChannel scatterGatherInSubFlowChannel;


	@Test
	public void testNestedScatterGatherSuccess() {
		PollableChannel replyChannel = new QueueChannel();
		this.scatterGatherInSubFlowChannel.send(
				org.springframework.integration.support.MessageBuilder.withPayload("baz")
						.setReplyChannel(replyChannel)
						.build());

		Message<?> receive = replyChannel.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("baz");

	}

	@Autowired
	@Qualifier("scatterGatherWireTapChannel")
	PollableChannel scatterGatherWireTapChannel;

	@Test
	public void testNestedScatterGatherSequenceTest() {
		PollableChannel replyChannel = new QueueChannel();
		this.scatterGatherInSubFlowChannel.send(
				MessageBuilder.withPayload("sequencetest")
						.setReplyChannel(replyChannel)
						.build());

		Message<?> wiretapMessage1 = scatterGatherWireTapChannel.receive(10000);
		assertThat(wiretapMessage1).isNotNull();
		MessageHeaders headers1 = wiretapMessage1.getHeaders();
		Message<?> wiretapMessage2 = scatterGatherWireTapChannel.receive(10000);
		assertThat(wiretapMessage2).isNotNull()
				.extracting(Message::getHeaders, as(InstanceOfAssertFactories.MAP))
				.containsAllEntriesOf(
						headers1.entrySet()
								.stream()
								.filter((entry) ->
										!MessageHeaders.ID.equals(entry.getKey())
												&& !MessageHeaders.TIMESTAMP.equals(entry.getKey()))
								.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
		Message<?> receive = replyChannel.receive(10000);

		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("sequencetest");

	}

	@Configuration
	@EnableIntegration
	@EnableMessageHistory({ "recipientListOrder*", "recipient1*", "recipient2*" })
	public static class ContextConfiguration {

		@Bean
		public QueueChannel evenChannel() {
			return new QueueChannel();
		}

		@Bean
		public IntegrationFlow routeFlow() {
			return IntegrationFlows.from("routerInput")
					.<Integer, Boolean>route(p -> p % 2 == 0,
							m -> m.channelMapping(true, evenChannel())
									.subFlowMapping(false, f ->
											f.<Integer>handle((p, h) -> p * 3))
									.defaultOutputToParentFlow())
					.channel(MessageChannels.queue("oddChannel"))
					.get();
		}

		@Bean
		public IntegrationFlow upperCase() {
			return f -> f
					.<String>handle((p, h) -> p.toUpperCase());
		}

		@Bean
		public IntegrationFlow routeSubflowToReplyChannelFlow() {
			return f -> f
					.<Boolean>route("true", m -> m
							.subFlowMapping(true, upperCase())
					);
		}

		@Bean
		public IntegrationFlow routeSubflowWithoutReplyToMainFlow() {
			return f -> f
					.<String, Boolean>route("BOO"::equals, m -> m
							.resolutionRequired(false)
							.subFlowMapping(true, sf -> sf
									.transform(String.class, String::toLowerCase)
									.channel(MessageChannels.queue("routerSubflowResult")))
							.defaultSubFlowMapping(sf -> sf.channel("defaultOutputChannel")));
		}

		@Bean
		public IntegrationFlow routerTwoSubFlows() {
			return f -> f
					.split()
					.<Integer, Boolean>route(p -> p % 2 == 0, m -> m
							.subFlowMapping(true, sf -> sf.<Integer>handle((p, h) -> p * 2))
							.subFlowMapping(false, sf -> sf.<Integer>handle((p, h) -> p * 3)))
					.aggregate()
					.channel(MessageChannels.queue("routerTwoSubFlowsOutput"));
		}

		@Bean(name = "foo-channel")
		public MessageChannel fooChannel() {
			return new QueueChannel();
		}

		@Bean(name = "bar-channel")
		public MessageChannel barChannel() {
			return new QueueChannel();
		}

		@Bean
		public MessageChannel defaultOutputChannel() {
			return new QueueChannel();
		}

		@Bean
		public IntegrationFlow recipientListFlow() {
			return IntegrationFlows.from("recipientListInput")
					.<String, String>transform(p -> p.replaceFirst("Payload", ""))
					.routeToRecipients(r -> r
							.recipient("foo-channel", "'foo' == payload")
							.recipientMessageSelector("bar-channel", m ->
									m.getHeaders().containsKey("recipient")
											&& (boolean) m.getHeaders().get("recipient"))
							.recipientFlow("'foo' == payload or 'bar' == payload or 'baz' == payload",
									f -> f.<String, String>transform(String::toUpperCase)
											.channel(MessageChannels.queue("recipientListSubFlow1Result")))
							.recipientFlow((String p) -> p.startsWith("baz"),
									f -> f.transform("Hello "::concat)
											.channel(MessageChannels.queue("recipientListSubFlow2Result")))
							.recipientFlow(new FunctionExpression<Message<?>>(m -> "bax".equals(m.getPayload())),
									f -> f.channel(MessageChannels.queue("recipientListSubFlow3Result")))
							.defaultOutputToParentFlow())
					.channel("defaultOutputChannel")
					.get();
		}

		@Bean
		public RoutingTestBean routingTestBean() {
			return new RoutingTestBean();
		}

		@Bean
		public IntegrationFlow routeMethodInvocationFlow() {
			return IntegrationFlows.from("routerMethodInput")
					.route("routingTestBean", "routeMessage")
					.get();
		}

		@Bean
		public IntegrationFlow routeMethodInvocationFlow2() {
			return IntegrationFlows.from("routerMethod2Input")
					.route(new RoutingTestBean())
					.get();
		}

		@Bean
		public IntegrationFlow routeMethodInvocationFlow3() {
			return f -> f.route((String p) -> routingTestBean().routePayload(p));
		}

		@Bean
		public IntegrationFlow routeMultiMethodInvocationFlow() {
			return IntegrationFlows.from("routerMultiInput")
					.route(String.class, p -> p.equals("foo") || p.equals("bar")
									? new String[]{ "foo", "bar" }
									: null,
							s -> s.suffix("-channel"))
					.get();
		}

		@Bean
		public PollableChannel stringsChannel() {
			return new QueueChannel();
		}

		@Bean
		public PollableChannel integersChannel() {
			return new QueueChannel();
		}

		@Bean
		public IntegrationFlow payloadTypeRouteFlow() {
			return f -> f
					.<Object, Class<?>>route(Object::getClass, m -> m
							.channelMapping(String.class, "stringsChannel")
							.channelMapping(Integer.class, "integersChannel"));
		}

		@Bean
		public IntegrationFlow exceptionTypeRouteFlow() {
			return f -> f
					.routeByException(r -> r
							.channelMapping(RuntimeException.class, "runtimeExceptionChannel")
							.channelMapping(IllegalArgumentException.class, "illegalArgumentChannel")
							.subFlowMapping(MessageHandlingException.class, sf ->
									sf.channel("messageHandlingExceptionChannel"))
							.defaultOutputChannel("exceptionRouterDefaultChannel"));
		}

		@Bean
		public PollableChannel exceptionRouterDefaultChannel() {
			return new QueueChannel();
		}

		@Bean
		public PollableChannel illegalArgumentChannel() {
			return new QueueChannel();
		}

		@Bean
		public PollableChannel runtimeExceptionChannel() {
			return new QueueChannel();
		}

		@Bean
		public PollableChannel messageHandlingExceptionChannel() {
			return new QueueChannel();
		}

		@Bean
		public IntegrationFlow routerAsNonLastFlow() {
			return f -> f
					.<String, String>route(p -> p, r ->
							r.resolutionRequired(false)
									.defaultOutputToParentFlow())
					.channel(MessageChannels.queue("routerAsNonLastDefaultOutputChannel"));
		}

		@Bean
		public IntegrationFlow recipientListOrderFlow() {
			return f -> f
					.routeToRecipients(r -> r
							.recipient(alwaysRecipient())
							.recipient("recipient2.input")
							.recipient("recipient1.input"));
		}

		@Bean
		public IntegrationFlow recipient1() {
			return f -> f
					.<AtomicReference<String>>handle((p, h) -> {
						p.set(p.get() + "World");
						return p;
					})
					.channel("recipientListOrderResult");
		}

		@Bean
		public IntegrationFlow recipient2() {
			return f -> f
					.<AtomicReference<String>>handle((p, h) -> {
						p.set(p.get() + "Hello ");
						return p;
					})
					.channel("recipientListOrderResult");
		}

		@Bean
		public PollableChannel recipientListOrderResult() {
			return new QueueChannel();
		}

		@Bean
		public QueueChannel alwaysRecipient() {
			return new QueueChannel();
		}

		@Bean
		public IntegrationFlow scatterGatherFlow() {
			return f -> f
					.scatterGather(scatterer -> scatterer
									.applySequence(true)
									.recipientFlow(m -> true, sf -> sf.handle((p, h) -> Math.random() * 10))
									.recipientFlow(m -> true, sf -> sf.handle((p, h) -> Math.random() * 10))
									.recipientFlow(m -> true, sf -> sf.handle((p, h) -> Math.random() * 10)),
							gatherer -> gatherer
									.releaseStrategy(group ->
											group.size() == 3 ||
													group.getMessages()
															.stream()
															.anyMatch(m -> (Double) m.getPayload() > 5)),
							scatterGather -> scatterGather
									.gatherTimeout(10_000));
		}

		@Bean
		public IntegrationFlow nestedScatterGatherFlow() {
			return f -> f
					.split(s -> s.delimiters(" "))
					.scatterGather(
							scatterer -> scatterer
									.recipientFlow(f1 -> f1.handle((p, h) -> p + " - flow 1"))
									.recipientFlow(f2 -> f2.handle((p, h) -> p + " - flow 2"))
									.applySequence(true),
							gatherer -> gatherer
									.outputProcessor(mg -> mg
											.getMessages()
											.stream()
											.map(m -> m.getPayload().toString())
											.collect(Collectors.joining(", "))),
							scatterGather -> scatterGather.gatherTimeout(10_000))
					.aggregate()
					.<List<String>, String>transform(source ->
							source.stream()
									.map(s -> "- " + s)
									.collect(Collectors.joining("\n")));
		}


		@Bean
		public IntegrationFlow scatterGatherAndExecutorChannelSubFlow(TaskExecutor taskExecutor) {
			return f -> f
					.scatterGather(
							scatterer -> scatterer
									.applySequence(true)
									.recipientFlow(f1 -> f1.transform(p -> "Sub-flow#1"))
									.recipientFlow(f2 -> f2
											.channel(c -> c.executor(taskExecutor))
											.transform(p -> {
												throw new RuntimeException("Sub-flow#2");
											})),
							null,
							s -> s.errorChannel("scatterGatherErrorChannel"));
		}

		@ServiceActivator(inputChannel = "scatterGatherErrorChannel")
		public Message<?> processAsyncScatterError(MessagingException payload) {
			return MessageBuilder.withPayload(payload.getCause())
					.copyHeaders(payload.getFailedMessage().getHeaders())
					.build();
		}


		@Bean
		public IntegrationFlow propagateErrorFromGatherer(TaskExecutor taskExecutor) {
			return IntegrationFlows.from(Function.class)
					.scatterGather(s -> s
									.applySequence(true)
									.recipientFlow(subFlow -> subFlow
											.channel(c -> c.executor(taskExecutor))
											.transform(p -> "foo")),
							g -> g
									.outputProcessor(group -> {
										throw new RuntimeException("intentional");
									}),
							sg -> sg.gatherTimeout(100))
					.transform(m -> "This should not be executed, results must have been propagated to Error Channel")
					.get();
		}

		@Bean
		public PollableChannel scatterGatherWireTapChannel() {
			return new QueueChannel();
		}

		@Bean
		public IntegrationFlow scatterGatherInSubFlow() {
			return flow -> flow.scatterGather(s -> s.applySequence(true)
							.recipientFlow(inflow -> inflow.wireTap(scatterGatherWireTapChannel())
									.scatterGather(s1 -> s1.applySequence(true)
													.recipientFlow(IntegrationFlowDefinition::bridge)
													.recipientFlow("sequencetest"::equals,
															IntegrationFlowDefinition::bridge),
											g -> g.outputProcessor(MessageGroup::getOne)
									).wireTap(scatterGatherWireTapChannel()).bridge()),
					g -> g.outputProcessor(MessageGroup::getOne));
		}

	}

	private static class RoutingTestBean {

		RoutingTestBean() {
			super();
		}

		public String routePayload(String name) {
			return name + "-channel";
		}

		@Router
		public String routeByHeader(@Header("targetChannel") String name) {
			return name + "-channel";
		}

		@SuppressWarnings("unused")
		public String routeMessage(Message<?> message) {
			if (message.getPayload().equals("foo")) {
				return "foo-channel";
			}
			else if (message.getPayload().equals("bar")) {
				return "bar-channel";
			}
			return null;
		}

	}

}
