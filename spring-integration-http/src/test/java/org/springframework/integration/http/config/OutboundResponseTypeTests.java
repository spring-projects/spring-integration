/*
 * Copyright 2002-2012 the original author or authors.
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
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.message.GenericMessage;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * @author Oleg Zhurakousky
 *
 * see https://jira.springsource.org/browse/INT-2397
 */
public class OutboundResponseTypeTests {

	private static HttpServer server;
	private static MyHandler httpHandler;

	@BeforeClass
	public static void createServer() throws Exception {
		httpHandler = new MyHandler();
		server = HttpServer.create(new InetSocketAddress(51235), 0);
		server.createContext("/testApps/outboundResponse", httpHandler);
		server.start();
	}
	@AfterClass
	public static void stopServer() throws Exception {
		server.stop(0);
	}

	@Test
	public void testDefaultResponseType() throws Exception{

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"OutboundResponseTypeTests-context.xml", this.getClass());

		MessageChannel channel = context.getBean("requestChannel", MessageChannel.class);
		QueueChannel replyChannel = context.getBean("replyChannel", QueueChannel.class);

		channel.send(new GenericMessage<String>("Hello"));
		Message<?> message = replyChannel.receive(5000);
		assertNotNull(message);
		assertTrue(message.getPayload() instanceof ResponseEntity);
	}

	@Test
	public void testWithResponseTypeSet() throws Exception{

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"OutboundResponseTypeTests-context.xml", this.getClass());

		MessageChannel channel = context.getBean("resTypeSetChannel", MessageChannel.class);
		QueueChannel replyChannel = context.getBean("replyChannel", QueueChannel.class);

		channel.send(new GenericMessage<String>("Hello"));
		Message<?> message = replyChannel.receive(5000);
		assertNotNull(message);
		assertTrue(message.getPayload() instanceof String);
	}

	@Test
	public void testWithResponseTypeExpressionSet() throws Exception{

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"OutboundResponseTypeTests-context.xml", this.getClass());

		MessageChannel channel = context.getBean("resTypeExpressionSetChannel", MessageChannel.class);
		QueueChannel replyChannel = context.getBean("replyChannel", QueueChannel.class);

		channel.send(new GenericMessage<String>("java.lang.String"));
		Message<?> message = replyChannel.receive(5000);
		assertNotNull(message);
		assertTrue(message.getPayload() instanceof String);
	}

	@Test
	public void testWithResponseTypeExpressionSetAsClass() throws Exception{

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"OutboundResponseTypeTests-context.xml", this.getClass());

		MessageChannel channel = context.getBean("resTypeExpressionSetChannel", MessageChannel.class);
		QueueChannel replyChannel = context.getBean("replyChannel", QueueChannel.class);

		channel.send(new GenericMessage<Class<?>>(String.class));
		Message<?> message = replyChannel.receive(5000);
		assertNotNull(message);
		assertTrue(message.getPayload() instanceof String);
	}

	@Test(expected=BeanDefinitionParsingException.class)
	public void testMutuallyExclusivityInMethodAndMethodExpression() throws Exception{

		new ClassPathXmlApplicationContext(
				"OutboundResponseTypeTests-context-fail.xml", this.getClass());
	}

	static class MyHandler implements HttpHandler {
		private String httpMethod = "POST";

		public void setHttpMethod(String httpMethod){
			this.httpMethod = httpMethod;
		}

		public void handle(HttpExchange t) throws IOException {
			String requestMethod = t.getRequestMethod();
			String response = null;
			if (requestMethod.equalsIgnoreCase(this.httpMethod)){
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
}
