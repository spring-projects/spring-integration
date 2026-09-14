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

package org.springframework.integration.http.outbound;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javax.xml.transform.Source;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.http.converter.SerializingHttpMessageConverter;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.support.TestApplicationContextAware;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.DefaultUriBuilderFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gary Russell
 * @author Gunnar Hillert
 * @author Florian Schöffl
 * @author Glenn Renfro
 * @author Arun Sethumadhavan
 * @author Burak Kalayci
 */
public class HttpRequestExecutingMessageHandlerTests implements TestApplicationContextAware {

	// Used in the HttpOutboundWithinChainTests-context.xml
	public static ParameterizedTypeReference<List<String>> testParameterizedTypeReference() {
		return new ParameterizedTypeReference<>() {

		};

	}

	@BeforeEach
	void setUp() {
		TEST_INTEGRATION_CONTEXT.registerBean("integrationSimpleEvaluationContext", SimpleEvaluationContext
				.forReadOnlyDataBinding()
				.build());
	}

	@Test
	public void simpleStringKeyStringValueFormData() {
		CapturingRestClient capturing = new CapturingRestClient();
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"https://www.springsource.org/spring-integration", capturing.client());
		handler.setHttpMethod(HttpMethod.POST);
		setBeanFactory(handler);
		handler.afterPropertiesSet();
		Map<String, String> form = new LinkedHashMap<>();
		form.put("a", "1");
		form.put("b", "2");
		form.put("c", "3");
		Message<?> message =
				MessageBuilder.withPayload(form)
						.setHeader(MessageHeaders.CONTENT_TYPE,
								MediaType.APPLICATION_FORM_URLENCODED_VALUE + ";charset=UTF-8")
						.build();
		QueueChannel replyChannel = new QueueChannel();
		handler.setOutputChannel(replyChannel);

		assertThatException()
				.isThrownBy(() -> handler.handleMessage(message))
				.withStackTraceContaining("intentional");

