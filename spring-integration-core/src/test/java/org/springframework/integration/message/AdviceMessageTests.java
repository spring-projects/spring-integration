/*
 * Copyright 2002-present the original author or authors.
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
 */
public class AdviceMessageTests {

	@Test
	public void equalsWhenPayloadHeadersAndInputMessageMatch() {
		Message<?> inputMessage = new GenericMessage<>("input");
		MessageHeaders headers = new GenericMessage<>("ignored").getHeaders();

		AdviceMessage<String> one = new AdviceMessage<>("test", headers, inputMessage);
		AdviceMessage<String> two = new AdviceMessage<>("test", headers, inputMessage);

		assertThat(one).isEqualTo(two);
		assertThat(one.hashCode()).isEqualTo(two.hashCode());
	}

	@Test
	public void notEqualsWhenInputMessageDiffers() {
		MessageHeaders headers = new GenericMessage<>("ignored").getHeaders();

		AdviceMessage<String> one = new AdviceMessage<>("test", headers, new GenericMessage<>("input1"));
		AdviceMessage<String> two = new AdviceMessage<>("test", headers, new GenericMessage<>("input2"));

		assertThat(one).isNotEqualTo(two);
	}

	@Test
	public void notEqualsWhenPayloadDiffersButInputMessageIsShared() {
		Message<?> inputMessage = new GenericMessage<>("input");

		AdviceMessage<String> one = new AdviceMessage<>("test1", inputMessage);
		AdviceMessage<String> two = new AdviceMessage<>("test2", inputMessage);

		assertThat(one).isNotEqualTo(two);
	}

	@Test
	public void notEqualsWhenHeadersDifferButPayloadAndInputMessageAreShared() {
		Message<?> inputMessage = new GenericMessage<>("input");

		AdviceMessage<String> one = new AdviceMessage<>("test", Map.of("testHeader", "one"), inputMessage);
		AdviceMessage<String> two = new AdviceMessage<>("test", Map.of("testHeader", "two"), inputMessage);

		assertThat(one).isNotEqualTo(two);
	}

	@Test
	public void notEqualsPlainGenericMessage() {
		Message<?> inputMessage = new GenericMessage<>("input");
		MessageHeaders headers = new GenericMessage<>("ignored").getHeaders();

		AdviceMessage<String> adviceMessage = new AdviceMessage<>("test", headers, inputMessage);
		GenericMessage<String> genericMessage = new GenericMessage<>("test", headers);

		assertThat(adviceMessage).isNotEqualTo(genericMessage);
	}

	@Test
	public void notEqualsNullOrOtherType() {
		AdviceMessage<String> adviceMessage = new AdviceMessage<>("test", new GenericMessage<>("input"));

		assertThat(adviceMessage).isNotEqualTo(null);
		assertThat(adviceMessage).isNotEqualTo("test");
	}

}
