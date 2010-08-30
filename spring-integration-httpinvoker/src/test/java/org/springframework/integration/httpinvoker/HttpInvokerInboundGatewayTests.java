/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.httpinvoker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.Executors;

import org.junit.Test;

import org.springframework.integration.Message;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.GenericMessage;
import org.springframework.integration.core.MessageChannel;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;

/**
 * @author Mark Fisher
 */
@SuppressWarnings("deprecation")
public class HttpInvokerInboundGatewayTests {

	@Test
	public void testRequestOnly() throws Exception {
		QueueChannel channel = new QueueChannel();
		HttpInvokerInboundGateway gateway = new HttpInvokerInboundGateway();
		gateway.setRequestChannel(channel);
		gateway.setExpectReply(false);
		gateway.afterPropertiesSet();
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setContent(createRequestContent(new GenericMessage<String>("test")));
		gateway.handleRequest(request, response);
		Message<?> message = channel.receive(500);
		assertNotNull(message);
		assertEquals("test", message.getPayload());
	}

	@Test
	public void testRequestReply() throws Exception {
		final QueueChannel channel = new QueueChannel();
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			public void run() {
				Message<?> message = channel.receive();
				MessageChannel replyChannel = (MessageChannel) message.getHeaders().getReplyChannel();
				replyChannel.send(new GenericMessage<String>(message.getPayload().toString().toUpperCase()));
			}
		});
		HttpInvokerInboundGateway gateway = new HttpInvokerInboundGateway();
		gateway.setRequestChannel(channel);
		gateway.setExpectReply(true);
		gateway.afterPropertiesSet();
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setContent(createRequestContent(new GenericMessage<String>("test")));
		gateway.handleRequest(request, response);
		Message<?> reply = extractMessageFromResponse(response);
		assertEquals("TEST", reply.getPayload());
	}


	private static byte[] createRequestContent(Message<?> message) throws IOException {
		RemoteInvocation invocation = new RemoteInvocation(
				"exchange", new Class[] { Message.class }, new Object[] { message });
		ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		try {
			oos.writeObject(invocation);
			oos.flush();
		}
		finally {
			oos.close();
		}
		return baos.toByteArray();
	}

	private static Message<?> extractMessageFromResponse(MockHttpServletResponse response) throws IOException, ClassNotFoundException {
		byte[] responseContent = response.getContentAsByteArray();
		ByteArrayInputStream bais = new ByteArrayInputStream(responseContent);
		ObjectInputStream ois = new ObjectInputStream(bais);
		RemoteInvocationResult remoteResult = (RemoteInvocationResult) ois.readObject();
		Object resultValue = remoteResult.getValue();
		assertTrue(resultValue instanceof Message<?>);
		return (Message<?>) resultValue;
	}

}
