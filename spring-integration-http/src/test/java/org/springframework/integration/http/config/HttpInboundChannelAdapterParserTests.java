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

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.expression.Expression;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.http.AbstractHttpInboundTests;
import org.springframework.integration.http.converter.SerializingHttpMessageConverter;
import org.springframework.integration.http.inbound.HttpRequestHandlingController;
import org.springframework.integration.http.inbound.HttpRequestHandlingMessagingGateway;
import org.springframework.integration.http.support.DefaultHttpHeaderMapper;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.Validator;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.HandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willReturn;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Biju Kunjummen
 * @author Glenn Renfro
 */
@SpringJUnitConfig
@DirtiesContext
public class HttpInboundChannelAdapterParserTests extends AbstractHttpInboundTests {

	@Autowired
	private PollableChannel requests;

	@Autowired
	private HandlerMapping integrationRequestMappingHandlerMapping;

	@Autowired
	private HttpRequestHandlingMessagingGateway defaultAdapter;

	@Autowired
	private HttpRequestHandlingMessagingGateway postOnlyAdapter;

	@Autowired
	@Qualifier("adapterWithCustomConverterWithDefaults")
	private HttpRequestHandlingMessagingGateway adapterWithCustomConverterWithDefaults;

	@Autowired
	private HttpRequestHandlingMessagingGateway putOrDeleteAdapter;

	@Autowired
	private HttpRequestHandlingMessagingGateway withMappedHeaders;

	@Autowired
	private HttpRequestHandlingMessagingGateway inboundAdapterWithExpressions;

	@Autowired
	@Qualifier("adapterWithCustomConverterNoDefaults")
	private HttpRequestHandlingMessagingGateway adapterWithCustomConverterNoDefaults;

	@Autowired
	@Qualifier("adapterNoCustomConverterNoDefaults")
	private HttpRequestHandlingMessagingGateway adapterNoCustomConverterNoDefaults;

	@Autowired
	private HttpRequestHandlingController inboundController;

	@Autowired
	private HttpRequestHandlingController inboundControllerViewExp;

	@Autowired
	private MessageChannel autoChannel;

	@Autowired
	@Qualifier("autoChannel.adapter")
	private HttpRequestHandlingMessagingGateway autoChannelAdapter;

	@Autowired
	private Validator validator;

