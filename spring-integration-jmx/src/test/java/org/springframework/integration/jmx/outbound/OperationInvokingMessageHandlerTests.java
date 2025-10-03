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

package org.springframework.integration.jmx.outbound;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.jmx.config.DynamicRouterTests;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jmx.support.MBeanServerFactoryBean;
import org.springframework.jmx.support.ObjectNameManager;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;

/**
 * See DynamicRouterTests for additional tests where the MBean is registered by the Spring exporter.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 *
 * @see DynamicRouterTests
 */
public class OperationInvokingMessageHandlerTests {

	private static MBeanServerFactoryBean factoryBean;

	private static MBeanServer server;

	private final String objectName = "si:name=test";

	@BeforeAll
	public static void setupClass() {
		factoryBean = new MBeanServerFactoryBean();
		factoryBean.afterPropertiesSet();
		server = factoryBean.getObject();
	}

	@AfterAll
	public static void tearDown() {
		factoryBean.destroy();
	}

	@BeforeEach
	public void setup() throws Exception {
		server.registerMBean(new TestOps(), ObjectNameManager.getInstance(this.objectName));
	}

	@AfterEach
	public void cleanup() throws Exception {
		server.unregisterMBean(ObjectNameManager.getInstance(this.objectName));
	}

	@Test
	public void invocationWithMapPayload() {
		QueueChannel outputChannel = new QueueChannel();
		OperationInvokingMessageHandler handler = new OperationInvokingMessageHandler(server);
		handler.setObjectName(this.objectName);
		handler.setOutputChannel(outputChannel);
		handler.setOperationName("x");
		handler.setBeanFactory(mock());
		handler.afterPropertiesSet();
		Map<String, Object> params = new HashMap<>();
		params.put("p1", "p1Value");
		params.put("p2", "p2Value");
		Message<?> message = MessageBuilder.withPayload(params).build();
		handler.handleMessage(message);
		Message<?> reply = outputChannel.receive(0);
		assertThat(reply).isNotNull();
		assertThat(reply.getPayload()).isEqualTo("p1Valuep2Value");
	}

	@Test
	public void invocationWithPayloadNoReturnValue() {
		QueueChannel outputChannel = new QueueChannel();
		OperationInvokingMessageHandler handler = new OperationInvokingMessageHandler(server);
		handler.setObjectName(this.objectName);
		handler.setOutputChannel(outputChannel);
		handler.setOperationName("y");
		handler.setBeanFactory(mock());
		handler.afterPropertiesSet();
		Message<?> message = MessageBuilder.withPayload("test").build();
		assertThatNoException()
				.isThrownBy(() -> handler.handleMessage(message));
	}

	@Test
	public void invocationWithMapPayloadNotEnoughParameters() {
		QueueChannel outputChannel = new QueueChannel();
		OperationInvokingMessageHandler handler = new OperationInvokingMessageHandler(server);
		handler.setObjectName(this.objectName);
		handler.setOutputChannel(outputChannel);
		handler.setOperationName("x");
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		Map<String, Object> params = new HashMap<>();
		params.put("p1", "p1Value");
		Message<?> message = MessageBuilder.withPayload(params).build();
		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> handler.handleMessage(message));
	}

	@Test
	public void invocationWithListPayload() {
		QueueChannel outputChannel = new QueueChannel();
		OperationInvokingMessageHandler handler = new OperationInvokingMessageHandler(server);
		handler.setObjectName(this.objectName);
		handler.setOutputChannel(outputChannel);
		handler.setOperationName("x");
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		List<Object> params = Arrays.asList(new Object[] {"test", 123});
		Message<?> message = MessageBuilder.withPayload(params).build();
		handler.handleMessage(message);
		Message<?> reply = outputChannel.receive(0);
		assertThat(reply).isNotNull();
		assertThat(reply.getPayload()).isEqualTo("test123");
	}

	public interface TestOpsMBean {

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
		public void y(String s) {
		}

	}

}
