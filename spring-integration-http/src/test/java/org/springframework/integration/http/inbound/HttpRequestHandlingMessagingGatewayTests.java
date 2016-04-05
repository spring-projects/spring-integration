/*
 * Copyright 2002-2016 the original author or authors.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.http.AbstractHttpInboundTests;
import org.springframework.integration.http.HttpHeaders;
import org.springframework.integration.http.converter.SerializingHttpMessageConverter;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.SerializationUtils;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Biju Kunjummen
 * @since 2.0
 */
public class HttpRequestHandlingMessagingGatewayTests extends AbstractHttpInboundTests {

	@Test
	@SuppressWarnings("unchecked")
	public void getRequestGeneratesMapPayload() throws Exception {
		QueueChannel requestChannel = new QueueChannel();
		HttpRequestHandlingMessagingGateway gateway = new HttpRequestHandlingMessagingGateway(false);
		gateway.setBeanFactory(mock(BeanFactory.class));
		gateway.setRequestChannel(requestChannel);
		gateway.afterPropertiesSet();
		gateway.start();

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.setParameter("foo", "bar");
		MockHttpServletResponse response = new MockHttpServletResponse();
		gateway.handleRequest(request, response);
		Message<?> message = requestChannel.receive(0);
		assertNotNull(message);
		assertEquals(LinkedMultiValueMap.class, message.getPayload().getClass());
		LinkedMultiValueMap<String, String> map = (LinkedMultiValueMap<String, String>) message.getPayload();
		assertEquals(1, map.get("foo").size());
		assertEquals("bar", map.getFirst("foo"));
	}

	@Test
	public void stringExpectedWithoutReply() throws Exception {
		QueueChannel requestChannel = new QueueChannel();
		HttpRequestHandlingMessagingGateway gateway = new HttpRequestHandlingMessagingGateway(false);
		gateway.setBeanFactory(mock(BeanFactory.class));
		gateway.setRequestPayloadType(String.class);
		gateway.setRequestChannel(requestChannel);
		gateway.afterPropertiesSet();
		gateway.start();

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("text/plain");
		request.setContent("hello".getBytes());
		MockHttpServletResponse response = new MockHttpServletResponse();
		gateway.handleRequest(request, response);
		Message<?> message = requestChannel.receive(0);
		assertNotNull(message);
		assertEquals(String.class, message.getPayload().getClass());
		assertEquals("hello", message.getPayload());
	}

	@Test
	public void stringExpectedWithReply() throws Exception {
		stringExpectedWithReplyGuts(true);
	}

	@Test
	public void stringExpectedWithReplyNoContentType() throws Exception {
		stringExpectedWithReplyGuts(false);
	}

