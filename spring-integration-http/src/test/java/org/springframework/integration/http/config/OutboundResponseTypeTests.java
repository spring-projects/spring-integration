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
package org.springframework.integration.http.config;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.test.util.SocketUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gary Russell
 * @since 2.2
 *
 * <p>
 * see https://jira.springsource.org/browse/INT-2397
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class OutboundResponseTypeTests {

	private static HttpServer server;

	private static MyHandler httpHandler;

	@Autowired
	private QueueChannel replyChannel;

	@Autowired
	private MessageChannel requestChannel;

	@Autowired
	private MessageChannel resTypeSetChannel;

	@Autowired
	private MessageChannel resPrimitiveStringPresentationChannel;

	@Autowired
	private MessageChannel resTypeExpressionSetChannel;

	@Autowired
	private MessageChannel resTypeExpressionSetSerializationChannel;

	@Autowired
	private MessageChannel invalidResponseTypeChannel;

	private static int port = SocketUtils.findAvailableServerSocket();

	@BeforeClass
	public static void createServer() throws Exception {
		httpHandler = new MyHandler();
		server = HttpServer.create(new InetSocketAddress(port), 0);
		server.createContext("/testApps/outboundResponse", httpHandler);
		server.start();
	}

	@AfterClass
	public static void stopServer() throws Exception {
		server.stop(0);
	}

	@Test
	public void testDefaultResponseType() throws Exception {
		this.requestChannel.send(new GenericMessage<String>("Hello"));
		Message<?> message = this.replyChannel.receive(5000);
		assertNotNull(message);
		assertTrue(message.getPayload() instanceof ResponseEntity);
	}

	@Test
	public void testWithResponseTypeSet() throws Exception {
		this.resTypeSetChannel.send(new GenericMessage<String>("Hello"));
		Message<?> message = this.replyChannel.receive(5000);
		assertNotNull(message);
		assertTrue(message.getPayload() instanceof String);
	}

	@Test
	public void testWithResponseTypeExpressionSet() throws Exception {
		this.resTypeExpressionSetChannel.send(new GenericMessage<String>("java.lang.String"));
		Message<?> message = this.replyChannel.receive(5000);
		assertNotNull(message);
		assertTrue(message.getPayload() instanceof String);
	}

	@Test
	public void testWithResponseTypeExpressionSetAsClass() throws Exception {
		this.resTypeExpressionSetSerializationChannel.send(new GenericMessage<Class<?>>(String.class));
		Message<?> message = this.replyChannel.receive(5000);
		assertNotNull(message);
		assertTrue(message.getPayload() instanceof String);
	}

	@Test
	public void testInt2706ResponseTypeExpressionAsPrimitive() throws Exception {
		this.resTypeExpressionSetChannel.send(new GenericMessage<String>("byte[]"));
		Message<?> message = this.replyChannel.receive(5000);
		assertNotNull(message);
		assertTrue(message.getPayload() instanceof byte[]);
	}

	@Test
	public void testInt2706ResponseTypePrimitiveArrayClassAsString() throws Exception {
		this.resPrimitiveStringPresentationChannel.send(new GenericMessage<byte[]>("hello".getBytes()));
		Message<?> message = this.replyChannel.receive(5000);
		assertNotNull(message);
		assertTrue(message.getPayload() instanceof byte[]);
	}

	@Test
	public void testInt3052InvalidResponseType() throws Exception {
		try {
			this.invalidResponseTypeChannel.send(new GenericMessage<byte[]>("hello".getBytes()));
			fail("IllegalArgumentException expected.");
		}
		catch (Exception e) {
			assertThat(e, Matchers.instanceOf(MessageHandlingException.class));
			Throwable t = e.getCause();
			assertThat(t, Matchers.instanceOf(IllegalArgumentException.class));
			assertThat(t.getMessage(),
					Matchers.containsString("'expectedResponseType' can be an instance of 'Class<?>', 'String' or 'ParameterizedTypeReference<?>'"));
		}
	}

	@Test
	public void testMutuallyExclusivityInMethodAndMethodExpression() throws Exception {
		try {
			new ClassPathXmlApplicationContext("OutboundResponseTypeTests-context-fail.xml", this.getClass());
			fail("Expected BeansException");
		}
		catch (BeansException e) {
			assertTrue(e instanceof BeanDefinitionParsingException);
			assertTrue(e.getMessage().contains("The 'expected-response-type' and 'expected-response-type-expression' are mutually exclusive"));
		}
	}

	static class MyHandler implements HttpHandler {

		private String httpMethod = "POST";

		public void setHttpMethod(String httpMethod) {
			this.httpMethod = httpMethod;
		}

		public void handle(HttpExchange t) throws IOException {
			String requestMethod = t.getRequestMethod();
			String response = null;
			if (requestMethod.equalsIgnoreCase(this.httpMethod)) {
				response = httpMethod;
				t.getResponseHeaders().add("Content-Type", MediaType.TEXT_PLAIN.toString()); //Required for Spring 3.0.x
				t.sendResponseHeaders(200, response.length());
			}
			else {
				response = "Request is NOT valid";
				t.sendResponseHeaders(404, 0);
			}

			OutputStream os = t.getResponseBody();
			os.write(response.getBytes());
			os.close();
		}
	}

	public static class Port {

		public String getPort() {
			return Integer.toString(port);
		}

	}

}