	@Test
	@SuppressWarnings("unchecked")
	public void getRequestOk() throws Exception {
		assertThat(TestUtils.<Boolean>getPropertyValue(this.defaultAdapter, "autoStartup")).isFalse();
		assertThat(TestUtils.<Integer>getPropertyValue(this.defaultAdapter, "phase")).isEqualTo(1001);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.setParameter("foo", "bar");
		MockHttpServletResponse response = new MockHttpServletResponse();
		this.defaultAdapter.handleRequest(request, response);
		assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
		this.defaultAdapter.start();
		response = new MockHttpServletResponse();
		willReturn(true).given(this.validator).supports(any());
		this.defaultAdapter.handleRequest(request, response);
		assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_SWITCHING_PROTOCOLS);
		Message<?> message = requests.receive(0);
		assertThat(message).isNotNull();
		Object payload = message.getPayload();
		assertThat(payload instanceof MultiValueMap).isTrue();
		MultiValueMap<String, String> map = (MultiValueMap<String, String>) payload;
		assertThat(map.size()).isEqualTo(1);
		assertThat(map.keySet().iterator().next()).isEqualTo("foo");
		assertThat(map.get("foo").size()).isEqualTo(1);
		assertThat(map.getFirst("foo")).isEqualTo("bar");
		assertThat(TestUtils.<Object>getPropertyValue(this.defaultAdapter, "errorChannel")).isNotNull();
		assertThat(TestUtils.<Validator>getPropertyValue(this.defaultAdapter, "validator"))
				.isSameAs(this.validator);
	}

	@Test
	public void getRequestWithHeaders() {
		DefaultHttpHeaderMapper headerMapper =
				TestUtils.<DefaultHttpHeaderMapper>getPropertyValue(withMappedHeaders, "headerMapper");

		HttpHeaders headers = new HttpHeaders();
		headers.set("foo", "foo");
		headers.set("bar", "bar");
		headers.set("baz", "baz");
		Map<String, Object> map = headerMapper.toHeaders(headers);
		assertThat(map.size() == 2).isTrue();
		assertThat(map.get("foo")).isEqualTo("foo");
		assertThat(map.get("bar")).isEqualTo("bar");
	}

	@Test
	// INT-1677
	public void withExpressions() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("text/plain");
		request.setParameter("foo", "bar");
		request.setContent("hello".getBytes());

		String requestURI = "/fname/bill/lname/clinton";

		//See org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping#handleMatch
		Map<String, String> uriTemplateVariables =
				new AntPathMatcher().extractUriTemplateVariables("/fname/{f}/lname/{l}", requestURI);
		request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVariables);

		request.setRequestURI(requestURI);

		MockHttpServletResponse response = new MockHttpServletResponse();
		inboundAdapterWithExpressions.handleRequest(request, response);
		assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
		Message<?> message = requests.receive(0);
		assertThat(message).isNotNull();
		Object payload = message.getPayload();
		assertThat(payload instanceof String).isTrue();
		assertThat(payload).isEqualTo("bill");
		assertThat(message.getHeaders().get("lname")).isEqualTo("clinton");
	}

	@Test
	public void getRequestNotAllowed() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.setParameter("foo", "bar");
		request.setRequestURI("/postOnly");

		assertThatExceptionOfType(HttpRequestMethodNotSupportedException.class)
				.isThrownBy(() -> this.integrationRequestMappingHandlerMapping.getHandler(request))
				.satisfies((ex) -> {
					assertThat(ex.getMethod()).isEqualTo("GET");
					assertThat(ex.getSupportedMethods()).containsExactly("POST");
				});
	}

	@Test
	public void postRequestWithTextContentOk() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContent("test".getBytes());
		request.setContentType("text/plain");

		MockHttpServletResponse response = new MockHttpServletResponse();
		postOnlyAdapter.handleRequest(request, response);
		assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
		Message<?> message = requests.receive(0);
		MessageHistory history = MessageHistory.read(message);
		assertThat(history).isNotNull();
		Properties componentHistoryRecord = TestUtils.locateComponentInHistory(history, "postOnlyAdapter", 0);
		assertThat(componentHistoryRecord).isNotNull();
		assertThat(componentHistoryRecord.get("type")).isEqualTo("http:inbound-channel-adapter");
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("test");
	}

	@Test
	@DirtiesContext
	public void postRequestWithSerializedObjectContentOk() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		Object obj = new TestObject("testObject");
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		new ObjectOutputStream(byteStream).writeObject(obj);
		request.setContent(byteStream.toByteArray());
		request.setContentType("application/x-java-serialized-object");

		MockHttpServletResponse response = new MockHttpServletResponse();

		adapterWithCustomConverterWithDefaults.handleRequest(request, response);
		assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
		Message<?> message = requests.receive(0);
		assertThat(message).isNotNull();
		assertThat(message.getPayload() instanceof TestObject).isTrue();
		assertThat(((TestObject) message.getPayload()).text).isEqualTo("testObject");
	}

	@Test
	public void putOrDeleteMethodsSupported() {
		HttpMethod[] supportedMethods =
				TestUtils.<HttpMethod[]>getPropertyValue(putOrDeleteAdapter, "requestMapping.methods");
		assertThat(supportedMethods.length).isEqualTo(2);
		assertThat(supportedMethods).containsExactly(HttpMethod.PUT, HttpMethod.DELETE);
	}

	@Test
	public void testController() {
		String errorCode = TestUtils.getPropertyValue(inboundController, "errorCode");
		assertThat(errorCode).isEqualTo("oops");
		Expression viewExpression = TestUtils.getPropertyValue(inboundController, "viewExpression");
		assertThat(viewExpression.getExpressionString()).isEqualTo("foo");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.setParameter("foo", "bar");
		MockHttpServletResponse response = new MockHttpServletResponse();
		inboundController.handleRequest(request, response);
		assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_ACCEPTED);
		Message<?> message = requests.receive(0);
		assertThat(message).isNotNull();
	}

	@Test
	public void testInt2717ControllerWithViewExpression() {
		Expression viewExpression = TestUtils.getPropertyValue(inboundControllerViewExp, "viewExpression");
		assertThat(viewExpression.getExpressionString()).isEqualTo("'foo'");
	}

	@Test
	public void testAutoChannel() {
		assertThat(TestUtils.<MessageChannel>getPropertyValue(autoChannelAdapter, "requestChannel"))
				.isSameAs(autoChannel);
	}

	@Test
	public void testInboundAdapterWithMessageConverterDefaults() {
		List<HttpMessageConverter<?>> messageConverters = TestUtils.getPropertyValue(
				adapterWithCustomConverterWithDefaults, "messageConverters");
		assertThat(messageConverters.size() > 1)
				.as("There should be more than 1 message converter. The customized one and the defaults.").isTrue();

		//First converter should be the customized one
		assertThat(messageConverters.get(0)).isInstanceOf(SerializingHttpMessageConverter.class);
	}

	@Test
	public void testInboundAdapterWithNoMessageConverterDefaults() {
		List<HttpMessageConverter<?>> messageConverters = TestUtils.getPropertyValue(
				adapterWithCustomConverterNoDefaults, "messageConverters");
		//First converter should be the customized one
		assertThat(messageConverters.get(0)).isInstanceOf(SerializingHttpMessageConverter.class);
		assertThat(messageConverters).as("There should be only the customized MessageConverter registered.")
				.hasSize(1);
	}

	@Test
	public void testInboundAdapterWithNoMessageConverterNoDefaults() {
		List<HttpMessageConverter<?>> messageConverters =
				TestUtils.getPropertyValue(adapterNoCustomConverterNoDefaults, "messageConverters");
		assertThat(messageConverters.size() > 1).as("There should be more than 1 message converter. The defaults.")
				.isTrue();
	}

	@SuppressWarnings("serial")
	private static class TestObject implements Serializable {

		String text;

		TestObject(String text) {
			this.text = text;
		}

	}

}
