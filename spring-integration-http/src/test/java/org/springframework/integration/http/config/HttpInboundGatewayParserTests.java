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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.converter.Converter;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.http.converter.SerializingHttpMessageConverter;
import org.springframework.integration.http.inbound.HttpRequestHandlingController;
import org.springframework.integration.http.inbound.HttpRequestHandlingMessagingGateway;
import org.springframework.integration.http.support.DefaultHttpHeaderMapper;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Gunnar Hillert
 * @author Biju Kunjummen
 * @author Artem Bilan
 * @author Glenn Renfro
 */
@SpringJUnitConfig
@DirtiesContext
public class HttpInboundGatewayParserTests {

	@Autowired
	@Qualifier("inboundGateway")
	private HttpRequestHandlingMessagingGateway gateway;

	@Autowired
	@Qualifier("inboundGatewayWithOneCustomConverter")
	private HttpRequestHandlingMessagingGateway gatewayWithOneCustomConverter;

	@Autowired
	@Qualifier("inboundGatewayNoDefaultConverters")
	private HttpRequestHandlingMessagingGateway gatewayNoDefaultConverters;

	@Autowired
	@Qualifier("inboundGatewayWithCustomAndDefaultConverters")
	private HttpRequestHandlingMessagingGateway gatewayWithCustomAndDefaultConverters;

	@Autowired
	@Qualifier("withMappedHeaders")
	private HttpRequestHandlingMessagingGateway withMappedHeaders;

	@Autowired
	@Qualifier("withMappedHeadersAndConverter")
	private HttpRequestHandlingMessagingGateway withMappedHeadersAndConverter;

	@Autowired
	private HttpRequestHandlingController inboundController;

	@Autowired
	private HttpRequestHandlingController inboundControllerViewExp;

	@Autowired
	private SubscribableChannel requests;

	@Autowired
	private PollableChannel responses;

	@Test
	public void checkConfig() {
		assertThat(this.gateway).isNotNull();
		assertThat(TestUtils.<Boolean>getPropertyValue(this.gateway, "expectReply")).isTrue();
		assertThat(TestUtils.<Boolean>getPropertyValue(this.gateway, "convertExceptions")).isTrue();
		assertThat(TestUtils.<PollableChannel>getPropertyValue(this.gateway, "replyChannel"))
				.isSameAs(this.responses);
		assertThat(TestUtils.<Object>getPropertyValue(this.gateway, "errorChannel")).isNotNull();
		MessagingTemplate messagingTemplate =
				TestUtils.<MessagingTemplate>getPropertyValue(this.gateway, "messagingTemplate");
		assertThat(TestUtils.<Long>getPropertyValue(messagingTemplate, "sendTimeout"))
				.isEqualTo(1234L);
		assertThat(TestUtils.<Long>getPropertyValue(messagingTemplate, "receiveTimeout"))
				.isEqualTo(4567L);

		boolean registerDefaultConverters =
				TestUtils.<Boolean>getPropertyValue(this.gateway, "mergeWithDefaultConverters");
		assertThat(registerDefaultConverters).as("By default the register-default-converters flag should be false")
				.isFalse();
		@SuppressWarnings("unchecked")
		List<HttpMessageConverter<?>> messageConverters =
				TestUtils.getPropertyValue(this.gateway, "messageConverters");

		assertThat(messageConverters.size() > 0)
				.as("The default converters should have been registered, given there are no custom converters")
				.isTrue();

		assertThat(TestUtils.<Boolean>getPropertyValue(this.gateway, "autoStartup")).isFalse();
		assertThat(TestUtils.<Integer>getPropertyValue(this.gateway, "phase")).isEqualTo(1001);
	}

	@Test
	@DirtiesContext
	public void checkFlow() throws Exception {
		this.requests.subscribe(m -> {
		});
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.addHeader("Accept", "application/my-serialized");
		request.setParameter("foo", "bar");

		MockHttpServletResponse response = new MockHttpServletResponse();
		List<HttpMessageConverter<?>> converters = new ArrayList<>();
		SerializingHttpMessageConverter serializingHttpMessageConverter = new SerializingHttpMessageConverter();
		serializingHttpMessageConverter.setSupportedMediaTypes(
				Collections.singletonList(new MediaType("application", "my-serialized")));
		converters.add(serializingHttpMessageConverter);
		this.gateway.setMessageConverters(converters);
		this.gateway.afterPropertiesSet();
		this.gateway.start();

		this.gateway.handleRequest(request, response);
		assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);

