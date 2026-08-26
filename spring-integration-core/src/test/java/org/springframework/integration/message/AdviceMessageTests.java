/*
 * Copyright 2026-present the original author or authors.
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

package org.springframework.integration.message;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Alexander Makarov
 *
 * @since 7.0.7
 */
public class AdviceMessageTests {

	private static final Message<?> INPUT_MESSAGE = new GenericMessage<>("input");

	@Test
	void equalsWhenPayloadHeadersAndInputMessageMatch() {
		MessageHeaders headers = new MessageHeaders(null);

		AdviceMessage<String> one = new AdviceMessage<>("test", headers, INPUT_MESSAGE);
		AdviceMessage<String> two = new AdviceMessage<>("test", headers, INPUT_MESSAGE);

		assertThat(one).isEqualTo(two);
		assertThat(one.hashCode()).isEqualTo(two.hashCode());
	}

	@Test
	void notEqualsWhenInputMessageDiffers() {
		MessageHeaders headers = new MessageHeaders(null);

		AdviceMessage<String> one = new AdviceMessage<>("test", headers, new GenericMessage<>("input1"));
		AdviceMessage<String> two = new AdviceMessage<>("test", headers, new GenericMessage<>("input2"));

		assertThat(one).isNotEqualTo(two);
	}

	@Test
	void notEqualsWhenPayloadDiffersButInputMessageIsShared() {
		AdviceMessage<String> one = new AdviceMessage<>("test1", INPUT_MESSAGE);
		AdviceMessage<String> two = new AdviceMessage<>("test2", INPUT_MESSAGE);

		assertThat(one).isNotEqualTo(two);
	}

	@Test
	void notEqualsWhenHeadersDifferButPayloadAndInputMessageAreShared() {
		AdviceMessage<String> one = new AdviceMessage<>("test", Map.of("testHeader", "one"), INPUT_MESSAGE);
		AdviceMessage<String> two = new AdviceMessage<>("test", Map.of("testHeader", "two"), INPUT_MESSAGE);

		assertThat(one).isNotEqualTo(two);
	}

	@Test
	void notEqualsPlainGenericMessage() {
		MessageHeaders headers = new MessageHeaders(null);

		AdviceMessage<String> adviceMessage = new AdviceMessage<>("test", headers, INPUT_MESSAGE);
		GenericMessage<String> genericMessage = new GenericMessage<>("test", headers);

		assertThat(adviceMessage).isNotEqualTo(genericMessage);
	}

	@Test
	void notEqualsNullOrOtherType() {
		AdviceMessage<String> adviceMessage = new AdviceMessage<>("test", new GenericMessage<>("input"));

		assertThat(adviceMessage).isNotEqualTo(null).isNotEqualTo("test");
	}

}
