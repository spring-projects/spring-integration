/*
 * Copyright 2013-2016 the original author or authors.
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

package org.springframework.integration.kafka.config.xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.MessageTimeoutException;
import org.springframework.integration.kafka.outbound.KafkaProducerMessageHandler;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Soby Chacko
 * @author Artem Bilan
 * @author Gary Russell
 * @since 0.5
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@DirtiesContext
public class KafkaOutboundAdapterParserTests {

	@Autowired
	private ApplicationContext appContext;

	@Test
	public void testOutboundAdapterConfiguration() {
		KafkaProducerMessageHandler<?, ?> messageHandler
			= this.appContext.getBean("kafkaOutboundChannelAdapter.handler", KafkaProducerMessageHandler.class);
		assertThat(messageHandler).isNotNull();
		assertThat(messageHandler.getOrder()).isEqualTo(3);
		assertThat(TestUtils.getPropertyValue(messageHandler, "topicExpression.literalValue")).isEqualTo("foo");
		assertThat(TestUtils.getPropertyValue(messageHandler, "messageKeyExpression.expression")).isEqualTo("'bar'");
		assertThat(TestUtils.getPropertyValue(messageHandler, "partitionIdExpression.expression")).isEqualTo("'2'");

		messageHandler
				= this.appContext.getBean("kafkaOutboundChannelAdapter2.handler", KafkaProducerMessageHandler.class);
		assertThat(messageHandler).isNotNull();
		assertThat(TestUtils.getPropertyValue(messageHandler, "partitionIdExpression.literalValue")).isEqualTo("0");
	}


	@Test
	public void testSyncMode() {
		MockProducer<Integer, String> mockProducer =
				new MockProducer<>(false, new IntegerSerializer(), new StringSerializer());
		KafkaTemplate<Integer, String> template = new KafkaTemplate<>(() -> mockProducer);
		KafkaProducerMessageHandler<Integer, String> handler = new KafkaProducerMessageHandler<>(template);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

		handler.setSync(true);
		handler.setTopicExpression(new LiteralExpression("foo"));

		Executors.newSingleThreadExecutor()
				.submit(() -> {
					RuntimeException exception = new RuntimeException("Async Producer Mock exception");
					while (!mockProducer.errorNext(exception)) {
						Thread.sleep(100);
					}
					return null;
				});

		assertThatExceptionOfType(MessageHandlingException.class)
				.isThrownBy(() -> handler.handleMessage(new GenericMessage<>("foo")))
				.withMessageContaining("Async Producer Mock exception")
				.withCauseExactlyInstanceOf(ExecutionException.class)
				.withRootCauseExactlyInstanceOf(RuntimeException.class);

		handler.setSendTimeout(1);

		assertThatExceptionOfType(MessageTimeoutException.class)
				.isThrownBy(() -> handler.handleMessage(new GenericMessage<>("foo")))
				.withMessageContaining("Timeout waiting for response from KafkaProducer")
				.withCauseExactlyInstanceOf(TimeoutException.class);
	}

}
