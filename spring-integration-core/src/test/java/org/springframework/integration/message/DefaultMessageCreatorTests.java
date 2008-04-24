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
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * @author Mark Fisher
 */
public class DefaultMessageCreatorTests {

	@Test
	public void testStringPayload() {
		DefaultMessageCreator creator = new DefaultMessageCreator();
		Message<?> message = creator.createMessage("testing");
		assertEquals("testing", message.getPayload());
	}

	@Test
	public void testObjectPayload() {
		DefaultMessageCreator creator = new DefaultMessageCreator();
		Object test = new Object();
		Message<?> message = creator.createMessage(test);
		assertEquals(test, message.getPayload());
	}

	@Test
	public void testNull() {
		DefaultMessageCreator creator = new DefaultMessageCreator();
		Message<?> message = creator.createMessage(null);
		assertNull(message);
	}

}
