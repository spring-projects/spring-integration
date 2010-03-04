package org.springframework.integration.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.jdbc.SimpleJdbcTestUtils;
import org.springframework.transaction.annotation.Transactional;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class JdbcMessageStoreTests {
	
	@Autowired
	private DataSource dataSource;

	@Autowired
	@Qualifier("messageIncrementer")
	private DataFieldMaxValueIncrementer messageIncrementer;
	
	
	private JdbcMessageStore messageStore;

	private SimpleJdbcTemplate jdbcTemplate;

	@Before
	public void init() {
		messageStore = new JdbcMessageStore(dataSource, messageIncrementer);
		jdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}
	
	@Test
	@Transactional
	public void testGetNonExistent() throws Exception {
		Message<?> result = messageStore.get(12345);
		assertNull(result);
	}

	@Test
	@Transactional
	public void testAddAndGet() throws Exception {
		Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId("X").build();
		message = messageStore.put(message);
		Message<?> result = messageStore.get(message.getHeaders().get(JdbcMessageStore.ID_KEY));
		assertNotNull(result);
		assertEquals(message.getPayload(), result.getPayload());
	}

	@Test
	@Transactional
	public void testAddAndUpdate() throws Exception {
		Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId("X").build();
		message = messageStore.put(message);
		message = MessageBuilder.fromMessage(message).setCorrelationId("Y").build();
		message = messageStore.put(message);
		assertEquals("Y", messageStore.get(message.getHeaders().get(JdbcMessageStore.ID_KEY)).getHeaders().getCorrelationId());
	}

	@Test
	@Transactional
	public void testAddAndList() throws Exception {
		Message<String> message = MessageBuilder.withPayload("foo").build();
		messageStore.put(message);
		assertEquals(1, SimpleJdbcTestUtils.countRowsInTable(jdbcTemplate, "INT_MESSAGE"));
		assertEquals(1, messageStore.list().size());
	}

	@Test
	@Transactional
	public void testAddAndListByCorrelationId() throws Exception {
		Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId("X").build();
		messageStore.put(message);
		assertEquals(1, messageStore.list("X").size());
	}

	@Test
	@Transactional
	public void testAddAndDelete() throws Exception {
		Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId("X").build();
		message = messageStore.put(message);
		assertNotNull(messageStore.delete(message.getHeaders().get(JdbcMessageStore.ID_KEY)));
	}

}
