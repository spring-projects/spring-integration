/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.jmx.config;

import java.util.Set;

import javax.management.Attribute;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.jmx.JmxHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class NotificationPublishingChannelAdapterParserTests {

	@Autowired
	private MessageChannel channel;

	@Autowired
	private TestListener listener;

	@Autowired
	private MessageChannel publishingWithinChainChannel;

	@Autowired
	private BeanFactory beanFactory;

	@Autowired
	private MBeanServer server;

	private static volatile int adviceCalled;

	@After
	public void clearListener() {
		listener.lastNotification = null;
	}

	@Test
	public void publishStringMessage() throws Exception {
		adviceCalled = 0;
		assertThat(listener.lastNotification).isNull();
		Message<?> message = MessageBuilder.withPayload("XYZ")
				.setHeader(JmxHeaders.NOTIFICATION_TYPE, "test.type").build();
		channel.send(message);
		assertThat(listener.lastNotification).isNotNull();
		Notification notification = listener.lastNotification;
		assertThat(notification.getMessage()).isEqualTo("XYZ");
		assertThat(notification.getType()).isEqualTo("test.type");
		assertThat(notification.getUserData()).isNull();
		assertThat(adviceCalled).isEqualTo(1);
	}

	@Test
	public void publishUserData() throws Exception {
		assertThat(listener.lastNotification).isNull();
		TestData data = new TestData();
		Message<?> message = MessageBuilder.withPayload(data)
				.setHeader(JmxHeaders.NOTIFICATION_TYPE, "test.type").build();
		channel.send(message);
		assertThat(listener.lastNotification).isNotNull();
		Notification notification = listener.lastNotification;
		assertThat(notification.getMessage()).isNull();
		assertThat(notification.getUserData()).isEqualTo(data);
		assertThat(notification.getType()).isEqualTo("test.type");
	}

	@Test
	public void defaultNotificationType() throws Exception {
		assertThat(listener.lastNotification).isNull();
		Message<?> message = MessageBuilder.withPayload("test").build();
		channel.send(message);
		assertThat(listener.lastNotification).isNotNull();
		Notification notification = listener.lastNotification;
		assertThat(notification.getType()).isEqualTo("default.type");
	}

	@Test //INT-2275
	public void publishStringMessageWithinChain() throws Exception {
		assertThat(this.beanFactory.getBean(
				"chainWithJmxNotificationPublishing$child."
						+ "jmx-notification-publishing-channel-adapter-within-chain.handler",
				MessageHandler.class)).isNotNull();
		assertThat(listener.lastNotification).isNull();
		Message<?> message = MessageBuilder.withPayload("XYZ")
				.setHeader(JmxHeaders.NOTIFICATION_TYPE, "test.type").build();
		publishingWithinChainChannel.send(message);
		assertThat(listener.lastNotification).isNotNull();
		Notification notification = listener.lastNotification;
		assertThat(notification.getMessage()).isEqualTo("XYZ");
		assertThat(notification.getType()).isEqualTo("test.type");
		assertThat(notification.getUserData()).isNull();
		Set<ObjectName> names = server
				.queryNames(new ObjectName("*:type=MessageHandler," + "name=\"chainWithJmxNotificationPublishing$child."
						+ "jmx-notification-publishing-channel-adapter-within-chain\",*"), null);
		assertThat(names.size()).isEqualTo(1);
		names = server
				.queryNames(new ObjectName("*:type=MessageChannel,"
						+ "name=org.springframework.integration.test.anon,source=anonymous,*"), null);
		assertThat(names.size()).isEqualTo(1);
	}

	@Test
	@DirtiesContext
	public void changeMessageHistoryPatterns() throws Exception {
		Set<ObjectInstance> mbeans = server.queryMBeans(null, null);
		boolean tested = false;
		for (ObjectInstance mbean : mbeans) {
			if (mbean.toString().contains("MessageHistoryConfigurer")) {
				ObjectName objectName = mbean.getObjectName();
				try {
					server.setAttribute(objectName, new Attribute("ComponentNamePatternsString", "foo, bar"));
					fail("Exception expected");
				}
				catch (MBeanException e) {
					assertThat(e.getTargetException()).isInstanceOf(IllegalStateException.class);
					assertThat(e.getTargetException().getMessage()).contains("cannot be changed");
				}
				catch (Exception e) {
					throw e;
				}
				server.invoke(objectName, "stop", new Object[] {}, new String[] {});
				server.setAttribute(objectName, new Attribute("ComponentNamePatternsString", "foo, bar"));
				assertThat(server.getAttribute(objectName, "ComponentNamePatternsString")).isEqualTo("bar,foo");
				tested = true;
				break;
			}
		}
		assertThat(tested).isTrue();
	}

	private static class TestData {

		TestData() {
			super();
		}

	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
			adviceCalled++;
			return callback.execute();
		}

	}

}
