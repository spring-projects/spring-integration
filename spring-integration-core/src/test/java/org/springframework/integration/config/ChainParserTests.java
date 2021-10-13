/*
 * Copyright 2002-2021 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.log.LogAccessor;
import org.springframework.expression.Expression;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.gateway.GatewayProxyFactoryBean;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.handler.MessageHandlerChain;
import org.springframework.integration.handler.ReplyRequiredException;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.json.JsonObjectMapper;
import org.springframework.integration.test.predicate.MessagePredicate;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.transformer.MessageTransformingHandler;
import org.springframework.integration.transformer.ObjectToMapTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.StringUtils;

/**
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Dave Turanski
 * @author Artem Bilan
 * @author Gunnar Hillert
 * @author Gary Russell
 */
@SpringJUnitConfig
@DirtiesContext
public class ChainParserTests {

	@Autowired
	private BeanFactory beanFactory;

	@Autowired
	@Qualifier("filterInput")
	private MessageChannel filterInput;

	@Autowired
	@Qualifier("pollableInput1")
	private MessageChannel pollableInput1;

	@Autowired
	@Qualifier("pollableInput2")
	private MessageChannel pollableInput2;

	@Autowired
	@Qualifier("headerEnricherInput")
	private MessageChannel headerEnricherInput;

	@Autowired
	@Qualifier("output")
	private PollableChannel output;

	@Autowired
	@Qualifier("replyOutput")
	private PollableChannel replyOutput;

	@Autowired
	@Qualifier("beanInput")
	private MessageChannel beanInput;

	@Autowired
	@Qualifier("aggregatorInput")
	private MessageChannel aggregatorInput;

	@Autowired
	private MessageChannel payloadTypeRouterInput;

	@Autowired
	private MessageChannel headerValueRouterInput;

	@Autowired
	private MessageChannel headerValueRouterWithMappingInput;

	@Autowired
	private MessageChannel loggingChannelAdapterChannel;

	@Autowired
	@Qualifier("logChain.handler")
	private MessageHandlerChain logChain;

	@Autowired
	private MessageChannel outboundChannelAdapterChannel;

	@Autowired
	private TestConsumer testConsumer;

	@Autowired
	@Qualifier("chainWithSendTimeout.handler")
	private MessageHandlerChain chainWithSendTimeout;

	@Autowired
	@Qualifier("claimCheckInput")
	private MessageChannel claimCheckInput;

	@Autowired
	@Qualifier("claimCheckOutput")
	private PollableChannel claimCheckOutput;

	@Autowired
	private PollableChannel strings;

	@Autowired
	private PollableChannel numbers;

	@Autowired
	private MessageChannel chainReplyRequiredChannel;

	@Autowired
	private MessageChannel chainMessageRejectedExceptionChannel;

	@Autowired
	private MessageChannel chainWithNoOutputChannel;

	@Autowired
	private MessageChannel chainWithTransformNoOutputChannel;

	public static Message<?> successMessage = MessageBuilder.withPayload("success").build();

	@Test
	public void chainWithAcceptingFilter() {
		Message<?> message = MessageBuilder.withPayload("test").build();
		this.filterInput.send(message);
		Message<?> reply = this.output.receive(1000);
		assertThat(reply).isNotNull();
		assertThat(reply.getPayload()).isEqualTo("foo");
	}

	@Test
	public void chainWithRejectingFilter() {
		Message<?> message = MessageBuilder.withPayload(123).build();
		this.filterInput.send(message);
		Message<?> reply = this.output.receive(0);
		assertThat(reply).isNull();
	}

	@Test
	public void chainWithHeaderEnricher() {
		Message<?> message = MessageBuilder.withPayload(123).build();
		this.headerEnricherInput.send(message);
		Message<?> reply = this.replyOutput.receive(1000);
		assertThat(reply).isNotNull();
		assertThat(reply.getPayload()).isEqualTo("foo");
		assertThat(new IntegrationMessageHeaderAccessor(reply).getCorrelationId()).isEqualTo("ABC");
		assertThat(reply.getHeaders().get("testValue")).isEqualTo("XYZ");
		assertThat(reply.getHeaders().get("testRef")).isEqualTo(123);
	}

