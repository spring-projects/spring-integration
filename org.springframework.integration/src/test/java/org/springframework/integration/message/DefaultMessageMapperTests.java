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

import org.junit.Test;

/**
 * @author Mark Fisher
 */
public class DefaultMessageMapperTests {

	@Test
	public void testStringPayload() {
		DefaultMessageMapper mapper = new DefaultMessageMapper();
		String result = (String) mapper.mapMessage(new StringMessage("testing"));
		assertEquals("testing", result);
	}

	@Test
	public void testObjectPayload() {
		DefaultMessageMapper mapper = new DefaultMessageMapper();
		Object test = new Object();
		Object result = mapper.mapMessage(new GenericMessage<Object>(test));
		assertEquals(test, result);
	}

	@Test
	public void testNullMessage() {
		DefaultMessageMapper mapper = new DefaultMessageMapper();
		Object result = mapper.mapMessage(null);
		assertEquals(null, result);
	}

}
