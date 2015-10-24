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

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import java.util.Map;

import kafka.producer.Partitioner;
import kafka.serializer.Encoder;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.serialization.Serializer;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.integration.kafka.rule.KafkaEmbedded;
import org.springframework.integration.kafka.rule.KafkaRule;
import org.springframework.integration.kafka.rule.KafkaRunning;
import org.springframework.integration.kafka.support.KafkaProducerContext;
import org.springframework.integration.kafka.support.ProducerConfiguration;
import org.springframework.integration.kafka.support.ProducerListener;
import org.springframework.integration.kafka.support.ProducerMetadata;
import org.springframework.integration.kafka.util.EncoderAdaptingSerializer;
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
public class KafkaProducerContextParserTests {

	@ClassRule
	public static KafkaRule kafkaRule = new KafkaEmbedded(1);

	@Autowired
	private ApplicationContext appContext;

	@Test
	@SuppressWarnings({"unchecked","rawtypes"})
	public void testProducerContextConfiguration(){
		final KafkaProducerContext producerContext = appContext.getBean("producerContext", KafkaProducerContext.class);
		Assert.assertNotNull(producerContext);

		final Map<String, ProducerConfiguration<?,?>> producerConfigurations = producerContext.getProducerConfigurations();
		assertEquals(producerConfigurations.size(), 2);

		final ProducerConfiguration<?,?> producerConfigurationTest1 = producerConfigurations.get("test1");
		Assert.assertNotNull(producerConfigurationTest1);
		final ProducerMetadata<?,?> producerMetadataTest1 = producerConfigurationTest1.getProducerMetadata();
		assertEquals(producerMetadataTest1.getTopic(), "test1");
		assertEquals(producerMetadataTest1.getCompressionType(), ProducerMetadata.CompressionType.none);
		assertEquals(producerMetadataTest1.getKeyClassType(), java.lang.String.class);
		assertEquals(producerMetadataTest1.getValueClassType(), java.lang.String.class);

		final Encoder<?> valueEncoder = appContext.getBean("valueEncoder", Encoder.class);
		Assert.assertThat((Class) producerMetadataTest1.getKeySerializer().getClass(), equalTo((Class) EncoderAdaptingSerializer.class));
		Assert.assertThat(((EncoderAdaptingSerializer) producerMetadataTest1.getKeySerializer()).getEncoder(), equalTo((Encoder) valueEncoder));
		Assert.assertThat((Class)producerMetadataTest1.getValueSerializer().getClass(), equalTo((Class)EncoderAdaptingSerializer.class));
		Assert.assertThat(((EncoderAdaptingSerializer)producerMetadataTest1.getValueSerializer()).getEncoder(), equalTo((Encoder)valueEncoder));

		DirectFieldAccessor directFieldAccessor = new DirectFieldAccessor(producerConfigurationTest1);
		Producer<?,?> producerTest1 = (Producer<?, ?>) directFieldAccessor.getPropertyValue("producer");
		assertEquals(producerConfigurationTest1.getProducerMetadata(), producerMetadataTest1);

		final ProducerConfiguration<?,?> producerConfigurationTest2 = producerConfigurations.get("test2");
		Assert.assertNotNull(producerConfigurationTest2);
		final ProducerMetadata<?,?> producerMetadataTest2 = producerConfigurationTest2.getProducerMetadata();
		assertEquals(producerMetadataTest2.getTopic(), "test2");
		assertEquals(producerMetadataTest2.getCompressionType(), ProducerMetadata.CompressionType.none);

		DirectFieldAccessor directFieldAccessor2 = new DirectFieldAccessor(producerConfigurationTest2);
		Producer<?,?> producerTest2 = (Producer<?, ?>) directFieldAccessor2.getPropertyValue("producer");
		assertEquals(producerConfigurationTest2.getProducerMetadata(), producerMetadataTest2);

		final Serializer<?> stringSerializer = appContext.getBean("stringSerializer", Serializer.class);
		assertSame(stringSerializer, producerConfigurationTest2.getProducerMetadata().getKeySerializer());
		assertSame(stringSerializer, producerConfigurationTest2.getProducerMetadata().getValueSerializer());

		final Partitioner partitioner = appContext.getBean("partitioner", Partitioner.class);
		assertSame(partitioner, producerConfigurationTest2.getProducerMetadata().getPartitioner());

		final ConversionService conversionService = appContext.getBean("conversionService", ConversionService.class);
		ConversionService configuredConversionService = (ConversionService) directFieldAccessor2.getPropertyValue("conversionService");
		assertSame(conversionService, configuredConversionService);

		final ProducerListener producerListener = appContext.getBean("producerListener", ProducerListener.class);
		ProducerListener configuredProducerListener = (ProducerListener) directFieldAccessor2.getPropertyValue("producerListener");
		assertSame(producerListener, configuredProducerListener);

		assertEquals(9876,producerConfigurationTest2.getProducerMetadata().getBatchBytes());

		assertFalse(TestUtils.getPropertyValue(producerContext, "autoStartup", Boolean.class));
		assertEquals(123, TestUtils.getPropertyValue(producerContext, "phase"));
	}

	public static class StubConversionService implements ConversionService {
		@Override
		public boolean canConvert(Class<?> sourceType, Class<?> targetType) {
			return false;
		}

		@Override
		public boolean canConvert(TypeDescriptor sourceType, TypeDescriptor targetType) {
			return false;
		}

		@Override
		public <T> T convert(Object source, Class<T> targetType) {
			return null;
		}

		@Override
		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			return null;
		}
	}
}
