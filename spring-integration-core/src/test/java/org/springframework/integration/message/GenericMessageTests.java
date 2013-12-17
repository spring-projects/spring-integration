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

package org.springframework.integration.message;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Mark Fisher
 */
public class GenericMessageTests {

	@Test
	public void testMessageHeadersCopiedFromMap() {
		Map<String, Object> headerMap = new HashMap<String, Object>();
		headerMap.put("testAttribute", new Integer(123));
		headerMap.put("testProperty", "foo");
		headerMap.put(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE, 42);
		headerMap.put(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER, 24);
		GenericMessage<String> message = new GenericMessage<String>("test", headerMap);
		assertEquals(new Integer(123), message.getHeaders().get("testAttribute"));
		assertEquals("foo", message.getHeaders().get("testProperty", String.class));
		assertEquals(new Integer(42), new IntegrationMessageHeaderAccessor(message).getSequenceSize());
		assertEquals(new Integer(24), new IntegrationMessageHeaderAccessor(message).getSequenceNumber());
	}

}
