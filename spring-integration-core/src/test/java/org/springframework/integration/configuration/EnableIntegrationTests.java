/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.integration.configuration;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.GatewayHeader;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.Payload;
import org.springframework.integration.annotation.Publisher;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.channel.interceptor.GlobalChannelInterceptorBeanPostProcessor;
import org.springframework.integration.channel.interceptor.WireTap;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.config.EnableMessageHistory;
import org.springframework.integration.config.EnablePublisher;
import org.springframework.integration.config.GlobalChannelInterceptor;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.history.MessageHistoryConfigurer;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.ChannelInterceptorAdapter;
import org.springframework.stereotype.Component;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

/**
 * @author Artem Bilan
 * @since 4.0
 */
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = {EnableIntegrationTests.ContextConfiguration.class, EnableIntegrationTests.ContextConfiguration2.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class EnableIntegrationTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private MessageChannel input;

	@Autowired
	private QueueChannel output;

	@Autowired
	private PollableChannel publishedChannel;

	@Autowired
	private PollableChannel wireTapChannel;

	@Autowired
	private MessageHistoryConfigurer configurer;

	@Autowired
	private TestGateway testGateway;

	@Autowired
	private TestChannelInterceptor testChannelInterceptor;

	@Autowired
	private GlobalChannelInterceptorBeanPostProcessor beanPostProcessor;

	@Test
	public void testAnnotatedServiceActivator() {
		assertEquals(4, TestUtils.getPropertyValue(this.beanPostProcessor, "channelInterceptors", List.class).size());
		this.input.send(MessageBuilder.withPayload("Foo").build());

		Message<?> interceptedMessage = this.wireTapChannel.receive(1000);
		assertNotNull(interceptedMessage);
		assertEquals("Foo", interceptedMessage.getPayload());

		Message<?> receive = this.output.receive(1000);
		assertNotNull(receive);
		assertEquals("FOO", receive.getPayload());

		MessageHistory messageHistory = receive.getHeaders().get(MessageHistory.HEADER_NAME, MessageHistory.class);
		assertNotNull(messageHistory);
		String messageHistoryString = messageHistory.toString();
		assertThat(messageHistoryString, Matchers.containsString("input"));
		assertThat(messageHistoryString, Matchers.containsString("AnnotationTestService.handle.serviceActivator.handler"));
		assertThat(messageHistoryString, Matchers.not(Matchers.containsString("output")));

		receive = this.publishedChannel.receive(1000);
		assertNotNull(receive);
		assertEquals("foo", receive.getPayload());

		messageHistory = receive.getHeaders().get(MessageHistory.HEADER_NAME, MessageHistory.class);
		assertNotNull(messageHistory);
		messageHistoryString = messageHistory.toString();
		assertThat(messageHistoryString, Matchers.not(Matchers.containsString("input")));
		assertThat(messageHistoryString, Matchers.not(Matchers.containsString("output")));
		assertThat(messageHistoryString, Matchers.containsString("publishedChannel"));

		assertNull(this.wireTapChannel.receive(0));
		assertThat(this.testChannelInterceptor.getInvoked(), Matchers.greaterThan(0));
	}

	@Test
	@DirtiesContext
	public void testChangePatterns() {
		try {
			this.configurer.setComponentNamePatterns(new String[]{"*"});
			fail("ExpectedException");
		}
		catch (IllegalStateException e) {
			assertThat(e.getMessage(), containsString("cannot be changed"));
		}
		this.configurer.stop();
		this.configurer.setComponentNamePatterns(new String[]{"*"});
		assertEquals("*", TestUtils.getPropertyValue(this.configurer, "componentNamePatterns", String[].class)[0]);
	}

	@Test
	public void testMessagingGateway() {
		String payload = "bar";
		assertEquals(payload.toUpperCase(), this.testGateway.echo(payload));
	}

	@Test
	public void testParentChildAnnotationConfiguration() {
		AnnotationConfigApplicationContext child = new AnnotationConfigApplicationContext();
		child.register(ChildConfiguration.class);
		child.setParent(this.context);
		child.refresh();
		AbstractMessageChannel foo = child.getBean("foo", AbstractMessageChannel.class);
		ChannelInterceptor baz = child.getBean("baz", ChannelInterceptor.class);
		assertTrue(foo.getChannelInterceptors().contains(baz));
		assertFalse(this.output.getChannelInterceptors().contains(baz));
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
		assertTrue(foo.getChannelInterceptors().contains(baz));
		assertFalse(this.output.getChannelInterceptors().contains(baz));
		child.close();
	}

	@Configuration
	@ComponentScan
	@IntegrationComponentScan
	@EnableIntegration
	@PropertySource("classpath:org/springframework/integration/configuration/EnableIntegrationTests.properties")
	@EnableMessageHistory({"input", "publishedChannel", "*AnnotationTestService*"})
	public static class ContextConfiguration {

		@Bean
		public MessageChannel input() {
			return new DirectChannel();
		}

		@Bean
		public PollableChannel output() {
			return new QueueChannel();
		}

		@Bean
		public PollableChannel wireTapChannel() {
			return new QueueChannel();
		}


		@Bean
		@GlobalChannelInterceptor(patterns = "input")
		public WireTap wireTap() {
			return new WireTap(this.wireTapChannel());
		}

	}

	@Component
	@GlobalChannelInterceptor
	public static class TestChannelInterceptor extends ChannelInterceptorAdapter {

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

	@Configuration
	@EnableIntegration
	@ImportResource("classpath:org/springframework/integration/configuration/EnableIntegrationTests-context.xml")
	@EnableMessageHistory("${message.history.tracked.components}")
	@EnablePublisher("publishedChannel")
	public static class ContextConfiguration2 {

		@Bean
		public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
			return new PropertySourcesPlaceholderConfigurer();
		}

		@Bean
		public PollableChannel publishedChannel() {
			return new QueueChannel();
		}

		@Bean
		public DirectChannel gatewayChannel() {
			return new DirectChannel();
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

	}


	@MessageEndpoint
	public static class AnnotationTestService {

		@ServiceActivator(inputChannel = "input", outputChannel = "output")
		@Publisher
		@Payload("#args[0].toLowerCase()")
		public String handle(String payload) {
			return payload.toUpperCase();
		}

		@Transformer(inputChannel = "gatewayChannel")
		public String transform(Message<String> message) {
			assertTrue(message.getHeaders().containsKey("foo"));
			assertEquals("FOO", message.getHeaders().get("foo"));
			assertTrue(message.getHeaders().containsKey("calledMethod"));
			assertEquals("echo", message.getHeaders().get("calledMethod"));
			return this.handle(message.getPayload());
		}
	}

	@MessagingGateway(defaultRequestChannel = "gatewayChannel", defaultHeaders = @GatewayHeader(name = "foo", value = "FOO"))
	public static interface TestGateway {

		@Gateway(headers = @GatewayHeader(name = "calledMethod", expression = "#gatewayMethod.name"))
		String echo(String payload);

	}

	// Error because the annotation is on a class; it must be on an interface
//	@MessagingGateway(defaultRequestChannel = "gatewayChannel", defaultHeaders = @GatewayHeader(name = "foo", value = "FOO"))
//	public static class TestGateway2 { }

}
