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
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.ResponseErrorHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 * @author Biju Kunjummen
 * @author Glenn Renfro
 */
@SpringJUnitConfig
@DirtiesContext
public class HttpOutboundGatewayParserTests {

	public static final ResponseErrorHandler mockResponseErrorHandler = mock();

	@Autowired
	@Qualifier("minimalConfig")
	private EventDrivenConsumer minimalConfigEndpoint;

	@Autowired
	@Qualifier("fullConfig")
	private EventDrivenConsumer fullConfigEndpoint;

	@Autowired
	@Qualifier("withUrlExpression")
	private EventDrivenConsumer withUrlExpressionEndpoint;

	@Autowired
	@Qualifier("withAdvice")
	private EventDrivenConsumer withAdvice;

	@Autowired
	@Qualifier("withPoller1")
	private AbstractEndpoint withPoller1;

	@Autowired
	private ApplicationContext applicationContext;

	private static volatile int adviceCalled;

	@Test
	public void minimalConfig() {
		HttpRequestExecutingMessageHandler handler =
				(HttpRequestExecutingMessageHandler) this.minimalConfigEndpoint.getHandler();
		MessageChannel requestChannel = this.minimalConfigEndpoint.getInputChannel();
		assertThat(requestChannel).isEqualTo(this.applicationContext.getBean("requests"));
		Object replyChannel = handler.getOutputChannel();
		assertThat(replyChannel).isNull();
		Object requestFactory = TestUtils.getPropertyValue(handler, "restTemplate.requestFactory");
		assertThat(requestFactory).isInstanceOf(SimpleClientHttpRequestFactory.class);
		Expression uriExpression = TestUtils.getPropertyValue(handler, "uriExpression");
		assertThat(uriExpression.getValue()).isEqualTo("http://localhost/test1");
		assertThat(TestUtils.<Expression>getPropertyValue(handler, "httpMethodExpression").getExpressionString())
				.isEqualTo(HttpMethod.POST.name());
		assertThat(TestUtils.<Object>getPropertyValue(handler, "charset")).isEqualTo(StandardCharsets.UTF_8);
		assertThat(TestUtils.<Boolean>getPropertyValue(handler, "extractPayload")).isEqualTo(true);
		assertThat(TestUtils.<Boolean>getPropertyValue(handler, "transferCookies")).isEqualTo(false);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void fullConfig() {
		DirectFieldAccessor endpointAccessor = new DirectFieldAccessor(this.fullConfigEndpoint);
		HttpRequestExecutingMessageHandler handler =
				(HttpRequestExecutingMessageHandler) this.fullConfigEndpoint.getHandler();
		MessageChannel requestChannel = this.fullConfigEndpoint.getInputChannel();
		assertThat(requestChannel).isEqualTo(this.applicationContext.getBean("requests"));
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		assertThat(handlerAccessor.getPropertyValue("order")).isEqualTo(77);
		assertThat(endpointAccessor.getPropertyValue("autoStartup")).isEqualTo(Boolean.FALSE);
		Object replyChannel = handlerAccessor.getPropertyValue("outputChannel");
		assertThat(replyChannel).isNotNull();
		assertThat(replyChannel).isEqualTo(this.applicationContext.getBean("replies"));
		Object requestFactory = TestUtils.getPropertyValue(handler, "restTemplate.requestFactory");
		assertThat(requestFactory).isInstanceOf(SimpleClientHttpRequestFactory.class);
		Object converterListBean = this.applicationContext.getBean("converterList");
		assertThat(TestUtils.<Object>getPropertyValue(handler, "restTemplate.messageConverters"))
				.isEqualTo(converterListBean);

		assertThat(TestUtils.<Expression>getPropertyValue(handler, "expectedResponseTypeExpression").getValue())
				.isEqualTo(String.class.getName());
		Expression uriExpression = (Expression) handlerAccessor.getPropertyValue("uriExpression");
		assertThat(uriExpression.getValue()).isEqualTo("http://localhost/test2");
		assertThat(TestUtils.<Expression>getPropertyValue(handler, "httpMethodExpression").getExpressionString())
				.isEqualTo(HttpMethod.PUT.name());
		assertThat(handlerAccessor.getPropertyValue("charset")).isEqualTo(StandardCharsets.UTF_8);
		assertThat(handlerAccessor.getPropertyValue("extractPayload")).isEqualTo(false);
		Object requestFactoryBean = this.applicationContext.getBean("testRequestFactory");
		assertThat(requestFactory).isEqualTo(requestFactoryBean);
		Object errorHandlerBean = this.applicationContext.getBean("testErrorHandler");
		assertThat(TestUtils.<Object>getPropertyValue(handler, "restTemplate.errorHandler"))
				.isEqualTo(errorHandlerBean);
		Object sendTimeout = new DirectFieldAccessor(
				handlerAccessor.getPropertyValue("messagingTemplate")).getPropertyValue("sendTimeout");
		assertThat(sendTimeout).isEqualTo(1234L);
		Map<String, Expression> uriVariableExpressions =
				(Map<String, Expression>) handlerAccessor.getPropertyValue("uriVariableExpressions");
		assertThat(uriVariableExpressions.size()).isEqualTo(1);
		assertThat(uriVariableExpressions.get("foo").getExpressionString()).isEqualTo("headers.bar");
		DirectFieldAccessor mapperAccessor = new DirectFieldAccessor(handlerAccessor.getPropertyValue("headerMapper"));
		String[] mappedRequestHeaders = (String[]) mapperAccessor.getPropertyValue("outboundHeaderNames");
		String[] mappedResponseHeaders = (String[]) mapperAccessor.getPropertyValue("inboundHeaderNames");
		assertThat(mappedRequestHeaders.length).isEqualTo(2);
		assertThat(mappedResponseHeaders.length).isEqualTo(1);
		assertThat(ObjectUtils.containsElement(mappedRequestHeaders, "requestHeader1")).isTrue();
		assertThat(ObjectUtils.containsElement(mappedRequestHeaders, "requestHeader2")).isTrue();
		assertThat(mappedResponseHeaders[0]).isEqualTo("responseHeader");
		assertThat(handlerAccessor.getPropertyValue("transferCookies")).isEqualTo(true);
		assertThat(handlerAccessor.getPropertyValue("extractResponseBody")).isEqualTo(false);
	}

	@Test
	public void withUrlExpression() {
		HttpRequestExecutingMessageHandler handler =
				(HttpRequestExecutingMessageHandler) this.withUrlExpressionEndpoint.getHandler();
		MessageChannel requestChannel = this.withUrlExpressionEndpoint.getInputChannel();
		assertThat(requestChannel).isEqualTo(this.applicationContext.getBean("requests"));
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		Object replyChannel = handlerAccessor.getPropertyValue("outputChannel");
		assertThat(replyChannel).isNull();
		DirectFieldAccessor templateAccessor = new DirectFieldAccessor(handlerAccessor.getPropertyValue("restTemplate"));
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
		assertThat(handlerAccessor.getPropertyValue("transferCookies")).isEqualTo(false);

		//INT-3055
		Object uriVariablesExpression = handlerAccessor.getPropertyValue("uriVariablesExpression");
		assertThat(uriVariablesExpression).isNotNull();
		assertThat(((Expression) uriVariablesExpression).getExpressionString()).isEqualTo("@uriVariables");
		Object uriVariableExpressions = handlerAccessor.getPropertyValue("uriVariableExpressions");
		assertThat(uriVariableExpressions).isNotNull();
		assertThat(((Map<?, ?>) uriVariableExpressions).isEmpty()).isTrue();
	}

	@Test
	public void withAdvice() {
		MessageHandler handler = this.withAdvice.getHandler();
		handler.handleMessage(new GenericMessage<>("foo"));
		assertThat(adviceCalled).isEqualTo(1);
	}

	@Test
	public void testInt2718FailForGatewayRequestChannelAttribute() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext("HttpOutboundGatewayWithinChainTests-fail-context.xml",
								getClass()))
				.withMessageContaining("'request-channel' attribute isn't allowed for a nested");
	}

	@Test
	public void withPoller() {
		assertThat(this.withPoller1).isInstanceOf(PollingConsumer.class);
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
			adviceCalled++;
			return null;
		}

	}

}
