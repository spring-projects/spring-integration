/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.transformer;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.junit.Test;

import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class PayloadTransformerAdapterTests {

	@Test
	public void testSimpleMethod() {
		PayloadTransformerAdapter adapter = new PayloadTransformerAdapter();
		adapter.setObject(new TestBean());
		adapter.setMethodName("exclaim");
		Message<?> message = new StringMessage("foo");
		Message<?> result = adapter.transform(message);
		assertEquals("FOO!", result.getPayload());
	}

	@Test
	public void testTypeConversion() {
		PayloadTransformerAdapter adapter = new PayloadTransformerAdapter();
		adapter.setObject(new TestBean());
		adapter.setMethodName("exclaim");
		Message<?> message = new GenericMessage<Integer>(123);
		Message<?> result = adapter.transform(message);
		assertEquals("123!", result.getPayload());
	}

	@Test(expected=MessagingException.class)
	public void testTypeConversionFailure() {
		PayloadTransformerAdapter adapter = new PayloadTransformerAdapter();
		adapter.setObject(new TestBean());
		adapter.setMethodName("exclaim");
		Message<?> message = new GenericMessage<Date>(new Date());
		adapter.transform(message);
	}


	private static class TestBean {

		String exclaim(String input) {
			return input.toUpperCase() + "!";
		}
	}

}
