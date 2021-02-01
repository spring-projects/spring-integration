/*
 * Copyright 2002-2021 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.xml.transform.Source;

import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.http.converter.SerializingHttpMessageConverter;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gary Russell
 * @author Gunnar Hillert
 * @author Florian Schöffl
 */
public class HttpRequestExecutingMessageHandlerTests {

	public static ParameterizedTypeReference<List<String>> testParameterizedTypeReference() {
		return new ParameterizedTypeReference<List<String>>() {

		};
	}

	@Test
	public void simpleStringKeyStringValueFormData() {
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"https://www.springsource.org/spring-integration");
		MockRestTemplate template = new MockRestTemplate();
		new DirectFieldAccessor(handler).setPropertyValue("restTemplate", template);
		handler.setHttpMethod(HttpMethod.POST);
		setBeanFactory(handler);
		handler.afterPropertiesSet();
		Map<String, String> form = new LinkedHashMap<>();
		form.put("a", "1");
		form.put("b", "2");
		form.put("c", "3");
		Message<?> message = MessageBuilder.withPayload(form).build();
		QueueChannel replyChannel = new QueueChannel();
		handler.setOutputChannel(replyChannel);

		assertThatExceptionOfType(Exception.class)
				.isThrownBy(() -> handler.handleMessage(message))
				.withStackTraceContaining("intentional");