		HttpEntity<?> request = capturing.lastRequestEntity();
		Object body = request.getBody();
		assertThat(request.getHeaders().getContentType()).isNotNull();
		assertThat(body).isInstanceOf(MultiValueMap.class);
		MultiValueMap<?, ?> map = (MultiValueMap<?, ?>) body;
		assertThat(map.get("a").iterator().next()).isEqualTo("1");
		assertThat(map.get("b").iterator().next()).isEqualTo("2");
		assertThat(map.get("c").iterator().next()).isEqualTo("3");
		assertThat(request.getHeaders().getContentType()).isNotEqualTo(MediaType.APPLICATION_FORM_URLENCODED);
		assertThat(request.getHeaders().getContentType().equalsTypeAndSubtype(MediaType.APPLICATION_FORM_URLENCODED))
				.isTrue();
	}

	@Test
	public void simpleStringKeyObjectValueFormData() {
		CapturingRestClient capturing = new CapturingRestClient();
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"https://www.springsource.org/spring-integration", capturing.client());
		handler.setHttpMethod(HttpMethod.POST);
		setBeanFactory(handler);
		handler.afterPropertiesSet();
		Map<String, Object> form = new LinkedHashMap<>();
		form.put("a", new City("Philadelphia"));
		form.put("b", new City("Ambler"));
		form.put("c", new City("Mohnton"));
		Message<?> message = MessageBuilder.withPayload(form).build();
		QueueChannel replyChannel = new QueueChannel();
		handler.setOutputChannel(replyChannel);

		assertThatException()
				.isThrownBy(() -> handler.handleMessage(message))
				.withStackTraceContaining("intentional");

		HttpEntity<?> request = capturing.lastRequestEntity();
		Object body = request.getBody();
		assertThat(body instanceof MultiValueMap<?, ?>).isTrue();
		MultiValueMap<?, ?> map = (MultiValueMap<?, ?>) body;
		assertThat(map.get("a").get(0).toString()).isEqualTo("Philadelphia");
		assertThat(map.get("b").get(0).toString()).isEqualTo("Ambler");
		assertThat(map.get("c").get(0).toString()).isEqualTo("Mohnton");
		assertThat(request.getHeaders().getContentType()).isEqualTo(MediaType.MULTIPART_FORM_DATA);
	}

	@Test
	public void simpleObjectKeyObjectValueFormData() {
		CapturingRestClient capturing = new CapturingRestClient();
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"https://www.springsource.org/spring-integration", capturing.client());
		handler.setHttpMethod(HttpMethod.POST);
		setBeanFactory(handler);
		handler.afterPropertiesSet();
		Map<Object, Object> form = new LinkedHashMap<>();
		form.put(1, new City("Philadelphia"));
		form.put(2, new City("Ambler"));
		form.put(3, new City("Mohnton"));
		Message<?> message = MessageBuilder.withPayload(form).build();
		QueueChannel replyChannel = new QueueChannel();
		handler.setOutputChannel(replyChannel);

		assertThatException()
				.isThrownBy(() -> handler.handleMessage(message))
				.withStackTraceContaining("intentional");

		HttpEntity<?> request = capturing.lastRequestEntity();
		Object body = request.getBody();
		assertThat(body instanceof Map<?, ?>).isTrue();
		Map<?, ?> map = (Map<?, ?>) body;
		assertThat(map.get(1).toString()).isEqualTo("Philadelphia");
		assertThat(map.get(2).toString()).isEqualTo("Ambler");
		assertThat(map.get(3).toString()).isEqualTo("Mohnton");
		assertThat(request.getHeaders().getContentType()).isNull();
	}

	@Test
	public void stringKeyStringArrayValueFormData() {
		CapturingRestClient capturing = new CapturingRestClient();
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"https://www.springsource.org/spring-integration", capturing.client());
		handler.setHttpMethod(HttpMethod.POST);
		setBeanFactory(handler);
		handler.afterPropertiesSet();
		Map<String, Object> form = new LinkedHashMap<>();
		form.put("a", new String[] {"1", "2", "3"});
		form.put("b", "4");
		form.put("c", new String[] {"5"});
		form.put("d", "6");
		Message<?> message = MessageBuilder.withPayload(form).build();

		assertThatException()
				.isThrownBy(() -> handler.handleMessage(message))
				.withStackTraceContaining("intentional");

		HttpEntity<?> request = capturing.lastRequestEntity();
		Object body = request.getBody();
		assertThat(body instanceof MultiValueMap<?, ?>).isTrue();
		MultiValueMap<?, ?> map = (MultiValueMap<?, ?>) body;

		List<?> aValue = map.get("a");
		assertThat(aValue.size()).isEqualTo(3);
		assertThat(aValue.get(0)).isEqualTo("1");
		assertThat(aValue.get(1)).isEqualTo("2");
		assertThat(aValue.get(2)).isEqualTo("3");

		List<?> bValue = map.get("b");
		assertThat(bValue.size()).isEqualTo(1);
		assertThat(bValue.get(0)).isEqualTo("4");

		List<?> cValue = map.get("c");
		assertThat(cValue.size()).isEqualTo(1);
		assertThat(cValue.get(0)).isEqualTo("5");

		List<?> dValue = map.get("d");
		assertThat(dValue.size()).isEqualTo(1);
		assertThat(dValue.get(0)).isEqualTo("6");
		assertThat(request.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_FORM_URLENCODED);
	}

	@Test
	public void stringKeyPrimitiveArrayValueMixedFormData() {
		CapturingRestClient capturing = new CapturingRestClient();
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"https://www.springsource.org/spring-integration", capturing.client());
		handler.setHttpMethod(HttpMethod.POST);
		setBeanFactory(handler);
		handler.afterPropertiesSet();
		Map<String, Object> form = new LinkedHashMap<>();
		form.put("a", new int[] {1, 2, 3});
		form.put("b", "4");
		form.put("c", new String[] {"5"});
		form.put("d", "6");
		Message<?> message = MessageBuilder.withPayload(form).build();

		assertThatException()
				.isThrownBy(() -> handler.handleMessage(message))
				.withStackTraceContaining("intentional");

		HttpEntity<?> request = capturing.lastRequestEntity();
		Object body = request.getBody();
		assertThat(body instanceof MultiValueMap<?, ?>).isTrue();
		MultiValueMap<?, ?> map = (MultiValueMap<?, ?>) body;

		List<?> aValue = map.get("a");
		assertThat(aValue.size()).isEqualTo(1);
		Object value = aValue.get(0);
		assertThat(value.getClass().isArray()).isTrue();
		int[] y = (int[]) value;
		assertThat(y[0]).isEqualTo(1);
		assertThat(y[1]).isEqualTo(2);
		assertThat(y[2]).isEqualTo(3);

		List<?> bValue = map.get("b");
		assertThat(bValue.size()).isEqualTo(1);
		assertThat(bValue.get(0)).isEqualTo("4");

		List<?> cValue = map.get("c");
		assertThat(cValue.size()).isEqualTo(1);
		assertThat(cValue.get(0)).isEqualTo("5");

		List<?> dValue = map.get("d");
		assertThat(dValue.size()).isEqualTo(1);
		assertThat(dValue.get(0)).isEqualTo("6");
		assertThat(request.getHeaders().getContentType()).isEqualTo(MediaType.MULTIPART_FORM_DATA);
	}

	@Test
	public void stringKeyNullArrayValueMixedFormData() {
		CapturingRestClient capturing = new CapturingRestClient();
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"https://www.springsource.org/spring-integration", capturing.client());
		handler.setHttpMethod(HttpMethod.POST);
		setBeanFactory(handler);
		handler.afterPropertiesSet();
		Map<String, Object> form = new LinkedHashMap<>();
		form.put("a", new Object[] {null, 4, null});
		form.put("b", "4");
		Message<?> message = MessageBuilder.withPayload(form).build();

		assertThatException()
				.isThrownBy(() -> handler.handleMessage(message))
				.withStackTraceContaining("intentional");

		HttpEntity<?> request = capturing.lastRequestEntity();
		Object body = request.getBody();
		assertThat(body instanceof MultiValueMap<?, ?>).isTrue();
		MultiValueMap<?, ?> map = (MultiValueMap<?, ?>) body;

		List<?> aValue = map.get("a");
		assertThat(aValue.size()).isEqualTo(3);
		assertThat(aValue.get(0)).isNull();
		assertThat(aValue.get(1)).isEqualTo(4);
		assertThat(aValue.get(2)).isNull();

		List<?> bValue = map.get("b");
		assertThat(bValue.size()).isEqualTo(1);
		assertThat(bValue.get(0)).isEqualTo("4");

		assertThat(request.getHeaders().getContentType()).isEqualTo(MediaType.MULTIPART_FORM_DATA);
	}

	/**
	 * This test and the one below might look identical, but they are not. This test
	 * injected "5" into the list as a String, resulting in the Content-TYpe being
	 * application/x-www-form-urlencoded
	 */
	@Test
	public void stringKeyNullCollectionValueMixedFormDataString() {
		CapturingRestClient capturing = new CapturingRestClient();
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"https://www.springsource.org/spring-integration", capturing.client());
		handler.setHttpMethod(HttpMethod.POST);
		setBeanFactory(handler);
		handler.afterPropertiesSet();
		Map<String, Object> form = new LinkedHashMap<>();
		List<Object> list = new ArrayList<>();
		list.add(null);
		list.add("5");
		list.add(null);
		form.put("a", list);
		form.put("b", "4");
		Message<?> message = MessageBuilder.withPayload(form).build();

		assertThatException()
				.isThrownBy(() -> handler.handleMessage(message))
				.withStackTraceContaining("intentional");

		HttpEntity<?> request = capturing.lastRequestEntity();
		Object body = request.getBody();
		assertThat(body instanceof MultiValueMap<?, ?>).isTrue();
		MultiValueMap<?, ?> map = (MultiValueMap<?, ?>) body;

		List<?> aValue = map.get("a");
		assertThat(aValue.size()).isEqualTo(3);
		assertThat(aValue.get(0)).isNull();
		assertThat(aValue.get(1)).isEqualTo("5");
		assertThat(aValue.get(2)).isNull();

		List<?> bValue = map.get("b");
		assertThat(bValue.size()).isEqualTo(1);
		assertThat(bValue.get(0)).isEqualTo("4");

		assertThat(request.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_FORM_URLENCODED);
	}

	/**
	 * This test and the one above might look identical, but they are not. This test
	 * injected 5 into the list as int resulting in Content-type being multipart/form-data
	 */
	@Test
	public void stringKeyNullCollectionValueMixedFormDataObject() {
		CapturingRestClient capturing = new CapturingRestClient();
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"https://www.springsource.org/spring-integration", capturing.client());
		handler.setHttpMethod(HttpMethod.POST);
		setBeanFactory(handler);
		handler.afterPropertiesSet();
		Map<String, Object> form = new LinkedHashMap<>();
		List<Object> list = new ArrayList<>();
		list.add(null);
		list.add(5);
		list.add(null);
		form.put("a", list);
		form.put("b", "4");
		Message<?> message = MessageBuilder.withPayload(form).build();

		assertThatException()
				.isThrownBy(() -> handler.handleMessage(message))
				.withStackTraceContaining("intentional");

		HttpEntity<?> request = capturing.lastRequestEntity();
		Object body = request.getBody();
		assertThat(body instanceof MultiValueMap<?, ?>).isTrue();
		MultiValueMap<?, ?> map = (MultiValueMap<?, ?>) body;

		List<?> aValue = map.get("a");
		assertThat(aValue.size()).isEqualTo(3);
		assertThat(aValue.get(0)).isNull();
		assertThat(aValue.get(1)).isEqualTo(5);
		assertThat(aValue.get(2)).isNull();

		List<?> bValue = map.get("b");
		assertThat(bValue.size()).isEqualTo(1);
		assertThat(bValue.get(0)).isEqualTo("4");

		assertThat(request.getHeaders().getContentType()).isEqualTo(MediaType.MULTIPART_FORM_DATA);
	}

	@Test
	public void stringKeyStringCollectionValueFormData() {
		CapturingRestClient capturing = new CapturingRestClient();
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"https://www.springsource.org/spring-integration", capturing.client());
		handler.setHttpMethod(HttpMethod.POST);
		setBeanFactory(handler);
		handler.afterPropertiesSet();
		Map<String, Object> form = new LinkedHashMap<>();
		List<String> listA = new ArrayList<>();
		listA.add("1");
		listA.add("2");
		form.put("a", listA);
		form.put("b", Collections.EMPTY_LIST);
		form.put("c", Collections.singletonList("3"));
		Message<?> message = MessageBuilder.withPayload(form).build();

		assertThatException()
				.isThrownBy(() -> handler.handleMessage(message))
				.withStackTraceContaining("intentional");

		HttpEntity<?> request = capturing.lastRequestEntity();
		Object body = request.getBody();
		assertThat(body instanceof MultiValueMap<?, ?>).isTrue();
		MultiValueMap<?, ?> map = (MultiValueMap<?, ?>) body;

		List<?> aValue = map.get("a");
		assertThat(aValue.size()).isEqualTo(2);
		assertThat(aValue.get(0)).isEqualTo("1");
		assertThat(aValue.get(1)).isEqualTo("2");

		List<?> bValue = map.get("b");
		assertThat(bValue).isEmpty();

		List<?> cValue = map.get("c");
		assertThat(cValue.size()).isEqualTo(1);
		assertThat(cValue.get(0)).isEqualTo("3");

		assertThat(request.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_FORM_URLENCODED);
	}

	@Test
	public void stringKeyObjectCollectionValueFormData() {
		CapturingRestClient capturing = new CapturingRestClient();
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"https://www.springsource.org/spring-integration", capturing.client());
		handler.setHttpMethod(HttpMethod.POST);
		setBeanFactory(handler);
		handler.afterPropertiesSet();
		Map<String, Object> form = new LinkedHashMap<>();
		List<Object> listA = new ArrayList<>();
		listA.add(new City("Philadelphia"));
		listA.add(new City("Ambler"));
		form.put("a", listA);
		form.put("b", Collections.EMPTY_LIST);
		form.put("c", Collections.singletonList(new City("Mohnton")));
		Message<?> message = MessageBuilder.withPayload(form).build();

		assertThatException()
				.isThrownBy(() -> handler.handleMessage(message))
				.withStackTraceContaining("intentional");

		HttpEntity<?> request = capturing.lastRequestEntity();
		Object body = request.getBody();
		assertThat(body instanceof MultiValueMap<?, ?>).isTrue();
		MultiValueMap<?, ?> map = (MultiValueMap<?, ?>) body;

		List<?> aValue = map.get("a");
		assertThat(aValue.size()).isEqualTo(2);
		assertThat(aValue.get(0).toString()).isEqualTo("Philadelphia");
		assertThat(aValue.get(1).toString()).isEqualTo("Ambler");

		List<?> bValue = map.get("b");
		assertThat(bValue).isEmpty();

		List<?> cValue = map.get("c");
		assertThat(cValue.size()).isEqualTo(1);
		assertThat(cValue.get(0).toString()).isEqualTo("Mohnton");

		assertThat(request.getHeaders().getContentType()).isEqualTo(MediaType.MULTIPART_FORM_DATA);
	}

	@Test
	public void nameOnlyWithNullValues() {
		CapturingRestClient capturing = new CapturingRestClient();
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"https://www.springsource.org/spring-integration", capturing.client());
		handler.setHttpMethod(HttpMethod.POST);
		setBeanFactory(handler);
		handler.afterPropertiesSet();
		Map<String, Object> form = new LinkedHashMap<>();
		form.put("a", null);
		form.put("b", "foo");
		form.put("c", null);
		Message<?> message = MessageBuilder.withPayload(form).build();

		assertThatException()
				.isThrownBy(() -> handler.handleMessage(message))
				.withStackTraceContaining("intentional");

		HttpEntity<?> request = capturing.lastRequestEntity();
		Object body = request.getBody();
		assertThat(body instanceof MultiValueMap<?, ?>).isTrue();
		MultiValueMap<?, ?> map = (MultiValueMap<?, ?>) body;
		assertThat(map.containsKey("a")).isTrue();
		assertThat(map.get("a").size() == 1).isTrue();
		assertThat(map.get("a").get(0)).isNull();
		List<?> entryB = map.get("b");
		assertThat(entryB.get(0)).isEqualTo("foo");
		assertThat(map.containsKey("c")).isTrue();
		assertThat(map.get("c").size() == 1).isTrue();
		assertThat(map.get("c").get(0)).isNull();
		assertThat(request.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_FORM_URLENCODED);
	}

	@Test
	public void contentAsByteArray() {
		CapturingRestClient capturing = new CapturingRestClient();
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"https://www.springsource.org/spring-integration", capturing.client());
		handler.setHttpMethod(HttpMethod.POST);
		setBeanFactory(handler);
		handler.afterPropertiesSet();

		byte[] bytes = "Hello World".getBytes();
		Message<?> message = MessageBuilder.withPayload(bytes).build();

		assertThatException()
				.isThrownBy(() -> handler.handleMessage(message))
				.withStackTraceContaining("intentional");

		HttpEntity<?> request = capturing.lastRequestEntity();
		Object body = request.getBody();
		assertThat(body).asInstanceOf(InstanceOfAssertFactories.BYTE_ARRAY).isEqualTo(bytes);
		assertThat(request.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
	}

	@Test
	public void contentAsXmlSource() {
		CapturingRestClient capturing = new CapturingRestClient();
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"https://www.springsource.org/spring-integration", capturing.client());
		handler.setHttpMethod(HttpMethod.POST);
		setBeanFactory(handler);
		handler.afterPropertiesSet();

		Message<?> message = MessageBuilder.withPayload(mock(Source.class)).build();

		assertThatException()
				.isThrownBy(() -> handler.handleMessage(message))
				.withStackTraceContaining("intentional");

		HttpEntity<?> request = capturing.lastRequestEntity();
		Object body = request.getBody();
		assertThat(body).isInstanceOf(Source.class);
		assertThat(request.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_XML);
	}

	@Test // no assertions just a warn message in a log
	public void testWarnMessageForNonPostPutAndExtractPayload() {
		// should see a warn message

		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"https://www.springsource.org/spring-integration");
		handler.setHttpMethod(HttpMethod.GET);
		handler.setExtractPayload(true);
		setBeanFactory(handler);
		handler.afterPropertiesSet();

		// should not see a warn message since 'setExtractPayload' is not set explicitly

		handler = new HttpRequestExecutingMessageHandler("https://www.springsource.org/spring-integration");
		handler.setHttpMethod(HttpMethod.GET);
		setBeanFactory(handler);
		handler.afterPropertiesSet();

		// should not see a warn message since HTTP method is not GET

		handler = new HttpRequestExecutingMessageHandler("https://www.springsource.org/spring-integration");
		handler.setHttpMethod(HttpMethod.POST);
		handler.setExtractPayload(true);
		setBeanFactory(handler);
		handler.afterPropertiesSet();
	}

	@Test
	public void contentTypeIsNotSetForGetAndHeadRequest() {
		// GET
		CapturingRestClient capturing = new CapturingRestClient();
		HttpRequestExecutingMessageHandler handler =
				new HttpRequestExecutingMessageHandler("https://www.springsource.org/spring-integration",
						capturing.client());
		handler.setHttpMethod(HttpMethod.GET);
		setBeanFactory(handler);
		handler.afterPropertiesSet();

		assertThatException()
				.isThrownBy(() -> handler.handleMessage(MessageBuilder.withPayload(mock(Source.class)).build()))
				.withStackTraceContaining("intentional");

		assertThat(capturing.lastRequestEntity().getHeaders().getContentType()).isNull();

		//HEAD
		handler.setHttpMethod(HttpMethod.HEAD);

		assertThatException()
				.isThrownBy(() -> handler.handleMessage(MessageBuilder.withPayload(mock(Source.class)).build()))
				.withStackTraceContaining("intentional");

		assertThat(capturing.lastRequestEntity().getHeaders().getContentType()).isNull();

		//DELETE
		handler.setHttpMethod(HttpMethod.DELETE);

		assertThatException()
				.isThrownBy(() -> handler.handleMessage(MessageBuilder.withPayload(mock(Source.class)).build()))
				.withStackTraceContaining("intentional");

		assertThat(capturing.lastRequestEntity().getHeaders().getContentType()).isEqualTo(MediaType.TEXT_XML);

		//TRACE
		handler.setHttpMethod(HttpMethod.TRACE);

		assertThatException()
				.isThrownBy(() -> handler.handleMessage(MessageBuilder.withPayload(mock(Source.class)).build()))
				.withStackTraceContaining("intentional");

		assertThat(capturing.lastRequestEntity().getHeaders().getContentType()).isNull();
	}

	@Test
	public void exchangeWithRestClient() throws IOException {
		ClientHttpRequestFactory requestFactory = mock(ClientHttpRequestFactory.class);
		ClientHttpRequest clientRequest = mock(ClientHttpRequest.class);
		when(clientRequest.getHeaders()).thenReturn(new HttpHeaders());
		when(clientRequest.getBody()).thenReturn(new ByteArrayOutputStream());

		ClientHttpResponse response = mock(ClientHttpResponse.class);
		when(response.getStatusCode()).thenReturn(HttpStatus.OK);
		when(response.getStatusText()).thenReturn("OK");
		when(response.getBody()).thenReturn(new ByteArrayInputStream("testReply".getBytes(StandardCharsets.UTF_8)));
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.TEXT_PLAIN);
		when(response.getHeaders()).thenReturn(responseHeaders);

		when(clientRequest.execute()).thenReturn(response);
		when(requestFactory.createRequest(any(URI.class), any(HttpMethod.class))).thenReturn(clientRequest);

		RestClient restClient = RestClient.builder()
				.requestFactory(requestFactory)
				.build();

		HttpRequestExecutingMessageHandler handler =
				new HttpRequestExecutingMessageHandler("https://www.springsource.org/spring-integration", restClient);
		handler.setHttpMethod(HttpMethod.GET);
		handler.setExpectedResponseType(String.class);
		setBeanFactory(handler);
		handler.afterPropertiesSet();

		QueueChannel outputChannel = new QueueChannel();
		handler.setOutputChannel(outputChannel);
		handler.handleMessage(new GenericMessage<>("request"));

		Message<?> receive = outputChannel.receive(10_000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("testReply");
		assertThat(receive.getHeaders())
				.containsEntry(org.springframework.integration.http.HttpHeaders.STATUS_CODE, HttpStatus.OK);
	}

	@Test
	public void failWhenSetRequestFactoryWithExternalRestClient() {
		HttpRequestExecutingMessageHandler handler =
				new HttpRequestExecutingMessageHandler("https://www.springsource.org/spring-integration",
						RestClient.create());

		assertThatIllegalArgumentException()
				.isThrownBy(() -> handler.setRequestFactory(mock()))
				.withMessageContaining("externally configured RestClient");
	}

	@Test
	public void defaultStatusHandlerAppliesOnlyToMatchingPredicate() throws IOException {
		MockClientHttpRequest clientRequest = new MockClientHttpRequest();
		clientRequest.setResponse(new MockClientHttpResponse(new byte[0], HttpStatus.NOT_FOUND));
		ClientHttpRequestFactory requestFactory = (uri, httpMethod) -> clientRequest;

		AtomicReference<HttpStatusCode> handledStatus = new AtomicReference<>();

		HttpRequestExecutingMessageHandler handler =
				new HttpRequestExecutingMessageHandler("https://www.springsource.org/spring-integration");
		handler.setRequestFactory(requestFactory);
		handler.setHttpMethod(HttpMethod.GET);
		handler.setExpectedResponseType(String.class);
		handler.defaultStatusHandler(HttpStatusCode::is4xxClientError,
				(request, response) -> handledStatus.set(response.getStatusCode()));
		setBeanFactory(handler);
		handler.afterPropertiesSet();
		handler.setOutputChannel(new QueueChannel());

		handler.handleMessage(new GenericMessage<>("request"));

		assertThat(handledStatus.get())
				.isEqualTo(HttpStatus.NOT_FOUND);

		handledStatus.set(null);

		clientRequest.setResponse(new MockClientHttpResponse(new byte[0], HttpStatus.INTERNAL_SERVER_ERROR));

		assertThatExceptionOfType(MessageHandlingException.class)
				.isThrownBy(() -> handler.handleMessage(new GenericMessage<>("request")))
				.withCauseInstanceOf(RestClientException.class);

		assertThat(handledStatus.get())
				.isNull();
	}

	@Test
	public void testOutboundChannelAdapterWithinChain() {
		ConfigurableApplicationContext ctx = new ClassPathXmlApplicationContext(
				"HttpOutboundWithinChainTests-context.xml", this.getClass());
		MessageChannel channel = ctx.getBean("httpOutboundChannelAdapterWithinChain", MessageChannel.class);
		CapturingRequestFactory requestFactory = ctx.getBean("requestFactory", CapturingRequestFactory.class);
		channel.send(MessageBuilder.withPayload("test").build());

		assertThat(requestFactory.actualUrl.get()).hasToString("http://localhost/test1/%2f");

		HttpRequestExecutingMessageHandler handler = ctx.getBean("chain$child.adapter.handler",
				HttpRequestExecutingMessageHandler.class);

		assertThat(TestUtils.<Boolean>getPropertyValue(handler, "trustedSpel")).isEqualTo(Boolean.TRUE);
		ctx.close();
	}

	@Test
	public void testHttpOutboundGatewayWithinChain() {
		ConfigurableApplicationContext ctx = new ClassPathXmlApplicationContext(
				"HttpOutboundWithinChainTests-context.xml", this.getClass());
		MessageChannel channel = ctx.getBean("httpOutboundGatewayWithinChain", MessageChannel.class);
		CapturingRequestFactory requestFactory = ctx.getBean("requestFactory", CapturingRequestFactory.class);
		channel.send(MessageBuilder.withPayload("test").build());

		PollableChannel output = ctx.getBean("replyChannel", PollableChannel.class);
		Message<?> receive = output.receive();
		assertThat(((ResponseEntity<?>) receive.getPayload()).getStatusCode()).isEqualTo(HttpStatus.OK);

		assertThat(requestFactory.actualUrl.get())
				.hasToString("http://localhost:51235/%2f/testApps?param=http+Outbound+Gateway+Within+Chain");

		ctx.close();
	}

	@Test
	public void testUriExpression() {
		AtomicReference<URI> actualUri = new AtomicReference<>();
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				new SpelExpressionParser().parseExpression("headers['foo']"));
		handler.setRequestFactory((uri, httpMethod) -> {
			actualUri.set(uri);
			throw new RuntimeException("intentional");
		});
		setBeanFactory(handler);
		handler.afterPropertiesSet();
		String theURL = "https://bar/baz?foo#bar";
		Message<?> message = MessageBuilder.withPayload("").setHeader("foo", theURL).build();

		assertThatException()
				.isThrownBy(() -> handler.handleMessage(message));

		assertThat(actualUri.get()).hasToString(theURL);
	}

	@Test
	public void testUriEncoded() {
		SpelExpressionParser parser = new SpelExpressionParser();
		AtomicReference<URI> actualUri = new AtomicReference<>();

		HttpRequestExecutingMessageHandler handler =
				new HttpRequestExecutingMessageHandler("https://example.com?query={query}");
		handler.setUriVariableExpressions(Collections.singletonMap("query", parser.parseExpression("payload")));
		handler.setRequestFactory((uri, httpMethod) -> {
			actualUri.set(uri);
			throw new RuntimeException("intentional");
		});
		setBeanFactory(handler);
		handler.afterPropertiesSet();

		assertThatException()
				.isThrownBy(() -> handler.handleMessage(new GenericMessage<>("test-äöü&%")));

		assertThat(actualUri.get()).hasToString("https://example.com?query=test-%C3%A4%C3%B6%C3%BC%26%25");
	}

	@Test
	public void testUriEncodedDisabled() {
		SpelExpressionParser parser = new SpelExpressionParser();
		AtomicReference<URI> actualUri = new AtomicReference<>();

		HttpRequestExecutingMessageHandler handler =
				new HttpRequestExecutingMessageHandler("https://example.com?query={query}");
		handler.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
		handler.setUriVariableExpressions(Collections.singletonMap("query", parser.parseExpression("payload")));
		handler.setRequestFactory((uri, httpMethod) -> {
			actualUri.set(uri);
			throw new RuntimeException("intentional");
		});
		setBeanFactory(handler);
		handler.afterPropertiesSet();

		assertThatException()
				.isThrownBy(() -> handler.handleMessage(new GenericMessage<>("test-äöü")));

		assertThat(actualUri.get()).hasToString("https://example.com?query=test-äöü");
	}

	@Test
	public void testInt2455UriNotEncoded() {
		AtomicReference<URI> actualUri = new AtomicReference<>();

		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				new SpelExpressionParser().parseExpression("'https://my.RabbitMQ.com/api/' + payload"));
		handler.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
		handler.setRequestFactory((uri, httpMethod) -> {
			actualUri.set(uri);
			throw new RuntimeException("intentional");
		});
		setBeanFactory(handler);
		handler.afterPropertiesSet();

		assertThatException()
				.isThrownBy(() -> handler.handleMessage(new GenericMessage<>("queues/%2f/si.test.queue?foo#bar")));

		assertThat(actualUri.get()).hasToString("https://my.RabbitMQ.com/api/queues/%2f/si.test.queue?foo#bar");
	}

	@Test
	public void acceptHeaderForSerializableResponse() throws IOException {
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"https://www.springsource.org/spring-integration");
		handler.setHttpMethod(HttpMethod.GET);
		handler.setExpectedResponseType(Foo.class);

		List<HttpMessageConverter<?>> converters = new ArrayList<>();
		converters.add(new SerializingHttpMessageConverter());
		handler.setMessageConverters(converters);

		HttpHeaders requestHeaders = setUpMocksToCaptureSentHeaders(handler);
		setBeanFactory(handler);
		handler.afterPropertiesSet();
		assertThat(TestUtils.<List<HttpMessageConverter<?>>>getPropertyValue(handler, "restClient.messageConverters"))
				.anyMatch(SerializingHttpMessageConverter.class::isInstance);

		assertThatException()
				.isThrownBy(() -> handler.handleMessage(new GenericMessage<>("foo")))
				.withStackTraceContaining("404 Not Found");

		assertThat(requestHeaders.getAccept()).isEmpty();
	}

	@Test
	public void acceptHeaderForSerializableResponseMessageExchange() throws IOException {
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"https://www.springsource.org/spring-integration");

		handler.setHttpMethod(HttpMethod.GET);
		handler.setExtractPayload(false);
		handler.setExpectedResponseType(GenericMessage.class);

		List<HttpMessageConverter<?>> converters = new ArrayList<>();
		converters.add(new SerializingHttpMessageConverter());
		handler.setMessageConverters(converters);

		HttpHeaders requestHeaders = setUpMocksToCaptureSentHeaders(handler);
		setBeanFactory(handler);
		handler.afterPropertiesSet();
		assertThat(TestUtils.<List<HttpMessageConverter<?>>>getPropertyValue(handler, "restClient.messageConverters"))
				.anyMatch(SerializingHttpMessageConverter.class::isInstance);

		assertThatException()
				.isThrownBy(() -> handler.handleMessage(new GenericMessage<>("foo")))
				.withStackTraceContaining("404 Not Found");

		assertThat(requestHeaders.getAccept()).isEmpty();
	}

	@Test
	public void testNoContentTypeAndSmartConverter() {
		Sinks.One<HttpHeaders> httpHeadersSink = Sinks.one();

		HttpRequestExecutingMessageHandler handler =
				new HttpRequestExecutingMessageHandler("https://www.springsource.org/spring-integration");
		handler.setRequestFactory((uri, httpMethod) ->
				new MockClientHttpRequest(httpMethod, uri) {

					@Override
					protected ClientHttpResponse executeInternal() {
						httpHeadersSink.tryEmitValue(getHeaders());
						throw new RuntimeException("intentional");
					}

				});
		setBeanFactory(handler);
		handler.afterPropertiesSet();

		assertThatException()
				.isThrownBy(() -> handler.handleMessage(new GenericMessage<>(new City("London"))));

		StepVerifier.create(httpHeadersSink.asMono())
				.assertNext(headers ->
						assertThat(headers.headerSet())
								.contains(Map.entry(HttpHeaders.CONTENT_TYPE,
										List.of(MediaType.APPLICATION_JSON_VALUE))))
				.verifyComplete();
	}

	private static void setBeanFactory(HttpRequestExecutingMessageHandler handler) {
		handler.setBeanFactory(TEST_INTEGRATION_CONTEXT);
	}

	private static HttpHeaders setUpMocksToCaptureSentHeaders(HttpRequestExecutingMessageHandler handler)
			throws IOException {

		HttpHeaders headers = new HttpHeaders();

		ClientHttpRequestFactory requestFactory = mock();
		ClientHttpRequest clientRequest = mock();
		when(clientRequest.getHeaders()).thenReturn(headers);

		when(requestFactory.createRequest(any(URI.class), any(HttpMethod.class))).thenReturn(clientRequest);

		ClientHttpResponse response = mock(ClientHttpResponse.class);
		when(response.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
		when(response.getStatusText()).thenReturn("Not Found");
		when(response.getBody()).thenReturn(new ByteArrayInputStream(new byte[0]));

		HttpHeaders responseHeaders = new HttpHeaders();
		when(response.getHeaders()).thenReturn(responseHeaders);

		when(clientRequest.execute()).thenReturn(response);

		handler.setRequestFactory(requestFactory);

		return headers;
	}

	public record City(String name) {

		@Override
		public String toString() {
			return this.name;
		}

	}

	private static final class CapturingRestClient {

		private final HttpHeaders headers = new HttpHeaders();

		private final AtomicReference<Object> body = new AtomicReference<>();

		private RestClient client() {
			RestClient restClient = mock(RestClient.class);
			RestClient.RequestBodyUriSpec spec = mock(RestClient.RequestBodyUriSpec.class);

			when(restClient.method(any())).thenReturn(spec);
			when(spec.uri(any(URI.class))).thenReturn(spec);
			when(spec.uri(anyString(), anyMap())).thenReturn(spec);
			when(spec.headers(any())).thenAnswer(invocation -> {
				this.headers.clear();
				this.body.set(null);
				Consumer<HttpHeaders> headerConsumer = invocation.getArgument(0);
				headerConsumer.accept(this.headers);
				return spec;
			});
			when(spec.body(any(Object.class))).thenAnswer(invocation -> {
				this.body.set(invocation.getArgument(0));
				return spec;
			});
			when(spec.retrieve()).thenThrow(new RuntimeException("intentional"));
			return restClient;
		}

		private HttpEntity<?> lastRequestEntity() {
			return new HttpEntity<>(this.body.get(), this.headers);
		}

	}

	public static final class CapturingRequestFactory implements ClientHttpRequestFactory {

		public final AtomicReference<URI> actualUrl = new AtomicReference<>();

		@Override
		public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
			this.actualUrl.set(uri);
			MockClientHttpRequest request = new MockClientHttpRequest(httpMethod, uri);
			request.setResponse(new MockClientHttpResponse(new byte[0], HttpStatus.OK));
			return request;
		}

	}

	private static class Foo implements Serializable {

		private static final long serialVersionUID = 1L;

	}

}
