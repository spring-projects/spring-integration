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

package org.springframework.integration.jmx.config;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.channel.QueueChannelOperations;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Dave Syer
 * @author Gary Russell
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class PriorityChannelTests {

	@Autowired
	private MBeanServer server;

	@Autowired
	private PollableChannel testChannel;

	@Test
	public void testHandlerMBeanRegistration() throws Exception {
		Set<ObjectName> names = server.queryNames(new ObjectName("test.PriorityChannel:type=MessageHandler,*"), null);
		// Error handler plus the service activator
		assertEquals(2, names.size());
	}

	@Test
	public void testChannelMBeanRegistration() throws Exception {
		Set<ObjectName> names = server.queryNames(new ObjectName("test.PriorityChannel:type=MessageChannel,name=testChannel,*"), null);
		assertEquals(1, names.size());
		assertEquals(0, server.getAttribute(names.iterator().next(), "QueueSize"));
		assertEquals(0, ((QueueChannelOperations) testChannel).getQueueSize());
	}

	public static class Source {
		public String get() {
			return "foo";
		}
	}

}
