package org.springframework.integration.jdbc.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.jdbc.JdbcMessageStore;
import org.springframework.integration.store.MessageStore;
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

	@After
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

}
