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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.expression.Expression;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.http.AbstractHttpInboundTests;
import org.springframework.integration.http.converter.SerializingHttpMessageConverter;
import org.springframework.integration.http.inbound.HttpRequestHandlingController;
import org.springframework.integration.http.inbound.HttpRequestHandlingMessagingGateway;
import org.springframework.integration.http.support.DefaultHttpHeaderMapper;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.MultiValueMap;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.HandlerMapping;


/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Biju Kunjummen
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
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

	@Autowired @Qualifier("autoChannel.adapter")
	private HttpRequestHandlingMessagingGateway autoChannelAdapter;

	@Test
	@SuppressWarnings("unchecked")
	public void getRequestOk() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.setParameter("foo", "bar");
		MockHttpServletResponse response = new MockHttpServletResponse();
		defaultAdapter.handleRequest(request, response);
		assertEquals(HttpServletResponse.SC_SWITCHING_PROTOCOLS, response.getStatus());
		Message<?> message = requests.receive(0);
		assertNotNull(message);
		Object payload = message.getPayload();
		assertTrue(payload instanceof MultiValueMap);
		MultiValueMap<String, String> map = (MultiValueMap<String, String>) payload;
		assertEquals(1, map.size());
		assertEquals("foo", map.keySet().iterator().next());
		assertEquals(1, map.get("foo").size());
		assertEquals("bar", map.getFirst("foo"));
		assertNotNull(TestUtils.getPropertyValue(defaultAdapter, "errorChannel"));
	}

	@Test
	public void getRequestWithHeaders() throws Exception {
		DefaultHttpHeaderMapper headerMapper =
			(DefaultHttpHeaderMapper) TestUtils.getPropertyValue(withMappedHeaders, "headerMapper");

		HttpHeaders headers = new HttpHeaders();
		headers.set("foo", "foo");
		headers.set("bar", "bar");
		headers.set("baz", "baz");
		Map<String, Object> map = headerMapper.toHeaders(headers);
		assertTrue(map.size() == 2);
		assertEquals("foo", map.get("foo"));
		assertEquals("bar", map.get("bar"));
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
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		Message<?> message = requests.receive(0);
		assertNotNull(message);
		Object payload = message.getPayload();
		assertTrue(payload instanceof String);
		assertEquals("bill", payload);
		assertEquals("clinton", message.getHeaders().get("lname"));
	}

	@Test
	public void getRequestNotAllowed() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.setParameter("foo", "bar");
		request.setRequestURI("/postOnly");
		try {
			this.integrationRequestMappingHandlerMapping.getHandler(request);
		}
		catch (HttpRequestMethodNotSupportedException e) {
			assertEquals("GET", e.getMethod());
			assertArrayEquals(new String[] {"POST"}, e.getSupportedMethods());
		}

	}

	@Test
	public void postRequestWithTextContentOk() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContent("test".getBytes());
		request.setContentType("text/plain");

		MockHttpServletResponse response = new MockHttpServletResponse();
		postOnlyAdapter.handleRequest(request, response);
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		Message<?> message = requests.receive(0);
		MessageHistory history = MessageHistory.read(message);
		assertNotNull(history);
		Properties componentHistoryRecord = TestUtils.locateComponentInHistory(history, "postOnlyAdapter", 0);
		assertNotNull(componentHistoryRecord);
		assertEquals("http:inbound-channel-adapter", componentHistoryRecord.get("type"));
		assertNotNull(message);
		assertEquals("test", message.getPayload());
	}

	@Test @DirtiesContext
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
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		Message<?> message = requests.receive(0);
		assertNotNull(message);
		assertTrue(message.getPayload() instanceof TestObject);
		assertEquals("testObject", ((TestObject) message.getPayload()).text);
	}


	@Test
	public void putOrDeleteMethodsSupported() throws Exception {
		HttpMethod[] supportedMethods =
				TestUtils.getPropertyValue(putOrDeleteAdapter, "requestMapping.methods", HttpMethod[].class);
		assertEquals(2, supportedMethods.length);
		assertArrayEquals(new HttpMethod[]{HttpMethod.PUT, HttpMethod.DELETE}, supportedMethods);
	}

	@Test
	public void testController() throws Exception {
		String errorCode = TestUtils.getPropertyValue(inboundController, "errorCode", String.class);
		assertEquals("oops", errorCode);
		Expression viewExpression = TestUtils.getPropertyValue(inboundController, "viewExpression", Expression.class);
		assertEquals("foo", viewExpression.getExpressionString());

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.setParameter("foo", "bar");
		MockHttpServletResponse response = new MockHttpServletResponse();
		inboundController.handleRequest(request, response);
		assertEquals(HttpServletResponse.SC_ACCEPTED, response.getStatus());
		Message<?> message = requests.receive(0);
		assertNotNull(message);
	}

	@Test
	public void testInt2717ControllerWithViewExpression() throws Exception {
		Expression viewExpression = TestUtils.getPropertyValue(inboundControllerViewExp, "viewExpression", Expression.class);
		assertEquals("'foo'", viewExpression.getExpressionString());
	}

	@Test
	public void testAutoChannel() {
		assertSame(autoChannel, TestUtils.getPropertyValue(autoChannelAdapter, "requestChannel"));
	}

	@Test
	public void testInboundAdapterWithMessageConverterDefaults() {
		@SuppressWarnings("unchecked")
		List<HttpMessageConverter<?>> messageConverters = TestUtils.getPropertyValue(adapterWithCustomConverterWithDefaults, "messageConverters", List.class);
		assertTrue("There should be more than 1 message converter. The customized one and the defaults.", messageConverters.size() > 1);

		//First converter should be the customized one
		assertThat(messageConverters.get(0), instanceOf(SerializingHttpMessageConverter.class));
	}

	@Test
	public void testInboundAdapterWithNoMessageConverterDefaults() {
		@SuppressWarnings("unchecked")
		List<HttpMessageConverter<?>> messageConverters = TestUtils.getPropertyValue(adapterWithCustomConverterNoDefaults, "messageConverters", List.class);
		//First converter should be the customized one
		assertThat(messageConverters.get(0), instanceOf(SerializingHttpMessageConverter.class));
		assertTrue("There should be only the customized messageconverter registered.", messageConverters.size() == 1);
	}

	@Test
	public void testInboundAdapterWithNoMessageConverterNoDefaults() {
		@SuppressWarnings("unchecked")
		List<HttpMessageConverter<?>> messageConverters = TestUtils.getPropertyValue(adapterNoCustomConverterNoDefaults, "messageConverters", List.class);
		assertTrue("There should be more than 1 message converter. The defaults.", messageConverters.size() > 1);
	}

	@SuppressWarnings("serial")
	private static class TestObject implements Serializable {

		String text;

		TestObject(String text) {
			this.text = text;
		}
	}

}
