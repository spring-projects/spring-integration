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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.jdbc.outbound.JdbcMessageHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Gunnar Hillert
 * @author Glenn Renfro
 *
 * @since 2.0
 *
 */
public class JdbcMessageHandlerParserTests {

	private JdbcTemplate jdbcTemplate;

	private MessageChannel channel;

	private ConfigurableApplicationContext context;

	private static volatile int adviceCalled;

	@Test
	public void testSimpleOutboundChannelAdapter() {
		setUp("handlingWithJdbcOperationsJdbcOutboundChannelAdapterTest.xml", getClass());
		Message<?> message = MessageBuilder.withPayload("foo").setHeader("business.key", "FOO").build();
		channel.send(message);
		Map<String, Object> map = this.jdbcTemplate.queryForMap("SELECT * from FOOS");
		assertThat(map.get("ID")).as("Wrong id").isEqualTo("FOO");
		assertThat(map.get("name")).as("Wrong id").isEqualTo("foo");
		JdbcMessageHandler handler = context.getBean(JdbcMessageHandler.class);
		assertThat(TestUtils.<Integer>getPropertyValue(handler, "order")).isEqualTo(23);
		assertThat(adviceCalled).isEqualTo(1);
	}

	@Test
	public void testDollarHeaderOutboundChannelAdapter() {
		setUp("handlingDollarHeaderJdbcOutboundChannelAdapterTest.xml", getClass());
		Message<?> message = MessageBuilder.withPayload("foo").setHeader("$foo_id", "abc").build();
		channel.send(message);
		Map<String, Object> map = this.jdbcTemplate.queryForMap("SELECT * from FOOS");
		assertThat(map.get("ID")).as("Wrong id").isEqualTo(message.getHeaders().get("$foo_id").toString());
		assertThat(map.get("name")).as("Wrong id").isEqualTo("foo");
	}

	@Test
	public void testMapPayloadOutboundChannelAdapter() {
		setUp("handlingMapPayloadJdbcOutboundChannelAdapterTest.xml", getClass());
		assertThat(context.containsBean("jdbcAdapter")).isTrue();
		UUID testId = UUID.randomUUID();
		Message<?> message = MessageBuilder.withPayload(Map.of("id", testId, "foo", "bar")).build();
		channel.send(message);
		Map<String, Object> map = this.jdbcTemplate.queryForMap("SELECT * from FOOS");
		assertThat(map.get("ID")).as("Wrong id").isEqualTo(testId.toString());
		assertThat(map.get("name")).as("Wrong name").isEqualTo("bar");
	}

	@Test
	public void testMapPayloadNestedQueryOutboundChannelAdapter() {
		setUp("handlingMapPayloadNestedQueryJdbcOutboundChannelAdapterTest.xml", getClass());
		Message<?> message = MessageBuilder.withPayload(Collections.singletonMap("foo", "bar")).build();
		channel.send(message);
		Map<String, Object> map = this.jdbcTemplate.queryForMap("SELECT * from FOOS");
		assertThat(map.get("ID")).as("Wrong id").isEqualTo(message.getHeaders().getId().toString());
		assertThat(map.get("name")).as("Wrong name").isEqualTo("bar");
	}

	@Test
	public void testParameterSourceOutboundChannelAdapter() {
		setUp("handlingParameterSourceJdbcOutboundChannelAdapterTest.xml", getClass());
		Message<?> message = MessageBuilder.withPayload("foo").build();
		channel.send(message);
		Map<String, Object> map = this.jdbcTemplate.queryForMap("SELECT * from FOOS");
		assertThat(map.get("ID")).as("Wrong id").isEqualTo(message.getHeaders().getId().toString());
		assertThat(map.get("name")).as("Wrong name").isEqualTo("bar");
	}

	@Test
	public void testOutboundAdapterWithPoller() throws Exception {
		setUp("JdbcOutboundAdapterWithPollerTest-context.xml", this.getClass());
		MessageChannel target = context.getBean("target", MessageChannel.class);
		Message<?> message = MessageBuilder.withPayload("foo").setHeader("business.key", "FOO").build();
		target.send(message);

		JdbcTemplate jdbcTemplate = context.getBean("jdbcTemplate", JdbcTemplate.class);
		int n = 0;
		List<Map<String, Object>> result = jdbcTemplate.query("SELECT * from FOOW", new ColumnMapRowMapper());
		while (result.isEmpty() && n++ < 100) {
			Thread.sleep(100);
			result = jdbcTemplate.query("SELECT * from FOOW", new ColumnMapRowMapper());
		}
		assertThat(n < 100).isTrue();

		assertThat(result.size()).isEqualTo(1);
		Map<String, Object> map = result.get(0);
		assertThat(map.get("ID")).as("Wrong id").isEqualTo("FOO");
		assertThat(map.get("name")).as("Wrong id").isEqualTo("foo");
	}

	@Test
	public void testOutboundChannelAdapterWithinChain() {
		setUp("handlingJdbcOutboundChannelAdapterWithinChainTest.xml", getClass());
		Message<?> message = MessageBuilder.withPayload("foo").setHeader("business.key", "FOO").build();
		channel.send(message);
		Map<String, Object> map = this.jdbcTemplate.queryForMap("SELECT * from FOOS");
		assertThat(map.get("ID")).as("Wrong id").isEqualTo("FOO");
		assertThat(map.get("name")).as("Wrong id").isEqualTo("foo");
	}

	@AfterEach
	public void tearDown() {
		if (context != null) {
			context.close();
		}
	}

	public void setUp(String name, Class<?> cls) {
		context = new ClassPathXmlApplicationContext(name, cls);
		jdbcTemplate = new JdbcTemplate(this.context.getBean("dataSource", DataSource.class));
		channel = this.context.getBean("target", MessageChannel.class);
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
			adviceCalled++;
			return callback.execute();
		}

	}

}
