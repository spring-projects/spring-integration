/*
 * Copyright 2002-2013 the original author or authors.
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
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Test;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.jdbc.JdbcMessageHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

/**
 * @author Dave Syer
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Gunnar Hillert
 * @since 2.0
 *
 */
public class JdbcMessageHandlerParserTests {

	private JdbcTemplate jdbcTemplate;

	private MessageChannel channel;

	private ConfigurableApplicationContext context;

	private static volatile int adviceCalled;

	@Test
	public void testSimpleOutboundChannelAdapter(){
		setUp("handlingWithJdbcOperationsJdbcOutboundChannelAdapterTest.xml", getClass());
		Message<?> message = MessageBuilder.withPayload("foo").setHeader("business.key", "FOO").build();
		channel.send(message);
		Map<String, Object> map = this.jdbcTemplate.queryForMap("SELECT * from FOOS");
		assertEquals("Wrong id", "FOO", map.get("ID"));
		assertEquals("Wrong id", "foo", map.get("name"));
		JdbcMessageHandler handler = context.getBean(JdbcMessageHandler.class);
		assertEquals(23, TestUtils.getPropertyValue(handler, "order"));
		assertEquals(1, adviceCalled);
	}

	@Test
	public void testDollarHeaderOutboundChannelAdapter(){
		setUp("handlingDollarHeaderJdbcOutboundChannelAdapterTest.xml", getClass());
		Message<?> message = MessageBuilder.withPayload("foo").setHeader("$foo_id", "abc").build();
		channel.send(message);
		Map<String, Object> map = this.jdbcTemplate.queryForMap("SELECT * from FOOS");
		assertEquals("Wrong id", message.getHeaders().get("$foo_id").toString(), map.get("ID"));
		assertEquals("Wrong id", "foo", map.get("name"));
	}

	@Test
	public void testMapPayloadOutboundChannelAdapter(){
		setUp("handlingMapPayloadJdbcOutboundChannelAdapterTest.xml", getClass());
		assertTrue(context.containsBean("jdbcAdapter"));
		System.out.println(context.getBean("jdbcAdapter").getClass().getName());
		Message<?> message = MessageBuilder.withPayload(Collections.singletonMap("foo", "bar")).build();
		channel.send(message);
		Map<String, Object> map = this.jdbcTemplate.queryForMap("SELECT * from FOOS");
		assertEquals("Wrong id", message.getHeaders().getId().toString(), map.get("ID"));
		assertEquals("Wrong name", "bar", map.get("name"));
	}

	@Test
	public void testMapPayloadNestedQueryOutboundChannelAdapter(){
		setUp("handlingMapPayloadNestedQueryJdbcOutboundChannelAdapterTest.xml", getClass());
		Message<?> message = MessageBuilder.withPayload(Collections.singletonMap("foo", "bar")).build();
		channel.send(message);
		Map<String, Object> map = this.jdbcTemplate.queryForMap("SELECT * from FOOS");
		assertEquals("Wrong id", message.getHeaders().getId().toString(), map.get("ID"));
		assertEquals("Wrong name", "bar", map.get("name"));
	}

	@Test
	public void testParameterSourceOutboundChannelAdapter(){
		setUp("handlingParameterSourceJdbcOutboundChannelAdapterTest.xml", getClass());
		Message<?> message = MessageBuilder.withPayload("foo").build();
		channel.send(message);
		Map<String, Object> map = this.jdbcTemplate.queryForMap("SELECT * from FOOS");
		assertEquals("Wrong id", message.getHeaders().getId().toString(), map.get("ID"));
		assertEquals("Wrong name", "bar", map.get("name"));
	}

	@Test
	public void testOutboundAdapterWithPoller() throws Exception{
		setUp("JdbcOutboundAdapterWithPollerTest-context.xml", this.getClass());
		MessageChannel target = context.getBean("target", MessageChannel.class);
		Message<?> message = MessageBuilder.withPayload("foo").setHeader("business.key", "FOO").build();
		target.send(message);
		Thread.sleep(2000);
		Map<String, Object> map = (context.getBean("jdbcTemplate", JdbcTemplate.class)).queryForMap("SELECT * from FOOW");
		assertEquals("Wrong id", "FOO", map.get("ID"));
		assertEquals("Wrong id", "foo", map.get("name"));
	}

	@Test
	public void testOutboundChannelAdapterWithinChain(){
		setUp("handlingJdbcOutboundChannelAdapterWithinChainTest.xml", getClass());
		Message<?> message = MessageBuilder.withPayload("foo").setHeader("business.key", "FOO").build();
		channel.send(message);
		Map<String, Object> map = this.jdbcTemplate.queryForMap("SELECT * from FOOS");
		assertEquals("Wrong id", "FOO", map.get("ID"));
		assertEquals("Wrong id", "foo", map.get("name"));
	}

	@After
	public void tearDown(){
		if(context != null){
			context.close();
		}
	}

	public void setUp(String name, Class<?> cls){
		context = new ClassPathXmlApplicationContext(name, cls);
		jdbcTemplate = new JdbcTemplate(this.context.getBean("dataSource",DataSource.class));
		channel = this.context.getBean("target", MessageChannel.class);
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception {
			adviceCalled++;
			return callback.execute();
		}

	}
}
