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

package org.springframework.integration.config.annotation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Dave Syer
 * @author Mark Fisher
 * @author Gary Russell
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@MessageEndpoint
public class AnnotatedEndpointActivationTests {

	@Autowired
	@Qualifier("input")
	private MessageChannel input;

	@Autowired
	@Qualifier("output")
	private PollableChannel output;

	@Autowired
	private AbstractApplicationContext applicationContext;

	// This has to be static because the MessageBus registers the handler
	// more than once (every time a test instance is created), but only one of
	// them will get the message.
	private static volatile int count = 0;


	@ServiceActivator(inputChannel = "input", outputChannel = "output")
	public String process(String message) {
		count++;
		String result = message + ": " + count;
		return result;
	}

	@ServiceActivator(inputChannel = "inputImplicit", outputChannel = "output")
	public String processImplicit(String message) {
		count++;
		String result = message + ": " + count;
		return result;
	}

	@Before
	public void resetCount() {
		count = 0;
	}

	@Test
	public void configCheck() {
		assertTrue(true);
	}

	@Test
	public void sendAndReceive() {
		this.input.send(new GenericMessage<String>("foo"));
		Message<?> message = this.output.receive(100);
		assertNotNull(message);
		assertEquals("foo: 1", message.getPayload());
		assertEquals(1, count);
	}

	@Test
	public void sendAndReceiveImplicitInputChannel() {
		MessageChannel input = this.applicationContext.getBean("inputImplicit", MessageChannel.class);
		input.send(new GenericMessage<String>("foo"));
		Message<?> message = this.output.receive(100);
		assertNotNull(message);
		assertEquals("foo: 1", message.getPayload());
		assertEquals(1, count);
	}

	@Test(expected = MessageDeliveryException.class)
	public void stopContext() {
		applicationContext.stop();
		this.input.send(new GenericMessage<String>("foo"));
	}

	@Test
	public void stopAndRestartContext() {
		applicationContext.stop();
		applicationContext.start();
		this.input.send(new GenericMessage<String>("foo"));
		Message<?> message = this.output.receive(100);
		assertNotNull(message);
		assertEquals("foo: 1", message.getPayload());
		assertEquals(1, count);
	}

}
