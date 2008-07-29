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
import static org.junit.Assert.assertNull;

import java.util.Map;

import org.junit.Test;

import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;

/**
 * @author Mark Fisher
 */
public class HeaderTransformingMessageHandlerTests {

	@Test
	public void addHeader() {
		HeaderTransformer transformer = new HeaderTransformer() {
			public void transform(Map<String, Object> headers) {
				headers.put("header2", "baz");
			}
		};
		HeaderTransformingMessageHandler handler = new HeaderTransformingMessageHandler(transformer);
		Message<String> message = MessageBuilder.fromPayload("foo").setHeader("header1", "bar").build();
		Message<?> result = handler.handle(message);
		assertEquals("bar", result.getHeaders().get("header1"));
		assertEquals("baz", result.getHeaders().get("header2"));
	}

	@Test
	public void replaceHeader() {
		HeaderTransformer transformer = new HeaderTransformer() {
			public void transform(Map<String, Object> headers) {
				headers.put("header1", "baz");
			}
		};
		HeaderTransformingMessageHandler handler = new HeaderTransformingMessageHandler(transformer);
		Message<String> message = MessageBuilder.fromPayload("foo").setHeader("header1", "bar").build();
		Message<?> result = handler.handle(message);
		assertEquals("baz", result.getHeaders().get("header1"));
	}

	@Test
	public void removeHeader() {
		HeaderTransformer transformer = new HeaderTransformer() {
			public void transform(Map<String, Object> headers) {
				headers.remove("header1");
			}
		};
		HeaderTransformingMessageHandler handler = new HeaderTransformingMessageHandler(transformer);
		Message<String> message = MessageBuilder.fromPayload("foo").setHeader("header1", "bar").build();
		Message<?> result = handler.handle(message);
		assertNull(result.getHeaders().get("header1"));
	}

}
