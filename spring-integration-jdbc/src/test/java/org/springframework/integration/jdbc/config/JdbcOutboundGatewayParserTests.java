package org.springframework.integration.jdbc.config;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

public class JdbcOutboundGatewayParserTests {

	private SimpleJdbcTemplate jdbcTemplate;

	private MessageChannel channel;

	private ConfigurableApplicationContext context;

	private MessagingTemplate messagingTemplate;

	@Test
	public void testMapPayloadMapReply() {
		setUp("handlingMapPayloadJdbcOutboundGatewayTest.xml", getClass());
		assertTrue(context.containsBean("jdbcGateway"));
		Message<?> message = MessageBuilder.withPayload(Collections.singletonMap("foo", "bar")).build();
		channel.send(message); 
		Map<String, Object> map = this.jdbcTemplate.queryForMap("SELECT * from FOOS");
		assertEquals("Wrong id", message.getHeaders().getId().toString(), map.get("ID"));
		assertEquals("Wrong name", "bar", map.get("name"));
		Message<?> reply = messagingTemplate.receive();
		assertNotNull(reply);
		@SuppressWarnings("unchecked")
		Map<String, ?> payload = (Map<String, ?>) reply.getPayload();
		assertEquals("bar", payload.get("name"));
	}

	@Test
	public void testKeyGeneration() {
		setUp("handlingKeyGenerationJdbcOutboundGatewayTest.xml", getClass());
		Message<?> message = MessageBuilder.withPayload(Collections.singletonMap("foo", "bar")).build();
		channel.send(message);
		Message<?> reply = messagingTemplate.receive();
		assertNotNull(reply);
		@SuppressWarnings("unchecked")
		Map<String, ?> payload = (Map<String, ?>) reply.getPayload();
		Object id = payload.get("SCOPE_IDENTITY()");
		assertNotNull(id);
		Map<String, Object> map = this.jdbcTemplate.queryForMap("SELECT * from BARS");
		assertEquals("Wrong id", id, map.get("ID"));
		assertEquals("Wrong name", "bar", map.get("name"));
	}

	@Test
	public void testCountUpdates() {
		setUp("handlingCountUpdatesJdbcOutboundGatewayTest.xml", getClass());
		Message<?> message = MessageBuilder.withPayload(Collections.singletonMap("foo", "bar")).build();
		channel.send(message);
		Message<?> reply = messagingTemplate.receive();
		assertNotNull(reply);
		@SuppressWarnings("unchecked")
		Map<String, ?> payload = (Map<String, ?>) reply.getPayload();
		assertEquals(1, payload.get("updated"));
	}

	@Test
	public void testWithPoller() throws Exception{
		ApplicationContext ac = new ClassPathXmlApplicationContext("JdbcOutboundGatewayWithPollerTest-context.xml", this.getClass());
		Message<?> message = MessageBuilder.withPayload(Collections.singletonMap("foo", "bar")).build();
		MessageChannel target = ac.getBean("target", MessageChannel.class);
		PollableChannel output = ac.getBean("output", PollableChannel.class);
		target.send(message);
		Thread.sleep(1000);
		Map<String, Object> map = (ac.getBean("jdbcTemplate", JdbcTemplate.class)).queryForMap("SELECT * from BAZZ");
		assertEquals("Wrong id", message.getHeaders().getId().toString(), map.get("ID"));
		assertEquals("Wrong name", "bar", map.get("name"));
		Message<?> reply = output.receive(1000);
		assertNotNull(reply);
		@SuppressWarnings("unchecked")
		Map<String, ?> payload = (Map<String, ?>) reply.getPayload();
		assertEquals("bar", payload.get("name"));
	}
	
	@After
	public void tearDown() {
		if (context != null) {
			context.close();
		}
	}

	protected void setupMessagingTemplate() {
		PollableChannel pollableChannel = this.context.getBean("output", PollableChannel.class);
		this.messagingTemplate = new MessagingTemplate(pollableChannel);
		this.messagingTemplate.setReceiveTimeout(500);
	}

	public void setUp(String name, Class<?> cls) {
		context = new ClassPathXmlApplicationContext(name, cls);
		jdbcTemplate = new SimpleJdbcTemplate(this.context.getBean("dataSource", DataSource.class));
		channel = this.context.getBean("target", MessageChannel.class);
		setupMessagingTemplate();
	}

}
