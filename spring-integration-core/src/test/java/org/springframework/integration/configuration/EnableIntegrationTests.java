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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.Payload;
import org.springframework.integration.annotation.Publisher;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.config.EnableMessageHistory;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.history.MessageHistoryConfigurer;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
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
	private MessageChannel input;

	@Autowired
	private PollableChannel output;

	@Autowired
	private PollableChannel publishedChannel;

	@Autowired
	private MessageHistoryConfigurer configurer;

	@Autowired
	private TestGateway testGateway;

	@Test
	public void testAnnotatedServiceActivator() {
		this.input.send(MessageBuilder.withPayload("Foo").build());
		Message<?> receive = this.output.receive(1000);
		assertNotNull(receive);
		assertEquals("FOO", receive.getPayload());

		MessageHistory messageHistory = receive.getHeaders().get(MessageHistory.HEADER_NAME, MessageHistory.class);
		assertNotNull(messageHistory);
		String messageHistoryString = messageHistory.toString();
		assertThat(messageHistoryString, Matchers.containsString("input"));
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
	}

	@Test @DirtiesContext
	public void testChangePatterns() {
		try {
			this.configurer.setComponentNamePatterns(new String[] {"*"});
			fail("ExpectedException");
		}
		catch (IllegalStateException e) {
			assertThat(e.getMessage(), containsString("cannot be changed"));
		}
		this.configurer.stop();
		this.configurer.setComponentNamePatterns(new String[] {"*"});
		assertEquals("*", TestUtils.getPropertyValue(this.configurer, "componentNamePatterns", String[].class)[0]);
	}

	@Test
	public void testMessagingGateway() {
		String payload = "bar";
		assertEquals(payload.toUpperCase(), this.testGateway.echo(payload));
	}

	@Configuration
	@ComponentScan
	@IntegrationComponentScan
	@EnableIntegration
	@PropertySource("classpath:org/springframework/integration/configuration/EnableIntegrationTests.properties")
	@EnableMessageHistory({"input", "publishedChannel"})
	public static class ContextConfiguration {

		@Bean
		public MessageChannel input() {
			return new DirectChannel();
		}

		@Bean
		public PollableChannel output() {
			return new QueueChannel();
		}

	}

	@Configuration
	@EnableIntegration
	@ImportResource("classpath:org/springframework/integration/configuration/EnableIntegrationTests-context.xml")
	@EnableMessageHistory("${message.history.tracked.components}")
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

	@MessageEndpoint
	public static class AnnotationTestService {

		@ServiceActivator(inputChannel = "input", outputChannel = "output")
		@Publisher(channel = "publishedChannel")
		@Payload("#args[0].toLowerCase()")
		public String handle(String payload) {
			return payload.toUpperCase();
		}

		@Transformer(inputChannel = "gatewayChannel")
		public String transform(String payload) {
			return this.handle(payload);
		}
	}

	@MessagingGateway(defaultRequestChannel = "gatewayChannel")
	public static interface TestGateway {

		String echo(String payload);

	}

}
