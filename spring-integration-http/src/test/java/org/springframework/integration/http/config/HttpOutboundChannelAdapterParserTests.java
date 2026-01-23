/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.http.config;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;

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
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Biju Kunjummen
 * @author Shiliang Li
 * @author Glenn Renfro
 */
@SpringJUnitConfig
@DirtiesContext
public class HttpOutboundChannelAdapterParserTests {

	@Autowired
	@Qualifier("minimalConfig")
	private AbstractEndpoint minimalConfig;

	@Autowired
	@Qualifier("fullConfig")
	private AbstractEndpoint fullConfig;

	@Autowired
	@Qualifier("restTemplateConfig")
	private AbstractEndpoint restTemplateConfig;

	@Autowired
	@Qualifier("customRestTemplate")
	private RestTemplate customRestTemplate;

	@Autowired
	@Qualifier("withUrlAndTemplate")
	private AbstractEndpoint withUrlAndTemplate;

	@Autowired
	@Qualifier("withUrlExpression")
	private AbstractEndpoint withUrlExpression;

	@Autowired
	@Qualifier("withAdvice")
	private AbstractEndpoint withAdvice;

	@Autowired
	@Qualifier("withUrlExpressionAndTemplate")
	private AbstractEndpoint withUrlExpressionAndTemplate;

	@Autowired
	@Qualifier("withPoller1")
	private AbstractEndpoint withPoller1;

	@Autowired
	private ApplicationContext applicationContext;

	private static volatile int adviceCalled;

