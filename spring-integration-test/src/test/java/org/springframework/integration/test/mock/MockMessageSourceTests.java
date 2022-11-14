/*
 * Copyright 2017-2022 the original author or authors.
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

package org.springframework.integration.test.mock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

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
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Artem Bilan
 *
 * @since 5.0
 */
@SpringJUnitConfig(classes = MockMessageSourceTests.Config.class)
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

	@AfterEach
	public void tearDown() {
		this.mockIntegrationContext.resetBeans("mySourceEndpoint", "inboundChannelAdapter");
		this.results.purge(null);
	}

	@Test
	public void testMockMessageSource() {
		this.mockIntegrationContext.substituteMessageSourceFor("mySourceEndpoint",
				MockIntegration.mockMessageSource("foo", "bar", "baz"));

		Message<?> receive = this.results.receive(10_000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("FOO");

		receive = this.results.receive(10_000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("BAR");

		for (int i = 0; i < 10; i++) {
			receive = this.results.receive(10_000);
			assertThat(receive).isNotNull();
			assertThat(receive.getPayload()).isEqualTo("BAZ");
		}

		this.applicationContext.getBean("mySourceEndpoint", Lifecycle.class).stop();
	}

	@Test
	public void testMockMessageSourceInConfig() {
		Lifecycle channelAdapter =
				this.applicationContext.getBean("testingMessageSource.inboundChannelAdapter", Lifecycle.class);
		channelAdapter.start();

		Message<?> receive = this.results.receive(10_000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo(1);

		receive = this.results.receive(10_000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo(2);

		for (int i = 0; i < 10; i++) {
			receive = this.results.receive(10_000);
			assertThat(receive).isNotNull();
			assertThat(receive.getPayload()).isEqualTo(3);
		}

		channelAdapter.stop();
	}

	@Test
	public void testMockMessageSourceInXml() {
		Lifecycle channelAdapter = this.applicationContext.getBean("inboundChannelAdapter", Lifecycle.class);
		channelAdapter.start();

		Message<?> receive = this.results.receive(10_000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("a");

		receive = this.results.receive(10_000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("b");

		for (int i = 0; i < 10; i++) {
			receive = this.results.receive(10_000);
			assertThat(receive).isNotNull();
			assertThat(receive.getPayload()).isEqualTo("c");
		}

		channelAdapter.stop();
	}

	@Test
	public void testMockMessageSourceDynamicFlow() {
		QueueChannel out = new QueueChannel();
		StandardIntegrationFlow flow = IntegrationFlow
				.from(MockIntegration.mockMessageSource("foo", "bar", "baz"))
				.<String, String>transform(String::toUpperCase)
				.channel(out)
				.get();
		IntegrationFlowRegistration registration = this.integrationFlowContext.registration(flow).register();

		Message<?> receive = out.receive(10_000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("FOO");

		receive = out.receive(10_000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("BAR");

		for (int i = 0; i < 10; i++) {
			receive = out.receive(10_000);
			assertThat(receive).isNotNull();
			assertThat(receive.getPayload()).isEqualTo("BAZ");
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
			assertThat(e).isInstanceOf(BeanNotOfRequiredTypeException.class);
			assertThat(e.getMessage()).contains("Bean named 'errorChannel' is expected to be of type " +
					"'org.springframework.integration.endpoint.SourcePollingChannelAdapter' " +
					"but was actually of type " +
					"'org.springframework.integration.channel.PublishSubscribeChannel");

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
		public QueueChannel results() {
			return new QueueChannel();
		}

		@Bean
		public IntegrationFlow myFlow() {
			return IntegrationFlow
					.from(() -> new GenericMessage<>("myData"),
							e -> e.id("mySourceEndpoint"))
					.<String, String>transform(String::toUpperCase)
					.channel(results())
					.get();
		}

		@InboundChannelAdapter(channel = "results")
		@Bean
		public MessageSource<Integer> testingMessageSource() {
			return MockIntegration.mockMessageSource(1, 2, 3);
		}

	}

}
