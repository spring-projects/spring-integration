/*
 * Copyright 2026-present the original author or authors.
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

package org.springframework.integration.kafka.dsl;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.kafka.inbound.KafkaShareMessageDrivenChannelAdapter;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.DefaultShareConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ShareConsumerFactory;
import org.springframework.kafka.listener.AbstractShareKafkaMessageListenerContainer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 *
 * @since 7.2
 */
@SpringJUnitConfig
@DirtiesContext
@EmbeddedKafka(
		topics = KafkaShareDslTests.TEST_TOPIC,
		partitions = 1,
		brokerProperties = {
				"share.coordinator.state.topic.replication.factor=1",
				"share.coordinator.state.topic.min.isr=1"
		})
public class KafkaShareDslTests {

	static final String TEST_TOPIC = "share-dsl-test-topic";

	static final String GROUP_ID = "share-dsl-group";

	@Value("${spring.kafka.bootstrap-servers}")
	private String bootstrapServers;

	@Autowired
	private ConfigurableApplicationContext applicationContext;

	@Autowired
	private PollableChannel shareListeningResults;

	@Test
	void testShareMessageDrivenChannelAdapter() throws Exception {
		setShareAutoOffsetResetEarliest();

		KafkaShareMessageDrivenChannelAdapter<?, ?> adapter =
				this.applicationContext.getBean(KafkaShareMessageDrivenChannelAdapter.class);
		adapter.start();
		try {
			Map<String, Object> producerProps = new HashMap<>();
			producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapServers);
			producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
			producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
			producerProps.put(ProducerConfig.LINGER_MS_CONFIG, 0);
			DefaultKafkaProducerFactory<String, String> producerFactory =
					new DefaultKafkaProducerFactory<>(producerProps);
			KafkaTemplate<String, String> template = new KafkaTemplate<>(producerFactory);
			assertThat(template.send(TEST_TOPIC, "key", "test")).succeedsWithin(Duration.ofSeconds(10));

			Message<?> received = this.shareListeningResults.receive(30000);
			assertThat(received).isNotNull();
			assertThat(received.getPayload()).isEqualTo("TEST");

			assertThat(this.applicationContext.containsBean("shareContainer")).isTrue();
			AbstractShareKafkaMessageListenerContainer<?, ?> container =
					this.applicationContext.getBean("shareContainer", AbstractShareKafkaMessageListenerContainer.class);
			assertThat(container.isAutoStartup()).isFalse();

			producerFactory.destroy();
		}
		finally {
			adapter.stop();
		}
	}

	private void setShareAutoOffsetResetEarliest() throws Exception {
		Map<String, Object> adminProperties = new HashMap<>();
		adminProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapServers);
		ConfigEntry entry = new ConfigEntry("share.auto.offset.reset", "earliest");
		AlterConfigOp op = new AlterConfigOp(entry, AlterConfigOp.OpType.SET);
		Map<ConfigResource, Collection<AlterConfigOp>> configs =
				Map.of(new ConfigResource(ConfigResource.Type.GROUP, GROUP_ID), List.of(op));
		try (Admin admin = Admin.create(adminProperties)) {
			admin.incrementalAlterConfigs(configs).all().get();
		}
	}

	@Configuration(proxyBeanMethods = false)
	@EnableIntegration
	public static class ContextConfiguration {

		@Value("${spring.kafka.bootstrap-servers}")
		private String embeddedKafkaBrokers;

		@Bean
		public ShareConsumerFactory<String, String> shareConsumerFactory() {
			Map<String, Object> props = new HashMap<>();
			props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.embeddedKafkaBrokers);
			props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
			props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
			props.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
			return new DefaultShareConsumerFactory<>(props);
		}

		@Bean
		public IntegrationFlow shareListenerFromKafkaFlow(ShareConsumerFactory<String, String> shareConsumerFactory) {
			return IntegrationFlow
					.from(Kafka.shareMessageDrivenChannelAdapter(shareConsumerFactory, TEST_TOPIC)
							.configureListenerContainer(c -> c.id("shareContainer"))
							.autoStartup(false))
					.<String, String>transform(String::toUpperCase)
					.channel(c -> c.queue("shareListeningResults"))
					.get();
		}

	}

}