	@Test
	public void minimalConfig() {
		DirectFieldAccessor endpointAccessor = new DirectFieldAccessor(this.minimalConfig);
		RestTemplate restTemplate =
				TestUtils.<RestTemplate>getPropertyValue(this.minimalConfig, "handler.restTemplate");
		assertThat(restTemplate).isNotSameAs(customRestTemplate);
		HttpRequestExecutingMessageHandler handler = (HttpRequestExecutingMessageHandler) endpointAccessor
				.getPropertyValue("handler");
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		assertThat(handlerAccessor.getPropertyValue("expectReply")).isEqualTo(false);
		assertThat(endpointAccessor.getPropertyValue("inputChannel"))
				.isEqualTo(this.applicationContext.getBean("requests"));
		assertThat(handlerAccessor.getPropertyValue("outputChannel")).isNull();
		DirectFieldAccessor templateAccessor =
				new DirectFieldAccessor(handlerAccessor.getPropertyValue("restTemplate"));
		ClientHttpRequestFactory requestFactory = (ClientHttpRequestFactory)
				templateAccessor.getPropertyValue("requestFactory");
		assertThat(requestFactory instanceof SimpleClientHttpRequestFactory).isTrue();
		Expression uriExpression = (Expression) handlerAccessor.getPropertyValue("uriExpression");
		assertThat(uriExpression.getValue()).isEqualTo("http://localhost/test1");
		assertThat(TestUtils.<Expression>getPropertyValue(handler, "httpMethodExpression").getExpressionString())
				.isEqualTo(HttpMethod.POST.name());
		assertThat(handlerAccessor.getPropertyValue("charset")).isEqualTo(StandardCharsets.UTF_8);
		assertThat(handlerAccessor.getPropertyValue("extractPayload")).isEqualTo(true);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void fullConfig() {
		DirectFieldAccessor endpointAccessor = new DirectFieldAccessor(this.fullConfig);
		HttpRequestExecutingMessageHandler handler = (HttpRequestExecutingMessageHandler) endpointAccessor
				.getPropertyValue("handler");
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		assertThat(handlerAccessor.getPropertyValue("expectReply")).isEqualTo(false);
		assertThat(endpointAccessor.getPropertyValue("inputChannel"))
				.isEqualTo(this.applicationContext.getBean("requests"));
		assertThat(handlerAccessor.getPropertyValue("outputChannel")).isNull();
		assertThat(handlerAccessor.getPropertyValue("order")).isEqualTo(77);
		assertThat(endpointAccessor.getPropertyValue("autoStartup")).isEqualTo(Boolean.FALSE);
		DirectFieldAccessor templateAccessor =
				new DirectFieldAccessor(handlerAccessor.getPropertyValue("restTemplate"));
		ClientHttpRequestFactory requestFactory = (ClientHttpRequestFactory)
				templateAccessor.getPropertyValue("requestFactory");
		assertThat(TestUtils.<Expression>getPropertyValue(handler, "expectedResponseTypeExpression").getValue())
				.isEqualTo(Boolean.class.getName());
		assertThat(requestFactory instanceof SimpleClientHttpRequestFactory).isTrue();
		Object converterListBean = this.applicationContext.getBean("converterList");
		assertThat(templateAccessor.getPropertyValue("messageConverters")).isEqualTo(converterListBean);
		Object requestFactoryBean = this.applicationContext.getBean("testRequestFactory");
		assertThat(requestFactory).isEqualTo(requestFactoryBean);
		Object errorHandlerBean = this.applicationContext.getBean("testErrorHandler");
		assertThat(templateAccessor.getPropertyValue("errorHandler")).isEqualTo(errorHandlerBean);
		Expression uriExpression = (Expression) handlerAccessor.getPropertyValue("uriExpression");
		assertThat(uriExpression.getValue()).isEqualTo("http://localhost/test2/{foo}");
		assertThat(TestUtils.<Expression>getPropertyValue(handler, "httpMethodExpression").getExpressionString())
				.isEqualTo(HttpMethod.GET.name());
		assertThat(handlerAccessor.getPropertyValue("charset")).isEqualTo(StandardCharsets.UTF_8);
		assertThat(handlerAccessor.getPropertyValue("extractPayload")).isEqualTo(false);
		Map<String, Expression> uriVariableExpressions =
				(Map<String, Expression>) handlerAccessor.getPropertyValue("uriVariableExpressions");
		assertThat(uriVariableExpressions.size()).isEqualTo(1);
		assertThat(uriVariableExpressions.get("foo").getExpressionString()).isEqualTo("headers.bar");
		DirectFieldAccessor mapperAccessor = new DirectFieldAccessor(handlerAccessor.getPropertyValue("headerMapper"));
		String[] mappedRequestHeaders = (String[]) mapperAccessor.getPropertyValue("outboundHeaderNames");
		String[] mappedResponseHeaders = (String[]) mapperAccessor.getPropertyValue("inboundHeaderNames");
		assertThat(mappedRequestHeaders.length).isEqualTo(2);
		assertThat(mappedResponseHeaders.length).isEqualTo(0);
		assertThat(ObjectUtils.containsElement(mappedRequestHeaders, "requestHeader1")).isTrue();
		assertThat(ObjectUtils.containsElement(mappedRequestHeaders, "requestHeader2")).isTrue();
		assertThat(
				TestUtils.<DefaultUriBuilderFactory.EncodingMode>getPropertyValue(
						handler, "restTemplate.uriTemplateHandler.encodingMode"))
				.isEqualTo(DefaultUriBuilderFactory.EncodingMode.VALUES_ONLY);
	}

	@Test
	public void restTemplateConfig() {
		RestTemplate restTemplate =
				TestUtils.getPropertyValue(this.restTemplateConfig, "handler.restTemplate");
		assertThat(restTemplate).isEqualTo(customRestTemplate);
	}

	@Test
	public void failWithRestTemplateAndRestAttributes() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext("HttpOutboundChannelAdapterParserTests-fail-context.xml",
								getClass()));
	}

	@Test
	public void withUrlAndTemplate() {
		DirectFieldAccessor endpointAccessor = new DirectFieldAccessor(this.withUrlAndTemplate);
		RestTemplate restTemplate = TestUtils.getPropertyValue(this.withUrlAndTemplate, "handler.restTemplate");
		assertThat(restTemplate).isSameAs(customRestTemplate);
		HttpRequestExecutingMessageHandler handler = (HttpRequestExecutingMessageHandler) endpointAccessor
				.getPropertyValue("handler");
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		assertThat(handlerAccessor.getPropertyValue("expectReply")).isEqualTo(false);
		assertThat(endpointAccessor.getPropertyValue("inputChannel"))
				.isEqualTo(this.applicationContext.getBean("requests"));
		assertThat(handlerAccessor.getPropertyValue("outputChannel")).isNull();
		DirectFieldAccessor templateAccessor =
				new DirectFieldAccessor(handlerAccessor.getPropertyValue("restTemplate"));
		ClientHttpRequestFactory requestFactory = (ClientHttpRequestFactory)
				templateAccessor.getPropertyValue("requestFactory");
		assertThat(requestFactory instanceof SimpleClientHttpRequestFactory).isTrue();
		Expression uriExpression = (Expression) handlerAccessor.getPropertyValue("uriExpression");
		assertThat(uriExpression.getValue()).isEqualTo("http://localhost/test1");
		assertThat(TestUtils.<Expression>getPropertyValue(handler, "httpMethodExpression").getExpressionString())
				.isEqualTo(HttpMethod.POST.name());
		assertThat(handlerAccessor.getPropertyValue("charset")).isEqualTo(StandardCharsets.UTF_8);
		assertThat(handlerAccessor.getPropertyValue("extractPayload")).isEqualTo(true);

		//INT-3055
		Object uriVariablesExpression = handlerAccessor.getPropertyValue("uriVariablesExpression");
		assertThat(uriVariablesExpression).isNotNull();
		assertThat(((Expression) uriVariablesExpression).getExpressionString()).isEqualTo("@uriVariables");
		Object uriVariableExpressions = handlerAccessor.getPropertyValue("uriVariableExpressions");
		assertThat(uriVariableExpressions).isNotNull();
		assertThat(((Map<?, ?>) uriVariableExpressions).isEmpty()).isTrue();
	}

	@Test
	public void withUrlExpression() {
		DirectFieldAccessor endpointAccessor = new DirectFieldAccessor(this.withUrlExpression);
		RestTemplate restTemplate = TestUtils.getPropertyValue(this.withUrlExpression, "handler.restTemplate");
		assertThat(restTemplate).isNotSameAs(customRestTemplate);
		HttpRequestExecutingMessageHandler handler = (HttpRequestExecutingMessageHandler) endpointAccessor
				.getPropertyValue("handler");
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		assertThat(handlerAccessor.getPropertyValue("expectReply")).isEqualTo(false);
		assertThat(endpointAccessor.getPropertyValue("inputChannel"))
				.isEqualTo(this.applicationContext.getBean("requests"));
		assertThat(handlerAccessor.getPropertyValue("outputChannel")).isNull();
		DirectFieldAccessor templateAccessor =
				new DirectFieldAccessor(handlerAccessor.getPropertyValue("restTemplate"));
		ClientHttpRequestFactory requestFactory = (ClientHttpRequestFactory)
				templateAccessor.getPropertyValue("requestFactory");
		assertThat(requestFactory instanceof SimpleClientHttpRequestFactory).isTrue();
		SpelExpression expression = (SpelExpression) handlerAccessor.getPropertyValue("uriExpression");
		assertThat(expression).isNotNull();
		assertThat(expression.getExpressionString()).isEqualTo("'http://localhost/test1'");
		assertThat(TestUtils.<Expression>getPropertyValue(handler, "httpMethodExpression").getExpressionString())
				.isEqualTo(HttpMethod.POST.name());
		assertThat(handlerAccessor.getPropertyValue("charset")).isEqualTo(StandardCharsets.UTF_8);
		assertThat(handlerAccessor.getPropertyValue("extractPayload")).isEqualTo(true);
	}

	@Test
	public void withAdvice() {
		MessageHandler handler = TestUtils.getPropertyValue(this.withAdvice, "handler");
		handler.handleMessage(new GenericMessage<String>("foo"));
		assertThat(adviceCalled).isEqualTo(1);
	}

	@Test
	public void withUrlExpressionAndTemplate() {
		DirectFieldAccessor endpointAccessor = new DirectFieldAccessor(this.withUrlExpressionAndTemplate);
		RestTemplate restTemplate =
				TestUtils.getPropertyValue(this.withUrlExpressionAndTemplate, "handler.restTemplate");
		assertThat(restTemplate).isSameAs(customRestTemplate);
		HttpRequestExecutingMessageHandler handler = (HttpRequestExecutingMessageHandler) endpointAccessor
				.getPropertyValue("handler");
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		assertThat(handlerAccessor.getPropertyValue("expectReply")).isEqualTo(false);
		assertThat(endpointAccessor.getPropertyValue("inputChannel"))
				.isEqualTo(this.applicationContext.getBean("requests"));
		assertThat(handlerAccessor.getPropertyValue("outputChannel")).isNull();
		DirectFieldAccessor templateAccessor =
				new DirectFieldAccessor(handlerAccessor.getPropertyValue("restTemplate"));
		ClientHttpRequestFactory requestFactory = (ClientHttpRequestFactory)
				templateAccessor.getPropertyValue("requestFactory");
		assertThat(requestFactory instanceof SimpleClientHttpRequestFactory).isTrue();
		SpelExpression expression = (SpelExpression) handlerAccessor.getPropertyValue("uriExpression");
		assertThat(expression).isNotNull();
		assertThat(expression.getExpressionString()).isEqualTo("'http://localhost/test1'");
		assertThat(TestUtils.<Expression>getPropertyValue(handler, "httpMethodExpression").getExpressionString())
				.isEqualTo(HttpMethod.POST.name());
		assertThat(handlerAccessor.getPropertyValue("charset")).isEqualTo(StandardCharsets.UTF_8);
		assertThat(handlerAccessor.getPropertyValue("extractPayload")).isEqualTo(true);
	}

	@Test
	public void withPoller() {
		assertThat(this.withPoller1).isInstanceOf(PollingConsumer.class);
	}

	@Test
	public void failWithUrlAndExpression() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext("HttpOutboundChannelAdapterParserTests-url-fail-context.xml",
								getClass()));
	}

	public static class StubErrorHandler implements ResponseErrorHandler {

		@Override
		public boolean hasError(ClientHttpResponse response) {
			return false;
		}

		@Override
		public void handleError(URI url, HttpMethod method, ClientHttpResponse response) {
		}

	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
			adviceCalled++;
			return null;
		}

	}

}