		HttpEntity<?> request = template.lastRequestEntity.get();
		Object body = request.getBody();
		assertThat(request.getHeaders().getContentType()).isNotNull();
		assertThat(body instanceof MultiValueMap<?, ?>).isTrue();
		MultiValueMap<?, ?> map = (MultiValueMap<?, ?>) body;
		assertThat(map.get("a").iterator().next()).isEqualTo("1");
		assertThat(map.get("b").iterator().next()).isEqualTo("2");
		assertThat(map.get("c").iterator().next()).isEqualTo("3");
		assertThat(request.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_FORM_URLENCODED);
	}

	@Test
	public void simpleStringKeyObjectValueFormData() {
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"https://www.springsource.org/spring-integration");
		MockRestTemplate template = new MockRestTemplate();
		new DirectFieldAccessor(handler).setPropertyValue("restTemplate", template);
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

		assertThatExceptionOfType(Exception.class)
				.isThrownBy(() -> handler.handleMessage(message))
				.withStackTraceContaining("intentional");

		HttpEntity<?> request = template.lastRequestEntity.get();
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
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"https://www.springsource.org/spring-integration");
		MockRestTemplate template = new MockRestTemplate();
		new DirectFieldAccessor(handler).setPropertyValue("restTemplate", template);
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

		assertThatExceptionOfType(Exception.class)
				.isThrownBy(() -> handler.handleMessage(message))
				.withStackTraceContaining("intentional");

		HttpEntity<?> request = template.lastRequestEntity.get();
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
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"https://www.springsource.org/spring-integration");
		MockRestTemplate template = new MockRestTemplate();
		new DirectFieldAccessor(handler).setPropertyValue("restTemplate", template);
		handler.setHttpMethod(HttpMethod.POST);
		setBeanFactory(handler);
		handler.afterPropertiesSet();
		Map<String, Object> form = new LinkedHashMap<>();
		form.put("a", new String[] { "1", "2", "3" });
		form.put("b", "4");
		form.put("c", new String[] { "5" });
		form.put("d", "6");
		Message<?> message = MessageBuilder.withPayload(form).build();

		assertThatExceptionOfType(Exception.class)
				.isThrownBy(() -> handler.handleMessage(message))
				.withStackTraceContaining("intentional");

		HttpEntity<?> request = template.lastRequestEntity.get();
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
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"https://www.springsource.org/spring-integration");
		MockRestTemplate template = new MockRestTemplate();
		new DirectFieldAccessor(handler).setPropertyValue("restTemplate", template);
		handler.setHttpMethod(HttpMethod.POST);
		setBeanFactory(handler);
		handler.afterPropertiesSet();
		Map<String, Object> form = new LinkedHashMap<>();
		form.put("a", new int[] { 1, 2, 3 });
		form.put("b", "4");
		form.put("c", new String[] { "5" });
		form.put("d", "6");
		Message<?> message = MessageBuilder.withPayload(form).build();

		assertThatExceptionOfType(Exception.class)
				.isThrownBy(() -> handler.handleMessage(message))
				.withStackTraceContaining("intentional");

		HttpEntity<?> request = template.lastRequestEntity.get();
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
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"https://www.springsource.org/spring-integration");
		MockRestTemplate template = new MockRestTemplate();
		new DirectFieldAccessor(handler).setPropertyValue("restTemplate", template);
		handler.setHttpMethod(HttpMethod.POST);
		setBeanFactory(handler);
		handler.afterPropertiesSet();
		Map<String, Object> form = new LinkedHashMap<>();
		form.put("a", new Object[] { null, 4, null });
		form.put("b", "4");
		Message<?> message = MessageBuilder.withPayload(form).build();

		assertThatExceptionOfType(Exception.class)
				.isThrownBy(() -> handler.handleMessage(message))
				.withStackTraceContaining("intentional");

		HttpEntity<?> request = template.lastRequestEntity.get();
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
	 * injected "5" into the list as String resulting in the Content-TYpe being
	 * application/x-www-form-urlencoded
	 */
	@Test
	public void stringKeyNullCollectionValueMixedFormDataString() {
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"https://www.springsource.org/spring-integration");
		MockRestTemplate template = new MockRestTemplate();
		new DirectFieldAccessor(handler).setPropertyValue("restTemplate", template);
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

		assertThatExceptionOfType(Exception.class)
				.isThrownBy(() -> handler.handleMessage(message))
				.withStackTraceContaining("intentional");

		HttpEntity<?> request = template.lastRequestEntity.get();
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
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"https://www.springsource.org/spring-integration");
		MockRestTemplate template = new MockRestTemplate();
		new DirectFieldAccessor(handler).setPropertyValue("restTemplate", template);
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

		assertThatExceptionOfType(Exception.class)
				.isThrownBy(() -> handler.handleMessage(message))
				.withStackTraceContaining("intentional");

		HttpEntity<?> request = template.lastRequestEntity.get();
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
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"https://www.springsource.org/spring-integration");
		MockRestTemplate template = new MockRestTemplate();
		new DirectFieldAccessor(handler).setPropertyValue("restTemplate", template);
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

		assertThatExceptionOfType(Exception.class)
				.isThrownBy(() -> handler.handleMessage(message))
				.withStackTraceContaining("intentional");

		HttpEntity<?> request = template.lastRequestEntity.get();
		Object body = request.getBody();
		assertThat(body instanceof MultiValueMap<?, ?>).isTrue();
		MultiValueMap<?, ?> map = (MultiValueMap<?, ?>) body;

		List<?> aValue = map.get("a");
		assertThat(aValue.size()).isEqualTo(2);
		assertThat(aValue.get(0)).isEqualTo("1");
		assertThat(aValue.get(1)).isEqualTo("2");

		List<?> bValue = map.get("b");
		assertThat(bValue.size()).isEqualTo(0);

		List<?> cValue = map.get("c");
		assertThat(cValue.size()).isEqualTo(1);
		assertThat(cValue.get(0)).isEqualTo("3");

		assertThat(request.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_FORM_URLENCODED);
	}

	@Test
	public void stringKeyObjectCollectionValueFormData() {
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"https://www.springsource.org/spring-integration");
		MockRestTemplate template = new MockRestTemplate();
		new DirectFieldAccessor(handler).setPropertyValue("restTemplate", template);
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

		assertThatExceptionOfType(Exception.class)
				.isThrownBy(() -> handler.handleMessage(message))
				.withStackTraceContaining("intentional");

		HttpEntity<?> request = template.lastRequestEntity.get();
		Object body = request.getBody();
		assertThat(body instanceof MultiValueMap<?, ?>).isTrue();
		MultiValueMap<?, ?> map = (MultiValueMap<?, ?>) body;

		List<?> aValue = map.get("a");
		assertThat(aValue.size()).isEqualTo(2);
		assertThat(aValue.get(0).toString()).isEqualTo("Philadelphia");
		assertThat(aValue.get(1).toString()).isEqualTo("Ambler");

		List<?> bValue = map.get("b");
		assertThat(bValue.size()).isEqualTo(0);

		List<?> cValue = map.get("c");
		assertThat(cValue.size()).isEqualTo(1);
		assertThat(cValue.get(0).toString()).isEqualTo("Mohnton");

		assertThat(request.getHeaders().getContentType()).isEqualTo(MediaType.MULTIPART_FORM_DATA);
	}

	@Test
	public void nameOnlyWithNullValues() {
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"https://www.springsource.org/spring-integration");
		MockRestTemplate template = new MockRestTemplate();
		new DirectFieldAccessor(handler).setPropertyValue("restTemplate", template);
		handler.setHttpMethod(HttpMethod.POST);
		setBeanFactory(handler);
		handler.afterPropertiesSet();
		Map<String, Object> form = new LinkedHashMap<>();
		form.put("a", null);
		form.put("b", "foo");
		form.put("c", null);
		Message<?> message = MessageBuilder.withPayload(form).build();

		assertThatExceptionOfType(Exception.class)
				.isThrownBy(() -> handler.handleMessage(message))
				.withStackTraceContaining("intentional");

		HttpEntity<?> request = template.lastRequestEntity.get();
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

	@SuppressWarnings("cast")
	@Test
	public void contentAsByteArray() {
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"https://www.springsource.org/spring-integration");
		MockRestTemplate template = new MockRestTemplate();
		new DirectFieldAccessor(handler).setPropertyValue("restTemplate", template);
		handler.setHttpMethod(HttpMethod.POST);
		setBeanFactory(handler);
		handler.afterPropertiesSet();

		byte[] bytes = "Hello World".getBytes();
		Message<?> message = MessageBuilder.withPayload(bytes).build();

		assertThatExceptionOfType(Exception.class)
				.isThrownBy(() -> handler.handleMessage(message))
				.withStackTraceContaining("intentional");

		HttpEntity<?> request = template.lastRequestEntity.get();
		Object body = request.getBody();
		assertThat(body instanceof byte[]).isTrue();
		assertThat(new String(bytes)).isEqualTo("Hello World");
		assertThat(request.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
	}

	@Test
	public void contentAsXmlSource() {
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"https://www.springsource.org/spring-integration");
		MockRestTemplate template = new MockRestTemplate();
		new DirectFieldAccessor(handler).setPropertyValue("restTemplate", template);
		handler.setHttpMethod(HttpMethod.POST);
		setBeanFactory(handler);
		handler.afterPropertiesSet();

		Message<?> message = MessageBuilder.withPayload(mock(Source.class)).build();

		assertThatExceptionOfType(Exception.class)
				.isThrownBy(() -> handler.handleMessage(message))
				.withStackTraceContaining("intentional");

		HttpEntity<?> request = template.lastRequestEntity.get();
		Object body = request.getBody();
		assertThat(body instanceof Source).isTrue();
		assertThat(request.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_XML);
	}

	@Test // no assertions just a warn message in a log
	public void testWarnMessageForNonPostPutAndExtractPayload() {
		// should see a warn message

		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"https://www.springsource.org/spring-integration");
		MockRestTemplate template = new MockRestTemplate();
		new DirectFieldAccessor(handler).setPropertyValue("restTemplate", template);
		handler.setHttpMethod(HttpMethod.GET);
		handler.setExtractPayload(true);
		setBeanFactory(handler);
		handler.afterPropertiesSet();

		// should not see a warn message since 'setExtractPayload' is not set explicitly

		handler = new HttpRequestExecutingMessageHandler("https://www.springsource.org/spring-integration");
		template = new MockRestTemplate();
		new DirectFieldAccessor(handler).setPropertyValue("restTemplate", template);
		handler.setHttpMethod(HttpMethod.GET);
		setBeanFactory(handler);
		handler.afterPropertiesSet();

		// should not see a warn message since HTTP method is not GET

		handler = new HttpRequestExecutingMessageHandler("https://www.springsource.org/spring-integration");
		template = new MockRestTemplate();
		new DirectFieldAccessor(handler).setPropertyValue("restTemplate", template);
		handler.setHttpMethod(HttpMethod.POST);
		handler.setExtractPayload(true);
		setBeanFactory(handler);
		handler.afterPropertiesSet();
	}

	@Test
	public void contentTypeIsNotSetForGetAndHeadRequest() {
		// GET
		HttpRequestExecutingMessageHandler handler =
				new HttpRequestExecutingMessageHandler("https://www.springsource.org/spring-integration");
		MockRestTemplate template = new MockRestTemplate();
		new DirectFieldAccessor(handler).setPropertyValue("restTemplate", template);
		handler.setHttpMethod(HttpMethod.GET);
		setBeanFactory(handler);
		handler.afterPropertiesSet();

		assertThatExceptionOfType(Exception.class)
				.isThrownBy(() -> handler.handleMessage(MessageBuilder.withPayload(mock(Source.class)).build()))
				.withStackTraceContaining("intentional");

		assertThat(template.lastRequestEntity.get().getHeaders().getContentType()).isNull();

		//HEAD
		handler.setHttpMethod(HttpMethod.HEAD);

		assertThatExceptionOfType(Exception.class)
				.isThrownBy(() -> handler.handleMessage(MessageBuilder.withPayload(mock(Source.class)).build()))
				.withStackTraceContaining("intentional");

		assertThat(template.lastRequestEntity.get().getHeaders().getContentType()).isNull();


		//DELETE
		handler.setHttpMethod(HttpMethod.DELETE);

		assertThatExceptionOfType(Exception.class)
				.isThrownBy(() -> handler.handleMessage(MessageBuilder.withPayload(mock(Source.class)).build()))
				.withStackTraceContaining("intentional");

		assertThat(template.lastRequestEntity.get().getHeaders().getContentType()).isEqualTo(MediaType.TEXT_XML);

		//TRACE
		handler.setHttpMethod(HttpMethod.TRACE);

		assertThatExceptionOfType(Exception.class)
				.isThrownBy(() -> handler.handleMessage(MessageBuilder.withPayload(mock(Source.class)).build()))
				.withStackTraceContaining("intentional");

		assertThat(template.lastRequestEntity.get().getHeaders().getContentType()).isNull();
	}

	@Test
	public void testOutboundChannelAdapterWithinChain() {
		ConfigurableApplicationContext ctx = new ClassPathXmlApplicationContext(
				"HttpOutboundWithinChainTests-context.xml", this.getClass());
		MessageChannel channel = ctx.getBean("httpOutboundChannelAdapterWithinChain", MessageChannel.class);
		MockRestTemplate2 restTemplate = ctx.getBean("restTemplate", MockRestTemplate2.class);
		channel.send(MessageBuilder.withPayload("test").build());

		assertThat(restTemplate.actualUrl.get()).isEqualTo("http://localhost/test1/%2f");

		HttpRequestExecutingMessageHandler handler = ctx.getBean("chain$child.adapter.handler",
				HttpRequestExecutingMessageHandler.class);

		assertThat(TestUtils.getPropertyValue(handler, "trustedSpel")).isEqualTo(Boolean.TRUE);
		ctx.close();
	}

	@Test
	public void testHttpOutboundGatewayWithinChain() {
		ConfigurableApplicationContext ctx = new ClassPathXmlApplicationContext(
				"HttpOutboundWithinChainTests-context.xml", this.getClass());
		MessageChannel channel = ctx.getBean("httpOutboundGatewayWithinChain", MessageChannel.class);
		MockRestTemplate2 restTemplate = ctx.getBean("restTemplate", MockRestTemplate2.class);
		channel.send(MessageBuilder.withPayload("test").build());

		PollableChannel output = ctx.getBean("replyChannel", PollableChannel.class);
		Message<?> receive = output.receive();
		assertThat(((ResponseEntity<?>) receive.getPayload()).getStatusCode()).isEqualTo(HttpStatus.OK);

		assertThat(restTemplate.actualUrl.get())
				.isEqualTo("http://localhost:51235/%2f/testApps?param=http+Outbound+Gateway+Within+Chain");

		ctx.close();
	}

	@Test
	public void testUriExpression() {
		MockRestTemplate restTemplate = new MockRestTemplate();
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				new SpelExpressionParser().parseExpression("headers['foo']"), restTemplate);
		setBeanFactory(handler);
		handler.afterPropertiesSet();
		String theURL = "https://bar/baz?foo#bar";
		Message<?> message = MessageBuilder.withPayload("").setHeader("foo", theURL).build();

		assertThatExceptionOfType(Exception.class)
				.isThrownBy(() -> handler.handleMessage(message));

		assertThat(restTemplate.actualUrl.get()).isEqualTo(theURL);
	}

	@Test
	public void testUriEncoded() {
		SpelExpressionParser parser = new SpelExpressionParser();
		MockRestTemplate restTemplate = new MockRestTemplate();

		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"https://example.com?query={query}",
				restTemplate
		);

		handler.setUriVariableExpressions(Collections.singletonMap("query", parser.parseExpression("payload")));
		setBeanFactory(handler);
		handler.afterPropertiesSet();

		assertThatExceptionOfType(Exception.class)
				.isThrownBy(() -> handler.handleMessage(new GenericMessage<>("test-äöü&%")));

		assertThat(restTemplate.actualUrl.get()).isEqualTo("https://example.com?query=test-%C3%A4%C3%B6%C3%BC%26%25");
	}

	@Test
	public void testUriEncodedDisabled() {
		SpelExpressionParser parser = new SpelExpressionParser();
		MockRestTemplate restTemplate = new MockRestTemplate();
		DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory();
		uriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
		restTemplate.setUriTemplateHandler(uriBuilderFactory);

		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"https://example.com?query={query}",
				restTemplate
		);

		handler.setUriVariableExpressions(Collections.singletonMap("query", parser.parseExpression("payload")));
		setBeanFactory(handler);
		handler.afterPropertiesSet();

		assertThatExceptionOfType(Exception.class)
				.isThrownBy(() -> handler.handleMessage(new GenericMessage<>("test-äöü")));

		assertThat(restTemplate.actualUrl.get()).isEqualTo("https://example.com?query=test-äöü");
	}

	@Test
	public void testInt2455UriNotEncoded() {
		MockRestTemplate restTemplate = new MockRestTemplate();
		DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory();
		uriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
		restTemplate.setUriTemplateHandler(uriBuilderFactory);

		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				new SpelExpressionParser().parseExpression("'https://my.RabbitMQ.com/api/' + payload"), restTemplate);
		setBeanFactory(handler);
		handler.afterPropertiesSet();

		assertThatExceptionOfType(Exception.class)
				.isThrownBy(() -> handler.handleMessage(new GenericMessage<>("queues/%2f/si.test.queue?foo#bar")));

		assertThat(restTemplate.actualUrl.get())
				.isEqualTo("https://my.RabbitMQ.com/api/queues/%2f/si.test.queue?foo#bar");
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
		setBeanFactory(handler);
		handler.afterPropertiesSet();

		RestTemplate restTemplate = TestUtils.getPropertyValue(handler, "restTemplate", RestTemplate.class);

		HttpHeaders requestHeaders = setUpMocksToCaptureSentHeaders(restTemplate);

		assertThatExceptionOfType(Exception.class)
				.isThrownBy(() -> handler.handleMessage(new GenericMessage<>("foo")))
				.withStackTraceContaining("404 Not Found");

		assertThat(requestHeaders.getAccept()).isNotNull();
		assertThat(requestHeaders.getAccept().size() > 0).isTrue();
		List<MediaType> accept = requestHeaders.getAccept();
		assertThat(accept.size() > 0).isTrue();
		assertThat(accept.get(0).getType()).isEqualTo("application");
		assertThat(accept.get(0).getSubtype()).isEqualTo("x-java-serialized-object");
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
		setBeanFactory(handler);
		handler.afterPropertiesSet();

		RestTemplate restTemplate = TestUtils.getPropertyValue(handler, "restTemplate", RestTemplate.class);

		HttpHeaders requestHeaders = setUpMocksToCaptureSentHeaders(restTemplate);

		assertThatExceptionOfType(Exception.class)
				.isThrownBy(() -> handler.handleMessage(new GenericMessage<>("foo")))
				.withStackTraceContaining("404 Not Found");

		assertThat(requestHeaders.getAccept()).isNotNull();
		assertThat(requestHeaders.getAccept().size() > 0).isTrue();
		List<MediaType> accept = requestHeaders.getAccept();
		assertThat(accept.size() > 0).isTrue();
		assertThat(accept.get(0).getType()).isEqualTo("application");
		assertThat(accept.get(0).getSubtype()).isEqualTo("x-java-serialized-object");
	}

	@Test
	public void testNoContentTypeAndSmartConverter() throws IOException {
		Sinks.One<HttpHeaders> httpHeadersSink = Sinks.one();
		RestTemplate testRestTemplate = new RestTemplate() {
			@Nullable
			protected <T> T doExecute(URI url, @Nullable HttpMethod method, @Nullable RequestCallback requestCallback,
					@Nullable ResponseExtractor<T> responseExtractor) throws RestClientException {

				try {
					ClientHttpRequest request = createRequest(url, method);
					requestCallback.doWithRequest(request);
					httpHeadersSink.tryEmitValue(request.getHeaders());
				}
				catch (IOException e) {
					throw new RestClientException("Not possible", e);
				}
				throw new RuntimeException("intentional");
			}
		};

		HttpRequestExecutingMessageHandler handler =
				new HttpRequestExecutingMessageHandler("https://www.springsource.org/spring-integration",
				testRestTemplate);
		setBeanFactory(handler);
		handler.afterPropertiesSet();

		assertThatExceptionOfType(Exception.class)
				.isThrownBy(() -> handler.handleMessage(new GenericMessage<>(new City("London"))));

		StepVerifier.create(httpHeadersSink.asMono())
				.assertNext(headers ->
						assertThat(headers)
								.containsEntry(HttpHeaders.CONTENT_TYPE,
										Arrays.asList(MediaType.APPLICATION_JSON_VALUE)))
				.verifyComplete();
	}

	private void setBeanFactory(HttpRequestExecutingMessageHandler handler) {
		handler.setBeanFactory(mock(BeanFactory.class));
	}

	private HttpHeaders setUpMocksToCaptureSentHeaders(RestTemplate restTemplate) throws IOException {

		HttpHeaders headers = new HttpHeaders();

		ClientHttpRequestFactory requestFactory = mock(ClientHttpRequestFactory.class);
		ClientHttpRequest clientRequest = mock(ClientHttpRequest.class);
		when(clientRequest.getHeaders()).thenReturn(headers);

		when(requestFactory.createRequest(any(URI.class), any(HttpMethod.class))).thenReturn(clientRequest);

		ClientHttpResponse response = mock(ClientHttpResponse.class);
		when(response.getRawStatusCode()).thenReturn(HttpStatus.NOT_FOUND.value());
		when(response.getStatusText()).thenReturn("Not Found");
		when(response.getBody()).thenReturn(new ByteArrayInputStream(new byte[0]));

		HttpHeaders responseHeaders = new HttpHeaders();
		when(response.getHeaders()).thenReturn(responseHeaders);

		when(clientRequest.execute()).thenReturn(response);

		restTemplate.setRequestFactory(requestFactory);

		return headers;
	}

	public static class City {

		private final String name;

		public City(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

		@Override
		public String toString() {
			return name;
		}

	}

	private static class MockRestTemplate extends RestTemplate {

		private final AtomicReference<HttpEntity<?>> lastRequestEntity = new AtomicReference<>();

		private final AtomicReference<String> actualUrl = new AtomicReference<>();

		@Nullable
		protected <T> T doExecute(URI url, @Nullable HttpMethod method, @Nullable RequestCallback requestCallback,
				@Nullable ResponseExtractor<T> responseExtractor) throws RestClientException {
			this.actualUrl.set(url.toString());
			this.lastRequestEntity.set(TestUtils.getPropertyValue(requestCallback, "requestEntity", HttpEntity.class));
			throw new RuntimeException("intentional");
		}

	}

	@SuppressWarnings("unused")
	private static class MockRestTemplate2 extends RestTemplate {

		private final AtomicReference<String> actualUrl = new AtomicReference<>();

		MockRestTemplate2() {
			DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory();
			uriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
			setUriTemplateHandler(uriBuilderFactory);
		}

		@Nullable
		protected <T> T doExecute(URI url, @Nullable HttpMethod method, @Nullable RequestCallback requestCallback,
				@Nullable ResponseExtractor<T> responseExtractor) throws RestClientException {
			this.actualUrl.set(url.toString());
			try {
				return responseExtractor.extractData(new MockClientHttpResponse(new byte[0], HttpStatus.OK));
			}
			catch (IOException ex) {
				throw new ResourceAccessException("I/O error on " + method.name() +
						" request for \"" + url + "\": " + ex.getMessage(), ex);
			}
		}

	}

	private static class Foo implements Serializable {

		private static final long serialVersionUID = 1L;

	}

}
