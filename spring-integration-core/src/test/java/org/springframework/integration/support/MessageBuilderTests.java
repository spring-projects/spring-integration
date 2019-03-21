/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.integration.support;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import org.springframework.messaging.Message;

/**
 * @author Gary Russell
 * @since 4.3.10
 *
 */
public class MessageBuilderTests {

	@Test
	public void testReadOnlyHeaders() {
		DefaultMessageBuilderFactory factory = new DefaultMessageBuilderFactory();
		Message<?> message = factory.withPayload("bar").setHeader("foo", "baz").setHeader("qux", "fiz").build();
		assertThat(message.getHeaders().get("foo"), equalTo("baz"));
		assertThat(message.getHeaders().get("qux"), equalTo("fiz"));
		factory.setReadOnlyHeaders("foo");
		message = factory.fromMessage(message).build();
		assertNull(message.getHeaders().get("foo"));
		assertThat(message.getHeaders().get("qux"), equalTo("fiz"));
		factory.addReadOnlyHeaders("qux");
		message = factory.fromMessage(message).build();
		assertNull(message.getHeaders().get("foo"));
		assertNull(message.getHeaders().get("qux"));
	}

}
