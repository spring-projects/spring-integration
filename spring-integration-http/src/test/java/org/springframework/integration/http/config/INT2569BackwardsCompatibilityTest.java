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

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.message.GenericMessage;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * This test is to handle specific incompatibility with Spring 3.1 that resulted
 * in breaking change. For more details see
 * https://jira.springsource.org/browse/INT-2569
 * 
 * @author Oleg Zhurakousky
 * @since 2.2
 */
public class INT2569BackwardsCompatibilityTest {
	
	private HttpServer server;
	private MyHandler httpHandler;
	
	@Before
	public void createServer() throws Exception {
		httpHandler = new MyHandler();
		server = HttpServer.create(new InetSocketAddress(51234), 0);
		server.createContext("/testApps/int2569", httpHandler);
		server.setExecutor(null); // creates a default executor
		server.start();
	}
	@After
	public void stopServer() throws Exception {
		server.stop(0);
	}

	@Test
	public void testValidUrlVariable() throws Exception{

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"INT2569BackwardsCompatibilityTest-context.xml", this.getClass());
		
		MessageChannel channel = context.getBean("inputChannel", MessageChannel.class);
		QueueChannel replyChannel = context.getBean("replyChannel", QueueChannel.class);
		
		httpHandler.setAssertString("?q=vmw");
		channel.send(new GenericMessage<String>("vmw"));
		Message<?> message = replyChannel.receive(5000);
		assertNotNull(message);
		assertEquals("Request is valid for ?q=vmw", message.getPayload());
		
		httpHandler.setAssertString("?q=ibm");
		channel.send(new GenericMessage<String>("ibm"));
		message = replyChannel.receive(5000);
		assertNotNull(message);
		assertEquals("Request is valid for ?q=ibm", message.getPayload());

		httpHandler.setAssertString("?q=orcl");
		channel.send(new GenericMessage<String>("orcl"));
		message = replyChannel.receive(5000);
		assertNotNull(message);
		assertEquals("Request is valid for ?q=orcl", message.getPayload());

		httpHandler.setAssertString("?q=ctxs");
		channel.send(new GenericMessage<String>("ctxs"));
		message = replyChannel.receive(5000);
		assertNotNull(message);
		assertEquals("Request is valid for ?q=ctxs", message.getPayload());

		httpHandler.setAssertString("?q=fb");
		channel.send(new GenericMessage<String>("fb"));
		message = replyChannel.receive(5000);
		assertNotNull(message);
		assertEquals("Request is valid for ?q=fb", message.getPayload());

		httpHandler.setAssertString("?q=t");
		channel.send(new GenericMessage<String>("t"));
		message = replyChannel.receive(5000);
		assertNotNull(message);
		assertEquals("Request is valid for ?q=t", message.getPayload());

		httpHandler.setAssertString("?q=aapl");
		channel.send(new GenericMessage<String>("aapl"));
		message = replyChannel.receive(5000);
		assertNotNull(message);
		assertEquals("Request is valid for ?q=aapl", message.getPayload());
	}
	
	@Test(expected=MessageHandlingException.class)
	public void testInvalidUrlVariable() throws Exception{

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"INT2569BackwardsCompatibilityTest-context-fail.xml", this.getClass());
		
		MessageChannel channel = context.getBean("vmwChannel", MessageChannel.class);
		
		httpHandler.setAssertString("?q=vmw");
		channel.send(new GenericMessage<String>("vmw"));
	}

	class MyHandler implements HttpHandler {
		private String requestSuffix;
		
		public void setAssertString(String requestSuffix){
			this.requestSuffix = requestSuffix;
		}
		
		public void handle(HttpExchange t) throws IOException {
			String requestUri = t.getRequestURI().toString();
			String response = null;
			if (requestUri.endsWith(requestSuffix)){
				response = "Request is valid for " + requestSuffix;
				t.sendResponseHeaders(200, response.length());
			}
			else {
				response = "Request is NOT valid";
				t.sendResponseHeaders(404, 0); // this emulates the same condition that we were getting before the test
			}
			
			OutputStream os = t.getResponseBody();
			os.write(response.getBytes());
			os.close();
		}
	}

}
