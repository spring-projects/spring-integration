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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.test.util.SocketUtils;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * @author Oleg Zhurakousky
 *
 * see https://jira.springsource.org/browse/INT-2397
 */
public class HttpOutboundGatewayWithMethodExpression {

	private HttpServer server;
	private MyHandler httpHandler;

	@Before
	public void createServer() throws Exception {
		int httpPort = SocketUtils.findAvailableServerSocket();
		System.setProperty("httpPort", String.valueOf(httpPort));

		httpHandler = new MyHandler();
		server = HttpServer.create(new InetSocketAddress(httpPort), 0);
		server.createContext("/testApps/httpMethod", httpHandler);
		server.start();
	}
	@After
	public void stopServer() throws Exception {
		server.stop(0);
	}

	@Test
	public void testDefaultMethod() throws Exception{

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"http-outbound-gateway-with-httpmethod-expression.xml", this.getClass());

		MessageChannel channel = context.getBean("defaultChannel", MessageChannel.class);
		QueueChannel replyChannel = context.getBean("replyChannel", QueueChannel.class);

		httpHandler.setHttpMethod("POST");
		channel.send(new GenericMessage<String>("Hello"));
		Message<?> message = replyChannel.receive(5000);
		assertNotNull(message);
		assertEquals("POST", message.getPayload());
	}

	@Test
	public void testExplicitlySetMethod() throws Exception{

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"http-outbound-gateway-with-httpmethod-expression.xml", this.getClass());

		MessageChannel channel = context.getBean("requestChannel", MessageChannel.class);
		QueueChannel replyChannel = context.getBean("replyChannel", QueueChannel.class);

		httpHandler.setHttpMethod("GET");
		channel.send(new GenericMessage<String>("GET"));
		Message<?> message = replyChannel.receive(5000);
		assertNotNull(message);
		assertEquals("GET", message.getPayload());
	}

	@Test(expected=BeanDefinitionParsingException.class)
	public void testMutuallyExclusivityInMethodAndMethodExpression() throws Exception{

		new ClassPathXmlApplicationContext(
				"http-outbound-gateway-with-httpmethod-expression-fail.xml", this.getClass());
	}

	class MyHandler implements HttpHandler {
		private String httpMethod;

		public void setHttpMethod(String httpMethod){
			this.httpMethod = httpMethod;
		}

		public void handle(HttpExchange t) throws IOException {
			String requestMethod = t.getRequestMethod();
			String response = null;
			if (requestMethod.equalsIgnoreCase(this.httpMethod)){
				response = httpMethod;
				t.getResponseHeaders().add("Content-Type", "text/plain"); //Required for Spring 3.0.x
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
