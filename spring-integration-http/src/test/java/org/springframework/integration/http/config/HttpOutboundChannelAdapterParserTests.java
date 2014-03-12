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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;

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
import org.springframework.messaging.MessageHandler;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Biju Kunjummen
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class HttpOutboundChannelAdapterParserTests {

	@Autowired @Qualifier("minimalConfig")
	private AbstractEndpoint minimalConfig;

	@Autowired @Qualifier("fullConfig")
	private AbstractEndpoint fullConfig;

	@Autowired @Qualifier("restTemplateConfig")
	private AbstractEndpoint restTemplateConfig;

	@Autowired @Qualifier("customRestTemplate")
	private RestTemplate customRestTemplate;

	@Autowired @Qualifier("withUrlAndTemplate")
	private AbstractEndpoint withUrlAndTemplate;

	@Autowired @Qualifier("withUrlExpression")
	private AbstractEndpoint withUrlExpression;

	@Autowired @Qualifier("withAdvice")
	private AbstractEndpoint withAdvice;

	@Autowired @Qualifier("withUrlExpressionAndTemplate")
	private AbstractEndpoint withUrlExpressionAndTemplate;

	@Autowired @Qualifier("withPoller1")
	private AbstractEndpoint withPoller1;

	@Autowired
	private ApplicationContext applicationContext;

	private static volatile int adviceCalled;

	@Test
	public void minimalConfig() {
		DirectFieldAccessor endpointAccessor = new DirectFieldAccessor(this.minimalConfig);
		RestTemplate restTemplate =
			TestUtils.getPropertyValue(this.minimalConfig, "handler.restTemplate", RestTemplate.class);
		assertNotSame(customRestTemplate, restTemplate);
		HttpRequestExecutingMessageHandler handler = (HttpRequestExecutingMessageHandler) endpointAccessor.getPropertyValue("handler");
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		assertEquals(false, handlerAccessor.getPropertyValue("expectReply"));
		assertEquals(this.applicationContext.getBean("requests"), endpointAccessor.getPropertyValue("inputChannel"));
		assertNull(handlerAccessor.getPropertyValue("outputChannel"));
		DirectFieldAccessor templateAccessor = new DirectFieldAccessor(handlerAccessor.getPropertyValue("restTemplate"));
		ClientHttpRequestFactory requestFactory = (ClientHttpRequestFactory)
				templateAccessor.getPropertyValue("requestFactory");
		assertTrue(requestFactory instanceof SimpleClientHttpRequestFactory);
		Expression uriExpression = (Expression) handlerAccessor.getPropertyValue("uriExpression");
		assertEquals("http://localhost/test1", uriExpression.getValue());
		assertEquals(HttpMethod.POST.name(), TestUtils.getPropertyValue(handler, "httpMethodExpression", Expression.class).getExpressionString());
		assertEquals("UTF-8", handlerAccessor.getPropertyValue("charset"));
		assertEquals(true, handlerAccessor.getPropertyValue("extractPayload"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void fullConfig() {
		DirectFieldAccessor endpointAccessor = new DirectFieldAccessor(this.fullConfig);
		HttpRequestExecutingMessageHandler handler = (HttpRequestExecutingMessageHandler) endpointAccessor.getPropertyValue("handler");
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		assertEquals(false, handlerAccessor.getPropertyValue("expectReply"));
		assertEquals(this.applicationContext.getBean("requests"), endpointAccessor.getPropertyValue("inputChannel"));
		assertNull(handlerAccessor.getPropertyValue("outputChannel"));
		assertEquals(77, handlerAccessor.getPropertyValue("order"));
		assertEquals(Boolean.FALSE, endpointAccessor.getPropertyValue("autoStartup"));
		DirectFieldAccessor templateAccessor = new DirectFieldAccessor(handlerAccessor.getPropertyValue("restTemplate"));
		ClientHttpRequestFactory requestFactory = (ClientHttpRequestFactory)
				templateAccessor.getPropertyValue("requestFactory");
		assertEquals(Boolean.class.getName(), TestUtils.getPropertyValue(handler, "expectedResponseTypeExpression", Expression.class).getValue());
		assertTrue(requestFactory instanceof SimpleClientHttpRequestFactory);
		Object converterListBean = this.applicationContext.getBean("converterList");
		assertEquals(converterListBean, templateAccessor.getPropertyValue("messageConverters"));
		Object requestFactoryBean = this.applicationContext.getBean("testRequestFactory");
		assertEquals(requestFactoryBean, requestFactory);
		Object errorHandlerBean = this.applicationContext.getBean("testErrorHandler");
		assertEquals(errorHandlerBean, templateAccessor.getPropertyValue("errorHandler"));
		Expression uriExpression = (Expression) handlerAccessor.getPropertyValue("uriExpression");
		assertEquals("http://localhost/test2/{foo}", uriExpression.getValue());
		assertEquals(HttpMethod.GET.name(), TestUtils.getPropertyValue(handler, "httpMethodExpression", Expression.class).getExpressionString());
		assertEquals("UTF-8", handlerAccessor.getPropertyValue("charset"));
		assertEquals(false, handlerAccessor.getPropertyValue("extractPayload"));
		Map<String, Expression> uriVariableExpressions =
				(Map<String, Expression>) handlerAccessor.getPropertyValue("uriVariableExpressions");
		assertEquals(1, uriVariableExpressions.size());
		assertEquals("headers.bar", uriVariableExpressions.get("foo").getExpressionString());
		DirectFieldAccessor mapperAccessor = new DirectFieldAccessor(handlerAccessor.getPropertyValue("headerMapper"));
		String[] mappedRequestHeaders = (String[]) mapperAccessor.getPropertyValue("outboundHeaderNames");
		String[] mappedResponseHeaders = (String[]) mapperAccessor.getPropertyValue("inboundHeaderNames");
		assertEquals(2, mappedRequestHeaders.length);
		assertEquals(0, mappedResponseHeaders.length);
		assertTrue(ObjectUtils.containsElement(mappedRequestHeaders, "requestHeader1"));
		assertTrue(ObjectUtils.containsElement(mappedRequestHeaders, "requestHeader2"));
	}

	@Test
	public void restTemplateConfig() {
		RestTemplate restTemplate =
			TestUtils.getPropertyValue(this.restTemplateConfig, "handler.restTemplate", RestTemplate.class);
		assertEquals(customRestTemplate, restTemplate);
	}

	@Test(expected=BeanDefinitionParsingException.class)
	public void failWithRestTemplateAndRestAttributes() {
		new ClassPathXmlApplicationContext("HttpOutboundChannelAdapterParserTests-fail-context.xml", this.getClass());
	}

	@Test
	public void withUrlAndTemplate() {
		DirectFieldAccessor endpointAccessor = new DirectFieldAccessor(this.withUrlAndTemplate);
		RestTemplate restTemplate =
			TestUtils.getPropertyValue(this.withUrlAndTemplate, "handler.restTemplate", RestTemplate.class);
		assertSame(customRestTemplate, restTemplate);
		HttpRequestExecutingMessageHandler handler = (HttpRequestExecutingMessageHandler) endpointAccessor.getPropertyValue("handler");
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		assertEquals(false, handlerAccessor.getPropertyValue("expectReply"));
		assertEquals(this.applicationContext.getBean("requests"), endpointAccessor.getPropertyValue("inputChannel"));
		assertNull(handlerAccessor.getPropertyValue("outputChannel"));
		DirectFieldAccessor templateAccessor = new DirectFieldAccessor(handlerAccessor.getPropertyValue("restTemplate"));
		ClientHttpRequestFactory requestFactory = (ClientHttpRequestFactory)
				templateAccessor.getPropertyValue("requestFactory");
		assertTrue(requestFactory instanceof SimpleClientHttpRequestFactory);
		Expression uriExpression = (Expression) handlerAccessor.getPropertyValue("uriExpression");
		assertEquals("http://localhost/test1", uriExpression.getValue());
		assertEquals(HttpMethod.POST.name(), TestUtils.getPropertyValue(handler, "httpMethodExpression", Expression.class).getExpressionString());
		assertEquals("UTF-8", handlerAccessor.getPropertyValue("charset"));
		assertEquals(true, handlerAccessor.getPropertyValue("extractPayload"));

		//INT-3055
		Object uriVariablesExpression = handlerAccessor.getPropertyValue("uriVariablesExpression");
		assertNotNull(uriVariablesExpression);
		assertEquals("@uriVariables", ((Expression) uriVariablesExpression).getExpressionString());
		Object uriVariableExpressions = handlerAccessor.getPropertyValue("uriVariableExpressions");
		assertNotNull(uriVariableExpressions);
		assertTrue(((Map<?, ?>) uriVariableExpressions).isEmpty());
	}

	@Test
	public void withUrlExpression() {
		DirectFieldAccessor endpointAccessor = new DirectFieldAccessor(this.withUrlExpression);
		RestTemplate restTemplate =
			TestUtils.getPropertyValue(this.withUrlExpression, "handler.restTemplate", RestTemplate.class);
		assertNotSame(customRestTemplate, restTemplate);
		HttpRequestExecutingMessageHandler handler = (HttpRequestExecutingMessageHandler) endpointAccessor.getPropertyValue("handler");
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		assertEquals(false, handlerAccessor.getPropertyValue("expectReply"));
		assertEquals(this.applicationContext.getBean("requests"), endpointAccessor.getPropertyValue("inputChannel"));
		assertNull(handlerAccessor.getPropertyValue("outputChannel"));
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
	}

	@Test
	public void withAdvice() {
		MessageHandler handler = TestUtils.getPropertyValue(this.withAdvice, "handler", MessageHandler.class);
		handler.handleMessage(new GenericMessage<String>("foo"));
		assertEquals(1, adviceCalled);
	}

	@Test
	public void withUrlExpressionAndTemplate() {
		DirectFieldAccessor endpointAccessor = new DirectFieldAccessor(this.withUrlExpressionAndTemplate);
		RestTemplate restTemplate =
			TestUtils.getPropertyValue(this.withUrlExpressionAndTemplate, "handler.restTemplate", RestTemplate.class);
		assertSame(customRestTemplate, restTemplate);
		HttpRequestExecutingMessageHandler handler = (HttpRequestExecutingMessageHandler) endpointAccessor.getPropertyValue("handler");
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		assertEquals(false, handlerAccessor.getPropertyValue("expectReply"));
		assertEquals(this.applicationContext.getBean("requests"), endpointAccessor.getPropertyValue("inputChannel"));
		assertNull(handlerAccessor.getPropertyValue("outputChannel"));
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
	}

	@Test
	public void withPoller() {
		assertThat(this.withPoller1, Matchers.instanceOf(PollingConsumer.class));
	}

	@Test(expected=BeanDefinitionParsingException.class)
	public void failWithUrlAndExpression() {
		new ClassPathXmlApplicationContext("HttpOutboundChannelAdapterParserTests-url-fail-context.xml", this.getClass());
	}

	public static class StubErrorHandler implements ResponseErrorHandler {

		@Override
		public boolean hasError(ClientHttpResponse response) throws IOException {
			return false;
		}

		@Override
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
