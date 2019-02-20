/*
 * Copyright 2002-2019 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;

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
		assertThat(servers.size()).isEqualTo(1);

		final MBeanServer server = servers.iterator().next();

		// MessageHandler

		final Set<ObjectName> messageHandlerObjectNames = server.queryNames(
			ObjectName.getInstance(
				"org.springframework.integration.jdbc.test:name=outboundChannelAdapter.adapter.storedProcExecutor,*"),
				null);
		assertThat(messageHandlerObjectNames.size()).isEqualTo(1);
		ObjectName messageHandlerObjectName = messageHandlerObjectNames.iterator().next();
		Map<String, Object> messageHandlerCacheStatistics = (Map<String, Object>) server
				.getAttribute(messageHandlerObjectName, "JdbcCallOperationsCacheStatisticsAsMap");

		assertThat(messageHandlerCacheStatistics.size()).isEqualTo(11);

		assertThat(messageHandlerCacheStatistics.get("hitCount")).isEqualTo(0L);
		assertThat(messageHandlerCacheStatistics.get("loadCount")).isEqualTo(0L);
		assertThat(messageHandlerCacheStatistics.get("loadExceptionCount")).isEqualTo(0L);
		assertThat(messageHandlerCacheStatistics.get("loadSuccessCount")).isEqualTo(0L);
		assertThat(messageHandlerCacheStatistics.get("missCount")).isEqualTo(0L);

		// StoredProcOutboundGateway
		final Set<ObjectName> storedProcOutboundGatewayObjectNames = server.queryNames(ObjectName
				.getInstance("org.springframework.integration.jdbc.test:name=my gateway.storedProcExecutor,*"), null);
		assertThat(storedProcOutboundGatewayObjectNames.size()).isEqualTo(1);
		ObjectName storedProcOutboundGatewayObjectName = storedProcOutboundGatewayObjectNames.iterator().next();
		Map<String, Object> storedProcOutboundGatewayCacheStatistics = (Map<String, Object>) server
				.getAttribute(storedProcOutboundGatewayObjectName, "JdbcCallOperationsCacheStatisticsAsMap");

		assertThat(messageHandlerCacheStatistics.size()).isEqualTo(11);

		assertThat(storedProcOutboundGatewayCacheStatistics.get("hitCount")).isEqualTo(0L);
		assertThat(storedProcOutboundGatewayCacheStatistics.get("loadCount")).isEqualTo(0L);
		assertThat(storedProcOutboundGatewayCacheStatistics.get("loadExceptionCount")).isEqualTo(0L);
		assertThat(storedProcOutboundGatewayCacheStatistics.get("loadSuccessCount")).isEqualTo(0L);
		assertThat(storedProcOutboundGatewayCacheStatistics.get("missCount")).isEqualTo(0L);

		// StoredProcPollingChannelAdapter

		final Set<ObjectName> storedProcPollingChannelAdapterObjectNames = server.queryNames(
				ObjectName.getInstance(
						"org.springframework.integration.jdbc.test:name=inbound-channel-adapter.storedProcExecutor,*"),
				null);
		assertThat(storedProcPollingChannelAdapterObjectNames.size()).isEqualTo(1);
		ObjectName storedProcPollingChannelAdapterObjectName = storedProcPollingChannelAdapterObjectNames.iterator()
				.next();
		Map<String, Object> storedProcPollingChannelAdapterCacheStatistics = (Map<String, Object>) server
				.getAttribute(storedProcPollingChannelAdapterObjectName, "JdbcCallOperationsCacheStatisticsAsMap");

		assertThat(storedProcPollingChannelAdapterCacheStatistics.size()).isEqualTo(11);

		assertThat(storedProcPollingChannelAdapterCacheStatistics.get("hitCount")).isEqualTo(0L);
		assertThat(storedProcPollingChannelAdapterCacheStatistics.get("loadCount")).isEqualTo(0L);
		assertThat(storedProcPollingChannelAdapterCacheStatistics.get("loadExceptionCount")).isEqualTo(0L);
		assertThat(storedProcPollingChannelAdapterCacheStatistics.get("loadSuccessCount")).isEqualTo(0L);
		assertThat(storedProcPollingChannelAdapterCacheStatistics.get("missCount")).isEqualTo(0L);

	}

	@Test
	@SuppressWarnings("unchecked")
	public void testOutboundGateWayJmxAttributes() throws Exception {

		final List<MBeanServer> servers = MBeanServerFactory.findMBeanServer(null);
		assertThat(servers.size()).isEqualTo(1);

		final MBeanServer server = servers.iterator().next();

		final Set<ObjectName> objectNames = server.queryNames(
				ObjectName.getInstance("org.springframework.integration.jdbc.test:name=my gateway.storedProcExecutor,*"),
				null);
		assertThat(objectNames.size()).isEqualTo(1);
		ObjectName name = objectNames.iterator().next();
		Map<String, Object> cacheStatistics =
				(Map<String, Object>) server.getAttribute(name, "JdbcCallOperationsCacheStatisticsAsMap");

		assertThat(cacheStatistics.size()).isEqualTo(11);

		assertThat(cacheStatistics.get("hitCount")).isEqualTo(0L);
		assertThat(cacheStatistics.get("loadCount")).isEqualTo(0L);
		assertThat(cacheStatistics.get("loadExceptionCount")).isEqualTo(0L);
		assertThat(cacheStatistics.get("loadSuccessCount")).isEqualTo(0L);
		assertThat(cacheStatistics.get("missCount")).isEqualTo(0L);

		userService.createUser(new User("myUsername", "myPassword", "myEmail"));

		List<Message<Collection<User>>> received = new ArrayList<Message<Collection<User>>>();

		received.add(consumer.poll(2000));

		Message<Collection<User>> message = received.get(0);

		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isNotNull();

		Map<String, Object> cacheStatistics2 =
				(Map<String, Object>) server.getAttribute(name, "JdbcCallOperationsCacheStatisticsAsMap");

		assertThat(cacheStatistics2.size()).isEqualTo(11);

		assertThat(cacheStatistics2.get("hitCount")).isEqualTo(0L);
		assertThat(cacheStatistics2.get("loadCount")).isEqualTo(1L);
		assertThat(cacheStatistics2.get("loadExceptionCount")).isEqualTo(0L);
		assertThat(cacheStatistics2.get("loadSuccessCount")).isEqualTo(1L);
		assertThat(cacheStatistics2.get("missCount")).isEqualTo(1L);

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
