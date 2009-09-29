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

import java.lang.reflect.Method;

import org.junit.Test;
import org.springframework.integration.core.Message;
import org.springframework.integration.handler.ArgumentArrayMessageMapper;

/**
 * @author Mark Fisher
 */
public class ArgumentArrayMessageMapperInitializationTests {

	@Test(expected = IllegalArgumentException.class)
	public void messageAndPayload() throws Exception {
		Method method = TestService.class.getMethod("messageAndPayload", Message.class, String.class);
		new ArgumentArrayMessageMapper(method);
	}

	@Test(expected = IllegalArgumentException.class)
	public void twoMessages() throws Exception {
		Method method = TestService.class.getMethod("twoMessages", Message.class, Message.class);
		new ArgumentArrayMessageMapper(method);
	}

	@Test(expected = IllegalArgumentException.class)
	public void twoPayloads() throws Exception {
		Method method = TestService.class.getMethod("twoPayloads", String.class, String.class);
		new ArgumentArrayMessageMapper(method);
	}


	private static interface TestService {

		void messageAndPayload(Message<?> message, String foo);

		void twoMessages(Message<?> message1, Message<?> message2);

		void twoPayloads(String foo, String bar);

	}

}
