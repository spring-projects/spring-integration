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

package org.springframework.integration.jmx.config;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
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
		assertNull(listener.lastNotification);
		Message<?> message = MessageBuilder.withPayload("XYZ")
				.setHeader(JmxHeaders.NOTIFICATION_TYPE, "test.type").build();
		channel.send(message);
		assertNotNull(listener.lastNotification);
		Notification notification = listener.lastNotification;
		assertEquals("XYZ", notification.getMessage());
		assertEquals("test.type", notification.getType());
		assertNull(notification.getUserData());
		assertEquals(1, adviceCalled);
	}

	@Test
	public void publishUserData() throws Exception {
		assertNull(listener.lastNotification);
		TestData data = new TestData();
		Message<?> message = MessageBuilder.withPayload(data)
				.setHeader(JmxHeaders.NOTIFICATION_TYPE, "test.type").build();
		channel.send(message);
		assertNotNull(listener.lastNotification);
		Notification notification = listener.lastNotification;
		assertNull(notification.getMessage());
		assertEquals(data, notification.getUserData());
		assertEquals("test.type", notification.getType());
	}

	@Test
	public void defaultNotificationType() throws Exception {
		assertNull(listener.lastNotification);
		Message<?> message = MessageBuilder.withPayload("test").build();
		channel.send(message);
		assertNotNull(listener.lastNotification);
		Notification notification = listener.lastNotification;
		assertEquals("default.type", notification.getType());
	}

	@Test //INT-2275
	public void publishStringMessageWithinChain() throws Exception {
		assertNotNull(this.beanFactory.getBean("chainWithJmxNotificationPublishing$child.jmx-notification-publishing-channel-adapter-within-chain.handler",
				MessageHandler.class));
		assertNull(listener.lastNotification);
		Message<?> message = MessageBuilder.withPayload("XYZ")
				.setHeader(JmxHeaders.NOTIFICATION_TYPE, "test.type").build();
		publishingWithinChainChannel.send(message);
		assertNotNull(listener.lastNotification);
		Notification notification = listener.lastNotification;
		assertEquals("XYZ", notification.getMessage());
		assertEquals("test.type", notification.getType());
		assertNull(notification.getUserData());
		Set<ObjectName> names = server.queryNames(
				new ObjectName("*:type=MessageHandler," +
						"name=chainWithJmxNotificationPublishing$child.jmx-notification-publishing-channel-adapter-within-chain,*")
				, null);
		assertEquals(1, names.size());
	}

	@Test @DirtiesContext
	public void changeMessageHistoryPatterns() throws Exception {
		Set<ObjectInstance> mbeans = server.queryMBeans(null, null);
		boolean tested = false;
		for (ObjectInstance mbean : mbeans) {
			if (mbean.toString().contains("MessageHistoryConfigurer")) {
				ObjectName objectName = mbean.getObjectName();
				try{
					server.setAttribute(objectName, new Attribute("ComponentNamePatternsString", "foo, bar"));
					fail("Exception expected");
				}
				catch (MBeanException e) {
					assertThat(e.getTargetException(), instanceOf(IllegalStateException.class));
					assertThat(e.getTargetException().getMessage(), containsString("cannot be changed"));
				}
				catch (Exception e) {
					throw e;
				}
				server.invoke(objectName, "stop", new Object[]{}, new String[]{});
				server.setAttribute(objectName, new Attribute("ComponentNamePatternsString", "foo, bar"));
				assertEquals("bar,foo", server.getAttribute(objectName, "ComponentNamePatternsString"));
				tested = true;
				break;
			}
		}
		assertTrue(tested);
	}

	private static class TestData {

	}

	public static class FooADvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception {
			adviceCalled++;
			System.out.println("foo");
			new RuntimeException("foo").printStackTrace();
			return callback.execute();
		}

	}
}