		assertThat("application/my-serialized").isEqualTo(response.getContentType());
	}

	@Test
	public void testController() {
		DirectFieldAccessor accessor = new DirectFieldAccessor(inboundController);
		String errorCode = (String) accessor.getPropertyValue("errorCode");
		assertThat(errorCode).isEqualTo("oops");
		LiteralExpression viewExpression = (LiteralExpression) accessor.getPropertyValue("viewExpression");
		assertThat(viewExpression.getValue()).isEqualTo("foo");
	}

	@Test
	public void testControllerViewExp() {
		DirectFieldAccessor accessor = new DirectFieldAccessor(inboundControllerViewExp);
		String errorCode = (String) accessor.getPropertyValue("errorCode");
		assertThat(errorCode).isEqualTo("oops");
		SpelExpression viewExpression = (SpelExpression) accessor.getPropertyValue("viewExpression");
		assertThat(viewExpression).isNotNull();
		assertThat(viewExpression.getExpressionString()).isEqualTo("'bar'");
	}

	@Test
	public void requestWithHeaders() {
		DefaultHttpHeaderMapper headerMapper =
				TestUtils.<DefaultHttpHeaderMapper>getPropertyValue(this.withMappedHeaders, "headerMapper");

		HttpHeaders headers = new HttpHeaders();
		headers.set("foo", "foo");
		headers.set("bar", "bar");
		headers.set("baz", "baz");
		Map<String, Object> map = headerMapper.toHeaders(headers);
		assertThat(map.size()).isEqualTo(2);
		assertThat(map.get("foo")).isEqualTo("foo");
		assertThat(map.get("bar")).isEqualTo("bar");

		Map<String, Object> mapOfHeaders = new HashMap<String, Object>();
		mapOfHeaders.put("abc", "abc");
		MessageHeaders mh = new MessageHeaders(mapOfHeaders);
		headers = new HttpHeaders();
		headerMapper.fromHeaders(mh, headers);
		assertThat(headers.size()).isEqualTo(1);
		List<String> abc = headers.get("abc");
		assertThat(abc.get(0)).isEqualTo("abc");
	}

	@Test
	public void requestWithHeadersWithConversionService() {
		DefaultHttpHeaderMapper headerMapper =
				TestUtils.<DefaultHttpHeaderMapper>getPropertyValue(this.withMappedHeadersAndConverter, "headerMapper");

		headerMapper.setUserDefinedHeaderPrefix("X-");

		HttpHeaders headers = new HttpHeaders();
		headers.set("foo", "foo");
		headers.set("bar", "bar");
		headers.set("baz", "baz");
		Map<String, Object> map = headerMapper.toHeaders(headers);
		assertThat(map.size()).isEqualTo(2);
		assertThat(map.get("foo")).isEqualTo("foo");
		assertThat(map.get("bar")).isEqualTo("bar");

		Map<String, Object> mapOfHeaders = new HashMap<>();
		mapOfHeaders.put("abc", "abc");
		Person person = new Person();
		person.setName("Oleg");
		mapOfHeaders.put("person", person);
		MessageHeaders mh = new MessageHeaders(mapOfHeaders);
		headers = new HttpHeaders();
		headerMapper.fromHeaders(mh, headers);
		assertThat(headers.size()).isEqualTo(2);
		List<String> abc = headers.get("X-abc");
		assertThat(abc.get(0)).isEqualTo("abc");
		List<String> personHeaders = headers.get("X-person");
		assertThat(personHeaders.get(0)).isEqualTo("Oleg");
	}

	@Test
	public void testInboundGatewayWithMessageConverterDefaults() {
		List<HttpMessageConverter<?>> messageConverters =
				TestUtils.getPropertyValue(this.gatewayWithOneCustomConverter, "messageConverters");
		assertThat(messageConverters.size())
				.as("There should be only 1 message converter, by default register-default-converters is off")
				.isEqualTo(1);

		//The converter should be the customized one
		assertThat(messageConverters.get(0)).isInstanceOf(SerializingHttpMessageConverter.class);
	}

	@Test
	public void testInboundGatewayWithNoMessageConverterDefaults() {
		List<HttpMessageConverter<?>> messageConverters =
				TestUtils.getPropertyValue(this.gatewayNoDefaultConverters, "messageConverters");
		//First converter should be the customized one
		assertThat(messageConverters.get(0)).isInstanceOf(SerializingHttpMessageConverter.class);

		assertThat(messageConverters.size())
				.as("There should be only 1 message converter, the register-default-converters is false").isEqualTo(1);
	}

	@Test
	public void testInboundGatewayWithCustomAndDefaultMessageConverters() {
		List<HttpMessageConverter<?>> messageConverters =
				TestUtils.getPropertyValue(this.gatewayWithCustomAndDefaultConverters, "messageConverters");
		//First converter should be the customized one
		assertThat(messageConverters.get(0)).isInstanceOf(SerializingHttpMessageConverter.class);

		assertThat(messageConverters.size() > 1).as("There should be more than one converter").isTrue();
	}

	public static class Person {

		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

	public static class PersonConverter implements Converter<Person, String> {

		@Override
		public String convert(Person source) {
			return source.getName();
		}

	}

}
