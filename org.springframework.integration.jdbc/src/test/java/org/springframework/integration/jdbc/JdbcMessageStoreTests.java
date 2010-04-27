package org.springframework.integration.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.springframework.integration.test.matcher.PayloadAndHeaderMatcher.sameExceptIgnorableHeaders;

import java.util.UUID;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class JdbcMessageStoreTests {
	
	@Autowired
	private DataSource dataSource;
	
	private JdbcMessageStore messageStore;

	@Before
	public void init() {
		messageStore = new JdbcMessageStore(dataSource);
	}
	
	@Test
	@Transactional
	public void testGetNonExistent() throws Exception {
		Message<?> result = messageStore.get(UUID.randomUUID());
		assertNull(result);
	}

	@Test
	@Transactional
	public void testAddAndGet() throws Exception {
		Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId("X").build();
		Message<String> saved = messageStore.put(message);
		assertNull(messageStore.get(message.getHeaders().getId()));
		Message<?> result = messageStore.get(saved.getHeaders().getId());
		assertNotNull(result);
		assertThat(saved, sameExceptIgnorableHeaders(result));
		assertNotNull(result.getHeaders().get(JdbcMessageStore.SAVED_KEY));
		assertNotNull(result.getHeaders().get(JdbcMessageStore.CREATED_DATE_KEY));
	}

	@Test
	@Transactional
	public void testAddAndUpdate() throws Exception {
		Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId("X").build();
		message = messageStore.put(message);
		message = MessageBuilder.fromMessage(message).setCorrelationId("Y").build();
		message = messageStore.put(message);
		assertEquals("Y", messageStore.get(message.getHeaders().getId()).getHeaders().getCorrelationId());
	}

	@Test
	@Transactional
	public void testAddAndUpdateAlreadySaved() throws Exception {
		Message<String> message = MessageBuilder.withPayload("foo").build();
		message = messageStore.put(message);
		Message<String> result = messageStore.put(message);
		assertEquals(message, result);
	}

	@Test
	@Transactional
	public void testAddAndUpdateAlreadySavedAndCopied() throws Exception {
		Message<String> message = MessageBuilder.withPayload("foo").build();
		Message<String> saved = messageStore.put(message);
		Message<String> copy = MessageBuilder.fromMessage(saved).build();
		Message<String> result = messageStore.put(copy);
		assertNotSame(copy, result);
		assertThat(saved, sameExceptIgnorableHeaders(result, JdbcMessageStore.CREATED_DATE_KEY));
		assertNotNull(messageStore.get(saved.getHeaders().getId()));
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
		assertNotNull(messageStore.delete(message.getHeaders().getId()));
	}

}
