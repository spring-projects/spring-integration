/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.kafka.offset;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import org.springframework.integration.kafka.core.Partition;
import org.springframework.integration.kafka.listener.KafkaTopicOffsetManager;

/**
 * @author Marius Bogoevici
 */
public class CodecTests {

	public static final String SOME_CONSUMER = "someConsumer";

	public static final String SOME_TOPIC = "someTopic";

	public static final int SOME_PARTITION_ID = 42;

	@Test
	public void testKeyCodec() {
		KafkaTopicOffsetManager.KeySerializerDecoder codec = new KafkaTopicOffsetManager.KeySerializerDecoder();

		byte[] encodedValue = codec.serialize("#unused", new KafkaTopicOffsetManager.Key(SOME_CONSUMER,
				new Partition(SOME_TOPIC, SOME_PARTITION_ID)));

		KafkaTopicOffsetManager.Key key = codec.fromBytes(encodedValue);

		assertThat(key.getConsumerId(), equalTo(SOME_CONSUMER));
		assertThat(key.getPartition().getTopic(), equalTo(SOME_TOPIC));
		assertThat(key.getPartition().getId(), equalTo(SOME_PARTITION_ID));
	}
	
}
