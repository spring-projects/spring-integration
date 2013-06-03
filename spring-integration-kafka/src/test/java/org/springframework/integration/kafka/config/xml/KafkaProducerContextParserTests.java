/*
 * Copyright 2002-2013 the original author or authors.
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

import org.junit.Assert;
import kafka.javaapi.producer.Producer;
import kafka.serializer.Encoder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.kafka.support.KafkaProducerContext;
import org.springframework.integration.kafka.support.ProducerConfiguration;
import org.springframework.integration.kafka.support.ProducerMetadata;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Map;

/**
 * @author Soby Chacko
 * @since 0.5
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class KafkaProducerContextParserTests {

	@Autowired
	private ApplicationContext appContext;

	@Test
	@SuppressWarnings("unchecked")
	public void testProducerContextConfiguration(){
		final KafkaProducerContext producerContext = appContext.getBean("producerContext", KafkaProducerContext.class);
		Assert.assertNotNull(producerContext);

		final Map<String, ProducerConfiguration> topicConfigurations = producerContext.getTopicsConfiguration();
		Assert.assertEquals(topicConfigurations.size(), 2);

		final ProducerConfiguration producerConfigurationTest1 = topicConfigurations.get("producerConfiguration_test1");
		Assert.assertNotNull(producerConfigurationTest1);
		final ProducerMetadata producerMetadataTest1 = producerConfigurationTest1.getProducerMetadata();
		Assert.assertEquals(producerMetadataTest1.getTopic(), "test1");
		Assert.assertEquals(producerMetadataTest1.getCompressionCodec(), "0");
		Assert.assertEquals(producerMetadataTest1.getKeyClassType(), java.lang.String.class);
		Assert.assertEquals(producerMetadataTest1.getValueClassType(), java.lang.String.class);

		final Encoder valueEncoder = appContext.getBean("valueEncoder", Encoder.class);
		Assert.assertEquals(producerMetadataTest1.getValueEncoder(), valueEncoder);
		Assert.assertEquals(producerMetadataTest1.getKeyEncoder(), valueEncoder);

		final Producer producerTest1 = appContext.getBean("prodFactory_test1", Producer.class);
		Assert.assertEquals(producerConfigurationTest1, new ProducerConfiguration(producerMetadataTest1, producerTest1));

		final ProducerConfiguration producerConfigurationTest2 = topicConfigurations.get("producerConfiguration_" + "test2");
		Assert.assertNotNull(producerConfigurationTest2);
		final ProducerMetadata producerMetadataTest2 = producerConfigurationTest2.getProducerMetadata();
		Assert.assertEquals(producerMetadataTest2.getTopic(), "test2");
		Assert.assertEquals(producerMetadataTest2.getCompressionCodec(), "0");

		final Producer producerTest2 = appContext.getBean("prodFactory_test2", Producer.class);
		Assert.assertEquals(producerConfigurationTest2, new ProducerConfiguration(producerMetadataTest2, producerTest2));
	}
}
