/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.integration.redis.inbound;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gunnar Hillert
 * @since 3.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode=ClassMode.AFTER_EACH_TEST_METHOD)
public class RedisQueueInboundChannelAdapterJmxTests {

	@Autowired
	StringRedisTemplate redisTemplate;

	@Before
	public void setup() {
		this.redisTemplate.delete("jmxtests");
	}

	@Test
	public void testCollectJmxAttributes() throws Exception {

		final List<MBeanServer> servers = MBeanServerFactory.findMBeanServer(null);
		assertEquals(1, servers.size());

		final MBeanServer server = servers.iterator().next();

		final Set<ObjectName> messageHandlerObjectNames = server.queryNames(ObjectName.getInstance("org.springframework.integration.redis.inbound:name=myRedisQueueInboundMessageSource,*"), null);
		assertEquals(1, messageHandlerObjectNames.size());
		final ObjectName messageHandlerObjectName = messageHandlerObjectNames.iterator().next();
		final long queueSize = (Long) server.getAttribute(messageHandlerObjectName, "QueueSize");

		assertEquals(0L, queueSize);

		redisTemplate.boundListOps("jmxtests").leftPush("Hello World");

		final long queueSize2 = (Long) server.getAttribute(messageHandlerObjectName, "QueueSize");

		assertEquals(1L, queueSize2);

		server.invoke(messageHandlerObjectName, "clearQueue", new Object[]{}, new String[]{});

		final long queueSize3 = (Long) server.getAttribute(messageHandlerObjectName, "QueueSize");

		assertEquals(0L, queueSize3);

	}

}
