/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.kafka.outbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.Collections;
import java.util.Map;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serializer;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.integration.kafka.support.KafkaHeaders;
import org.springframework.integration.kafka.support.KafkaProducerContext;
import org.springframework.integration.kafka.support.ProducerConfiguration;
import org.springframework.integration.kafka.support.ProducerListener;
import org.springframework.integration.kafka.support.ProducerListenerInvokingCallback;
import org.springframework.integration.kafka.support.ProducerMetadata;
import org.springframework.messaging.support.MessageBuilder;

/**
 * @author Marius Bogoevici
 */
public class ProducerListenerTests {

	@Test
	@SuppressWarnings({"unchecked","rawtypes"})
	public void testProducerListenerSet() throws Exception {
		KafkaProducerContext producerContext = new KafkaProducerContext();
		Serializer serializer = Mockito.mock(Serializer.class);
		ProducerMetadata<Object, Object> producerMetadata
				= new ProducerMetadata<>("default", Object.class, Object.class, serializer, serializer);
		Producer producer = Mockito.mock(Producer.class);
		ProducerConfiguration<Object, Object> producerConfiguration
				= new ProducerConfiguration<>(producerMetadata, producer);
		ProducerListener producerListener = mock(ProducerListener.class);
		producerConfiguration.setProducerListener(producerListener);
		Map<String, ProducerConfiguration<?, ?>> producerConfigurations
				= Collections.<String, ProducerConfiguration<?, ?>>singletonMap("default", producerConfiguration);
		producerContext.setProducerConfigurations(producerConfigurations);
		KafkaProducerMessageHandler handler = new KafkaProducerMessageHandler(producerContext);
		handler.handleMessage(
				MessageBuilder.withPayload("somePayload")
						.setHeader(KafkaHeaders.PARTITION_ID, 2)
						.setHeader(KafkaHeaders.MESSAGE_KEY, "someKey")
						.build());
		final ArgumentCaptor<Callback> argument = ArgumentCaptor.forClass(Callback.class);
		verify(producer).send(any(ProducerRecord.class), argument.capture());
		Callback callback = argument.getValue();
		assertThat(callback, CoreMatchers.instanceOf(ProducerListenerInvokingCallback.class));
		DirectFieldAccessor fieldAccessor = new DirectFieldAccessor(callback);
		assertEquals(fieldAccessor.getPropertyValue("topic"),"default");
		assertEquals(fieldAccessor.getPropertyValue("partition"),2);
		assertEquals(fieldAccessor.getPropertyValue("key"),"someKey");
		assertEquals(fieldAccessor.getPropertyValue("payload"),"somePayload");
		assertSame(fieldAccessor.getPropertyValue("producerListener"), producerListener);
		verifyNoMoreInteractions(producer);
	}

	@Test
	@SuppressWarnings({"unchecked","rawtypes"})
	public void testProducerListenerNotSet() throws Exception {
		KafkaProducerContext producerContext = new KafkaProducerContext();
		Serializer serializer = Mockito.mock(Serializer.class);
		ProducerMetadata<Object, Object> producerMetadata
				= new ProducerMetadata<>("default", Object.class, Object.class, serializer, serializer);
		Producer producer = Mockito.mock(Producer.class);
		ProducerConfiguration<Object, Object> producerConfiguration
				= new ProducerConfiguration<>(producerMetadata, producer);
		Map<String, ProducerConfiguration<?, ?>> producerConfigurations
				= Collections.<String, ProducerConfiguration<?, ?>>singletonMap("default", producerConfiguration);
		producerContext.setProducerConfigurations(producerConfigurations);
		KafkaProducerMessageHandler handler = new KafkaProducerMessageHandler(producerContext);
		handler.handleMessage(MessageBuilder.withPayload("somePayload").build());
		verify(producer).send(any(ProducerRecord.class));
		verifyNoMoreInteractions(producer);
	}
}
