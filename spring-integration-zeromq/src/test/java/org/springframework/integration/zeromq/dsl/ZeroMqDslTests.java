/*
 * Copyright 2022 the original author or authors.
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

package org.springframework.integration.zeromq.dsl;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.zeromq.SocketType;
import org.zeromq.ZContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Transformers;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.zeromq.ZeroMqHeaders;
import org.springframework.integration.zeromq.ZeroMqProxy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * @author Artem Bilan
 *
 * @since 5.4
 */
@SpringJUnitConfig
@DirtiesContext
public class ZeroMqDslTests {

	@Autowired
	ZContext context;

	@Autowired
	@Qualifier("publishToZeroMqPubSubFlow.input")
	MessageChannel publishToZeroMqPubSubFlowInput;

	@Autowired
	ZeroMqProxy subPubZeroMqProxy;

	@Autowired
	ZeroMqProxy pullPushZeroMqProxy;

	@Autowired
	IntegrationFlowContext integrationFlowContext;

	@Test
	void testZeroMqDslIntegration() throws InterruptedException {
		BlockingQueue<Message<?>> results = new LinkedBlockingQueue<>();

		await().until(() -> this.subPubZeroMqProxy.getBackendPort() > 0);

		for (int i = 0; i < 2; i++) {
			IntegrationFlow consumerFlow =
					IntegrationFlow.from(
									ZeroMq.inboundChannelAdapter(this.context, SocketType.SUB)
											.connectUrl("tcp://localhost:" + this.subPubZeroMqProxy.getBackendPort())
											.topics("someTopic")
											.consumeDelay(Duration.ofMillis(100)))
							.channel(ZeroMq.zeroMqChannel(this.context).zeroMqProxy(this.pullPushZeroMqProxy))
							.transform(Transformers.objectToString())
							.handle(results::offer)
							.get();

			this.integrationFlowContext.registration(consumerFlow).register();
		}

		// Give it some time to connect and subscribe
		Thread.sleep(2000);

		this.publishToZeroMqPubSubFlowInput.send(new GenericMessage<>("test"));

		Message<?> message = results.poll(10, TimeUnit.SECONDS);
		assertThat(message).isNotNull()
				.extracting(Message::getPayload)
				.isEqualTo("test");

		assertThat(message.getHeaders()).containsEntry(ZeroMqHeaders.TOPIC, "someTopic");

		message = results.poll(1, TimeUnit.SECONDS);
		assertThat(message).isNotNull()
				.extracting(Message::getPayload)
				.isEqualTo("test");

		// With Pub/Sub channel we would have 4 messages.
		assertThat(results.poll(1, TimeUnit.SECONDS)).isNull();

		this.integrationFlowContext.getRegistry()
				.values()
				.forEach(IntegrationFlowContext.IntegrationFlowRegistration::destroy);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableIntegration
	public static class ContextConfiguration {

		@Bean
		ZContext context() {
			return new ZContext();
		}

		@Bean
		ZeroMqProxy subPubZeroMqProxy(ZContext context) {
			return new ZeroMqProxy(context, ZeroMqProxy.Type.SUB_PUB);
		}

		@Bean
		ZeroMqProxy pullPushZeroMqProxy(ZContext context) {
			return new ZeroMqProxy(context);
		}

		@Bean
		IntegrationFlow publishToZeroMqPubSubFlow(ZContext context,
				@Qualifier("subPubZeroMqProxy") ZeroMqProxy subPubZeroMqProxy) {

			return flow ->
					flow.handle(
							ZeroMq.outboundChannelAdapter(context,
											() -> {
												await().until(() -> subPubZeroMqProxy.getFrontendPort() > 0);
												return "tcp://localhost:" + subPubZeroMqProxy.getFrontendPort();
											},
											SocketType.PUB)
									.topic("someTopic"));
		}

	}

}
