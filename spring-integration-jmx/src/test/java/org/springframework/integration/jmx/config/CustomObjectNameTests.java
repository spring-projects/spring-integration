/*
 * Copyright 2015-2016 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.management.Descriptor;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.router.AbstractMappingMessageRouter;
import org.springframework.integration.support.management.IntegrationManagedResource;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Gary Russell
 * @since 4.2.1
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class CustomObjectNameTests {

	@Autowired
	private MBeanServer server;

	@Autowired
	private MessageHandler customHandler;

	@Test
	public void testCustomMBeanRegistration() throws Exception {
		Set<ObjectName> names = server.queryNames(new ObjectName("custom:type=MessageChannel,*"), null);
		assertEquals(1, names.size());
		ObjectName name = names.iterator().next();
		assertEquals("custom:type=MessageChannel,name=foo", name.toString());
		MBeanInfo mBeanInfo = server.getMBeanInfo(name);
		assertEquals("custom channel", mBeanInfo.getDescription());
		names = server.queryNames(new ObjectName("custom:type=MessageHandler,*"), null);
		assertEquals(1, names.size());
		name = names.iterator().next();
		assertEquals("custom:type=MessageHandler,name=foo", name.toString());
		mBeanInfo = server.getMBeanInfo(name);
		assertEquals("custom handler", mBeanInfo.getDescription());
		Descriptor descriptor = mBeanInfo.getDescriptor();
		assertEquals("true", descriptor.getFieldValue("log"));
		assertEquals("foo", descriptor.getFieldValue("logFile"));
		assertEquals("1000", descriptor.getFieldValue("currencyTimeLimit"));
		assertEquals("bar", descriptor.getFieldValue("persistLocation"));
		assertEquals("baz", descriptor.getFieldValue("persistName"));
		assertEquals("10", descriptor.getFieldValue("persistPeriod"));
		assertEquals("Never", descriptor.getFieldValue("persistPolicy"));
		names = server.queryNames(new ObjectName("custom:type=MessageSource,*"), null);
		assertEquals(1, names.size());
		name = names.iterator().next();
		assertEquals("custom:type=MessageSource,name=foo", name.toString());
		names = server.queryNames(new ObjectName("custom:type=MessageRouter,*"), null);
		assertEquals(1, names.size());
		name = names.iterator().next();
		assertEquals("custom:type=MessageRouter,name=foo", name.toString());
		names = server.queryNames(new ObjectName("test.custom:type=MessageHandler,*"), null);
		assertEquals(2, names.size());
		Iterator<ObjectName> iterator = names.iterator();
		name = iterator.next();
		assertEquals("test.custom:type=MessageHandler,name=standardHandler,bean=handler", name.toString());
		name = iterator.next();
		assertEquals("test.custom:type=MessageHandler,name=errorLogger,bean=internal", name.toString());
		assertTrue(AopUtils.isJdkDynamicProxy(this.customHandler));
	}

	@IntegrationManagedResource(objectName = "${customChannelName}", description = "custom channel")
	public static class ChannelWithCustomObjectName extends AbstractMessageChannel {

		@Override
		protected boolean doSend(Message<?> message, long timeout) {
			return false;
		}

	}

	@IntegrationManagedResource(objectName = "${customHandlerName}", description = "custom handler",
			currencyTimeLimit = 1000, log = true, logFile = "foo", persistLocation = "bar", persistName = "baz", persistPeriod = 10,
			persistPolicy = "Never")
	@Transactional
	public static class HandlerWithCustomObjectName extends AbstractMessageHandler {

		@Override
		public void handleMessageInternal(Message<?> message) throws Exception {
		}

	}

	@IntegrationManagedResource("${customSourceName}")
	public static class SourceWithCustomObjectName extends AbstractMessageSource<String> {

		@Override
		public String getComponentType() {
			return "foo";
		}

		@Override
		protected Object doReceive() {
			return null;
		}

	}

	@IntegrationManagedResource("${customRouterName}")
	public static class RouterWithCustomObjectName extends AbstractMappingMessageRouter {

		@Override
		protected List<Object> getChannelKeys(Message<?> message) {
			return null;
		}

	}

	@IntegrationManagedResource
	public static class HandlerWithStandardObjectName extends AbstractMessageHandler {

		@Override
		protected void handleMessageInternal(Message<?> message) throws Exception {
		}

	}

	public static class TxConfig {

		@Bean
		public PlatformTransactionManager transactionManager() {
			return mock(PlatformTransactionManager.class);
		}

	}

}
