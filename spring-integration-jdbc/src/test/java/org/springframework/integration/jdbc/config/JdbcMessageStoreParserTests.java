package org.springframework.integration.jdbc.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.serializer.DefaultDeserializer;
import org.springframework.core.serializer.DefaultSerializer;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.integration.Message;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.jdbc.JdbcMessageStore;
import org.springframework.integration.store.MessageGroupQueue;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JdbcMessageStoreParserTests {

	private ClassPathXmlApplicationContext context;

	@Test
	public void testSimpleMessageStoreWithDataSource() {
		setUp("defaultJdbcMessageStore.xml", getClass());
		MessageStore store = context.getBean("messageStore", MessageStore.class);
		assertTrue(store instanceof JdbcMessageStore);
	}
	
	@Test
	public void validateMessagesAreNotPolledOnInit() {

		setUp("defaultJdbcMessageStoreWithChannel.xml", getClass());
		
		QueueChannel storeBackedChannel = context.getBean("storeBackedChannel", QueueChannel.class);
		MessageGroupQueue queue = TestUtils.getPropertyValue(storeBackedChannel, "queue", MessageGroupQueue.class);
		queue = Mockito.spy(queue);
		DirectFieldAccessor ac = new DirectFieldAccessor(storeBackedChannel);
		ac.setPropertyValue("queue", queue);
		
		Mockito.verify(queue, Mockito.times(0)).poll();
		storeBackedChannel.receive(0);
		Mockito.verify(queue, Mockito.times(1)).poll();
		
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
		Object serializer = TestUtils.getPropertyValue(store, "serializer.serializer");
		assertTrue(serializer instanceof EnhancedSerializer);
		Object deserializer = TestUtils.getPropertyValue(store, "deserializer.deserializer");
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
