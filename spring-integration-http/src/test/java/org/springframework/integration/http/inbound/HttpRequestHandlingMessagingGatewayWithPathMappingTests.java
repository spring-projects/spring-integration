/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.http.inbound;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.Map;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.http.RequestEntity;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.http.AbstractHttpInboundTests;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerMapping;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 * @author Biju Kunjummen
 */
public class HttpRequestHandlingMessagingGatewayWithPathMappingTests extends AbstractHttpInboundTests {

	private static final ExpressionParser PARSER = new SpelExpressionParser();


	@Test
	public void withoutExpression() throws IOException {
		DirectChannel echoChannel = new DirectChannel();
		echoChannel.subscribe(message -> {
			MessageChannel replyChannel = (MessageChannel) message.getHeaders().getReplyChannel();
			replyChannel.send(message);
		});
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("text/plain");

		request.setParameter("foo", "bar");
		String body = "hello";
		request.setContent(body.getBytes());
		request.setRequestURI("/fname/bill/lname/clinton");

		HttpRequestHandlingMessagingGateway gateway = new HttpRequestHandlingMessagingGateway(true);
		gateway.setBeanFactory(mock(BeanFactory.class));

		RequestMapping requestMapping = new RequestMapping();
		requestMapping.setPathPatterns("/fname/{f}/lname/{l}");
		gateway.setRequestMapping(requestMapping);
		gateway.afterPropertiesSet();
		gateway.start();

		gateway.setRequestChannel(echoChannel);

		MockHttpServletResponse response = new MockHttpServletResponse();

		RequestEntity<Object> httpEntity = prepareRequestEntity(body, new ServletServerHttpRequest(request));

		Object result = gateway.doHandleRequest(request, httpEntity, response);
		assertThat(result, instanceOf(Message.class));
		assertEquals("hello", ((Message<?>) result).getPayload());

	}

	@Test
	public void withPayloadExpressionPointingToPathVariable() throws Exception {
		DirectChannel echoChannel = new DirectChannel();
		echoChannel.subscribe(message -> {
			MessageChannel replyChannel = (MessageChannel) message.getHeaders().getReplyChannel();
			replyChannel.send(message);
		});
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		request.setMethod("POST");
		request.setContentType("text/plain");
		request.setParameter("foo", "bar");
		String body = "hello";
		request.setContent(body.getBytes());

		String requestURI = "/fname/bill/lname/clinton";

		//See org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping#handleMatch
		Map<String, String> uriTemplateVariables =
				new AntPathMatcher().extractUriTemplateVariables("/fname/{f}/lname/{l}", requestURI);
		request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVariables);

		request.setRequestURI(requestURI);

		HttpRequestHandlingMessagingGateway gateway = new HttpRequestHandlingMessagingGateway(true);
		gateway.setBeanFactory(mock(BeanFactory.class));

		RequestMapping requestMapping = new RequestMapping();
		requestMapping.setPathPatterns("/fname/{f}/lname/{l}");
		gateway.setRequestMapping(requestMapping);

		gateway.setRequestChannel(echoChannel);
		gateway.setPayloadExpression(PARSER.parseExpression("#pathVariables.f"));
		gateway.afterPropertiesSet();
		gateway.start();

		RequestEntity<Object> httpEntity = prepareRequestEntity(body, new ServletServerHttpRequest(request));

		Object result = gateway.doHandleRequest(request, httpEntity, response);
		assertThat(result, instanceOf(Message.class));
		assertEquals("bill", ((Message<?>) result).getPayload());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void withoutPayloadExpressionPointingToUriVariables() throws Exception {

		DirectChannel echoChannel = new DirectChannel();
		echoChannel.subscribe(message -> {
			MessageChannel replyChannel = (MessageChannel) message.getHeaders().getReplyChannel();
			replyChannel.send(message);
		});
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		request.setMethod("POST");
		request.setContentType("text/plain");
		request.setParameter("foo", "bar");
		String body = "hello";
		request.setContent(body.getBytes());

		String requestURI = "/fname/bill/lname/clinton";

		//See org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping#handleMatch
		Map<String, String> uriTemplateVariables =
				new AntPathMatcher().extractUriTemplateVariables("/fname/{f}/lname/{l}", requestURI);
		request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVariables);

		request.setRequestURI(requestURI);

		HttpRequestHandlingMessagingGateway gateway = new HttpRequestHandlingMessagingGateway(true);
		gateway.setBeanFactory(mock(BeanFactory.class));

		RequestMapping requestMapping = new RequestMapping();
		requestMapping.setPathPatterns("/fname/{f}/lname/{l}");
		gateway.setRequestMapping(requestMapping);

		gateway.setRequestChannel(echoChannel);
		gateway.setPayloadExpression(PARSER.parseExpression("#pathVariables"));
		gateway.afterPropertiesSet();
		gateway.start();

		RequestEntity<Object> httpEntity = prepareRequestEntity(body, new ServletServerHttpRequest(request));

		Object result = gateway.doHandleRequest(request, httpEntity, response);
		assertThat(result, instanceOf(Message.class));
		assertEquals("bill", ((Map<String, Object>) ((Message<?>) result).getPayload()).get("f"));
	}

	private static RequestEntity<Object> prepareRequestEntity(Object body, ServletServerHttpRequest request) throws IOException {
		return new RequestEntity<>(body, request.getHeaders(), request.getMethod(), request.getURI());
	}

}