	private void stringExpectedWithReplyGuts(boolean contentType) throws Exception {
		DirectChannel requestChannel = new DirectChannel();
		requestChannel.subscribe(new AbstractReplyProducingMessageHandler() {
			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				return requestMessage.getPayload().toString().toUpperCase();
			}
		});
		HttpRequestHandlingMessagingGateway gateway = new HttpRequestHandlingMessagingGateway(true);
		gateway.setStatusCodeExpression(new LiteralExpression("foo"));
		gateway.setBeanFactory(mock(BeanFactory.class));
		gateway.setRequestPayloadType(String.class);
		gateway.setRequestChannel(requestChannel);
		gateway.afterPropertiesSet();
		gateway.start();

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.addHeader("Accept", "x-application/octet-stream");
		if (contentType) {
			request.setContentType("text/plain");
		}
		request.setContent("hello".getBytes());
		MockHttpServletResponse response = new MockHttpServletResponse();
		gateway.handleRequest(request, response);
		assertEquals("HELLO", response.getContentAsString());
	}

	@Test // INT-1767
	public void noAcceptHeaderOnRequest() throws Exception {
		DirectChannel requestChannel = new DirectChannel();
		requestChannel.subscribe(new AbstractReplyProducingMessageHandler() {
			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				return requestMessage.getPayload().toString().toUpperCase();
			}
		});
		HttpRequestHandlingMessagingGateway gateway = new HttpRequestHandlingMessagingGateway(true);
		gateway.setBeanFactory(mock(BeanFactory.class));
		gateway.setRequestPayloadType(String.class);
		gateway.setRequestChannel(requestChannel);
		gateway.afterPropertiesSet();
		gateway.start();

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("text/plain");
		request.setContent("hello".getBytes());
		MockHttpServletResponse response = new MockHttpServletResponse();
		gateway.handleRequest(request, response);
		assertEquals("HELLO", response.getContentAsString());
		//INT-3120
		assertNull(response.getHeader("Accept-Charset"));
	}

	@Test
	public void testExceptionConversion() throws Exception {
		QueueChannel requestChannel = new QueueChannel() {
			@Override
			protected boolean doSend(Message<?> message, long timeout) {
				throw new RuntimeException("Planned");
			}
		};
		HttpRequestHandlingMessagingGateway gateway = new HttpRequestHandlingMessagingGateway(true);
		gateway.setBeanFactory(mock(BeanFactory.class));
		gateway.setRequestChannel(requestChannel);
		gateway.setConvertExceptions(true);
		gateway.setMessageConverters(Arrays.<HttpMessageConverter<?>>asList(new TestHttpMessageConverter()));
		gateway.afterPropertiesSet();
		gateway.start();

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Accept", "application/x-java-serialized-object");
		request.setMethod("GET");
		MockHttpServletResponse response = new MockHttpServletResponse();
		gateway.handleRequest(request, response);
		String content = response.getContentAsString();
		assertEquals("Planned", content);
	}

	@Test
	public void multiValueParameterMap() throws Exception {
		QueueChannel channel = new QueueChannel();
		HttpRequestHandlingMessagingGateway gateway = new HttpRequestHandlingMessagingGateway(false);
		gateway.setBeanFactory(mock(BeanFactory.class));
		gateway.setRequestChannel(channel);
		gateway.afterPropertiesSet();
		gateway.start();

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
		request.setParameter("foo", "123");
		request.addParameter("bar", "456");
		request.addParameter("bar", "789");
		MockHttpServletResponse response = new MockHttpServletResponse();
		gateway.handleRequest(request, response);
		Message<?> message = channel.receive(0);
		assertNotNull(message);
		assertNotNull(message.getPayload());
		assertEquals(LinkedMultiValueMap.class, message.getPayload().getClass());
		@SuppressWarnings("unchecked")
		LinkedMultiValueMap<String, String> map = (LinkedMultiValueMap<String, String>) message.getPayload();
		List<String> fooValues = map.get("foo");
		List<String> barValues = map.get("bar");
		assertEquals(1, fooValues.size());
		assertEquals("123", fooValues.get(0));
		assertEquals(2, barValues.size());
		assertEquals("456", barValues.get(0));
		assertEquals("789", barValues.get(1));
	}

	@Test
	public void serializableRequestBody() throws Exception {
		QueueChannel channel = new QueueChannel();
		HttpRequestHandlingMessagingGateway gateway = new HttpRequestHandlingMessagingGateway(false);
		gateway.setBeanFactory(mock(BeanFactory.class));
		gateway.setRequestPayloadType(TestBean.class);
		gateway.setRequestChannel(channel);

		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
		converters.add(new SerializingHttpMessageConverter());
		gateway.setMessageConverters(converters);
		gateway.afterPropertiesSet();
		gateway.start();

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/test");

		request.setContentType("application/x-java-serialized-object");
		TestBean testBean = new TestBean();
		testBean.setName("T. Bean");
		testBean.setAge(42);
		request.setContent(SerializationUtils.serialize(testBean));
		MockHttpServletResponse response = new MockHttpServletResponse();
		gateway.handleRequest(request, response);
		byte[] bytes = response.getContentAsByteArray();
		assertNotNull(bytes);
		Message<?> message = channel.receive(0);
		assertNotNull(message);
		assertNotNull(message.getPayload());
		assertEquals(TestBean.class, message.getPayload().getClass());
		TestBean result = (TestBean) message.getPayload();
		assertEquals("T. Bean", result.name);
		assertEquals(84, result.age);
	}

	@Test
	public void INT2680DuplicateContentTypeHeader() throws Exception {

		final DirectChannel requestChannel = new DirectChannel();
		requestChannel.subscribe(new AbstractReplyProducingMessageHandler() {
			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				return MessageBuilder.withPayload("Cartman".getBytes())
						.setHeader("Content-type", "text/plain")
						.build();
			}

		});

		final List<MediaType> supportedMediaTypes = new ArrayList<MediaType>();
		supportedMediaTypes.add(MediaType.TEXT_HTML);

		final ByteArrayHttpMessageConverter messageConverter = new ByteArrayHttpMessageConverter();
		messageConverter.setSupportedMediaTypes(supportedMediaTypes);

		final List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
		messageConverters.add(messageConverter);

		final HttpRequestHandlingMessagingGateway gateway = new HttpRequestHandlingMessagingGateway(true);
		gateway.setBeanFactory(mock(BeanFactory.class));
		gateway.setMessageConverters(messageConverters);
		gateway.setRequestChannel(requestChannel);
		gateway.start();

		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");

		final ContentTypeCheckingMockHttpServletResponse response = new ContentTypeCheckingMockHttpServletResponse();
		gateway.handleRequest(request, response);

		assertEquals("Cartman", response.getContentAsString());

		/* Before fixing INT2680, 2 content type headers were being written. */
		final List<String> contentTypes = response.getContentTypeList();

		assertEquals("Exptecting only 1 content type being set.", Integer.valueOf(1), Integer.valueOf(contentTypes.size()));
		assertEquals("text/plain", contentTypes.get(0));
	}

	@Test
	public void timeoutDefault() throws Exception {
		QueueChannel requestChannel = new QueueChannel();
		HttpRequestHandlingMessagingGateway gateway = new HttpRequestHandlingMessagingGateway(true);
		gateway.setBeanFactory(mock(BeanFactory.class));
		gateway.setRequestChannel(requestChannel);
		gateway.setReplyTimeout(0);
		gateway.afterPropertiesSet();
		gateway.start();
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		MockHttpServletResponse response = new MockHttpServletResponse();
		gateway.handleRequest(request, response);
		Message<?> message = requestChannel.receive(0);
		assertNotNull(message);
		assertEquals(500, response.getStatus());
	}

	@Test
	public void timeoutStatusExpression() throws Exception {
		QueueChannel requestChannel = new QueueChannel();
		HttpRequestHandlingMessagingGateway gateway = new HttpRequestHandlingMessagingGateway(true);
		gateway.setBeanFactory(mock(BeanFactory.class));
		gateway.setRequestChannel(requestChannel);
		gateway.setReplyTimeout(0);
		gateway.setStatusCodeExpression(new LiteralExpression("501"));
		gateway.afterPropertiesSet();
		gateway.start();

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		MockHttpServletResponse response = new MockHttpServletResponse();
		gateway.handleRequest(request, response);
		Message<?> message = requestChannel.receive(0);
		assertNotNull(message);
		assertEquals(501, response.getStatus());
	}

	@Test
	public void timeoutErrorFlow() throws Exception {
		QueueChannel requestChannel = new QueueChannel();
		HttpRequestHandlingMessagingGateway gateway = new HttpRequestHandlingMessagingGateway(true);
		gateway.setBeanFactory(mock(BeanFactory.class));
		gateway.setRequestChannel(requestChannel);
		gateway.setReplyTimeout(0);
		DirectChannel errorChannel = new DirectChannel();
		errorChannel.subscribe(new AbstractReplyProducingMessageHandler() {

			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				return new GenericMessage<String>("foo",
						Collections.<String, Object>singletonMap(HttpHeaders.STATUS_CODE, HttpStatus.GATEWAY_TIMEOUT));
			}

		});
		gateway.setErrorChannel(errorChannel);
		gateway.afterPropertiesSet();
		gateway.start();

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		MockHttpServletResponse response = new MockHttpServletResponse();
		gateway.handleRequest(request, response);
		Message<?> message = requestChannel.receive(0);
		assertNotNull(message);
		assertEquals(504, response.getStatus());
	}

	@Test
	public void timeoutErrorFlowTimeout() throws Exception {
		QueueChannel requestChannel = new QueueChannel();
		HttpRequestHandlingMessagingGateway gateway = new HttpRequestHandlingMessagingGateway(true);
		gateway.setBeanFactory(mock(BeanFactory.class));
		gateway.setRequestChannel(requestChannel);
		gateway.setReplyTimeout(0);
		QueueChannel errorChannel = new QueueChannel();
		gateway.setErrorChannel(errorChannel);
		gateway.setStatusCodeExpression(new LiteralExpression("501"));
		gateway.afterPropertiesSet();
		gateway.start();

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		MockHttpServletResponse response = new MockHttpServletResponse();
		gateway.handleRequest(request, response);
		Message<?> message = requestChannel.receive(0);
		assertNotNull(message);
		assertEquals(501, response.getStatus());
		assertThat(response.getContentAsString(), Matchers.containsString("from error channel"));
	}



	private class ContentTypeCheckingMockHttpServletResponse extends MockHttpServletResponse {

		private final List<String> contentTypeList = new ArrayList<String>();

		@Override
		public void addHeader(String name, String value) {

			if ("Content-Type".equalsIgnoreCase(name)) {
				this.contentTypeList.add(value);
			}

			super.addHeader(name, value);
		}

		public List<String> getContentTypeList() {
			return contentTypeList;
		}

	}

	private static class TestHttpMessageConverter extends AbstractHttpMessageConverter<Exception> {

		TestHttpMessageConverter() {
			setSupportedMediaTypes(Arrays.asList(MediaType.ALL));
		}

		@Override
		protected Exception readInternal(Class<? extends Exception> clazz, HttpInputMessage inputMessage) throws IOException,
				HttpMessageNotReadableException {
			return null;
		}

		@Override
		protected boolean supports(Class<?> clazz) {
			return true;
		}

		@Override
		protected void writeInternal(Exception t, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
			new PrintWriter(outputMessage.getBody()).append(t.getCause().getMessage()).flush();
		}
	}



	@SuppressWarnings("serial")
	public static class TestBean implements Serializable {

		String name;

		int age;

		public void setName(String name) {
			this.name = name;
		}

		public void setAge(int age) {
			this.age = age * 2;
		}
	}

}
