/*
 * Copyright 2016-2024 the original author or authors.
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

package org.springframework.integration.nats.support.config;

import java.io.IOException;
import java.time.Duration;

import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import io.nats.client.api.ConsumerConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowDefinition;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.nats.ConsumerProperties;
import org.springframework.integration.nats.NatsConsumerFactory;
import org.springframework.integration.nats.NatsSpringIntegrationScatterGatherTest;
import org.springframework.integration.nats.converter.MessageConverter;
import org.springframework.integration.nats.support.AbstractNatsIntegrationTestSupport;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Repository;

/**
 * @author Viktor Rohlenko
 * @author Vennila Pazhamalai
 * @author Vivek Duraisamy
 * @since 6.4.x
 *
 * @see <a
 * href="https://rohlenko.github.io/spring-integration-nats-site/gws-spring-integration-nats/index.html#stakeholders">See
 * all stakeholders and contact</a>
 */
@Configuration
@EnableIntegration
@IntegrationComponentScan
@Repository
public class DefaultContextConfig extends AbstractNatsIntegrationTestSupport {

	public static final String TEST_SUBJECT = "test-subject";
	public static final String TEST_STREAM = "test-stream";
	public static final String TEST_SUBJECT_CONSUMER = "test-subject-consumer";
	public static final int MAX_REDELIVER = 3;
	public static final Log LOG = LogFactory.getLog(NatsSpringIntegrationScatterGatherTest.class);
	@Autowired
	private Connection natsConnection;

	/**
	 * Use the default streamSetup from the concrete config class of test e.g. like
	 *
	 * <pre>
	 *    &#64;PostConstruct
	 *    public void streamSetup() throws IOException, JetStreamApiException {
	 * 			super.streamSetup();
	 *      }
	 * </pre>
	 *
	 * @throws IOException
	 * @throws JetStreamApiException
	 */
	protected void streamSetup() throws IOException, JetStreamApiException {
		createStreamConfig(this.natsConnection, TEST_STREAM, TEST_SUBJECT);
		// Consumer Config to limit the redelivery
		ConsumerConfiguration consumerConfiguration =
				ConsumerConfiguration.builder()
						.ackWait(Duration.ofSeconds(5))
						.maxDeliver(MAX_REDELIVER)
						.durable(TEST_SUBJECT_CONSUMER)
						.build();
		createConsumer(this.natsConnection, TEST_STREAM, consumerConfiguration);
	}

	@Bean
	public NatsConsumerFactory defaultConsumerFactory() {
		final ConsumerProperties consumerProperties =
				new ConsumerProperties(
						TEST_STREAM, TEST_SUBJECT, TEST_SUBJECT_CONSUMER, "test-subject-group");
		consumerProperties.setConsumerMaxWait(Duration.ofSeconds(1));
		return new NatsConsumerFactory(this.natsConnection, consumerProperties);
	}

	/*
	 * Message consumer which throws exception only on
	 * specific message payload
	 * */
	@ServiceActivator(inputChannel = "consumerDirectChannel")
	public void processMessage(@Payload final String message) {
		LOG.info("Message successfully received in service Activator: " + message);
	}

	@Bean
	public MessageChannel consumerDirectChannel() {
		DirectChannel channel = MessageChannels.direct().getObject();
		return channel;
	}

	@Bean
	public MessageConverter<String> messageConvertorString() {
		return new MessageConverter<>(String.class);
	}

	public Connection getNatsConnection() {
		return natsConnection;
	}

	@Bean
	public IntegrationFlow controlBus() {
		return IntegrationFlowDefinition::controlBus;
	}
}
