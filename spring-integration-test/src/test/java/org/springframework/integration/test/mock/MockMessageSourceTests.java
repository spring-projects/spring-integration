/*
 * Copyright 2017-2018 the original author or authors.
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

package org.springframework.integration.test.mock;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.Lifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.PollerSpec;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.dsl.StandardIntegrationFlow;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.dsl.context.IntegrationFlowContext.IntegrationFlowRegistration;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.test.context.MockIntegrationContext;
import org.springframework.integration.test.context.SpringIntegrationTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Artem Bilan
 *
 * @since 5.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = MockMessageSourceTests.Config.class)
@SpringIntegrationTest(noAutoStartup = {"inboundChannelAdapter", "*Source*"})
@DirtiesContext
public class MockMessageSourceTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private MockIntegrationContext mockIntegrationContext;

	@Autowired
	private QueueChannel results;

	@Autowired
	private IntegrationFlowContext integrationFlowContext;

	@After
	public void tearDown() {
		this.mockIntegrationContext.resetBeans("mySourceEndpoint", "inboundChannelAdapter");
		results.purge(null);
	}

	@Test
	public void testMockMessageSource() {
		this.mockIntegrationContext.substituteMessageSourceFor("mySourceEndpoint",
				MockIntegration.mockMessageSource("foo", "bar", "baz"));

		Message<?> receive = this.results.receive(10_000);
		assertNotNull(receive);
		assertEquals("FOO", receive.getPayload());

		receive = this.results.receive(10_000);
		assertNotNull(receive);
		assertEquals("BAR", receive.getPayload());

		for (int i = 0; i < 10; i++) {
			receive = this.results.receive(10_000);
			assertNotNull(receive);
			assertEquals("BAZ", receive.getPayload());
		}

		this.applicationContext.getBean("mySourceEndpoint", Lifecycle.class).stop();
	}

	@Test
	public void testMockMessageSourceInConfig() {
		this.applicationContext.getBean("mockMessageSourceTests.Config.testingMessageSource.inboundChannelAdapter",
				Lifecycle.class).start();

		Message<?> receive = this.results.receive(10_000);
		assertNotNull(receive);
		assertEquals(1, receive.getPayload());

		receive = this.results.receive(10_000);
		assertNotNull(receive);
		assertEquals(2, receive.getPayload());

		for (int i = 0; i < 10; i++) {
			receive = this.results.receive(10_000);
			assertNotNull(receive);
			assertEquals(3, receive.getPayload());
		}

		this.applicationContext.getBean("mockMessageSourceTests.Config.testingMessageSource.inboundChannelAdapter",
				Lifecycle.class).stop();
	}

	@Test
	public void testMockMessageSourceInXml() {
		this.applicationContext.getBean("inboundChannelAdapter", Lifecycle.class).start();

		Message<?> receive = this.results.receive(10_000);
		assertNotNull(receive);
		assertEquals("a", receive.getPayload());

		receive = this.results.receive(10_000);
		assertNotNull(receive);
		assertEquals("b", receive.getPayload());

		for (int i = 0; i < 10; i++) {
			receive = this.results.receive(10_000);
			assertNotNull(receive);
			assertEquals("c", receive.getPayload());
		}

		this.applicationContext.getBean("inboundChannelAdapter", Lifecycle.class).stop();
	}

	@Test
	public void testMockMessageSourceDynamicFlow() {
		QueueChannel out = new QueueChannel();
		StandardIntegrationFlow flow = IntegrationFlows
				.from(MockIntegration.mockMessageSource("foo", "bar", "baz"))
				.<String, String>transform(String::toUpperCase)
				.channel(out)
				.get();
		IntegrationFlowRegistration registration = this.integrationFlowContext.registration(flow).register();

		Message<?> receive = out.receive(10_000);
		assertNotNull(receive);
		assertEquals("FOO", receive.getPayload());

		receive = out.receive(10_000);
		assertNotNull(receive);
		assertEquals("BAR", receive.getPayload());

		for (int i = 0; i < 10; i++) {
			receive = out.receive(10_000);
			assertNotNull(receive);
			assertEquals("BAZ", receive.getPayload());
		}

		registration.destroy();
	}

	@Test
	public void testWrongBeanForInstead() {
		try {
			this.mockIntegrationContext.substituteMessageSourceFor("errorChannel", () -> null);
			fail("BeanNotOfRequiredTypeException expected");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(BeanNotOfRequiredTypeException.class));
			assertThat(e.getMessage(),
					containsString("Bean named 'errorChannel' is expected to be of type " +
							"'org.springframework.integration.endpoint.SourcePollingChannelAdapter' " +
							"but was actually of type " +
							"'org.springframework.integration.channel.PublishSubscribeChannel"));

		}
	}

	@Configuration
	@EnableIntegration
	@ImportResource("org/springframework/integration/test/mock/MockMessageSourceTests-context.xml")
	public static class Config {

		@Bean(name = PollerMetadata.DEFAULT_POLLER)
		public PollerSpec defaultPoller() {
			return Pollers.fixedDelay(10);
		}

		@Bean
		public IntegrationFlow myFlow() {
			return IntegrationFlows
					.from(() -> new GenericMessage<>("myData"),
							e -> e.id("mySourceEndpoint"))
					.<String, String>transform(String::toUpperCase)
					.channel(results())
					.get();
		}

		@Bean
		public QueueChannel results() {
			return new QueueChannel();
		}

		@InboundChannelAdapter(channel = "results")
		@Bean
		public MessageSource<Integer> testingMessageSource() {
			return MockIntegration.mockMessageSource(1, 2, 3);
		}

	}

}
