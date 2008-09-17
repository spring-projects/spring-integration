/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.adapter.rmi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.rmi.RemoteException;

import org.junit.Before;
import org.junit.Test;

import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.StringMessage;
import org.springframework.remoting.RemoteLookupFailureException;
import org.springframework.remoting.rmi.RmiServiceExporter;

/**
 * @author Mark Fisher
 */
public class RmiHandlerTests {

	private final RmiHandler handler = new RmiHandler("rmi://localhost:1099/testRemoteHandler");


	@Before
	public void createExporter() throws RemoteException {
		RmiServiceExporter exporter = new RmiServiceExporter();
		exporter.setService(new TestHandler());
		exporter.setServiceInterface(MessageHandler.class);
		exporter.setServiceName("testRemoteHandler");
		exporter.afterPropertiesSet();
	}


	@Test
	public void testSerializablePayload() throws RemoteException {
		Message<?> replyMessage = handler.handle(new StringMessage("test"));
		assertNotNull(replyMessage);
		assertEquals("TEST", replyMessage.getPayload());
	}

	@Test
	public void testSerializableAttribute() throws RemoteException {
		Message<String> requestMessage = MessageBuilder.withPayload("test")
				.setHeader("testAttribute", "foo").build();
		Message<?> replyMessage = handler.handle(requestMessage);
		assertNotNull(replyMessage);
		assertEquals("foo", replyMessage.getHeaders().get("testAttribute"));
	}

	@Test(expected=MessageHandlingException.class)
	public void testNonSerializablePayload() throws RemoteException {
		NonSerializableTestObject payload = new NonSerializableTestObject();
		Message<?> requestMessage = new GenericMessage<NonSerializableTestObject>(payload);
		handler.handle(requestMessage);
	}

	@Test(expected=MessageHandlingException.class)
	public void testNonSerializableAttribute() throws RemoteException {
		Message<String> requestMessage = MessageBuilder.withPayload("test")
				.setHeader("testAttribute", new NonSerializableTestObject()).build();
		handler.handle(requestMessage);
	}

	@Test
	public void testInvalidServiceName() throws RemoteException {
		RmiHandler handler = new RmiHandler("rmi://localhost:1099/noSuchService");
		boolean exceptionThrown = false;
		try {
			handler.handle(new StringMessage("test"));
		}
		catch (MessageHandlingException e) {
			assertEquals(RemoteLookupFailureException.class, e.getCause().getClass());
			exceptionThrown = true;
		}
		assertTrue(exceptionThrown);
	}

	@Test
	public void testInvalidHost() {
		RmiHandler handler = new RmiHandler("rmi://noSuchHost:1099/testRemoteHandler");
		boolean exceptionThrown = false;
		try {
			handler.handle(new StringMessage("test"));
		}
		catch (MessageHandlingException e) {
			assertEquals(RemoteLookupFailureException.class, e.getCause().getClass());
			exceptionThrown = true;
		}
		assertTrue(exceptionThrown);
	}

	@Test
	public void testInvalidUrl() throws RemoteException {
		RmiHandler handler = new RmiHandler("invalid");
		boolean exceptionThrown = false;
		try {
			handler.handle(new StringMessage("test"));
		}
		catch (MessageHandlingException e) {
			assertEquals(RemoteLookupFailureException.class, e.getCause().getClass());
			exceptionThrown = true;
		}
		assertTrue(exceptionThrown);
	}


	private static class TestHandler implements MessageHandler {

		public Message<?> handle(Message<?> message) {
			return new GenericMessage<String>(message.getPayload().toString().toUpperCase(), message.getHeaders());
		}
	}


	private static class NonSerializableTestObject {
	}

}
