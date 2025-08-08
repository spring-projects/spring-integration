/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
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
import org.springframework.jdbc.support.lob.LobHandler;
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
		assertThat(store instanceof JdbcMessageStore).isTrue();
	}

	@Test
	public void testSimpleMessageStoreWithTemplate() {
		setUp("jdbcOperationsJdbcMessageStore.xml", getClass());
		MessageStore store = context.getBean("messageStore", MessageStore.class);
		assertThat(store instanceof JdbcMessageStore).isTrue();
	}

	@Test
	public void testSimpleMessageStoreWithSerializer() {
		setUp("serializerJdbcMessageStore.xml", getClass());
		MessageStore store = context.getBean("messageStore", MessageStore.class);
		Object serializer = TestUtils.getPropertyValue(store, "serializer.serializer");
		assertThat(serializer instanceof EnhancedSerializer).isTrue();
		Object deserializer = TestUtils.getPropertyValue(store, "deserializer.deserializer");
		assertThat(deserializer instanceof EnhancedSerializer).isTrue();
	}

	@Test
	public void testMessageStoreWithAttributes() {
		setUp("soupedUpJdbcMessageStore.xml", getClass());
		MessageStore store = context.getBean("messageStore", MessageStore.class);
		assertThat(ReflectionTestUtils.getField(store, "region")).isEqualTo("FOO");
		assertThat(ReflectionTestUtils.getField(store, "tablePrefix")).isEqualTo("BAR_");
		assertThat(ReflectionTestUtils.getField(store, "lobHandler")).isEqualTo(context.getBean(LobHandler.class));
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
			Message<?> message = (Message<?>) targetDeserializer.deserialize(inputStream);
			return message;
		}

		public void serialize(Object object, OutputStream outputStream) throws IOException {
			Message<?> message = (Message<?>) object;
			message = MessageBuilder.fromMessage(message).setHeader("serializer", "CUSTOM").build();
			targetSerializer.serialize(message, outputStream);
		}

	}

}
