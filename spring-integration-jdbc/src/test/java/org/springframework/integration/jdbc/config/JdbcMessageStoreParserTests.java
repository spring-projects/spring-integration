/*
 * Copyright 2016-2024 the original author or authors.
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

package org.springframework.integration.jdbc.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.serializer.DefaultDeserializer;
import org.springframework.core.serializer.DefaultSerializer;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.integration.jdbc.store.JdbcMessageStore;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Dave Syer
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
public class JdbcMessageStoreParserTests {

	private ClassPathXmlApplicationContext context;

	@Test
	public void testSimpleMessageStoreWithDataSource() {
		setUp("defaultJdbcMessageStore.xml", getClass());
		MessageStore store = context.getBean("messageStore", MessageStore.class);
		assertThat(store).isInstanceOf(JdbcMessageStore.class);
	}

	@Test
	public void testSimpleMessageStoreWithTemplate() {
		setUp("jdbcOperationsJdbcMessageStore.xml", getClass());
		MessageStore store = context.getBean("messageStore", MessageStore.class);
		assertThat(store).isInstanceOf(JdbcMessageStore.class);
	}

	@Test
	public void testSimpleMessageStoreWithSerializer() {
		setUp("serializerJdbcMessageStore.xml", getClass());
		MessageStore store = context.getBean("messageStore", MessageStore.class);
		Object serializer = TestUtils.getPropertyValue(store, "serializer.serializer");
		assertThat(serializer).isInstanceOf(EnhancedSerializer.class);
		Object deserializer = TestUtils.getPropertyValue(store, "deserializer.deserializer");
		assertThat(deserializer).isInstanceOf(EnhancedSerializer.class);
	}

	@Test
	public void testMessageStoreWithAttributes() {
		setUp("soupedUpJdbcMessageStore.xml", getClass());
		MessageStore store = context.getBean("messageStore", MessageStore.class);
		assertThat(ReflectionTestUtils.getField(store, "region")).isEqualTo("FOO");
		assertThat(ReflectionTestUtils.getField(store, "tablePrefix")).isEqualTo("BAR_");
	}

	@AfterEach
	public void tearDown() {
		if (context != null) {
			context.close();
		}
	}

	public void setUp(String name, Class<?> cls) {
		context = new ClassPathXmlApplicationContext(name, cls);
	}

	public static class EnhancedSerializer implements Serializer<Object>, Deserializer<Object> {

		private final Serializer<Object> targetSerializer = new DefaultSerializer();

		private final Deserializer<Object> targetDeserializer = new DefaultDeserializer();

		public Object deserialize(InputStream inputStream) throws IOException {
			return targetDeserializer.deserialize(inputStream);
		}

		public void serialize(Object object, OutputStream outputStream) throws IOException {
			Message<?> message = (Message<?>) object;
			message = MessageBuilder.fromMessage(message).setHeader("serializer", "CUSTOM").build();
			targetSerializer.serialize(message, outputStream);
		}

	}

}
