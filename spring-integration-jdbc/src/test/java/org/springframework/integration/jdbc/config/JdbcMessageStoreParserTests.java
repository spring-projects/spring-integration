package org.springframework.integration.jdbc.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.After;
import org.junit.Test;
import org.springframework.commons.serializer.java.JavaStreamingConverter;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.jdbc.JdbcMessageStore;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.test.util.ReflectionTestUtils;

public class JdbcMessageStoreParserTests {

	private ClassPathXmlApplicationContext context;

	@Test
	public void testSimpleMessageStoreWithDataSource() {
		setUp("defaultJdbcMessageStore.xml", getClass());
		MessageStore store = context.getBean("messageStore", MessageStore.class);
		assertTrue(store instanceof JdbcMessageStore);
	}

	@Test
	public void testSimpleMessageStoreWithTemplate() {
		setUp("jdbcOperationsJdbcMessageStore.xml", getClass());
		MessageStore store = context.getBean("messageStore", MessageStore.class);
		assertTrue(store instanceof JdbcMessageStore);
	}

	@Test
	public void testSimpleMessageStoreWithSerializer() {
		setUp("serializerJdbcMessageStore.xml", getClass());
		MessageStore store = context.getBean("messageStore", MessageStore.class);
		Object serializer = TestUtils.getPropertyValue(store, "serializer.streamingConverter");
		assertTrue(serializer instanceof EnhancedSerializer);
		Object deserializer = TestUtils.getPropertyValue(store, "deserializer.streamingConverter");
		assertTrue(deserializer instanceof EnhancedSerializer);
	}

	@Test
	public void testMessageStoreWithAttributes() {
		setUp("soupedUpJdbcMessageStore.xml", getClass());
		MessageStore store = context.getBean("messageStore", MessageStore.class);
		assertEquals("FOO", ReflectionTestUtils.getField(store, "region"));
		assertEquals("BAR_", ReflectionTestUtils.getField(store, "tablePrefix"));
		assertEquals(context.getBean(LobHandler.class), ReflectionTestUtils.getField(store, "lobHandler"));
	}

	@After
	public void tearDown() {
		if (context != null) {
			context.close();
		}
	}

	public void setUp(String name, Class<?> cls) {
		context = new ClassPathXmlApplicationContext(name, cls);
	}

	public static class EnhancedSerializer extends JavaStreamingConverter {
		@Override
		public Object convert(InputStream inputStream) throws IOException {
			Message<?> message = (Message<?>) super.convert(inputStream);
			return message;
		}

		@Override
		public void convert(Object object, OutputStream outputStream) throws IOException {
			Message<?> message = (Message<?>) object;
			super.convert(MessageBuilder.fromMessage(message).setHeader("serializer", "CUSTOM").build(), outputStream);
		}
	}

}
