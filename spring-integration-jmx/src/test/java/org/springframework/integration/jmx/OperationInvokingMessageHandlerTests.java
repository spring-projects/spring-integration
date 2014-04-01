/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.jmx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.jmx.config.DynamicRouterTests;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jmx.support.MBeanServerFactoryBean;
import org.springframework.jmx.support.ObjectNameManager;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * See DynamicRouterTests for additional tests where the MBean is registered by the Spring exporter.
 * @see DynamicRouterTests
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @since 2.0
 */
public class OperationInvokingMessageHandlerTests {

	private final String objectName = "si:name=test";

	private volatile MBeanServer server;


	@Before
	public void setup() throws Exception {
		MBeanServerFactoryBean factoryBean = new MBeanServerFactoryBean();
		factoryBean.setLocateExistingServerIfPossible(true);
		factoryBean.afterPropertiesSet();
		this.server = factoryBean.getObject();
		this.server.registerMBean(new TestOps(), ObjectNameManager.getInstance(this.objectName));
	}

	@After
	public void cleanup() throws Exception {
		this.server.unregisterMBean(ObjectNameManager.getInstance(this.objectName));
	}


	@Test
	public void invocationWithMapPayload() throws Exception {
		QueueChannel outputChannel = new QueueChannel();
		OperationInvokingMessageHandler handler = new OperationInvokingMessageHandler();
		handler.setServer(this.server);
		handler.setObjectName(this.objectName);
		handler.setOutputChannel(outputChannel);
		handler.setOperationName("x");
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("p1", "foo");
		params.put("p2", "bar");
		Message<?> message = MessageBuilder.withPayload(params).build();
		handler.handleMessage(message);
		Message<?> reply = outputChannel.receive(0);
		assertNotNull(reply);
		assertEquals("foobar", reply.getPayload());
	}

	@Test
	public void invocationWithPayloadNoReturnValue() throws Exception {
		QueueChannel outputChannel = new QueueChannel();
		OperationInvokingMessageHandler handler = new OperationInvokingMessageHandler();
		handler.setServer(this.server);
		handler.setObjectName(this.objectName);
		handler.setOutputChannel(outputChannel);
		handler.setOperationName("y");
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		Message<?> message = MessageBuilder.withPayload("foo").build();
		handler.handleMessage(message);
	}

	@Test(expected=MessagingException.class)
	public void invocationWithMapPayloadNotEnoughParameters() throws Exception {
		QueueChannel outputChannel = new QueueChannel();
		OperationInvokingMessageHandler handler = new OperationInvokingMessageHandler();
		handler.setServer(this.server);
		handler.setObjectName(this.objectName);
		handler.setOutputChannel(outputChannel);
		handler.setOperationName("x");
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("p1", "foo");
		Message<?> message = MessageBuilder.withPayload(params).build();
		handler.handleMessage(message);
		Message<?> reply = outputChannel.receive(0);
		assertNotNull(reply);
		assertEquals("foobar", reply.getPayload());
	}

	@Test
	public void invocationWithListPayload() throws Exception {
		QueueChannel outputChannel = new QueueChannel();
		OperationInvokingMessageHandler handler = new OperationInvokingMessageHandler();
		handler.setServer(this.server);
		handler.setObjectName(this.objectName);
		handler.setOutputChannel(outputChannel);
		handler.setOperationName("x");
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		List<Object> params = Arrays.asList(new Object[] { "foo", new Integer(123) });
		Message<?> message = MessageBuilder.withPayload(params).build();
		handler.handleMessage(message);
		Message<?> reply = outputChannel.receive(0);
		assertNotNull(reply);
		assertEquals("foo123", reply.getPayload());
	}

	public static interface TestOpsMBean {

		String x(String s1, String s2);

		String x(String s, Integer i);

		void y(String s);
	}


	public static class TestOps implements TestOpsMBean {

		@Override
		public String x(String s1, String s2) {
			return s1 + s2;
		}

		@Override
		public String x(String s, Integer i) {
			return s + i;
		}

		@Override
		public void y(String s){}
	}

}
