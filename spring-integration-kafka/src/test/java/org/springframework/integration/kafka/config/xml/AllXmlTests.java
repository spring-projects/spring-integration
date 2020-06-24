/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.kafka.config.xml;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Gary Russell
 *
 * @since 5.4
 *
 */
@SpringJUnitConfig
@DirtiesContext
@EmbeddedKafka(topics = { "one", "two", "three", "four" })
public class AllXmlTests {

	@Autowired
	private KafkaTemplate<String, String> template;

	@Autowired
	private PollableChannel lastChannel;

	@Test
	public void testEndToEnd() {
		this.template.send("one", "foo");
		Message<?> received = this.lastChannel.receive(30_000);
		assertThat(received).isNotNull();
		assertThat(received.getPayload()).isEqualTo("fooonetwothreefour");
	}

}
