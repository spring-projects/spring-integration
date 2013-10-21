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
package org.springframework.integration.monitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.management.Notification;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * To run, add JVM Args:
 * <p>
 * -Dspring.profiles.active=remote
 * -Dcom.sun.management.jmxremote.port=11099<p>
 * -Dcom.sun.management.jmxremote.authenticate=false<p>
 * -Dcom.sun.management.jmxremote.ssl=false<p>
 *
 * @author Gary Russell
 * @since 2.2
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class RemoteMBeanServerTests {

	@Autowired
	private PollableChannel attrChannel;

	@Autowired
	private MessageChannel publishChannel;

	@Autowired
	private PollableChannel publishInChannel;

	@Autowired
	private MessageChannel opChannel;

	@Autowired
	private PollableChannel opOutChannel;

	@Test
	public void testAttrPoll() throws Exception {
		Message<?> message = attrChannel.receive(100000);
		assertNotNull(message);
		assertEquals("foo", message.getPayload());
	}

	@Test
	public void testPublish() {
		publishChannel.send(new GenericMessage<String>("bar"));
		Message<?> message = publishInChannel.receive(100000);
		assertNotNull(message);
		assertTrue(message.getPayload() instanceof Notification);
		Notification notification = (Notification) message.getPayload();
		assertEquals("bar", notification.getMessage());
		assertEquals("foo", notification.getType());
	}

	@Test
	public void testOperation() {
		opChannel.send(new GenericMessage<String>("foo"));
		Message<?> message = opOutChannel.receive(100000);
		assertNotNull(message);
		assertEquals("bar", message.getPayload());
	}

	public static interface TesterMBean {

		String getFoo();

		String fooBar(String foo);
	}

	public static class Tester implements TesterMBean {

		public String getFoo() {
			return "foo";
		}

		public String fooBar(String foo) {
			return "bar";
		}


	}
}
