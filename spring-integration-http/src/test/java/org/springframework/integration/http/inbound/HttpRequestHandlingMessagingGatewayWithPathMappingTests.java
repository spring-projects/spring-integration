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

package org.springframework.integration.http.inbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessagingException;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageHandler;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Gunnar Hillert
 */
public class HttpRequestHandlingMessagingGatewayWithPathMappingTests {

	private static ExpressionParser PARSER = new SpelExpressionParser();


	@Test
	public void withoutExpression() throws Exception {
		DirectChannel echoChannel = new DirectChannel();
		echoChannel.subscribe(new MessageHandler() {

			public void handleMessage(Message<?> message) throws MessagingException {
				MessageChannel replyChannel = (MessageChannel) message.getHeaders().getReplyChannel();
				replyChannel.send(message);
			}
		});
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		//request.setContentType("text/plain"); //Works in Spring 3.1.2.RELEASE but NOT in 3.0.7.RELEASE
		//Instead do:
		request.addHeader("Content-Type", "text/plain");

		request.setParameter("foo", "bar");
		request.setContent("hello".getBytes());
		request.setRequestURI("/fname/bill/lname/clinton");

		HttpRequestHandlingMessagingGateway gateway = new HttpRequestHandlingMessagingGateway(true);
		gateway.setPath("/fname/{f}/lname/{l}");
		gateway.setRequestChannel(echoChannel);

		MockHttpServletResponse response = new MockHttpServletResponse();

		Object result =  gateway.doHandleRequest(request, response);
		assertTrue(result instanceof Message);
		assertEquals("hello", ((Message<?>) result).getPayload());

	}

	@Test
	public void withPayloadExpressionPointingToPathVariable() throws Exception {
		DirectChannel echoChannel = new DirectChannel();
		echoChannel.subscribe(new MessageHandler() {
			public void handleMessage(Message<?> message) throws MessagingException {
				MessageChannel replyChannel = (MessageChannel) message.getHeaders().getReplyChannel();
				replyChannel.send(message);
			}
		});
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		request.setMethod("POST");
		request.setContentType("text/plain");
		request.setParameter("foo", "bar");
		request.setContent("hello".getBytes());
		request.setRequestURI("/fname/bill/lname/clinton");

		HttpRequestHandlingMessagingGateway gateway = new HttpRequestHandlingMessagingGateway(true);
		gateway.setPath("/fname/{f}/lname/{l}");
		gateway.setRequestChannel(echoChannel);
		gateway.setPayloadExpression(PARSER.parseExpression("#pathVariables.f"));

		Object result =  gateway.doHandleRequest(request, response);
		assertTrue(result instanceof Message);
		assertEquals("bill", ((Message<?>)result).getPayload());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void withoutPayloadExpressionPointingToUriVariables() throws Exception {

		DirectChannel echoChannel = new DirectChannel();
		echoChannel.subscribe(new MessageHandler() {

			public void handleMessage(Message<?> message) throws MessagingException {
				MessageChannel replyChannel = (MessageChannel) message.getHeaders().getReplyChannel();
				replyChannel.send(message);
			}
		});
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		request.setMethod("POST");
		request.setContentType("text/plain");
		request.setParameter("foo", "bar");
		request.setContent("hello".getBytes());
		request.setRequestURI("/fname/bill/lname/clinton");

		HttpRequestHandlingMessagingGateway gateway = new HttpRequestHandlingMessagingGateway(true);
		gateway.setPath("/fname/{f}/lname/{l}");
		gateway.setRequestChannel(echoChannel);
		gateway.setPayloadExpression(PARSER.parseExpression("#pathVariables"));

		Object result =  gateway.doHandleRequest(request, response);
		assertTrue(result instanceof Message);
		assertEquals("bill", ((Map<String, Object>) ((Message<?>)result).getPayload()).get("f"));
	}

}
