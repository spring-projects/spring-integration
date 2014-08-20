/*
 * Copyright 2002-2014 the original author or authors.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.messaging.Message;

/**
 * @author Soby Chacko
 * @since 0.5
 */
public class KafkaConsumerContextTest<K, V> {

	@Test
	@SuppressWarnings("unchecked")
	public void testMergeResultsFromMultipleConsumerConfiguration() {
		final KafkaConsumerContext<K, V> kafkaConsumerContext = new KafkaConsumerContext<K, V>();
		final ListableBeanFactory beanFactory = Mockito.mock(ListableBeanFactory.class);
		final ConsumerConfiguration<K, V> consumerConfiguration1 = Mockito.mock(ConsumerConfiguration.class);
		final ConsumerConfiguration<K, V> consumerConfiguration2 = Mockito.mock(ConsumerConfiguration.class);

		final Map<String, ConsumerConfiguration<K, V>> map = new HashMap<String, ConsumerConfiguration<K, V>>();
		map.put("config1", consumerConfiguration1);
		map.put("config2", consumerConfiguration2);

		kafkaConsumerContext.setConsumerConfigurations(map);

		final Map<String, Map<Integer, List<Object>>> result1 = new HashMap<String, Map<Integer, List<Object>>>();
		final List<Object> l1 = new ArrayList<Object>();
		l1.add("got message1 - l1");
		l1.add("got message2 - l1");
		final Map<Integer, List<Object>> innerMap1 = new HashMap<Integer, List<Object>>();
		innerMap1.put(1, l1);
		result1.put("topic1", innerMap1);

		final Map<String, Map<Integer, List<Object>>> result2 = new HashMap<String, Map<Integer, List<Object>>>();
		final List<Object> l2 = new ArrayList<Object>();
		l2.add("got message1 - l2");
		l2.add("got message2 - l2");
		l2.add("got message3 - l2");

		final Map<Integer, List<Object>> innerMap2 = new HashMap<Integer, List<Object>>();
		innerMap2.put(1, l2);
		result1.put("topic2", innerMap2);

		Mockito.when(consumerConfiguration1.receive()).thenReturn(result1);
		Mockito.when(consumerConfiguration2.receive()).thenReturn(result2);

		final Message<Map<String, Map<Integer, List<Object>>>> messages = kafkaConsumerContext.receive();
		Assert.assertEquals(messages.getPayload().size(), 2);
		Assert.assertEquals(messages.getPayload().get("topic1").size(), 1);
		Assert.assertEquals(messages.getPayload().get("topic1").get(1).get(0), "got message1 - l1");
		Assert.assertEquals(messages.getPayload().get("topic1").get(1).get(1), "got message2 - l1");

		Assert.assertEquals(messages.getPayload().get("topic2").size(), 1);
		Assert.assertEquals(messages.getPayload().get("topic2").get(1).get(0), "got message1 - l2");
		Assert.assertEquals(messages.getPayload().get("topic2").get(1).get(1), "got message2 - l2");
		Assert.assertEquals(messages.getPayload().get("topic2").get(1).get(2), "got message3 - l2");
	}

}
