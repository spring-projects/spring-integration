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

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.serialization.Serializer;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.core.convert.ConversionService;
import org.springframework.integration.kafka.support.ProducerConfiguration;
import org.springframework.integration.kafka.support.ProducerMetadata;

/**
 * @author Marius Bogoevici
 */
public class ProducerTest {

	@Test
	@SuppressWarnings({"unchecked","rawtypes"})
	public void producerContextDataConversionTest() {
		Producer<FooBase,BarBase> producer = (Producer<FooBase, BarBase>) Mockito.mock(Producer.class);
		Serializer serializer = Mockito.mock(Serializer.class);
		ProducerMetadata<FooBase,BarBase> producerMetadata =
				new ProducerMetadata<>("topic", FooBase.class, BarBase.class, serializer,serializer);
		ProducerConfiguration<FooBase,BarBase> producerConfiguration =
				new ProducerConfiguration<>(producerMetadata,producer);
		ConversionService conversionService = Mockito.mock(ConversionService.class);
		producerConfiguration.setConversionService(conversionService);
		producerConfiguration.convertAndSend("topic",0,new Foo(), new Bar());
		// the key and payload can be cast automatically, no conversion is necessary
		Mockito.verifyZeroInteractions(conversionService);
	}

	public static class FooBase {
	}

	public static class Foo extends FooBase {
	}

	public static class BarBase {
	}

	public static class Bar extends BarBase {
	}
}
