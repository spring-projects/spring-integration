/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.transformer;

import java.util.Date;

import org.junit.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 */
public class PayloadTransformerTests {

	@Test
	public void testSuccessfulTransformation() {
		TestPayloadTransformer transformer = new TestPayloadTransformer();
		Message<?> message = new GenericMessage<String>("foo");
		Message<?> result = transformer.transform(message);
		assertThat(result.getPayload()).isEqualTo(3);
	}

	@Test(expected = MessagingException.class)
	public void testExceptionThrownByTransformer() {
		TestPayloadTransformer transformer = new TestPayloadTransformer();
		Message<?> message = new GenericMessage<String>("bad");
		transformer.transform(message);
	}

	@Test(expected = MessagingException.class)
	public void testWrongPayloadType() {
		TestPayloadTransformer transformer = new TestPayloadTransformer();
		Message<?> message = new GenericMessage<Date>(new Date());
		transformer.transform(message);
	}

	private static class TestPayloadTransformer extends AbstractPayloadTransformer<String, Integer> {

		TestPayloadTransformer() {
			super();
		}

		@Override
		public Integer transformPayload(String s) {
			if (s.equals("bad")) {
				throw new IllegalStateException("bad input!");
			}
			return s.length();
		}

	}

}
