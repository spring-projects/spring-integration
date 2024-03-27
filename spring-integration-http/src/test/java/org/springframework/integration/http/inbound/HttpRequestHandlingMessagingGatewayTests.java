/*
 * Copyright 2002-2024 the original author or authors.
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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
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
import org.springframework.integration.channel.NullChannel;
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
import org.springframework.web.multipart.MultipartResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Biju Kunjummen
 *
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
		assertThat(message).isNotNull();
		assertThat(message.getPayload().getClass()).isEqualTo(LinkedMultiValueMap.class);
		LinkedMultiValueMap<String, String> map = (LinkedMultiValueMap<String, String>) message.getPayload();
		assertThat(map.get("foo")).hasSize(1);
		assertThat(map.getFirst("foo")).isEqualTo("bar");
	}

	@Test
	public void stringExpectedWithoutReply() throws Exception {
		QueueChannel requestChannel = new QueueChannel();
		HttpRequestHandlingMessagingGateway gateway = new HttpRequestHandlingMessagingGateway(false);
		gateway.setBeanFactory(mock(BeanFactory.class));
		gateway.setRequestPayloadTypeClass(String.class);
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
		assertThat(message).isNotNull();
		assertThat(message.getPayload().getClass()).isEqualTo(String.class);
		assertThat(message.getPayload()).isEqualTo("hello");
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
		gateway.setRequestPayloadTypeClass(String.class);
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
		assertThat(response.getContentAsString()).isEqualTo("HELLO");
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
		gateway.setRequestPayloadTypeClass(String.class);
		gateway.setRequestChannel(requestChannel);
		gateway.afterPropertiesSet();
		gateway.start();

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("text/plain");
		request.setContent("hello".getBytes());
		MockHttpServletResponse response = new MockHttpServletResponse();
		gateway.handleRequest(request, response);
		assertThat(response.getContentAsString()).isEqualTo("HELLO");
		//INT-3120
		assertThat(response.getHeader("Accept-Charset")).isNull();
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
		gateway.setMessageConverters(Collections.singletonList(new TestHttpMessageConverter()));
		gateway.afterPropertiesSet();
		gateway.start();

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Accept", "application/x-java-serialized-object");
		request.setMethod("GET");
		MockHttpServletResponse response = new MockHttpServletResponse();
		gateway.handleRequest(request, response);
		String content = response.getContentAsString();
		assertThat(content).isEqualTo("Planned");
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
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isNotNull();
		assertThat(message.getPayload().getClass()).isEqualTo(LinkedMultiValueMap.class);
		@SuppressWarnings("unchecked")
		LinkedMultiValueMap<String, String> map = (LinkedMultiValueMap<String, String>) message.getPayload();
		List<String> fooValues = map.get("foo");
		List<String> barValues = map.get("bar");
		assertThat(fooValues).containsExactly("123");
		assertThat(barValues).containsExactly("456", "789");
	}

	@Test
	public void serializableRequestBody() throws Exception {
		QueueChannel channel = new QueueChannel();
		HttpRequestHandlingMessagingGateway gateway = new HttpRequestHandlingMessagingGateway(false);
		gateway.setBeanFactory(mock(BeanFactory.class));
		gateway.setRequestPayloadTypeClass(TestBean.class);
		gateway.setRequestChannel(channel);

		List<HttpMessageConverter<?>> converters = new ArrayList<>();
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
		assertThat(bytes).isNotNull();
		Message<?> message = channel.receive(0);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isNotNull();
		assertThat(message.getPayload().getClass()).isEqualTo(TestBean.class);
		TestBean result = (TestBean) message.getPayload();
		assertThat(result.name).isEqualTo("T. Bean");
		assertThat(result.age).isEqualTo(42);
	}

	@Test
	public void testJsonRequestBody() throws Exception {
		QueueChannel channel = new QueueChannel();
		HttpRequestHandlingMessagingGateway gateway = new HttpRequestHandlingMessagingGateway(false);
		gateway.setBeanFactory(mock(BeanFactory.class));
		ParameterizedTypeReference<List<TestBean>> parameterizedTypeReference =
				new ParameterizedTypeReference<>() {

				};
		gateway.setRequestPayloadType(ResolvableType.forType(parameterizedTypeReference));
		gateway.setRequestChannel(channel);
		gateway.afterPropertiesSet();
		gateway.start();

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/test");

		request.setContentType("application/json");
		TestBean testBean = new TestBean();
		testBean.setName("T. Bean");
		testBean.setAge(42);
		request.setContent(new ObjectMapper().writeValueAsBytes(new TestBean[] {testBean}));
		MockHttpServletResponse response = new MockHttpServletResponse();
		gateway.handleRequest(request, response);
		byte[] bytes = response.getContentAsByteArray();
		assertThat(bytes).isNotNull();
		Message<?> message = channel.receive(0);

		assertThat(message).isNotNull()
				.extracting(Message::getPayload)
				.asInstanceOf(InstanceOfAssertFactories.LIST)
				.hasSize(1)
				.element(0)
				.isInstanceOf(TestBean.class)
				.satisfies((actual) -> {
					TestBean bean = (TestBean) actual;
					assertThat(bean).extracting(TestBean::getName).isEqualTo("T. Bean");
					assertThat(bean).extracting(TestBean::getAge).isEqualTo(42);
				});
	}

	@Test
	public void INT2680DuplicateContentTypeHeader() throws Exception {
		DirectChannel requestChannel = new DirectChannel();
		requestChannel.subscribe(new AbstractReplyProducingMessageHandler() {

			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				return MessageBuilder.withPayload("Cartman".getBytes())
						.setHeader("Content-type", "text/plain")
						.build();
			}

		});

		final List<MediaType> supportedMediaTypes = new ArrayList<>();
		supportedMediaTypes.add(MediaType.TEXT_HTML);

		final ByteArrayHttpMessageConverter messageConverter = new ByteArrayHttpMessageConverter();
		messageConverter.setSupportedMediaTypes(supportedMediaTypes);

		final List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
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

		assertThat(response.getContentAsString()).isEqualTo("Cartman");

		/* Before fixing INT2680, 2 content type headers were being written. */
		final List<String> contentTypes = response.getContentTypeList();

		assertThat(Integer.valueOf(contentTypes.size())).as("Expecting only 1 content type being set.")
				.isEqualTo(Integer.valueOf(1));
		assertThat(contentTypes.get(0)).isEqualTo("text/plain");
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
		assertThat(message).isNotNull();
		assertThat(response.getStatus()).isEqualTo(500);
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
		assertThat(message).isNotNull();
		assertThat(response.getStatus()).isEqualTo(501);
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
				return new GenericMessage<>("foo",
						Collections.singletonMap(HttpHeaders.STATUS_CODE, HttpStatus.GATEWAY_TIMEOUT));
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
		assertThat(message).isNotNull();
		assertThat(response.getStatus()).isEqualTo(504);
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
		assertThat(message).isNotNull();
		assertThat(response.getStatus()).isEqualTo(501);
		assertThat(response.getContentAsString()).contains("from error channel");
	}

	@Test
	public void testMultipart() throws Exception {
		HttpRequestHandlingMessagingGateway gateway = new HttpRequestHandlingMessagingGateway(false);
		gateway.setBeanFactory(mock(BeanFactory.class));
		MultipartResolver multipartResolver = mock(MultipartResolver.class);
		gateway.setMultipartResolver(multipartResolver);
		gateway.setRequestChannel(new NullChannel());
		gateway.setRequestPayloadTypeClass(String.class);
		gateway.afterPropertiesSet();
		gateway.start();

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("multipart/form-data");
		request.setContent("hello".getBytes());
		MockHttpServletResponse response = new MockHttpServletResponse();
		gateway.handleRequest(request, response);

		verify(multipartResolver).isMultipart(any(HttpServletRequest.class));
	}

	@Test
	public void deleteRequestBodyIgnored() throws Exception {
		QueueChannel channel = new QueueChannel();
		HttpRequestHandlingMessagingGateway gateway = new HttpRequestHandlingMessagingGateway(false);
		gateway.setBeanFactory(mock(BeanFactory.class));
		gateway.setRequestChannel(channel);
		gateway.afterPropertiesSet();
		gateway.start();

		MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/delete");
		request.setContent("This content is ignored for DELETE".getBytes());
		request.setParameter("one", "1");
		request.addParameter("two", "2");
		MockHttpServletResponse response = new MockHttpServletResponse();
		gateway.handleRequest(request, response);
		Message<?> message = channel.receive(0);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isNotNull();
		assertThat(message.getPayload().getClass()).isEqualTo(LinkedMultiValueMap.class);
		@SuppressWarnings("unchecked")
		LinkedMultiValueMap<String, String> map = (LinkedMultiValueMap<String, String>) message.getPayload();
		List<String> oneValues = map.get("one");
		List<String> twoValues = map.get("two");
		assertThat(oneValues).containsExactly("1");
		assertThat(twoValues).containsExactly("2");
	}

	private static class ContentTypeCheckingMockHttpServletResponse extends MockHttpServletResponse {

		private final List<String> contentTypeList = new ArrayList<>();

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
		protected Exception readInternal(Class<? extends Exception> clazz, HttpInputMessage inputMessage)
				throws HttpMessageNotReadableException {

			return null;
		}

		@Override
		protected boolean supports(Class<?> clazz) {
			return true;
		}

		@Override
		protected void writeInternal(Exception t, HttpOutputMessage outputMessage)
				throws IOException, HttpMessageNotWritableException {
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
			this.age = age;
		}

		public String getName() {
			return name;
		}

		public int getAge() {
			return age;
		}

	}

}
