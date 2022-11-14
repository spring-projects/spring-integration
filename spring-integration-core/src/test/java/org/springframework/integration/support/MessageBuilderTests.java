/*
 * Copyright 2017-2022 the original author or authors.
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

import org.junit.Test;

import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;

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
		assertThat(message.getHeaders().get("foo")).isEqualTo("baz");
		assertThat(message.getHeaders().get("qux")).isEqualTo("fiz");
		factory.setReadOnlyHeaders("foo");
		message = factory.fromMessage(message).build();
		assertThat(message.getHeaders().get("foo")).isNull();
		assertThat(message.getHeaders().get("qux")).isEqualTo("fiz");
		factory.addReadOnlyHeaders("qux");
		message = factory.fromMessage(message).build();
		assertThat(message.getHeaders().get("foo")).isNull();
		assertThat(message.getHeaders().get("qux")).isNull();
	}

}
