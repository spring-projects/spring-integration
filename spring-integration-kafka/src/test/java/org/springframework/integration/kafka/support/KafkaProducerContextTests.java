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
package org.springframework.integration.kafka.support;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;

import kafka.javaapi.producer.Producer;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ListableBeanFactory;

/**
 * @author Rajasekar Elango
 * @since 0.5
 */
public class KafkaProducerContextTests {

    @SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testTopicRegexForProducerConfiguration(){

		final KafkaProducerContext kafkaProducerContext = new KafkaProducerContext();
		final ListableBeanFactory beanFactory = Mockito.mock(ListableBeanFactory.class);

        final ProducerMetadata<String, String> producerMetadata = Mockito.mock(ProducerMetadata.class);

		String testRegex = "test.*";

		Mockito.when(producerMetadata.getTopic()).thenReturn(testRegex);
        final Producer<String, String> producer = Mockito.mock(Producer.class);

		final ProducerConfiguration<String, String> producerConfiguration = new ProducerConfiguration<String, String>(producerMetadata, producer);


		final Map<String, ProducerConfiguration> topicConfigurations = new HashMap<String, ProducerConfiguration>();
		topicConfigurations.put(testRegex, producerConfiguration);

		Mockito.when(beanFactory.getBeansOfType(ProducerConfiguration.class)).thenReturn(topicConfigurations);
		kafkaProducerContext.setBeanFactory(beanFactory);

		Assert.assertNotNull(kafkaProducerContext.getTopicConfiguration("test1"));
		Assert.assertNotNull(kafkaProducerContext.getTopicConfiguration("test2"));
		Assert.assertNotNull(kafkaProducerContext.getTopicConfiguration("testabc"));
		Assert.assertNull(kafkaProducerContext.getTopicConfiguration("dontmatch_testRegex"));

	}

}
