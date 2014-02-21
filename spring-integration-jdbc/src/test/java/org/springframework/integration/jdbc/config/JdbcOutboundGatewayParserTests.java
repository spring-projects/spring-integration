/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.jdbc.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.jdbc.JdbcOutboundGateway;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;

/**
 * @author Dave Syer
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Gunnar Hillert
 * @since 2.0
 *
 */
public class JdbcOutboundGatewayParserTests {

	private JdbcTemplate jdbcTemplate;

	private MessageChannel channel;

	private ConfigurableApplicationContext context;

	private MessagingTemplate messagingTemplate;

	private static volatile int adviceCalled;

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
		JdbcOutboundGateway gateway = context.getBean("jdbcGateway.handler", JdbcOutboundGateway.class);
		assertEquals(23, TestUtils.getPropertyValue(gateway, "order"));
		Assert.assertTrue(TestUtils.getPropertyValue(gateway, "requiresReply", Boolean.class));
		Object gw = context.getBean("jdbcGateway");
		assertEquals(1, adviceCalled);
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
		setUp("JdbcOutboundGatewayWithPollerTest-context.xml", this.getClass());
		Message<?> message = MessageBuilder.withPayload(Collections.singletonMap("foo", "bar")).build();
		MessageChannel target = context.getBean("target", MessageChannel.class);
		PollableChannel output = context.getBean("output", PollableChannel.class);
		target.send(message);
		Thread.sleep(1000);
		Map<String, Object> map = (context.getBean("jdbcTemplate", JdbcTemplate.class)).queryForMap("SELECT * from BAZZ");
		assertEquals("Wrong id", message.getHeaders().getId().toString(), map.get("ID"));
		assertEquals("Wrong name", "bar", map.get("name"));
		Message<?> reply = output.receive(1000);
		assertNotNull(reply);
		@SuppressWarnings("unchecked")
		Map<String, ?> payload = (Map<String, ?>) reply.getPayload();
		assertEquals("bar", payload.get("name"));
	}

	@Test
	public void testWithSelectQueryOnly() throws Exception{
		this.context = new ClassPathXmlApplicationContext("JdbcOutboundGatewayWithSelectTest-context.xml", this.getClass());
		Message<?> message = MessageBuilder.withPayload(Integer.valueOf(100)).build();
		MessageChannel requestChannel = context.getBean("request", MessageChannel.class);
		PollableChannel replyChannel = context.getBean("reply", PollableChannel.class);

		requestChannel.send(message);
		Thread.sleep(1000);

		@SuppressWarnings("unchecked")
		Message<Map<String, Object>> reply = (Message<Map<String, Object>>) replyChannel.receive(500);

		String id = (String) reply.getPayload().get("id");
		Integer status = (Integer) reply.getPayload().get("status");
		String name = (String) reply.getPayload().get("name");
		ApplicationContext ac = this.context;

		assertEquals("100", id);
		assertEquals(Integer.valueOf(3), status);
		assertEquals("Cartman", name);
	}

	@Test
	public void testReplyTimeoutIsSet() throws Exception {
		setUp("JdbcOutboundGatewayWithPollerTest-context.xml", getClass());

		PollingConsumer outboundGateway = this.context.getBean("jdbcOutboundGateway", PollingConsumer.class);

		DirectFieldAccessor accessor = new DirectFieldAccessor(outboundGateway);
		Object source = accessor.getPropertyValue("handler");
		accessor = new DirectFieldAccessor(source);
		source = accessor.getPropertyValue("messagingTemplate");

		MessagingTemplate messagingTemplate = (MessagingTemplate) source;

		accessor = new DirectFieldAccessor(messagingTemplate);

		Long  sendTimeout = (Long) accessor.getPropertyValue("sendTimeout");
		assertEquals("Wrong sendTimeout", Long.valueOf(444L),  sendTimeout);

	}

	@Test
	public void testDefaultMaxMessagesPerPollIsSet() throws Exception {

		setUp("JdbcOutboundGatewayWithPollerTest-context.xml", this.getClass());

		PollingConsumer pollingConsumer = context.getBean(PollingConsumer.class);

		DirectFieldAccessor accessor = new DirectFieldAccessor(pollingConsumer);
		Object source = accessor.getPropertyValue("handler");
		accessor = new DirectFieldAccessor(source);
		source = accessor.getPropertyValue("poller"); //JdbcPollingChannelAdapter
		accessor = new DirectFieldAccessor(source);
		Integer maxRowsPerPoll = (Integer) accessor.getPropertyValue("maxRowsPerPoll");
		assertEquals("maxRowsPerPoll should default to 1", Integer.valueOf(1),  maxRowsPerPoll);

	}

	@Test
	public void testMaxMessagesPerPollIsSet() throws Exception {

		setUp("JdbcOutboundGatewayWithPoller2Test-context.xml", this.getClass());

		PollingConsumer pollingConsumer = context.getBean(PollingConsumer.class);

		DirectFieldAccessor accessor = new DirectFieldAccessor(pollingConsumer);
		Object source = accessor.getPropertyValue("handler");
		accessor = new DirectFieldAccessor(source);
		source = accessor.getPropertyValue("poller"); //JdbcPollingChannelAdapter
		accessor = new DirectFieldAccessor(source);
		Integer maxRowsPerPoll = (Integer) accessor.getPropertyValue("maxRowsPerPoll");
		assertEquals("maxRowsPerPoll should default to 10", Integer.valueOf(10),  maxRowsPerPoll);

	}

	@Test //INT-1029
	public void testOutboundGatewayInsideChain() {
		setUp("handlingMapPayloadJdbcOutboundGatewayTest.xml", getClass());

		JdbcOutboundGateway jdbcMessageHandler =
				context.getBean("org.springframework.integration.handler.MessageHandlerChain#0$child.jdbc-outbound-gateway-within-chain.handler",
				JdbcOutboundGateway.class);

		MessageChannel channel = context.getBean("jdbcOutboundGatewayInsideChain", MessageChannel.class);

		assertFalse(TestUtils.getPropertyValue(jdbcMessageHandler, "requiresReply", Boolean.class));

		channel.send(MessageBuilder.withPayload(Collections.singletonMap("foo", "bar")).build());

		PollableChannel outbound = context.getBean("replyChannel", PollableChannel.class);
		Message<?> reply = outbound.receive();
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
		this.messagingTemplate = new MessagingTemplate();
		this.messagingTemplate.setDefaultDestination(pollableChannel);
		this.messagingTemplate.setReceiveTimeout(500);
	}

	public void setUp(String name, Class<?> cls) {
		context = new ClassPathXmlApplicationContext(name, cls);
		jdbcTemplate = new JdbcTemplate(this.context.getBean("dataSource", DataSource.class));
		channel = this.context.getBean("target", MessageChannel.class);
		setupMessagingTemplate();
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception {
			adviceCalled++;
			return callback.execute();
		}

	}
}
