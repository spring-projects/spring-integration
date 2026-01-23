/*
 * Copyright 2002-present the original author or authors.
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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.jdbc.MessagePreparedStatementSetter;
import org.springframework.integration.jdbc.outbound.JdbcOutboundGateway;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * @author Dave Syer
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Gunnar Hillert
 * @author Glenn Renfro
 *
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
		assertThat(this.context.containsBean("jdbcGateway")).isTrue();
		Message<?> message = MessageBuilder.withPayload(Collections.singletonMap("foo", "bar")).build();
		this.channel.send(message);

		Message<?> reply = this.messagingTemplate.receive();
		assertThat(reply).isNotNull();
		@SuppressWarnings("unchecked")
		Map<String, ?> payload = (Map<String, ?>) reply.getPayload();
		assertThat(payload.get("name")).isEqualTo("bar");

		Map<String, Object> map = this.jdbcTemplate.queryForMap("SELECT * from FOOS");
		assertThat(map.get("ID")).as("Wrong id").isEqualTo(message.getHeaders().getId().toString());
		assertThat(map.get("name")).as("Wrong name").isEqualTo("bar");

		JdbcOutboundGateway gateway = context.getBean("jdbcGateway.handler", JdbcOutboundGateway.class);
		assertThat(TestUtils.<Integer>getPropertyValue(gateway, "order")).isEqualTo(23);
		assertThat(TestUtils.<Boolean>getPropertyValue(gateway, "requiresReply")).isTrue();
		assertThat(adviceCalled).isEqualTo(1);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testKeyGeneration() {
		setUp("handlingKeyGenerationJdbcOutboundGatewayTest.xml", getClass());

		Message<?> message = MessageBuilder.withPayload(Collections.singletonMap("foo", "bar")).build();

		this.channel.send(message);

		Message<?> reply = this.messagingTemplate.receive();
		assertThat(reply).isNotNull();

		Map<String, ?> payload = (Map<String, ?>) reply.getPayload();
		Object id = payload.get("ID");
		assertThat(id).isNotNull();

		Map<String, Object> map = this.jdbcTemplate.queryForMap("SELECT * from BARS");
		assertThat(map.get("ID")).as("Wrong id").isEqualTo(id);
		assertThat(map.get("name")).as("Wrong name").isEqualTo("bar");

		this.jdbcTemplate.execute("DELETE FROM BARS");

		Object insertGateway = this.context.getBean("insertGatewayWithSetter.handler");
		JdbcTemplate handlerJdbcTemplate =
				TestUtils.getPropertyValue(insertGateway, "handler.jdbcOperations.classicJdbcTemplate");

		Log logger = spy(TestUtils.<Log>getPropertyValue(handlerJdbcTemplate, "logger"));

		given(logger.isDebugEnabled()).willReturn(true);

		new DirectFieldAccessor(handlerJdbcTemplate).setPropertyValue("logger", logger);

		MessageChannel setterRequest = this.context.getBean("setterRequest", MessageChannel.class);
		setterRequest.send(new GenericMessage<>("bar2"));
		reply = this.messagingTemplate.receive();
		assertThat(reply).isNotNull();

		payload = (Map<String, ?>) reply.getPayload();
		id = payload.get("ID");
		assertThat(id).isNotNull();
		map = this.jdbcTemplate.queryForMap("SELECT * from BARS");
		assertThat(map.get("ID")).as("Wrong id").isEqualTo(id);
		assertThat(map.get("name")).as("Wrong name").isEqualTo("bar2");

		verify(logger).debug("Executing prepared SQL statement [insert into bars (status, name) values (0, ?)]");
	}

	@Test
	public void testCountUpdates() {
		setUp("handlingCountUpdatesJdbcOutboundGatewayTest.xml", getClass());
		Message<?> message = MessageBuilder.withPayload(Collections.singletonMap("foo", "bar")).build();

		this.channel.send(message);

		Message<?> reply = this.messagingTemplate.receive();
		assertThat(reply).isNotNull();
		@SuppressWarnings("unchecked")
		Map<String, ?> payload = (Map<String, ?>) reply.getPayload();
		assertThat(payload.get("updated")).isEqualTo(1);
	}

	@Test
	public void testWithPoller() {
		setUp("JdbcOutboundGatewayWithPollerTest-context.xml", this.getClass());

		Object insertGateway = this.context.getBean("jdbcOutboundGateway.handler");
		JdbcTemplate pollerJdbcTemplate =
				TestUtils.getPropertyValue(insertGateway, "poller.jdbcOperations.classicJdbcTemplate");

		Log logger = spy(TestUtils.<Log>getPropertyValue(pollerJdbcTemplate, "logger"));

		given(logger.isDebugEnabled()).willReturn(true);

		new DirectFieldAccessor(pollerJdbcTemplate).setPropertyValue("logger", logger);

		Message<?> message = MessageBuilder.withPayload(Collections.singletonMap("foo", "bar")).build();

		this.channel.send(message);

		Message<?> reply = this.messagingTemplate.receive();
		assertThat(reply).isNotNull();
		@SuppressWarnings("unchecked")
		Map<String, ?> payload = (Map<String, ?>) reply.getPayload();
		assertThat(payload.get("name")).isEqualTo("bar");

		Map<String, Object> map = this.jdbcTemplate.queryForMap("SELECT * from BAZZ");
		assertThat(map.get("ID")).as("Wrong id").isEqualTo(message.getHeaders().getId().toString());
		assertThat(map.get("name")).as("Wrong name").isEqualTo("bar");

		verify(logger).debug("Executing prepared SQL statement [select * from bazz where id=?]");
	}

	@Test
	public void testWithSelectQueryOnly() {
		setUp("JdbcOutboundGatewayWithSelectTest-context.xml", getClass());
		Message<?> message = MessageBuilder.withPayload(100).build();

		this.channel.send(message);

		@SuppressWarnings("unchecked")
		Message<Map<String, Object>> reply = (Message<Map<String, Object>>) this.messagingTemplate.receive();

		String id = (String) reply.getPayload().get("id");
		Integer status = (Integer) reply.getPayload().get("status");
		String name = (String) reply.getPayload().get("name");

		assertThat(id).isEqualTo("100");
		assertThat(status).isEqualTo(Integer.valueOf(3));
		assertThat(name).isEqualTo("Cartman");
	}

	@Test
	public void testReplyTimeoutIsSet() {
		setUp("JdbcOutboundGatewayWithPollerTest-context.xml", getClass());

		PollingConsumer outboundGateway = this.context.getBean("jdbcOutboundGateway", PollingConsumer.class);

		DirectFieldAccessor accessor = new DirectFieldAccessor(outboundGateway);
		Object source = accessor.getPropertyValue("handler");
		accessor = new DirectFieldAccessor(source);
		source = accessor.getPropertyValue("messagingTemplate");

		MessagingTemplate messagingTemplate = (MessagingTemplate) source;

		accessor = new DirectFieldAccessor(messagingTemplate);

		Long sendTimeout = (Long) accessor.getPropertyValue("sendTimeout");
		assertThat(sendTimeout).as("Wrong sendTimeout").isEqualTo(Long.valueOf(444L));

	}

	@Test
	public void testDefaultMaxMessagesPerPollIsSet() {
		setUp("JdbcOutboundGatewayWithPollerTest-context.xml", this.getClass());

		PollingConsumer pollingConsumer = this.context.getBean(PollingConsumer.class);

		DirectFieldAccessor accessor = new DirectFieldAccessor(pollingConsumer);
		Object source = accessor.getPropertyValue("handler");
		accessor = new DirectFieldAccessor(source);
		source = accessor.getPropertyValue("poller"); //JdbcPollingChannelAdapter
		accessor = new DirectFieldAccessor(source);
		Integer maxRowsPerPoll = (Integer) accessor.getPropertyValue("maxRows");
		assertThat(maxRowsPerPoll).as("maxRowsPerPoll should default to 1").isEqualTo(Integer.valueOf(1));

	}

	@Test
	public void testMaxMessagesPerPollIsSet() {
		setUp("JdbcOutboundGatewayWithPoller2Test-context.xml", this.getClass());

		PollingConsumer pollingConsumer = this.context.getBean(PollingConsumer.class);

		DirectFieldAccessor accessor = new DirectFieldAccessor(pollingConsumer);
		Object source = accessor.getPropertyValue("handler");
		accessor = new DirectFieldAccessor(source);
		source = accessor.getPropertyValue("poller"); //JdbcPollingChannelAdapter
		accessor = new DirectFieldAccessor(source);
		Integer maxRowsPerPoll = (Integer) accessor.getPropertyValue("maxRows");
		assertThat(maxRowsPerPoll).as("maxRowsPerPoll should default to 10").isEqualTo(Integer.valueOf(10));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testOutboundGatewayInsideChain() {
		setUp("handlingMapPayloadJdbcOutboundGatewayTest.xml", getClass());

		String beanName = "org.springframework.integration.handler.MessageHandlerChain#" +
				"0$child.jdbc-outbound-gateway-within-chain.handler";
		JdbcOutboundGateway jdbcMessageHandler = this.context.getBean(beanName, JdbcOutboundGateway.class);

		MessageChannel channel = this.context.getBean("jdbcOutboundGatewayInsideChain", MessageChannel.class);

		assertThat(TestUtils.<Boolean>getPropertyValue(jdbcMessageHandler, "requiresReply")).isFalse();

		channel.send(MessageBuilder.withPayload(Collections.singletonMap("foo", "bar")).build());

		PollableChannel outbound = this.context.getBean("replyChannel", PollableChannel.class);
		Message<?> reply = outbound.receive(10000);

		assertThat(reply)
				.isNotNull()
				.extracting(Message::getPayload)
				.asInstanceOf(InstanceOfAssertFactories.LIST)
				.hasSize(1)
				.element(0)
				.isInstanceOf(Map.class)
				.satisfies(map -> assertThat((Map<String, String>) map)
						.containsEntry("name", "bar"));
	}

	@AfterEach
	public void tearDown() {
		if (this.context != null) {
			this.context.close();
		}
	}

	protected void setupMessagingTemplate() {
		PollableChannel pollableChannel = this.context.getBean("output", PollableChannel.class);
		this.messagingTemplate = new MessagingTemplate();
		this.messagingTemplate.setDefaultDestination(pollableChannel);
		this.messagingTemplate.setReceiveTimeout(10000);
	}

	public void setUp(String name, Class<?> cls) {
		this.context = new ClassPathXmlApplicationContext(name, cls);
		this.jdbcTemplate = new JdbcTemplate(this.context.getBean("dataSource", DataSource.class));
		this.channel = this.context.getBean("target", MessageChannel.class);
		setupMessagingTemplate();
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
			adviceCalled++;
			return callback.execute();
		}

	}

	public static class TestMessagePreparedStatementSetter implements MessagePreparedStatementSetter {

		@Override
		public void setValues(PreparedStatement ps, Message<?> requestMessage) throws SQLException {
			ps.setObject(1, requestMessage.getPayload());
		}

	}

}
