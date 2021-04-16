/*
 * Copyright 2014-2021 the original author or authors.
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

package org.springframework.integration.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.Lifecycle;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.expression.EnvironmentAccessor;
import org.springframework.context.expression.MapAccessor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.log.LogAccessor;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.ReflectivePropertyAccessor;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.annotation.Aggregator;
import org.springframework.integration.annotation.BridgeFrom;
import org.springframework.integration.annotation.BridgeTo;
import org.springframework.integration.annotation.EndpointId;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.GatewayHeader;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.Publisher;
import org.springframework.integration.annotation.Reactive;
import org.springframework.integration.annotation.Role;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.annotation.UseSpelInvoker;
import org.springframework.integration.aop.PublisherAnnotationBeanPostProcessor;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.channel.interceptor.WireTap;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.config.EnableMessageHistory;
import org.springframework.integration.config.EnablePublisher;
import org.springframework.integration.config.ExpressionControlBusFactoryBean;
import org.springframework.integration.config.GlobalChannelInterceptor;
import org.springframework.integration.config.IntegrationConverter;
import org.springframework.integration.config.SpelFunctionFactoryBean;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.core.Pausable;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.MethodInvokingMessageSource;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.endpoint.ReactiveStreamsConsumer;
import org.springframework.integration.expression.SpelPropertyAccessorRegistrar;
import org.springframework.integration.gateway.GatewayProxyFactoryBean;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.integration.handler.advice.ExpressionEvaluatingRequestHandlerAdvice;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.history.MessageHistoryConfigurer;
import org.springframework.integration.json.JsonPropertyAccessor;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.MutableMessageBuilder;
import org.springframework.integration.support.SmartLifecycleRoleController;
import org.springframework.integration.test.util.OnlyOnceTrigger;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.util.NoBeansOverrideAnnotationConfigContextLoader;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Component;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.ClassUtils;
import org.springframework.util.MultiValueMap;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Artem Bilan
 * @author Gary Russell
 * @author Michael Wiles
 *
 * @since 4.0
 */
@ContextConfiguration(loader = NoBeansOverrideAnnotationConfigContextLoader.class)
@SpringJUnitConfig(classes = { EnableIntegrationTests.ContextConfiguration.class,
		EnableIntegrationTests.ContextConfiguration2.class })