	@Test
	public void chainWithPollableInput() {
		Message<?> message = MessageBuilder.withPayload("test").build();
		this.pollableInput1.send(message);
		Message<?> reply = this.output.receive(3000);
		assertThat(reply).isNotNull();
		assertThat(reply.getPayload()).isEqualTo("foo");
	}

	@Test
	public void chainWithPollerReference() {
		Message<?> message = MessageBuilder.withPayload("test").build();
		this.pollableInput2.send(message);
		Message<?> reply = this.output.receive(3000);
		assertThat(reply).isNotNull();
		assertThat(reply.getPayload()).isEqualTo("foo");
	}

	@Test
	public void chainHandlerBean() {
		Message<?> message = MessageBuilder.withPayload("test").build();
		this.beanInput.send(message);
		Message<?> reply = this.output.receive(3000);
		assertThat(reply).isNotNull();
		assertThat(reply).matches(new MessagePredicate(successMessage));
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void chainNestingAndAggregation() {
		Message<?> message = MessageBuilder.withPayload("test").setCorrelationId(1).setSequenceSize(1).build();
		this.aggregatorInput.send(message);
		Message reply = this.output.receive(3000);
		assertThat(reply).isNotNull();
		assertThat(reply.getPayload()).isEqualTo("foo");
	}

	@Test
	public void chainWithPayloadTypeRouter() {
		Message<?> message1 = MessageBuilder.withPayload("test").build();
		Message<?> message2 = MessageBuilder.withPayload(123).build();
		this.payloadTypeRouterInput.send(message1);
		this.payloadTypeRouterInput.send(message2);
		Message<?> reply1 = this.strings.receive(1000);
		Message<?> reply2 = this.numbers.receive(1000);
		assertThat(reply1).isNotNull();
		assertThat(reply2).isNotNull();
		assertThat(reply1.getPayload()).isEqualTo("test");
		assertThat(reply2.getPayload()).isEqualTo(123);
	}

	@Test // INT-2315
	public void chainWithHeaderValueRouter() {
		Message<?> message1 = MessageBuilder.withPayload("test").setHeader("routingHeader", "strings").build();
		Message<?> message2 = MessageBuilder.withPayload(123).setHeader("routingHeader", "numbers").build();
		this.headerValueRouterInput.send(message1);
		this.headerValueRouterInput.send(message2);
		Message<?> reply1 = this.strings.receive(1000);
		Message<?> reply2 = this.numbers.receive(1000);
		assertThat(reply1).isNotNull();
		assertThat(reply2).isNotNull();
		assertThat(reply1.getPayload()).isEqualTo("test");
		assertThat(reply2.getPayload()).isEqualTo(123);
	}

	@Test // INT-2315
	public void chainWithHeaderValueRouterWithMapping() {
		Message<?> message1 = MessageBuilder.withPayload("test").setHeader("routingHeader", "isString").build();
		Message<?> message2 = MessageBuilder.withPayload(123).setHeader("routingHeader", "isNumber").build();
		this.headerValueRouterWithMappingInput.send(message1);
		this.headerValueRouterWithMappingInput.send(message2);
		Message<?> reply1 = this.strings.receive(0);
		Message<?> reply2 = this.numbers.receive(0);
		assertThat(reply1).isNotNull();
		assertThat(reply2).isNotNull();
		assertThat(reply1.getPayload()).isEqualTo("test");
		assertThat(reply2.getPayload()).isEqualTo(123);
	}

	@Test // INT-1165
	public void chainWithSendTimeout() {
		long sendTimeout = TestUtils.getPropertyValue(this.chainWithSendTimeout, "messagingTemplate.sendTimeout",
				Long.class);
		assertThat(sendTimeout).isEqualTo(9876);
	}

	@Test //INT-1622
	public void chainWithClaimChecks() {
		Message<?> message = MessageBuilder.withPayload("test").build();
		this.claimCheckInput.send(message);
		Message<?> reply = this.claimCheckOutput.receive(0);
		assertThat(reply.getPayload()).isEqualTo(message.getPayload());
	}

	@Test //INT-2275
	public void chainWithOutboundChannelAdapter() {
		this.outboundChannelAdapterChannel.send(successMessage);
		assertThat(testConsumer.getLastMessage()).isSameAs(successMessage);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void chainWithLoggingChannelAdapter() {
		LogAccessor logger = mock(LogAccessor.class);
		final AtomicReference<Supplier<? extends CharSequence>> log = new AtomicReference<>();
		when(logger.isWarnEnabled()).thenReturn(true);
		doAnswer(invocation -> {
			log.set(invocation.getArgument(0));
			return null;
		}).when(logger).warn(any(Supplier.class));

		@SuppressWarnings("unchecked")
		List<MessageHandler> handlers = TestUtils.getPropertyValue(this.logChain, "handlers", List.class);
		MessageHandler handler = handlers.get(2);
		assertThat(handler instanceof LoggingHandler).isTrue();
		DirectFieldAccessor dfa = new DirectFieldAccessor(handler);
		dfa.setPropertyValue("messageLogger", logger);

		this.loggingChannelAdapterChannel.send(MessageBuilder.withPayload(new byte[]{ 116, 101, 115, 116 }).build());
		assertThat(log.get()).isNotNull();
		assertThat(log.get().get()).isEqualTo("TEST");
	}

	@Test
	public void invalidNestedChainWithLoggingChannelAdapter() {
		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext("invalidNestedChainWithOutboundChannelAdapter-context.xml",
								getClass()))
				.withCauseInstanceOf(IllegalArgumentException.class)
				.withMessageContaining("output channel was provided")
				.withMessageContaining("does not implement the MessageProducer");
	}

	@Test //INT-2605
	public void checkSmartLifecycleConfig() {
		ConfigurableApplicationContext ctx = new ClassPathXmlApplicationContext(
				"ChainParserSmartLifecycleAttributesTest.xml", this.getClass());
		AbstractEndpoint chainEndpoint = ctx.getBean("chain", AbstractEndpoint.class);
		assertThat(chainEndpoint.isAutoStartup()).isEqualTo(false);
		assertThat(chainEndpoint.getPhase()).isEqualTo(256);

		MessageHandlerChain handlerChain = ctx.getBean("chain.handler", MessageHandlerChain.class);
		assertThat(TestUtils.getPropertyValue(handlerChain, "messagingTemplate.sendTimeout")).isEqualTo(3000L);
		assertThat(TestUtils.getPropertyValue(handlerChain, "running")).isEqualTo(false);
		//INT-3108
		MessageHandler serviceActivator = ctx.getBean("chain$child.sa-within-chain.handler", MessageHandler.class);
		assertThat(TestUtils.getPropertyValue(serviceActivator, "requiresReply", Boolean.class)).isTrue();
		ctx.close();
	}

	@Test
	public void testInt2755SubComponentsIdSupport() {
		assertThat(this.beanFactory.containsBean("subComponentsIdSupport1.handler")).isTrue();
		assertThat(this.beanFactory.containsBean("filterChain$child.filterWithinChain.handler")).isTrue();
		assertThat(this.beanFactory.containsBean("filterChain$child.serviceActivatorWithinChain.handler")).isTrue();
		assertThat(this.beanFactory.containsBean("aggregatorChain.handler")).isTrue();
		assertThat(this.beanFactory.containsBean("aggregatorChain$child.aggregatorWithinChain.handler")).isTrue();
		assertThat(this.beanFactory.containsBean("aggregatorChain$child.nestedChain.handler")).isTrue();
		assertThat(this.beanFactory
				.containsBean("aggregatorChain$child.nestedChain$child.filterWithinNestedChain.handler")).isTrue();
		assertThat(this.beanFactory.containsBean("aggregatorChain$child.nestedChain$child.doubleNestedChain.handler"))
				.isTrue();
		assertThat(this.beanFactory
				.containsBean("aggregatorChain$child.nestedChain$child.doubleNestedChain$child" +
						".filterWithinDoubleNestedChain.handler"))
				.isTrue();
		assertThat(this.beanFactory.containsBean("aggregatorChain2.handler")).isTrue();
		assertThat(this.beanFactory.containsBean("aggregatorChain2$child.aggregatorWithinChain.handler")).isTrue();
		assertThat(this.beanFactory.containsBean("aggregatorChain2$child.nestedChain.handler")).isTrue();
		assertThat(this.beanFactory
				.containsBean("aggregatorChain2$child.nestedChain$child.filterWithinNestedChain.handler")).isTrue();
		assertThat(this.beanFactory.containsBean("payloadTypeRouterChain$child.payloadTypeRouterWithinChain.handler"))
				.isTrue();
		assertThat(this.beanFactory.containsBean("headerValueRouterChain$child.headerValueRouterWithinChain.handler"))
				.isTrue();
		assertThat(this.beanFactory.containsBean("chainWithClaimChecks$child.claimCheckInWithinChain.handler"))
				.isTrue();
		assertThat(this.beanFactory.containsBean("chainWithClaimChecks$child.claimCheckOutWithinChain.handler"))
				.isTrue();
		assertThat(this.beanFactory.containsBean("outboundChain$child.outboundChannelAdapterWithinChain.handler"))
				.isTrue();
		assertThat(this.beanFactory.containsBean("logChain$child.transformerWithinChain.handler")).isTrue();
		assertThat(this.beanFactory.containsBean("logChain$child.loggingChannelAdapterWithinChain.handler")).isTrue();
		assertThat(this.beanFactory.containsBean("subComponentsIdSupport1$child.splitterWithinChain.handler")).isTrue();
		assertThat(this.beanFactory.containsBean("subComponentsIdSupport1$child.resequencerWithinChain.handler"))
				.isTrue();
		assertThat(this.beanFactory.containsBean("subComponentsIdSupport1$child.enricherWithinChain.handler")).isTrue();
		assertThat(this.beanFactory.containsBean("subComponentsIdSupport1$child.headerFilterWithinChain.handler"))
				.isTrue();
		assertThat(this.beanFactory
				.containsBean("subComponentsIdSupport1$child.payloadSerializingTransformerWithinChain.handler"))
				.isTrue();
		assertThat(this.beanFactory
				.containsBean("subComponentsIdSupport1$child.payloadDeserializingTransformerWithinChain.handler"))
				.isTrue();
		assertThat(this.beanFactory.containsBean("subComponentsIdSupport1$child.gatewayWithinChain.handler")).isTrue();
		//INT-3117
		GatewayProxyFactoryBean gatewayProxyFactoryBean = this.beanFactory.getBean("&subComponentsIdSupport1$child" +
						".gatewayWithinChain.handler",
				GatewayProxyFactoryBean.class);
		assertThat(TestUtils.getPropertyValue(gatewayProxyFactoryBean, "defaultRequestChannelName"))
				.isEqualTo("strings");
		assertThat(TestUtils.getPropertyValue(gatewayProxyFactoryBean, "defaultReplyChannelName")).isEqualTo(
				"numbers");
		assertThat(TestUtils
				.getPropertyValue(gatewayProxyFactoryBean, "defaultRequestTimeout", Expression.class).getValue())
				.isEqualTo(1000L);
		assertThat(TestUtils
				.getPropertyValue(gatewayProxyFactoryBean, "defaultReplyTimeout", Expression.class).getValue())
				.isEqualTo(100L);

		assertThat(this.beanFactory
				.containsBean("subComponentsIdSupport1$child.objectToStringTransformerWithinChain.handler")).isTrue();
		assertThat(this.beanFactory
				.containsBean("subComponentsIdSupport1$child.objectToMapTransformerWithinChain.handler")).isTrue();

		Object transformerHandler = this.beanFactory.getBean(
				"subComponentsIdSupport1$child.objectToMapTransformerWithinChain.handler");

		Object transformer = TestUtils.getPropertyValue(transformerHandler, "transformer");

		assertThat(transformer).isInstanceOf(ObjectToMapTransformer.class);
		assertThat(TestUtils.getPropertyValue(transformer, "shouldFlattenKeys", Boolean.class)).isFalse();
		assertThat(TestUtils.getPropertyValue(transformer, "jsonObjectMapper"))
				.isSameAs(this.beanFactory.getBean(JsonObjectMapper.class));

		assertThat(this.beanFactory
				.containsBean("subComponentsIdSupport1$child.mapToObjectTransformerWithinChain.handler")).isTrue();
		assertThat(this.beanFactory.containsBean("subComponentsIdSupport1$child.controlBusWithinChain.handler"))
				.isTrue();
		assertThat(this.beanFactory.containsBean("subComponentsIdSupport1$child.routerWithinChain.handler")).isTrue();
		assertThat(this.beanFactory
				.containsBean("exceptionTypeRouterChain$child.exceptionTypeRouterWithinChain.handler")).isTrue();
		assertThat(this.beanFactory
				.containsBean("recipientListRouterChain$child.recipientListRouterWithinChain.handler")).isTrue();

		MessageHandlerChain chain = this.beanFactory.getBean("headerEnricherChain.handler", MessageHandlerChain.class);
		List<?> handlers = TestUtils.getPropertyValue(chain, "handlers", List.class);

		assertThat(handlers.get(0) instanceof MessageTransformingHandler).isTrue();
		assertThat(TestUtils.getPropertyValue(handlers.get(0), "componentName"))
				.isEqualTo("headerEnricherChain$child.headerEnricherWithinChain");
		assertThat(TestUtils.getPropertyValue(handlers.get(0), "beanName"))
				.isEqualTo("headerEnricherChain$child.headerEnricherWithinChain.handler");
		assertThat(this.beanFactory.containsBean("headerEnricherChain$child.headerEnricherWithinChain.handler"))
				.isTrue();

		assertThat(handlers.get(1) instanceof ServiceActivatingHandler).isTrue();
		assertThat(TestUtils.getPropertyValue(handlers.get(1), "componentName"))
				.isEqualTo("headerEnricherChain$child#1");
		assertThat(TestUtils.getPropertyValue(handlers.get(1), "beanName"))
				.isEqualTo("headerEnricherChain$child#1.handler");
		assertThat(this.beanFactory.containsBean("headerEnricherChain$child#1.handler")).isFalse();

	}

	@Test
	public void testInt2755SubComponentException() {
		GenericMessage<String> testMessage = new GenericMessage<>("test");
		try {
			this.chainReplyRequiredChannel.send(testMessage);
			fail("Expected ReplyRequiredException");
		}
		catch (Exception e) {
			assertThat(e instanceof ReplyRequiredException).isTrue();
			assertThat(e.getMessage().contains("'chainReplyRequired$child.transformerReplyRequired'")).isTrue();
		}

		try {
			this.chainMessageRejectedExceptionChannel.send(testMessage);
			fail("Expected MessageRejectedException");
		}
		catch (Exception e) {
			assertThat(e instanceof MessageRejectedException).isTrue();
			assertThat(e.getMessage().contains("chainMessageRejectedException$child.filterMessageRejectedException"))
					.isTrue();
		}

	}

	@Test
	public void testChainWithNoOutput() {
		QueueChannel replyChannel = new QueueChannel();
		Message<String> message = MessageBuilder.withPayload("foo").setHeader("myReplyChannel", replyChannel).build();
		this.chainWithNoOutputChannel.send(message);
		Message<?> receive = replyChannel.receive(10000);
		assertThat(receive).isNotNull();

		message = MessageBuilder.withPayload("foo").setReplyChannel(replyChannel).build();
		Message<String> message2 = MessageBuilder.withPayload("bar").setHeader("myMessage", message).build();
		this.chainWithTransformNoOutputChannel.send(message2);
		receive = replyChannel.receive(10000);
		assertThat(receive).isNotNull();
	}

	public static class StubHandler extends AbstractReplyProducingMessageHandler {

		@Override
		protected Object handleRequestMessage(Message<?> requestMessage) {
			return successMessage;
		}

	}

	public static class StubAggregator {

		public String aggregate(List<String> strings) {
			return StringUtils.collectionToCommaDelimitedString(strings);
		}

	}

	public static class FooPojo {


		private String bar;

		public String getBar() {
			return bar;
		}

		public void setBar(String bar) {
			this.bar = bar;
		}

	}

}
