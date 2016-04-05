/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.integration.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.jdbc.storedproc.CreateUser;
import org.springframework.integration.jdbc.storedproc.User;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class StoredProcJmxManagedBeanTests {

	@Autowired
	private Consumer consumer;

	@Autowired
	CreateUser userService;

	@Test
	@SuppressWarnings("unchecked")
	public void testCollectJmxAttributes() throws Exception {

		final List<MBeanServer> servers = MBeanServerFactory.findMBeanServer(null);
		assertEquals(1, servers.size());

		final MBeanServer server = servers.iterator().next();

		// MessageHandler

		final Set<ObjectName> messageHandlerObjectNames = server.queryNames(
			ObjectName.getInstance(
				"org.springframework.integration.jdbc.test:name=outboundChannelAdapter.adapter.storedProcExecutor,*"),
				null);
		assertEquals(1, messageHandlerObjectNames.size());
		ObjectName messageHandlerObjectName = messageHandlerObjectNames.iterator().next();
		Map<String, Object> messageHandlerCacheStatistics = (Map<String, Object>) server
				.getAttribute(messageHandlerObjectName, "JdbcCallOperationsCacheStatisticsAsMap");

		assertEquals(11, messageHandlerCacheStatistics.size());

		assertEquals(0L, messageHandlerCacheStatistics.get("hitCount"));
		assertEquals(0L, messageHandlerCacheStatistics.get("loadCount"));
		assertEquals(0L, messageHandlerCacheStatistics.get("loadExceptionCount"));
		assertEquals(0L, messageHandlerCacheStatistics.get("loadSuccessCount"));
		assertEquals(0L, messageHandlerCacheStatistics.get("missCount"));

		// StoredProcOutboundGateway
		final Set<ObjectName> storedProcOutboundGatewayObjectNames = server.queryNames(ObjectName
				.getInstance("org.springframework.integration.jdbc.test:name=my gateway.storedProcExecutor,*"), null);
		assertEquals(1, storedProcOutboundGatewayObjectNames.size());
		ObjectName storedProcOutboundGatewayObjectName = storedProcOutboundGatewayObjectNames.iterator().next();
		Map<String, Object> storedProcOutboundGatewayCacheStatistics = (Map<String, Object>) server
				.getAttribute(storedProcOutboundGatewayObjectName, "JdbcCallOperationsCacheStatisticsAsMap");

		assertEquals(11, messageHandlerCacheStatistics.size());

		assertEquals(0L, storedProcOutboundGatewayCacheStatistics.get("hitCount"));
		assertEquals(0L, storedProcOutboundGatewayCacheStatistics.get("loadCount"));
		assertEquals(0L, storedProcOutboundGatewayCacheStatistics.get("loadExceptionCount"));
		assertEquals(0L, storedProcOutboundGatewayCacheStatistics.get("loadSuccessCount"));
		assertEquals(0L, storedProcOutboundGatewayCacheStatistics.get("missCount"));

		// StoredProcPollingChannelAdapter

		final Set<ObjectName> storedProcPollingChannelAdapterObjectNames = server.queryNames(
				ObjectName.getInstance(
						"org.springframework.integration.jdbc.test:name=inbound-channel-adapter.storedProcExecutor,*"),
				null);
		assertEquals(1, storedProcPollingChannelAdapterObjectNames.size());
		ObjectName storedProcPollingChannelAdapterObjectName = storedProcPollingChannelAdapterObjectNames.iterator()
				.next();
		Map<String, Object> storedProcPollingChannelAdapterCacheStatistics = (Map<String, Object>) server
				.getAttribute(storedProcPollingChannelAdapterObjectName, "JdbcCallOperationsCacheStatisticsAsMap");

		assertEquals(11, storedProcPollingChannelAdapterCacheStatistics.size());

		assertEquals(0L, storedProcPollingChannelAdapterCacheStatistics.get("hitCount"));
		assertEquals(0L, storedProcPollingChannelAdapterCacheStatistics.get("loadCount"));
		assertEquals(0L, storedProcPollingChannelAdapterCacheStatistics.get("loadExceptionCount"));
		assertEquals(0L, storedProcPollingChannelAdapterCacheStatistics.get("loadSuccessCount"));
		assertEquals(0L, storedProcPollingChannelAdapterCacheStatistics.get("missCount"));

	}

	@Test
	@SuppressWarnings("unchecked")
	public void testOutboundGateWayJmxAttributes() throws Exception {

		final List<MBeanServer> servers = MBeanServerFactory.findMBeanServer(null);
		assertEquals(1, servers.size());

		final MBeanServer server = servers.iterator().next();

		final Set<ObjectName> objectNames = server.queryNames(
				ObjectName.getInstance("org.springframework.integration.jdbc.test:name=my gateway.storedProcExecutor,*"),
				null);
		assertEquals(1, objectNames.size());
		ObjectName name = objectNames.iterator().next();
		Map<String, Object> cacheStatistics =
				(Map<String, Object>) server.getAttribute(name, "JdbcCallOperationsCacheStatisticsAsMap");

		assertEquals(11, cacheStatistics.size());

		assertEquals(0L, cacheStatistics.get("hitCount"));
		assertEquals(0L, cacheStatistics.get("loadCount"));
		assertEquals(0L, cacheStatistics.get("loadExceptionCount"));
		assertEquals(0L, cacheStatistics.get("loadSuccessCount"));
		assertEquals(0L, cacheStatistics.get("missCount"));

		userService.createUser(new User("myUsername", "myPassword", "myEmail"));

		List<Message<Collection<User>>> received = new ArrayList<Message<Collection<User>>>();

		received.add(consumer.poll(2000));

		Message<Collection<User>> message = received.get(0);

		assertNotNull(message);
		assertNotNull(message.getPayload());

		Map<String, Object> cacheStatistics2 =
				(Map<String, Object>) server.getAttribute(name, "JdbcCallOperationsCacheStatisticsAsMap");

		assertEquals(11, cacheStatistics2.size());

		assertEquals(0L, cacheStatistics2.get("hitCount"));
		assertEquals(1L, cacheStatistics2.get("loadCount"));
		assertEquals(0L, cacheStatistics2.get("loadExceptionCount"));
		assertEquals(1L, cacheStatistics2.get("loadSuccessCount"));
		assertEquals(1L, cacheStatistics2.get("missCount"));

	}

	static class Counter {

		private final AtomicInteger count = new AtomicInteger();

		public Integer next() throws InterruptedException {
			if (count.get() > 2) {
				//prevent message overload
				return null;
			}
			return count.incrementAndGet();
		}

	}


	static class Consumer {

		private final BlockingQueue<Message<Collection<User>>> messages =
				new LinkedBlockingQueue<Message<Collection<User>>>();

		@ServiceActivator
		public void receive(Message<Collection<User>> message) {
			messages.add(message);
		}

		Message<Collection<User>> poll(long timeoutInMillis) throws InterruptedException {
			return messages.poll(timeoutInMillis, TimeUnit.MILLISECONDS);
		}

	}

}