@DirtiesContext
public class EnableIntegrationTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private ContextConfiguration2 contextConfiguration2;

	@Autowired
	private PollableChannel input;

	@Autowired
	private SmartLifecycleRoleController roleController;

	@Autowired
	@Qualifier("annotationTestService.handle.serviceActivator")
	private PollingConsumer serviceActivatorEndpoint;

	@Autowired
	@Qualifier("annotationTestService.handle1.serviceActivator")
	private PollingConsumer serviceActivatorEndpoint1;

	@Autowired
	@Qualifier("annotationTestService.handle2.serviceActivator")
	private PollingConsumer serviceActivatorEndpoint2;

	@Autowired
	@Qualifier("annotationTestService.handle3.serviceActivator")
	private PollingConsumer serviceActivatorEndpoint3;

	@Autowired
	@Qualifier("annotationTestService.handle4.serviceActivator")
	private PollingConsumer serviceActivatorEndpoint4;

	@Autowired
	@Qualifier("annotationTestService.transform.transformer")
	private PollingConsumer transformer;

	@Autowired
	@Qualifier("annotationTestService")
	private Lifecycle annotationTestService;

	@Autowired
	private Trigger myTrigger;

	@Autowired
	private QueueChannel output;

	@Autowired
	private QueueChannel wireTapFromOutput;

	@Autowired
	private PollableChannel publishedChannel;

	@Autowired
	private PollableChannel wireTapChannel;

	@Autowired
	private MessageHistoryConfigurer configurer;

	@Autowired
	private TestGateway testGateway;

	@Autowired
	private CountDownLatch asyncAnnotationProcessLatch;

	@Autowired
	private AtomicReference<Thread> asyncAnnotationProcessThread;

	@Autowired
	private TestGateway2 testGateway2;

	@Autowired
	private TestChannelInterceptor testChannelInterceptor;

	@Autowired
	private AtomicInteger fbInterceptorCounter;

	@Autowired
	private MessageChannel numberChannel;

	@Autowired
	private TestConverter testConverter;

	@Autowired
	private MessageChannel bytesChannel;

	@Autowired
	private PollableChannel counterChannel;

	@Autowired
	private PollableChannel messageChannel;

	@Autowired
	private PollableChannel fooChannel;

	@Autowired
	private MessageChannel bridgeInput;

	@Autowired
	private PollableChannel bridgeOutput;

	@Autowired
	private MessageChannel pollableBridgeInput;

	@Autowired
	private PollableChannel pollableBridgeOutput;

	@Autowired
	private MessageChannel metaBridgeInput;

	@Autowired
	private PollableChannel metaBridgeOutput;

	@Autowired
	private MessageChannel bridgeToInput;

	@Autowired
	private AbstractEndpoint reactiveBridge;

	@Autowired
	private PollableChannel bridgeToOutput;

	@Autowired
	private PollableChannel pollableBridgeToInput;

	@Autowired
	private MessageChannel myBridgeToInput;

	@Autowired
	private MessageChannel controlBusChannel;

	@Autowired
	private CountDownLatch inputReceiveLatch;

	@Autowired
	@Qualifier("enableIntegrationTests.ContextConfiguration2.sendAsyncHandler.serviceActivator")
	private AbstractEndpoint sendAsyncHandler;

	@Autowired
	@Qualifier("enableIntegrationTests.ChildConfiguration.autoCreatedChannelMessageSource.inboundChannelAdapter")
	private Lifecycle autoCreatedChannelMessageSourceAdapter;

	@Autowired
	private PublisherAnnotationBeanPostProcessor publisherAnnotationBeanPostProcessor;

	@Autowired
	@Qualifier("controlBusEndpoint")
	private EventDrivenConsumer controlBusEndpoint;

	@Test
	public void testAnnotatedServiceActivator() throws Exception {
		this.serviceActivatorEndpoint.start();
		assertThat(this.inputReceiveLatch.await(10, TimeUnit.SECONDS)).isTrue();

		assertThat(TestUtils.getPropertyValue(this.serviceActivatorEndpoint, "maxMessagesPerPoll")).isEqualTo(10L);

		Trigger trigger = TestUtils.getPropertyValue(this.serviceActivatorEndpoint, "trigger", Trigger.class);
		assertThat(trigger).isInstanceOf(PeriodicTrigger.class);
		assertThat(TestUtils.getPropertyValue(trigger, "period")).isEqualTo(100L);
		assertThat(TestUtils.getPropertyValue(trigger, "fixedRate", Boolean.class)).isFalse();

		assertThat(this.annotationTestService.isRunning()).isTrue();
		LogAccessor logger = spy(TestUtils.getPropertyValue(this.serviceActivatorEndpoint, "logger", LogAccessor.class));
		when(logger.isDebugEnabled()).thenReturn(true);
		final CountDownLatch pollerInterruptedLatch = new CountDownLatch(1);
		doAnswer(invocation -> {
			pollerInterruptedLatch.countDown();
			invocation.callRealMethod();
			return null;
		}).when(logger).debug("Received no Message during the poll, returning 'false'");
		new DirectFieldAccessor(this.serviceActivatorEndpoint).setPropertyValue("logger", logger);

		this.serviceActivatorEndpoint.stop();
		assertThat(this.annotationTestService.isRunning()).isFalse();

		// wait until the service activator's poller is interrupted.
		assertThat(pollerInterruptedLatch.await(10, TimeUnit.SECONDS)).isTrue();
		this.serviceActivatorEndpoint.start();
		assertThat(this.annotationTestService.isRunning()).isTrue();

		trigger = TestUtils.getPropertyValue(this.serviceActivatorEndpoint1, "trigger", Trigger.class);
		assertThat(trigger).isInstanceOf(PeriodicTrigger.class);
		assertThat(TestUtils.getPropertyValue(trigger, "period")).isEqualTo(100L);
		assertThat(TestUtils.getPropertyValue(trigger, "fixedRate", Boolean.class)).isTrue();

		trigger = TestUtils.getPropertyValue(this.serviceActivatorEndpoint2, "trigger", Trigger.class);
		assertThat(trigger).isInstanceOf(CronTrigger.class);
		assertThat(TestUtils.getPropertyValue(trigger, "expression.expression")).isEqualTo("0 5 7 * * *");

		trigger = TestUtils.getPropertyValue(this.serviceActivatorEndpoint3, "trigger", Trigger.class);
		assertThat(trigger).isInstanceOf(PeriodicTrigger.class);
		assertThat(TestUtils.getPropertyValue(trigger, "period")).isEqualTo(11L);
		assertThat(TestUtils.getPropertyValue(trigger, "fixedRate", Boolean.class)).isFalse();

		trigger = TestUtils.getPropertyValue(this.serviceActivatorEndpoint4, "trigger", Trigger.class);
		assertThat(trigger).isInstanceOf(PeriodicTrigger.class);
		assertThat(TestUtils.getPropertyValue(trigger, "period")).isEqualTo(1000L);
		assertThat(TestUtils.getPropertyValue(trigger, "fixedRate", Boolean.class)).isFalse();
		assertThat(trigger).isSameAs(this.myTrigger);

		trigger = TestUtils.getPropertyValue(this.transformer, "trigger", Trigger.class);
		assertThat(trigger).isInstanceOf(PeriodicTrigger.class);
		assertThat(TestUtils.getPropertyValue(trigger, "period")).isEqualTo(10L);
		assertThat(TestUtils.getPropertyValue(trigger, "fixedRate", Boolean.class)).isFalse();

		this.input.send(MessageBuilder.withPayload("Foo").build());

		Message<?> interceptedMessage = this.wireTapChannel.receive(10_000);
		assertThat(interceptedMessage).isNotNull();
		assertThat(interceptedMessage.getPayload()).isEqualTo("Foo");

		Message<?> receive = this.output.receive(10_000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("FOO");

		receive = this.wireTapFromOutput.receive(10_000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("FOO");

		MessageHistory messageHistory = receive.getHeaders().get(MessageHistory.HEADER_NAME, MessageHistory.class);
		assertThat(messageHistory).isNotNull();
		String messageHistoryString = messageHistory.toString();
		assertThat(messageHistoryString).contains("input");
		assertThat(messageHistoryString).contains("annotationTestService.handle.serviceActivator");
		assertThat(messageHistoryString).doesNotContain("output");

		receive = this.publishedChannel.receive(10_000);

		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("foo");

		messageHistory = receive.getHeaders().get(MessageHistory.HEADER_NAME, MessageHistory.class);
		assertThat(messageHistory).isNotNull();
		messageHistoryString = messageHistory.toString();
		assertThat(messageHistoryString).doesNotContain("input");
		assertThat(messageHistoryString).doesNotContain("output");
		assertThat(messageHistoryString).contains("publishedChannel");

		assertThat(this.wireTapChannel.receive(0)).isNull();
		assertThat(this.testChannelInterceptor.getInvoked()).isGreaterThan(0);
		assertThat(this.fbInterceptorCounter.get()).isGreaterThan(0);

		assertThat(this.context.containsBean("annotationTestService.count.inboundChannelAdapter.source")).isTrue();
		Object messageSource = this.context.getBean("annotationTestService.count.inboundChannelAdapter.source");
		assertThat(messageSource).isInstanceOf(MethodInvokingMessageSource.class);

		assertThat(this.counterChannel.receive(10)).isNull();

		SmartLifecycle countSA = this.context.getBean("annotationTestService.count.inboundChannelAdapter",
				SmartLifecycle.class);
		assertThat(countSA.isAutoStartup()).isFalse();
		assertThat(countSA.getPhase()).isEqualTo(23);
		countSA.start();

		for (int i = 0; i < 10; i++) {
			Message<?> message = this.counterChannel.receive(10_000);
			assertThat(message).isNotNull();
			assertThat(message.getPayload()).isEqualTo(i + 1);
		}

		Message<?> message = this.fooChannel.receive(10_000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("foo");
		message = this.fooChannel.receive(10_000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("foo");
		assertThat(this.fooChannel.receive(10)).isNull();

		message = this.messageChannel.receive(10_000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("bar");
		assertThat(message.getHeaders().containsKey("foo")).isTrue();
		assertThat(message.getHeaders().get("foo")).isEqualTo("FOO");

		MessagingTemplate messagingTemplate = new MessagingTemplate(this.controlBusChannel);
		assertThat(messagingTemplate.convertSendAndReceive("@pausable.isRunning()", Boolean.class)).isEqualTo(false);
		this.controlBusChannel.send(new GenericMessage<>("@pausable.start()"));
		assertThat(messagingTemplate.convertSendAndReceive("@pausable.isRunning()", Boolean.class)).isEqualTo(true);
		this.controlBusChannel.send(new GenericMessage<>("@pausable.stop()"));
		assertThat(messagingTemplate.convertSendAndReceive("@pausable.isRunning()", Boolean.class)).isEqualTo(false);
		this.controlBusChannel.send(new GenericMessage<>("@pausable.pause()"));
		Object pausable = this.context.getBean("pausable");
		assertThat(TestUtils.getPropertyValue(pausable, "paused", Boolean.class)).isTrue();
		this.controlBusChannel.send(new GenericMessage<>("@pausable.resume()"));
		assertThat(TestUtils.getPropertyValue(pausable, "paused", Boolean.class)).isFalse();

		Map<String, ServiceActivatingHandler> beansOfType =
				this.context.getBeansOfType(ServiceActivatingHandler.class);

		assertThat(beansOfType.keySet()
				.contains("enableIntegrationTests.ContextConfiguration2.controlBus.serviceActivator.handler"))
				.isFalse();

		assertThat(this.controlBusEndpoint.getBeanName())
				.isEqualTo(this.contextConfiguration2.controlBusEndpoint.getBeanName());

		assertThat(this.controlBusEndpoint.getHandler())
				.isSameAs(this.contextConfiguration2.controlBusEndpoint.getHandler());
	}

	@Test
	@DirtiesContext
	public void testChangePatterns() {
		assertThatIllegalStateException()
				.isThrownBy(() -> this.configurer.setComponentNamePatterns(new String[]{ "*" }))
				.withMessageContaining("cannot be changed");
		this.configurer.stop();
		this.configurer.setComponentNamePatterns(new String[]{ "*" });
		assertThat(TestUtils.getPropertyValue(this.configurer, "componentNamePatterns", String[].class)[0])
				.isEqualTo("*");
	}

	@Test
	public void testMessagingGateway() throws InterruptedException {
		String payload = "bar";
		String result = this.testGateway.echo(payload);
		assertThat(result.substring(0, payload.length())).isEqualTo(payload.toUpperCase());
		assertThat(result).contains("InvocableHandlerMethod");
		assertThat(result).doesNotContain("SpelExpression");
		result = this.testGateway2.echo2(payload);
		assertThat(result).isNotNull();
		assertThat(result.substring(0, payload.length() + 1)).isEqualTo(payload.toUpperCase() + "2");
		assertThat(result).doesNotContain("InvocableHandlerMethod");
		assertThat(result).contains("SpelExpression");
		assertThat(result).contains("CompoundExpression.getValueInternal");
		assertThat(this.testGateway2.echo2("baz")).isNotNull();
		// third one should be compiled, but it's not since SF-5.0.2 - proxies aren't compilable SpELs any more
		result = this.testGateway2.echo2("baz");
		assertThat(result).isNotNull();
		assertThat(result.substring(0, 4)).isEqualTo("BAZ2");
		assertThat(result).doesNotContain("InvocableHandlerMethod");
		assertThat(result).contains("SpelExpression");
		this.testGateway.sendAsync("foo");
		assertThat(this.asyncAnnotationProcessLatch.await(1, TimeUnit.SECONDS)).isTrue();
		assertThat(this.asyncAnnotationProcessThread.get()).isNotSameAs(Thread.currentThread());

		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
				.isThrownBy(() -> this.context.getBean(ConditionalGateway.class));
	}

	/**
	 * Just creates an interim context to confirm that the
	 * DefaultConfiguringBeanFactoryPostProcessor does not fail when there is an extra
	 * application context in the hierarchy.
	 */
	@Test
	public void testDoubleParentChildAnnotationConfiguration() {

		assertThat(this.context.containsBeanDefinition(IntegrationContextUtils.NULL_CHANNEL_BEAN_NAME)).isTrue();

		AnnotationConfigApplicationContext parent;
		parent = new AnnotationConfigApplicationContext();
		parent.register(ChildConfiguration.class);
		parent.setParent(this.context);
		parent.refresh();

		assertThat(parent.containsBeanDefinition(IntegrationContextUtils.NULL_CHANNEL_BEAN_NAME)).isFalse();

		AnnotationConfigApplicationContext child;
		child = new AnnotationConfigApplicationContext();
		child.register(ChildConfiguration.class);
		child.setParent(parent);
		child.refresh();

		assertThat(child.containsBeanDefinition(IntegrationContextUtils.NULL_CHANNEL_BEAN_NAME)).isFalse();

		parent.close();
		child.close();
	}

	@Test
	public void testParentChildAnnotationConfiguration() {
		AnnotationConfigApplicationContext child = new AnnotationConfigApplicationContext();
		child.register(ChildConfiguration.class);
		child.setParent(this.context);
		child.refresh();
		AbstractMessageChannel foo = child.getBean("foo", AbstractMessageChannel.class);
		ChannelInterceptor baz = child.getBean("baz", ChannelInterceptor.class);
		assertThat(foo.getInterceptors().contains(baz)).isTrue();
		assertThat(this.output.getInterceptors().contains(baz)).isFalse();
		child.close();
	}

	@Test
	public void testParentChildAnnotationConfigurationFromAnotherPackage() {
		AnnotationConfigApplicationContext child = new AnnotationConfigApplicationContext();
		child.register(org.springframework.integration.configuration2.ChildConfiguration.class);
		child.setParent(this.context);
		child.refresh();
		AbstractMessageChannel foo = child.getBean("foo", AbstractMessageChannel.class);
		ChannelInterceptor baz = child.getBean("baz", ChannelInterceptor.class);
		assertThat(foo.getInterceptors().contains(baz)).isTrue();
		assertThat(this.output.getInterceptors().contains(baz)).isFalse();
		child.close();
	}

	@Test
	public void testIntegrationConverter() {
		this.numberChannel.send(new GenericMessage<>(10));
		this.numberChannel.send(new GenericMessage<>(true));
		assertThat(this.testConverter.getInvoked()).isGreaterThan(0);

		assertThat(this.bytesChannel.send(new GenericMessage<>("foo".getBytes()))).isTrue();
		assertThat(this.bytesChannel.send(new GenericMessage<>(MutableMessageBuilder.withPayload("").build())))
				.isTrue();

	}

	@Test
	public void testMetaAnnotations() {

		assertThat(this.context.getBeanNamesForType(GatewayProxyFactoryBean.class).length).isEqualTo(2);

		PollingConsumer consumer = this.context.getBean("annotationTestService.annCount.serviceActivator",
				PollingConsumer.class);
		assertThat(TestUtils.getPropertyValue(consumer, "autoStartup", Boolean.class)).isFalse();
		assertThat(TestUtils.getPropertyValue(consumer, "phase")).isEqualTo(23);
		assertThat(TestUtils.getPropertyValue(consumer, "inputChannel")).isSameAs(context.getBean("annInput"));
		assertThat(TestUtils.getPropertyValue(consumer, "handler.outputChannelName")).isEqualTo("annOutput");
		assertThat(TestUtils.getPropertyValue(consumer,
				"handler.adviceChain", List.class).get(0)).isSameAs(context.getBean("annAdvice"));
		assertThat(TestUtils.getPropertyValue(consumer, "trigger.period")).isEqualTo(1000L);

		consumer = this.context.getBean("annotationTestService.annCount1.serviceActivator",
				PollingConsumer.class);
		consumer.stop();
		assertThat(TestUtils.getPropertyValue(consumer, "autoStartup", Boolean.class)).isTrue();
		assertThat(TestUtils.getPropertyValue(consumer, "phase")).isEqualTo(23);
		assertThat(TestUtils.getPropertyValue(consumer, "inputChannel")).isSameAs(context.getBean("annInput1"));
		assertThat(TestUtils.getPropertyValue(consumer, "handler.outputChannel.beanName")).isEqualTo("annOutput");
		assertThat(TestUtils.getPropertyValue(consumer,
				"handler.adviceChain", List.class).get(0)).isSameAs(context.getBean("annAdvice1"));
		assertThat(TestUtils.getPropertyValue(consumer, "trigger.period")).isEqualTo(2000L);

		consumer = this.context.getBean("annotationTestService.annCount2.serviceActivator",
				PollingConsumer.class);
		assertThat(TestUtils.getPropertyValue(consumer, "autoStartup", Boolean.class)).isFalse();
		assertThat(TestUtils.getPropertyValue(consumer, "phase")).isEqualTo(23);
		assertThat(TestUtils.getPropertyValue(consumer, "inputChannel")).isSameAs(context.getBean("annInput"));
		assertThat(TestUtils.getPropertyValue(consumer, "handler.outputChannelName")).isEqualTo("annOutput");
		assertThat(TestUtils.getPropertyValue(consumer,
				"handler.adviceChain", List.class).get(0)).isSameAs(context.getBean("annAdvice"));
		assertThat(TestUtils.getPropertyValue(consumer, "trigger.period")).isEqualTo(1000L);

		// Tests when the channel is in a "middle" annotation
		consumer = this.context.getBean("annotationTestService.annCount5.serviceActivator", PollingConsumer.class);
		assertThat(TestUtils.getPropertyValue(consumer, "autoStartup", Boolean.class)).isFalse();
		assertThat(TestUtils.getPropertyValue(consumer, "phase")).isEqualTo(23);
		assertThat(TestUtils.getPropertyValue(consumer, "inputChannel")).isSameAs(context.getBean("annInput3"));
		assertThat(TestUtils.getPropertyValue(consumer, "handler.outputChannelName")).isEqualTo("annOutput");
		assertThat(TestUtils.getPropertyValue(consumer,
				"handler.adviceChain", List.class).get(0)).isSameAs(context.getBean("annAdvice"));
		assertThat(TestUtils.getPropertyValue(consumer, "trigger.period")).isEqualTo(1000L);

		consumer = this.context.getBean("annotationTestService.annAgg1.aggregator", PollingConsumer.class);
		assertThat(TestUtils.getPropertyValue(consumer, "autoStartup", Boolean.class)).isFalse();
		assertThat(TestUtils.getPropertyValue(consumer, "phase")).isEqualTo(23);
		assertThat(TestUtils.getPropertyValue(consumer, "inputChannel")).isSameAs(context.getBean("annInput"));
		assertThat(TestUtils.getPropertyValue(consumer, "handler.outputChannelName")).isEqualTo("annOutput");
		assertThat(TestUtils.getPropertyValue(consumer, "handler.discardChannelName")).isEqualTo("annOutput");
		assertThat(TestUtils.getPropertyValue(consumer, "trigger.period")).isEqualTo(1000L);
		assertThat(TestUtils.getPropertyValue(consumer, "handler.messagingTemplate.sendTimeout")).isEqualTo(-1L);
		assertThat(TestUtils.getPropertyValue(consumer, "handler.sendPartialResultOnExpiry", Boolean.class)).isFalse();

		consumer = this.context.getBean("annotationTestService.annAgg2.aggregator", PollingConsumer.class);
		assertThat(TestUtils.getPropertyValue(consumer, "autoStartup", Boolean.class)).isFalse();
		assertThat(TestUtils.getPropertyValue(consumer, "phase")).isEqualTo(23);
		assertThat(TestUtils.getPropertyValue(consumer, "inputChannel")).isSameAs(context.getBean("annInput"));
		assertThat(TestUtils.getPropertyValue(consumer, "handler.outputChannelName")).isEqualTo("annOutput");
		assertThat(TestUtils.getPropertyValue(consumer, "handler.discardChannelName")).isEqualTo("annOutput");
		assertThat(TestUtils.getPropertyValue(consumer, "trigger.period")).isEqualTo(1000L);
		assertThat(TestUtils.getPropertyValue(consumer, "handler.messagingTemplate.sendTimeout")).isEqualTo(75L);
		assertThat(TestUtils.getPropertyValue(consumer, "handler.sendPartialResultOnExpiry", Boolean.class)).isTrue();
	}

	@Test
	public void testBridgeAnnotations() {
		GenericMessage<?> testMessage = new GenericMessage<Object>("foo");
		this.bridgeInput.send(testMessage);
		Message<?> receive = this.bridgeOutput.receive(10_000);
		assertThat(receive).isNotNull();
		assertThat(testMessage).isSameAs(receive);
		assertThat(this.bridgeOutput.receive(10)).isNull();

		this.pollableBridgeInput.send(testMessage);
		receive = this.pollableBridgeOutput.receive(10_000);
		assertThat(receive).isNotNull();
		assertThat(testMessage).isSameAs(receive);
		assertThat(this.pollableBridgeOutput.receive(10)).isNull();


		assertThatExceptionOfType(MessageDeliveryException.class)
				.isThrownBy(() -> this.metaBridgeInput.send(testMessage))
				.withMessageContaining("Dispatcher has no subscribers");

		this.context.getBean("enableIntegrationTests.ContextConfiguration.metaBridgeOutput.bridgeFrom",
				Lifecycle.class).start();

		this.metaBridgeInput.send(testMessage);
		receive = this.metaBridgeOutput.receive(10_000);
		assertThat(receive).isNotNull();
		assertThat(testMessage).isSameAs(receive);
		assertThat(this.metaBridgeOutput.receive(10)).isNull();

		assertThat(this.reactiveBridge).isInstanceOf(ReactiveStreamsConsumer.class);
		this.bridgeToInput.send(testMessage);
		receive = this.bridgeToOutput.receive(10_000);
		assertThat(receive).isNotNull();
		assertThat(testMessage).isSameAs(receive);
		assertThat(this.bridgeToOutput.receive(10)).isNull();

		PollableChannel replyChannel = new QueueChannel();
		Message<?> bridgeMessage = MessageBuilder.fromMessage(testMessage).setReplyChannel(replyChannel).build();
		this.pollableBridgeToInput.send(bridgeMessage);
		receive = replyChannel.receive(10_000);
		assertThat(receive).isNotNull();
		assertThat(bridgeMessage).isSameAs(receive);
		assertThat(replyChannel.receive(10)).isNull();

		assertThatExceptionOfType(MessageDeliveryException.class)
				.isThrownBy(() -> this.myBridgeToInput.send(testMessage))
				.withMessageContaining("Dispatcher has no subscribers");

		this.context.getBean("enableIntegrationTests.ContextConfiguration.myBridgeToInput.bridgeTo",
				Lifecycle.class).start();

		this.myBridgeToInput.send(bridgeMessage);
		receive = replyChannel.receive(10_000);
		assertThat(receive).isNotNull();
		assertThat(bridgeMessage).isSameAs(receive);
		assertThat(replyChannel.receive(10)).isNull();
	}

	@Test
	public void testMonoGateway() throws Exception {

		final AtomicReference<List<Integer>> ref = new AtomicReference<>();
		final CountDownLatch consumeLatch = new CountDownLatch(1);

		Flux.just("1", "2", "3", "4", "5")
				.map(Integer::parseInt)
				.flatMap(this.testGateway::multiply)
				.collectList()
				.subscribe(integers -> {
					ref.set(integers);
					consumeLatch.countDown();
				});


		assertThat(consumeLatch.await(0, TimeUnit.SECONDS)).isTrue(); // runs on same thread

		List<Integer> integers = ref.get();
		assertThat(integers.size()).isEqualTo(5);

		assertThat(integers).containsExactly(2, 4, 6, 8, 10);
	}

	@Test
	@DirtiesContext
	public void testRoles() {
		assertThat(this.roleController.getRoles()).contains("foo", "bar");
		assertThat(this.roleController.allEndpointsRunning("foo")).isFalse();
		assertThat(this.roleController.noEndpointsRunning("foo")).isFalse();
		assertThat(this.roleController.allEndpointsRunning("bar")).isTrue();
		assertThat(this.roleController.noEndpointsRunning("bar")).isFalse();
		Map<String, Boolean> state = this.roleController.getEndpointsRunningStatus("foo");
		assertThat(state.get("annotationTestService.handle.serviceActivator")).isEqualTo(Boolean.FALSE);
		assertThat(state.get("enableIntegrationTests.ContextConfiguration2.sendAsyncHandler.serviceActivator"))
				.isEqualTo(Boolean.TRUE);
		this.roleController.startLifecyclesInRole("foo");
		assertThat(this.roleController.allEndpointsRunning("foo")).isTrue();
		this.roleController.stopLifecyclesInRole("foo");
		assertThat(this.roleController.allEndpointsRunning("foo")).isFalse();
		assertThat(this.roleController.noEndpointsRunning("foo")).isTrue();

		@SuppressWarnings("unchecked")
		MultiValueMap<String, SmartLifecycle> lifecycles = TestUtils.getPropertyValue(this.roleController,
				"lifecycles", MultiValueMap.class);
		assertThat(lifecycles.size()).isEqualTo(2);
		assertThat(lifecycles.get("foo").size()).isEqualTo(2);
		assertThat(lifecycles.get("bar").size()).isEqualTo(1);
		assertThat(this.serviceActivatorEndpoint.isRunning()).isFalse();
		assertThat(this.sendAsyncHandler.isRunning()).isFalse();
		assertThat(lifecycles.size()).isEqualTo(2);
		assertThat(lifecycles.get("foo").size()).isEqualTo(2);
	}

	@Test
	public void testSourcePollingChannelAdapterOutputChannelLateBinding() {
		QueueChannel testChannel = new QueueChannel();
		ConfigurableListableBeanFactory beanFactory =
				(ConfigurableListableBeanFactory) this.context.getAutowireCapableBeanFactory();
		beanFactory.registerSingleton("lateBindingChannel", testChannel);
		beanFactory.initializeBean(testChannel, "lateBindingChannel");

		this.autoCreatedChannelMessageSourceAdapter.start();
		Message<?> receive = testChannel.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("bar");
		this.autoCreatedChannelMessageSourceAdapter.stop();
	}

	@Test
	public void testIntegrationEvaluationContextCustomization() {
		EvaluationContext evaluationContext = this.context.getBean(StandardEvaluationContext.class);
		List<?> propertyAccessors = TestUtils.getPropertyValue(evaluationContext, "propertyAccessors", List.class);
		assertThat(propertyAccessors.size()).isEqualTo(4);
		assertThat(propertyAccessors.get(0)).isInstanceOf(JsonPropertyAccessor.class);
		assertThat(propertyAccessors.get(1)).isInstanceOf(EnvironmentAccessor.class);
		assertThat(propertyAccessors.get(2)).isInstanceOf(MapAccessor.class);
		assertThat(propertyAccessors.get(3)).isInstanceOf(ReflectivePropertyAccessor.class);
		Map<?, ?> variables = TestUtils.getPropertyValue(evaluationContext, "variables", Map.class);
		Object testSpelFunction = variables.get("testSpelFunction");
		assertThat(testSpelFunction).isEqualTo(ClassUtils.getStaticMethod(TestSpelFunction.class, "bar",
				Object.class));
	}

	@Autowired
	private MessageChannel myHandlerChannel;

	@Autowired
	private PollableChannel myHandlerSuccessChannel;

	@Test
	public void testAdvisedServiceActivator() {
		Date testDate = new Date();

		this.myHandlerChannel.send(new GenericMessage<>(testDate));

		Message<?> receive = this.myHandlerSuccessChannel.receive(10_000);

		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo(testDate);

		assertThat(this.publisherAnnotationBeanPostProcessor.isProxyTargetClass()).isTrue();
		assertThat(this.publisherAnnotationBeanPostProcessor.getOrder()).isEqualTo(Integer.MAX_VALUE - 1);
	}

	@Configuration
	@ComponentScan
	@IntegrationComponentScan
	@EnableIntegration
	//	INT-3853
	//	@PropertySource("classpath:org/springframework/integration/configuration/EnableIntegrationTests.properties")
	public static class ContextConfiguration {

		@Bean
		public CountDownLatch inputReceiveLatch() {
			return new CountDownLatch(1);
		}

		@Bean
		public QueueChannel input() {
			return new QueueChannel() {

				@Override
				protected Message<?> doReceive(long timeout) {
					inputReceiveLatch().countDown();
					return super.doReceive(timeout);
				}
			};
		}

		@Bean
		public QueueChannel input1() {
			return new QueueChannel();
		}

		@Bean
		public QueueChannel input2() {
			return new QueueChannel();
		}

		@Bean
		public QueueChannel input3() {
			return new QueueChannel();
		}

		@Bean
		public QueueChannel input4() {
			return new QueueChannel();
		}

		@Bean
		public Trigger myTrigger() {
			return new PeriodicTrigger(1000L);
		}

		@Bean
		public Trigger onlyOnceTrigger() {
			return new OnlyOnceTrigger();
		}

		@Bean
		public WireTap wireTapFromOutputInterceptor() {
			return new WireTap("wireTapFromOutput");
		}

		@Bean
		public PollableChannel output() {
			QueueChannel queueChannel = new QueueChannel();
			queueChannel.addInterceptor(wireTapFromOutputInterceptor());
			return queueChannel;
		}

		@Bean
		public PollableChannel wireTapFromOutput() {
			return new QueueChannel();
		}

		@Bean
		public PollableChannel wireTapChannel() {
			return new QueueChannel();
		}


		@Bean
		@GlobalChannelInterceptor(patterns = "${global.wireTap.pattern}")
		public WireTap wireTap() {
			return new WireTap(this.wireTapChannel());
		}

		@Bean
		public AtomicInteger fbInterceptorCounter() {
			return new AtomicInteger();
		}

		@Bean
		@GlobalChannelInterceptor
		public FactoryBean<ChannelInterceptor> ciFactoryBean() {
			return new AbstractFactoryBean<>() {

				@Override
				public Class<?> getObjectType() {
					return ChannelInterceptor.class;
				}

				@Override
				protected ChannelInterceptor createInstance() {
					return new ChannelInterceptor() {

						@Override
						public Message<?> preSend(Message<?> message, MessageChannel channel) {
							fbInterceptorCounter().incrementAndGet();
							return message;
						}

					};
				}
			};
		}

		@Bean
		@BridgeFrom("bridgeInput")
		public QueueChannel bridgeOutput() {
			return new QueueChannel();
		}

		@Bean
		public PollableChannel pollableBridgeInput() {
			return new QueueChannel();
		}


		@Bean
		@BridgeFrom(value = "pollableBridgeInput", poller = @Poller(fixedDelay = "1000"))
		public QueueChannel pollableBridgeOutput() {
			return new QueueChannel();
		}

		@Bean
		@MyBridgeFrom
		public QueueChannel metaBridgeOutput() {
			return new QueueChannel();
		}

		@Bean
		public QueueChannel bridgeToOutput() {
			return new QueueChannel();
		}

		@Bean
		@BridgeTo(value = "bridgeToOutput", reactive = @Reactive)
		@EndpointId("reactiveBridge")
		public MessageChannel bridgeToInput() {
			return new DirectChannel();
		}

		@Bean
		@BridgeTo(poller = @Poller(fixedDelay = "500"))
		public QueueChannel pollableBridgeToInput() {
			return new QueueChannel();
		}

		@Bean
		@MyBridgeTo
		public MessageChannel myBridgeToInput() {
			return new DirectChannel();
		}

		// Error because @Bridge* annotations are only for MessageChannel beans.
		/*@Bean
		@BridgeTo
		public String invalidBridgeAnnotation() {
			return "invalidBridgeAnnotation";
		}*/

		// Error because @Bridge* annotations are mutually exclusive.
		/*@Bean
		@BridgeTo
		@BridgeFrom("foo")
		public MessageChannel invalidBridgeAnnotation2() {
			return new DirectChannel();
		}*/

		// beans for metaAnnotation tests

		@Bean
		public MethodInterceptor annAdvice() {
			return mock(MethodInterceptor.class);
		}

		@Bean
		public QueueChannel annInput() {
			return new QueueChannel();
		}

		@Bean
		public QueueChannel annOutput() {
			return new QueueChannel();
		}

		@Bean
		public MethodInterceptor annAdvice1() {
			return mock(MethodInterceptor.class);
		}

		@Bean
		public QueueChannel annInput1() {
			return new QueueChannel();
		}

		@Bean
		public QueueChannel annInput3() {
			return new QueueChannel();
		}

	}

	@Component
	@GlobalChannelInterceptor
	public static class TestChannelInterceptor implements ChannelInterceptor {

		private final AtomicInteger invoked = new AtomicInteger();

		@Override
		public Message<?> preSend(Message<?> message, MessageChannel channel) {
			this.invoked.incrementAndGet();
			return message;
		}

		public Integer getInvoked() {
			return invoked.get();
		}

	}

	@Component
	@IntegrationConverter
	public static class TestConverter implements Converter<Boolean, Number> {

		private final AtomicInteger invoked = new AtomicInteger();

		@Override
		public Number convert(Boolean source) {
			this.invoked.incrementAndGet();
			return source ? 1 : 0;
		}

		public Integer getInvoked() {
			return invoked.get();
		}

	}

	@Configuration
	@EnableIntegration
	@ImportResource("classpath:org/springframework/integration/configuration/EnableIntegrationTests-context.xml")
	@EnableMessageHistory("${message.history.tracked.components}")
	@EnablePublisher(defaultChannel = "publishedChannel", proxyTargetClass = true, order = 2147483646)
	@EnableAsync
	public static class ContextConfiguration2 {

		/*
		INT-3853
		@Bean
		public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
			return new PropertySourcesPlaceholderConfigurer();
		}*/

		@Bean
		public MessageChannel sendAsyncChannel() {
			return new DirectChannel();
		}

		@Bean
		public CountDownLatch asyncAnnotationProcessLatch() {
			return new CountDownLatch(1);
		}

		@Bean
		public AtomicReference<Thread> asyncAnnotationProcessThread() {
			return new AtomicReference<>();
		}

		@Bean
		@ServiceActivator(inputChannel = "sendAsyncChannel")
		@Role("foo")
		public MessageHandler sendAsyncHandler() {
			return message -> {
				asyncAnnotationProcessThread().set(Thread.currentThread());
				asyncAnnotationProcessLatch().countDown();
			};
		}

		@Bean
		@ServiceActivator(inputChannel = "controlBusChannel")
		@EndpointId("controlBusEndpoint")
		@Role("bar")
		public ExpressionControlBusFactoryBean controlBus() {
			return new ExpressionControlBusFactoryBean();
		}

		@Autowired
		@Qualifier("controlBusEndpoint")
		@Lazy
		private EventDrivenConsumer controlBusEndpoint;

		@Bean
		public Pausable pausable(@Qualifier("controlBusChannel") @Lazy MessageChannel controlBusChannel) {
			return new Pausable() {

				private volatile boolean running;

				private volatile boolean paused;

				@Override
				public void start() {
					this.running = true;
				}

				@Override
				public void stop() {
					this.running = false;
				}

				@Override
				public boolean isRunning() {
					return this.running;
				}

				@Override
				public void pause() {
					this.paused = true;
				}

				@Override
				public void resume() {
					this.paused = false;
				}

			};
		}

		@Bean
		public PollableChannel publishedChannel() {
			return new QueueChannel();
		}

		@Bean
		public PollableChannel gatewayChannel() {
			return new QueueChannel();
		}

		@Bean
		public PollableChannel gatewayChannel2() {
			return new QueueChannel();
		}

		@Bean
		public PollableChannel counterChannel() {
			return new QueueChannel();
		}

		@Bean
		public PollableChannel fooChannel() {
			return new QueueChannel();
		}

		@Bean
		public PollableChannel messageChannel() {
			return new QueueChannel();
		}

		@Bean
		public QueueChannel numberChannel() {
			QueueChannel channel = new QueueChannel();
			channel.setDatatypes(Number.class);
			return channel;
		}

		@Bean
		public QueueChannel bytesChannel() {
			QueueChannel channel = new QueueChannel();
			channel.setDatatypes(byte[].class);
			return channel;
		}

		@Bean(name = PollerMetadata.DEFAULT_POLLER)
		public PollerMetadata defaultPoller() {
			PollerMetadata pollerMetadata = new PollerMetadata();
			pollerMetadata.setTrigger(new PeriodicTrigger(10));
			return pollerMetadata;
		}

		@Bean
		public PollerMetadata myPoller() {
			PollerMetadata pollerMetadata = new PollerMetadata();
			pollerMetadata.setTrigger(new PeriodicTrigger(11));
			return pollerMetadata;
		}

		@Bean
		@IntegrationConverter
		public SerializingConverter serializingConverter() {
			return new SerializingConverter();
		}

		@Bean
		public DirectChannel monoChannel() {
			return new DirectChannel();
		}

		@Bean
		public AnnotationTestService annotationTestService() {
			return new AnnotationTestService();
		}

		@Bean
		public SpelFunctionFactoryBean testSpelFunction() {
			return new SpelFunctionFactoryBean(TestSpelFunction.class, "bar");
		}

		@Bean
		public SpelPropertyAccessorRegistrar spelPropertyAccessorRegistrar() {
			return new SpelPropertyAccessorRegistrar(new JsonPropertyAccessor(), new EnvironmentAccessor());
		}

		@Bean
		@ServiceActivator(inputChannel = "myHandlerChannel", adviceChain = "myHandlerAdvice")
		public MessageHandler myHandler() {
			return message -> {
			};
		}

		@Bean
		public Advice myHandlerAdvice() {
			ExpressionEvaluatingRequestHandlerAdvice advice = new ExpressionEvaluatingRequestHandlerAdvice();
			advice.setSuccessChannel(myHandlerSuccessChannel());
			return advice;
		}

		@Bean
		public QueueChannel myHandlerSuccessChannel() {
			return new QueueChannel();
		}

	}

	@Configuration
	@EnableIntegration
	public static class ChildConfiguration {

		@Bean
		public MessageChannel foo() {
			return new DirectChannel();
		}

		@Bean
		@GlobalChannelInterceptor(patterns = "*")
		public WireTap baz() {
			return new WireTap(new NullChannel());
		}

		//Before INT-3961 it fails with the DestinationResolutionException
		@InboundChannelAdapter(channel = "lateBindingChannel", autoStartup = "false",
				poller = @Poller(fixedDelay = "100"))
		@Bean
		public MessageSource<String> autoCreatedChannelMessageSource() {
			return () -> new GenericMessage<>("bar");
		}

	}

	public static class AnnotationTestService implements Lifecycle {

		private final AtomicInteger counter = new AtomicInteger();

		private boolean running;

		@ServiceActivator(inputChannel = "input", outputChannel = "output", autoStartup = "false",
				poller = @Poller(maxMessagesPerPoll = "${poller.maxMessagesPerPoll}",
						fixedDelay = "${poller.interval}",
						receiveTimeout = "${poller.receiveTimeout}"))
		@Publisher
		@Payload("#args[0].toLowerCase()")
		@Role("foo")
		public String handle(String payload) {
			return payload.toUpperCase();
		}

		@ServiceActivator(inputChannel = "input1", outputChannel = "output",
				poller = @Poller(maxMessagesPerPoll = "${poller.maxMessagesPerPoll}",
						fixedRate = "${poller.interval}"))
		@Publisher
		@Payload("#args[0].toLowerCase()")
		public String handle1(String payload) {
			return payload.toUpperCase();
		}

		@ServiceActivator(inputChannel = "input2", outputChannel = "output",
				poller = @Poller(maxMessagesPerPoll = "${poller.maxMessagesPerPoll}", cron = "0 5 7 * * *"))
		@Publisher
		@Payload("#args[0].toLowerCase()")
		public String handle2(String payload) {
			return payload.toUpperCase();
		}

		@ServiceActivator(inputChannel = "input3", outputChannel = "output", poller = @Poller("myPoller"))
		@Publisher
		@Payload("#args[0].toLowerCase()")
		public String handle3(String payload) {
			return payload.toUpperCase();
		}

		@ServiceActivator(inputChannel = "input4", outputChannel = "output",
				poller = @Poller(trigger = "myTrigger"))
		@Publisher
		@Payload("#args[0].toLowerCase()")
		public String handle4(String payload) {
			return payload.toUpperCase();
		}

		/*
		 * This is an error because input5 is not defined and is therefore a DirectChannel.
		 */
		/*@ServiceActivator(inputChannel = "input5", outputChannel = "output",
							poller = @Poller("defaultPollerMetadata"))
		@Publisher
		@Payload("#args[0].toLowerCase()")
		public String handle5(String payload) {
			return payload.toUpperCase();
		}*/

		@Transformer(inputChannel = "gatewayChannel")
		public String transform(Message<String> message) {
			assertThat(message.getHeaders()).containsKey("foo");
			assertThat(message.getHeaders().get("foo")).isEqualTo("FOO");
			assertThat(message.getHeaders()).containsKey("calledMethod");
			assertThat(message.getHeaders().get("calledMethod")).isEqualTo("echo");
			return handle(message.getPayload()) + Arrays.asList(new Throwable().getStackTrace()).toString();
		}

		@Transformer(inputChannel = "gatewayChannel2")
		@UseSpelInvoker(compilerMode = "${xxxxxxxx:IMMEDIATE}")
		public String transform2(Message<String> message) {
			assertThat(message.getHeaders()).containsKey("foo");
			assertThat(message.getHeaders().get("foo")).isEqualTo("FOO");
			assertThat(message.getHeaders()).containsKey("calledMethod");
			assertThat(message.getHeaders().get("calledMethod")).isEqualTo("echo2");
			return handle(message.getPayload()) + "2" + Arrays.asList(new Throwable().getStackTrace()).toString();
		}

		@MyInboundChannelAdapter1
		public Integer count() {
			return this.counter.incrementAndGet();
		}

		@InboundChannelAdapter(value = "fooChannel",
				poller = @Poller(trigger = "onlyOnceTrigger", maxMessagesPerPoll = "2"))
		public String foo() {
			return "foo";
		}

		@InboundChannelAdapter(value = "messageChannel", poller = @Poller(fixedDelay = "${poller.interval}",
				maxMessagesPerPoll = "1"))
		public Message<?> message() {
			return MessageBuilder.withPayload("bar").setHeader("foo", "FOO").build();
		}

		/*
		 * This is an error because 'InboundChannelAdapter' method must not have any arguments.
		 */
		/*@InboundChannelAdapter("errorChannel")
		public String error1(Object arg) {
			return "foo";
		}*/

		/*
		 * This is an error because 'InboundChannelAdapter' return type must not be 'void'.
		 */
		/*@InboundChannelAdapter("errorChannel")
		public void error2() {
		}*/

		// metaAnnotation tests

		@MyServiceActivator
		public Integer annCount() {
			return 0;
		}

		@MyServiceActivator1(inputChannel = "annInput1", autoStartup = "true",
				adviceChain = { "annAdvice1" }, poller = @Poller(fixedRate = "2000"))
		public Integer annCount1() {
			return 0;
		}

		@MyServiceActivatorNoLocalAtts
		public Integer annCount2() {
			return 0;
		}

		@MyServiceActivator5
		public Integer annCount5() {
			return 0;
		}

		@MyServiceActivator8
		public Integer annCount8() {
			return 0;
		}

		@MyAggregator
		public Integer annAgg1(List<?> messages) {
			return 42;
		}

		@MyAggregatorDefaultOverrideDefaults
		public Integer annAgg2(List<?> messages) {
			return 42;
		}

		// Error because @Bridge* annotations are only for @Bean methods.
		/*@BridgeFrom("")
		public void invalidBridgeAnnotationMethod(Object payload) {}*/

		@ServiceActivator(inputChannel = "monoChannel")
		public Integer multiply(Integer value) {
			return value * 2;
		}

		@Override
		public void start() {
			this.running = true;
		}

		@Override
		public void stop() {
			this.running = false;
		}

		@Override
		public boolean isRunning() {
			return this.running;
		}

	}

	@Conditional(TestCondition.class)
	@MessagingGateway
	public interface ConditionalGateway {

		void testGateway(Object payload);

	}

	@TestMessagingGateway
	public interface TestGateway {

		@Gateway(headers = @GatewayHeader(name = "calledMethod", expression = "#gatewayMethod.name"))
		String echo(String payload);

		@Gateway(requestChannel = "sendAsyncChannel")
		@Async
		void sendAsync(String payload);

		@Gateway(requestChannel = "monoChannel")
		Mono<Integer> multiply(Integer value);

	}

	@TestMessagingGateway2
	public interface TestGateway2 {

		@Gateway(headers = @GatewayHeader(name = "calledMethod", expression = "#gatewayMethod.name"))
		String echo2(String payload);

	}

	@Target({ ElementType.TYPE, ElementType.ANNOTATION_TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@MessagingGateway(defaultRequestChannel = "gatewayChannel",
			defaultRequestTimeout = "${default.request.timeout:12300}", defaultReplyTimeout = "#{13400}",
			defaultHeaders = @GatewayHeader(name = "foo", value = "FOO"))
	public @interface TestMessagingGateway {

		String defaultRequestChannel() default "";

	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@TestMessagingGateway(defaultRequestChannel = "gatewayChannel2")
	public @interface TestMessagingGateway2 {

		String defaultRequestChannel() default "";

	}

	@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@ServiceActivator(autoStartup = "false",
			phase = "23",
			inputChannel = "annInput",
			outputChannel = "annOutput",
			adviceChain = { "annAdvice" },
			poller = @Poller(fixedDelay = "1000"))
	public @interface MyServiceActivator {

		String inputChannel() default "";

		String outputChannel() default "";

		String[] adviceChain() default { };

		String autoStartup() default "";

		String phase() default "";

		Poller[] poller() default { };

	}

	@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@MyServiceActivator
	public @interface MyServiceActivator1 {

		String inputChannel() default "";

		String outputChannel() default "";

		String[] adviceChain() default { };

		String autoStartup() default "";

		String phase() default "";

		Poller[] poller() default { };

	}

	@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@MyServiceActivator1
	public @interface MyServiceActivator2 {

		String inputChannel() default "";

	}

	@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@MyServiceActivator2
	public @interface MyServiceActivator3 {

		String inputChannel() default "";

	}

	@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@MyServiceActivator3(inputChannel = "annInput3")
	public @interface MyServiceActivator4 {

		String inputChannel() default "";

	}

	@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@MyServiceActivator4
	public @interface MyServiceActivator5 {

		String inputChannel() default "";

	}

	// Test prevent infinite recursion

	@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@MyServiceActivator5
	public @interface MyServiceActivator6 {

		String inputChannel() default "";

	}

	@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@MyServiceActivator8
	public @interface MyServiceActivator7 {

		String inputChannel() default "";

	}

	@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@MyServiceActivator7
	public @interface MyServiceActivator8 {

		String inputChannel() default "";

	}
	// end test infinite recursion

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@ServiceActivator(autoStartup = "false",
			phase = "23",
			inputChannel = "annInput",
			outputChannel = "annOutput",
			adviceChain = { "annAdvice" },
			poller = @Poller(fixedDelay = "1000"))
	public @interface MyServiceActivatorNoLocalAtts {

	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@Aggregator(autoStartup = "false",
			phase = "23",
			inputChannel = "annInput",
			outputChannel = "annOutput",
			discardChannel = "annOutput",
			poller = @Poller(fixedDelay = "1000"))
	public @interface MyAggregator {

		String inputChannel() default "";

		String outputChannel() default "";

		String discardChannel() default "";

		long sendTimeout() default 1000L;

		boolean sendPartialResultsOnExpiry() default false;

		String autoStartup() default "";

		String phase() default "";

		Poller[] poller() default { };

	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@Aggregator(autoStartup = "false",
			phase = "23",
			inputChannel = "annInput",
			outputChannel = "annOutput",
			discardChannel = "annOutput",
			sendPartialResultsOnExpiry = "false",
			sendTimeout = "1000",
			poller = @Poller(fixedDelay = "1000"))
	public @interface MyAggregatorDefaultOverrideDefaults {

		String sendPartialResultsOnExpiry() default "true";

		String sendTimeout() default "75";

	}

	@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@InboundChannelAdapter(value = "counterChannel", autoStartup = "false", phase = "23")
	public @interface MyInboundChannelAdapter {

		String value() default "";

		String autoStartup() default "";

		String phase() default "";

		Poller[] poller() default { };

	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@MyInboundChannelAdapter
	public @interface MyInboundChannelAdapter1 {

	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@BridgeFrom(value = "metaBridgeInput", autoStartup = "false")
	public @interface MyBridgeFrom {

		String value() default "";

	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@BridgeTo(autoStartup = "false")
	public @interface MyBridgeTo {

	}

	// Error because the annotation is on a class; it must be on an interface
	//	@MessagingGateway(defaultRequestChannel = "gatewayChannel",
	//        defaultHeaders = @GatewayHeader(name = "foo", value = "FOO"))
	//	public static class TestGateway2 { }


	public static class TestSpelFunction {

		public static Object bar(Object o) {
			return o;
		}

	}

	public static class TestCondition implements Condition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			return context.getBeanFactory().containsBean("DoesNotExist");
		}

	}

}
