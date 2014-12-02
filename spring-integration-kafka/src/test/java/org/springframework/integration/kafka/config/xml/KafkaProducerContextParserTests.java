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
import static org.junit.Assert.assertFalse;

import java.util.Map;

import kafka.javaapi.producer.Producer;
import kafka.serializer.Encoder;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.kafka.rule.KafkaRunning;
import org.springframework.integration.kafka.support.KafkaProducerContext;
import org.springframework.integration.kafka.support.ProducerConfiguration;
import org.springframework.integration.kafka.support.ProducerMetadata;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Soby Chacko
 * @author Gary Russell
 * @since 0.5
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class KafkaProducerContextParserTests<K,V,T> {

	@ClassRule
	public static KafkaRunning kafkaRunning = KafkaRunning.isRunning();

	@Autowired
	private ApplicationContext appContext;

	@Test
	@SuppressWarnings("unchecked")
	public void testProducerContextConfiguration(){
		final KafkaProducerContext<K,V> producerContext = appContext.getBean("producerContext", KafkaProducerContext.class);
		Assert.assertNotNull(producerContext);

		final Map<String, ProducerConfiguration<K,V>> producerConfigurations = producerContext.getProducerConfigurations();
		Assert.assertEquals(producerConfigurations.size(), 2);

		final ProducerConfiguration<K,V> producerConfigurationTest1 = producerConfigurations.get("test1");
		Assert.assertNotNull(producerConfigurationTest1);
		final ProducerMetadata<K,V> producerMetadataTest1 = producerConfigurationTest1.getProducerMetadata();
		Assert.assertEquals(producerMetadataTest1.getTopic(), "test1");
		Assert.assertEquals(producerMetadataTest1.getCompressionCodec(), "0");
		Assert.assertEquals(producerMetadataTest1.getKeyClassType(), java.lang.String.class);
		Assert.assertEquals(producerMetadataTest1.getValueClassType(), java.lang.String.class);

		final Encoder<T> valueEncoder = appContext.getBean("valueEncoder", Encoder.class);
		Assert.assertEquals(producerMetadataTest1.getValueEncoder(), valueEncoder);
		Assert.assertEquals(producerMetadataTest1.getKeyEncoder(), valueEncoder);

		final Producer<K,V> producerTest1 = producerConfigurationTest1.getProducer();
		Assert.assertEquals(producerConfigurationTest1, new ProducerConfiguration<K,V>(producerMetadataTest1, producerTest1));

		final ProducerConfiguration<K,V> producerConfigurationTest2 = producerConfigurations.get("test2");
		Assert.assertNotNull(producerConfigurationTest2);
		final ProducerMetadata<K,V> producerMetadataTest2 = producerConfigurationTest2.getProducerMetadata();
		Assert.assertEquals(producerMetadataTest2.getTopic(), "test2");
		Assert.assertEquals(producerMetadataTest2.getCompressionCodec(), "0");

		final Producer<K,V> producerTest2 = producerConfigurationTest2.getProducer();
		Assert.assertEquals(producerConfigurationTest2, new ProducerConfiguration<K,V>(producerMetadataTest2, producerTest2));

		assertFalse(TestUtils.getPropertyValue(producerContext, "autoStartup", Boolean.class));
		assertEquals(123, TestUtils.getPropertyValue(producerContext, "phase"));
	}
}
