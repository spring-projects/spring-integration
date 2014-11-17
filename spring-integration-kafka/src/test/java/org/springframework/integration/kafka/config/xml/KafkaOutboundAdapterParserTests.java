/*
 * Copyright 2013-2014 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.kafka.outbound.KafkaProducerMessageHandler;
import org.springframework.integration.kafka.rule.KafkaRunning;
import org.springframework.integration.kafka.support.KafkaProducerContext;
import org.springframework.integration.test.util.TestUtils;
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
public class KafkaOutboundAdapterParserTests<K, V> {

	@ClassRule
	public static KafkaRunning kafkaRunning = KafkaRunning.isRunning();

	@Autowired
	private ApplicationContext appContext;

	@Test
	@SuppressWarnings("unchecked")
	public void testOutboundAdapterConfiguration() {
		PollingConsumer pollingConsumer = this.appContext.getBean("kafkaOutboundChannelAdapter", PollingConsumer.class);
		KafkaProducerMessageHandler<K, V> messageHandler = this.appContext.getBean(KafkaProducerMessageHandler.class);
		assertNotNull(pollingConsumer);
		assertNotNull(messageHandler);
		assertEquals(messageHandler.getOrder(), 3);
		assertEquals("foo", TestUtils.getPropertyValue(messageHandler, "topicExpression.literalValue"));
		assertEquals("'bar'", TestUtils.getPropertyValue(messageHandler, "messageKeyExpression.expression"));
		KafkaProducerContext<K, V> producerContext = messageHandler.getKafkaProducerContext();
		assertNotNull(producerContext);
		assertEquals(producerContext.getProducerConfigurations().size(), 2);
	}

}
