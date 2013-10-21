/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.integration.rmi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class BackToBackTests {

	@Autowired
	private SubscribableChannel good;

	@Autowired
	private SubscribableChannel bad;

	@Autowired
	private SubscribableChannel ugly;

	@Autowired
	private PollableChannel reply;

	@Autowired
	private AbstractApplicationContext context;

	@Test
	public void testGood() {
		good.send(new GenericMessage<String>("foo"));
		Message<?> reply = this.reply.receive(0);
		assertNotNull(reply);
		assertEquals("reply:foo", reply.getPayload());
	}

	@Test
	public void testBad() {
		bad.send(new GenericMessage<String>("foo"));
		Message<?> reply = this.reply.receive(0);
		assertNotNull(reply);
		assertEquals("error:foo", reply.getPayload());
	}

	@Test
	public void testUgly() {
		context.setId("context");
		try {
			ugly.send(new GenericMessage<String>("foo"));
			fail("Expected exception");
		}
		catch (Exception e) {
			assertEquals("Dispatcher has no subscribers for channel 'context.baz'.", e.getCause().getMessage());
		}
	}

}
