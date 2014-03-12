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

package org.springframework.integration.http.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.ResponseErrorHandler;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 * @author Biju Kunjummen
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class HttpOutboundGatewayParserTests {

	@Autowired @Qualifier("minimalConfig")
	private AbstractEndpoint minimalConfigEndpoint;

	@Autowired @Qualifier("fullConfig")
	private AbstractEndpoint fullConfigEndpoint;

	@Autowired @Qualifier("withUrlExpression")
	private AbstractEndpoint withUrlExpressionEndpoint;

	@Autowired @Qualifier("withAdvice")
	private AbstractEndpoint withAdvice;

	@Autowired @Qualifier("withPoller1")
	private AbstractEndpoint withPoller1;

	@Autowired
	private ApplicationContext applicationContext;

	private static volatile int adviceCalled;

	@Test
	public void minimalConfig() {
		HttpRequestExecutingMessageHandler handler = (HttpRequestExecutingMessageHandler) new DirectFieldAccessor(
				this.minimalConfigEndpoint).getPropertyValue("handler");
		MessageChannel requestChannel = (MessageChannel) new DirectFieldAccessor(
				this.minimalConfigEndpoint).getPropertyValue("inputChannel");
		assertEquals(this.applicationContext.getBean("requests"), requestChannel);
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		Object replyChannel = handlerAccessor.getPropertyValue("outputChannel");
		assertNull(replyChannel);
		DirectFieldAccessor templateAccessor = new DirectFieldAccessor(handlerAccessor.getPropertyValue("restTemplate"));
		ClientHttpRequestFactory requestFactory = (ClientHttpRequestFactory)
				templateAccessor.getPropertyValue("requestFactory");
		assertTrue(requestFactory instanceof SimpleClientHttpRequestFactory);
		Expression uriExpression = (Expression) handlerAccessor.getPropertyValue("uriExpression");
		assertEquals("http://localhost/test1", uriExpression.getValue());
		assertEquals(HttpMethod.POST.name(), TestUtils.getPropertyValue(handler, "httpMethodExpression", Expression.class).getExpressionString());
		assertEquals("UTF-8", handlerAccessor.getPropertyValue("charset"));
		assertEquals(true, handlerAccessor.getPropertyValue("extractPayload"));
		assertEquals(false, handlerAccessor.getPropertyValue("transferCookies"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void fullConfig() throws Exception {
		DirectFieldAccessor endpointAccessor = new DirectFieldAccessor(this.fullConfigEndpoint);
		HttpRequestExecutingMessageHandler handler = (HttpRequestExecutingMessageHandler) endpointAccessor.getPropertyValue("handler");
		MessageChannel requestChannel = (MessageChannel) new DirectFieldAccessor(
				this.fullConfigEndpoint).getPropertyValue("inputChannel");
		assertEquals(this.applicationContext.getBean("requests"), requestChannel);
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		assertEquals(77, handlerAccessor.getPropertyValue("order"));
		assertEquals(Boolean.FALSE, endpointAccessor.getPropertyValue("autoStartup"));
		Object replyChannel = handlerAccessor.getPropertyValue("outputChannel");
		assertNotNull(replyChannel);
		assertEquals(this.applicationContext.getBean("replies"), replyChannel);
		DirectFieldAccessor templateAccessor = new DirectFieldAccessor(handlerAccessor.getPropertyValue("restTemplate"));
		ClientHttpRequestFactory requestFactory = (ClientHttpRequestFactory)
				templateAccessor.getPropertyValue("requestFactory");
		assertTrue(requestFactory instanceof SimpleClientHttpRequestFactory);
		Object converterListBean = this.applicationContext.getBean("converterList");
		assertEquals(converterListBean, templateAccessor.getPropertyValue("messageConverters"));

		assertEquals(String.class.getName(), TestUtils.getPropertyValue(handler, "expectedResponseTypeExpression", Expression.class).getValue());
		Expression uriExpression = (Expression) handlerAccessor.getPropertyValue("uriExpression");
		assertEquals("http://localhost/test2", uriExpression.getValue());
		assertEquals(HttpMethod.PUT.name(), TestUtils.getPropertyValue(handler, "httpMethodExpression", Expression.class).getExpressionString());
		assertEquals("UTF-8", handlerAccessor.getPropertyValue("charset"));
		assertEquals(false, handlerAccessor.getPropertyValue("extractPayload"));
		Object requestFactoryBean = this.applicationContext.getBean("testRequestFactory");
		assertEquals(requestFactoryBean, requestFactory);
		Object errorHandlerBean = this.applicationContext.getBean("testErrorHandler");
		assertEquals(errorHandlerBean, templateAccessor.getPropertyValue("errorHandler"));
		Object sendTimeout = new DirectFieldAccessor(
				handlerAccessor.getPropertyValue("messagingTemplate")).getPropertyValue("sendTimeout");
		assertEquals(new Long("1234"), sendTimeout);
		Map<String, Expression> uriVariableExpressions =
				(Map<String, Expression>) handlerAccessor.getPropertyValue("uriVariableExpressions");
		assertEquals(1, uriVariableExpressions.size());
		assertEquals("headers.bar", uriVariableExpressions.get("foo").getExpressionString());
		DirectFieldAccessor mapperAccessor = new DirectFieldAccessor(handlerAccessor.getPropertyValue("headerMapper"));
		String[] mappedRequestHeaders = (String[]) mapperAccessor.getPropertyValue("outboundHeaderNames");
		String[] mappedResponseHeaders = (String[]) mapperAccessor.getPropertyValue("inboundHeaderNames");
		assertEquals(2, mappedRequestHeaders.length);
		assertEquals(1, mappedResponseHeaders.length);
		assertTrue(ObjectUtils.containsElement(mappedRequestHeaders, "requestHeader1"));
		assertTrue(ObjectUtils.containsElement(mappedRequestHeaders, "requestHeader2"));
		assertEquals("responseHeader", mappedResponseHeaders[0]);
		assertEquals(true, handlerAccessor.getPropertyValue("transferCookies"));
	}

	@Test
	public void withUrlExpression() {
		HttpRequestExecutingMessageHandler handler = (HttpRequestExecutingMessageHandler) new DirectFieldAccessor(
				this.withUrlExpressionEndpoint).getPropertyValue("handler");
		MessageChannel requestChannel = (MessageChannel) new DirectFieldAccessor(
				this.withUrlExpressionEndpoint).getPropertyValue("inputChannel");
		assertEquals(this.applicationContext.getBean("requests"), requestChannel);
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		Object replyChannel = handlerAccessor.getPropertyValue("outputChannel");
		assertNull(replyChannel);
		DirectFieldAccessor templateAccessor = new DirectFieldAccessor(handlerAccessor.getPropertyValue("restTemplate"));
		ClientHttpRequestFactory requestFactory = (ClientHttpRequestFactory)
				templateAccessor.getPropertyValue("requestFactory");
		assertTrue(requestFactory instanceof SimpleClientHttpRequestFactory);
		SpelExpression expression = (SpelExpression) handlerAccessor.getPropertyValue("uriExpression");
		assertNotNull(expression);
		assertEquals("'http://localhost/test1'", expression.getExpressionString());
		assertEquals(HttpMethod.POST.name(), TestUtils.getPropertyValue(handler, "httpMethodExpression", Expression.class).getExpressionString());
		assertEquals("UTF-8", handlerAccessor.getPropertyValue("charset"));
		assertEquals(true, handlerAccessor.getPropertyValue("extractPayload"));
		assertEquals(false, handlerAccessor.getPropertyValue("transferCookies"));

		//INT-3055
		Object uriVariablesExpression = handlerAccessor.getPropertyValue("uriVariablesExpression");
		assertNotNull(uriVariablesExpression);
		assertEquals("@uriVariables", ((Expression) uriVariablesExpression).getExpressionString());
		Object uriVariableExpressions = handlerAccessor.getPropertyValue("uriVariableExpressions");
		assertNotNull(uriVariableExpressions);
		assertTrue(((Map<?, ?>) uriVariableExpressions).isEmpty());
	}

	@Test
	public void withAdvice() {
		HttpRequestExecutingMessageHandler handler = (HttpRequestExecutingMessageHandler) new DirectFieldAccessor(
				this.withAdvice).getPropertyValue("handler");
		handler.handleMessage(new GenericMessage<String>("foo"));
		assertEquals(1, adviceCalled);
	}

	@Test
	public void testInt2718FailForGatewayRequestChannelAttribute() {
		try {
			new ClassPathXmlApplicationContext("HttpOutboundGatewayWithinChainTests-fail-context.xml", this.getClass());
			fail("Expected BeanDefinitionParsingException");
		}
		catch (BeansException e) {
			assertTrue(e instanceof BeanDefinitionParsingException);
			assertTrue(e.getMessage().contains("'request-channel' attribute isn't allowed for a nested"));
		}
	}

	@Test
	public void withPoller() {
		assertThat(this.withPoller1, Matchers.instanceOf(PollingConsumer.class));
	}



	public static class StubErrorHandler implements ResponseErrorHandler {

		public boolean hasError(ClientHttpResponse response) throws IOException {
			return false;
		}

		public void handleError(ClientHttpResponse response) throws IOException {
		}
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception {
			adviceCalled++;
			return null;
		}

	}
}
