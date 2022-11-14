/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.aggregator;

import java.util.Comparator;

import org.junit.Test;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class MessageSequenceComparatorTests {

	@Test
	public void testLessThan() {
		Comparator<Message<?>> comparator = new MessageSequenceComparator();
		Message<String> message1 = MessageBuilder.withPayload("test1")
				.setSequenceNumber(1).build();
		Message<String> message2 = MessageBuilder.withPayload("test2")
				.setSequenceNumber(2).build();
		assertThat(comparator.compare(message1, message2)).isEqualTo(-1);
	}

	@Test
	public void testEqual() {
		Comparator<Message<?>> comparator = new MessageSequenceComparator();
		Message<String> message1 = MessageBuilder.withPayload("test1")
				.setSequenceNumber(3).build();
		Message<String> message2 = MessageBuilder.withPayload("test2")
				.setSequenceNumber(3).build();
		assertThat(comparator.compare(message1, message2)).isEqualTo(0);
	}

	@Test
	public void testGreaterThan() {
		Comparator<Message<?>> comparator = new MessageSequenceComparator();
		Message<String> message1 = MessageBuilder.withPayload("test1")
				.setSequenceNumber(5).build();
		Message<String> message2 = MessageBuilder.withPayload("test2")
				.setSequenceNumber(3).build();
		assertThat(comparator.compare(message1, message2)).isEqualTo(1);
	}

	@Test
	public void testEqualWithDefaultValues() {
		Comparator<Message<?>> comparator = new MessageSequenceComparator();
		Message<String> message1 = MessageBuilder.withPayload("test1").build();
		Message<String> message2 = MessageBuilder.withPayload("test2").build();
		assertThat(comparator.compare(message1, message2)).isEqualTo(0);
	}

}
