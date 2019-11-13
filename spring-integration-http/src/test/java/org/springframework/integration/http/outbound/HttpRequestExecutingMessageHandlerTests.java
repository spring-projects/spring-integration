/*
 * Copyright 2002-2019 the original author or authors.
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
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.xml.transform.Source;

import org.junit.Test;
import org.mockito.Mockito;

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
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

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
		Exception exception = null;
		try {
			handler.handleMessage(message);
		}
		catch (Exception e) {
			exception = e;
		}
		assertThat(exception.getCause().getMessage()).isEqualTo("intentional");
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
		Exception exception = null;
		try {
			handler.handleMessage(message);
		}
		catch (Exception e) {
			exception = e;
		}
		assertThat(exception.getCause().getMessage()).isEqualTo("intentional");
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
		Exception exception = null;
		try {
			handler.handleMessage(message);
		}
		catch (Exception e) {
			exception = e;
		}
		assertThat(exception.getCause().getMessage()).isEqualTo("intentional");
		HttpEntity<?> request = template.lastRequestEntity.get();
		Object body = request.getBody();
		assertThat(body instanceof Map<?, ?>).isTrue();
		Map<?, ?> map = (Map<?, ?>) body;
		assertThat(map.get(1).toString()).isEqualTo("Philadelphia");
		assertThat(map.get(2).toString()).isEqualTo("Ambler");
		assertThat(map.get(3).toString()).isEqualTo("Mohnton");
		assertThat(request.getHeaders().getContentType().getType()).isEqualTo("application");
		assertThat(request.getHeaders().getContentType().getSubtype()).isEqualTo("x-java-serialized-object");
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
		Exception exception = null;
		try {
			handler.handleMessage(message);
		}
		catch (Exception e) {
			exception = e;
		}
		assertThat(exception.getCause().getMessage()).isEqualTo("intentional");
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
		Exception exception = null;
		try {
			handler.handleMessage(message);
		}
		catch (Exception e) {
			exception = e;
		}
		assertThat(exception.getCause().getMessage()).isEqualTo("intentional");
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
		Exception exception = null;
		try {
			handler.handleMessage(message);
		}
		catch (Exception e) {
			exception = e;
		}
		assertThat(exception.getCause().getMessage()).isEqualTo("intentional");
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
		Exception exception = null;
		try {
			handler.handleMessage(message);
		}
		catch (Exception e) {
			exception = e;
		}
		assertThat(exception.getCause().getMessage()).isEqualTo("intentional");
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
		Exception exception = null;
		try {
			handler.handleMessage(message);
		}
		catch (Exception e) {
			exception = e;
		}
		assertThat(exception.getCause().getMessage()).isEqualTo("intentional");
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
		Exception exception = null;
		try {
			handler.handleMessage(message);
		}
		catch (Exception e) {
			exception = e;
		}
		assertThat(exception.getCause().getMessage()).isEqualTo("intentional");
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
		Exception exception = null;
		try {
			handler.handleMessage(message);
		}
		catch (Exception e) {
			exception = e;
		}
		assertThat(exception.getCause().getMessage()).isEqualTo("intentional");
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
		Exception exception = null;
		try {
			handler.handleMessage(message);
		}
		catch (Exception e) {
			exception = e;
		}
		assertThat(exception.getCause().getMessage()).isEqualTo("intentional");
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
		Exception exception = null;
		try {
			handler.handleMessage(message);
		}
		catch (Exception e) {
			exception = e;
		}
		assertThat(exception.getCause().getMessage()).isEqualTo("intentional");
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
		Exception exception = null;
		try {
			handler.handleMessage(message);
		}
		catch (Exception e) {
			exception = e;
		}
		assertThat(exception.getCause().getMessage()).isEqualTo("intentional");
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

		Message<?> message = MessageBuilder.withPayload(mock(Source.class)).build();
		try {
			handler.handleMessage(message);
			fail("An Exception expected");
		}
		catch (Exception e) {
			assertThat(e.getCause().getMessage()).isEqualTo("intentional");
			assertThat(template.lastRequestEntity.get().getHeaders().getContentType()).isNull();
		}

		//HEAD
		handler.setHttpMethod(HttpMethod.HEAD);

		message = MessageBuilder.withPayload(mock(Source.class)).build();
		try {
			handler.handleMessage(message);
			fail("An Exception expected");
		}
		catch (Exception e) {
			assertThat(e.getCause().getMessage()).isEqualTo("intentional");
			assertThat(template.lastRequestEntity.get().getHeaders().getContentType()).isNull();
		}


		//DELETE
		handler.setHttpMethod(HttpMethod.DELETE);

		message = MessageBuilder.withPayload(mock(Source.class)).build();
		try {
			handler.handleMessage(message);
			fail("An Exception expected");
		}
		catch (Exception e) {
			assertThat(e.getCause().getMessage()).isEqualTo("intentional");
			assertThat(template.lastRequestEntity.get().getHeaders().getContentType()).isEqualTo(MediaType.TEXT_XML);
		}

		//TRACE
		handler.setHttpMethod(HttpMethod.TRACE);

		message = MessageBuilder.withPayload(mock(Source.class)).build();
		try {
			handler.handleMessage(message);
			fail("An Exception expected");
		}
		catch (Exception e) {
			assertThat(e.getCause().getMessage()).isEqualTo("intentional");
			assertThat(template.lastRequestEntity.get().getHeaders().getContentType()).isNull();
		}
	}

	@Test // INT-2275
	public void testOutboundChannelAdapterWithinChain() throws URISyntaxException {
		ConfigurableApplicationContext ctx = new ClassPathXmlApplicationContext(
				"HttpOutboundWithinChainTests-context.xml", this.getClass());
		MessageChannel channel = ctx.getBean("httpOutboundChannelAdapterWithinChain", MessageChannel.class);
		RestTemplate restTemplate = ctx.getBean("restTemplate", RestTemplate.class);
		channel.send(MessageBuilder.withPayload("test").build());
		Mockito.verify(restTemplate).exchange(Mockito.eq(new URI("http://localhost/test1/%2f")),
				Mockito.eq(HttpMethod.POST), Mockito.any(HttpEntity.class), Mockito.<Class<Object>>eq(null));
		HttpRequestExecutingMessageHandler handler = ctx.getBean("chain$child.adapter.handler",
				HttpRequestExecutingMessageHandler.class);
		assertThat(TestUtils.getPropertyValue(handler, "trustedSpel")).isEqualTo(Boolean.TRUE);
		ctx.close();
	}

	@Test // INT-1029
	public void testHttpOutboundGatewayWithinChain() throws URISyntaxException {
		ConfigurableApplicationContext ctx = new ClassPathXmlApplicationContext(
				"HttpOutboundWithinChainTests-context.xml", this.getClass());
		MessageChannel channel = ctx.getBean("httpOutboundGatewayWithinChain", MessageChannel.class);
		RestTemplate restTemplate = ctx.getBean("restTemplate", RestTemplate.class);
		channel.send(MessageBuilder.withPayload("test").build());

		PollableChannel output = ctx.getBean("replyChannel", PollableChannel.class);
		Message<?> receive = output.receive();
		assertThat(((ResponseEntity<?>) receive.getPayload()).getStatusCode()).isEqualTo(HttpStatus.OK);
		Mockito.verify(restTemplate).exchange(
				Mockito.eq(new URI("http://localhost:51235/%2f/testApps?param=http+Outbound+Gateway+Within+Chain")),
				Mockito.eq(HttpMethod.POST), Mockito.any(HttpEntity.class),
				Mockito.eq(new ParameterizedTypeReference<List<String>>() {

				}));
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
		try {
			handler.handleRequestMessage(message);
		}
		catch (Exception e) {
		}
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

		// This flag is set by default to true, but for sake of clarity for the reader we explicitly set it here again
		handler.setEncodeUri(true);

		handler.setUriVariableExpressions(Collections.singletonMap("query", parser.parseExpression("payload")));
		setBeanFactory(handler);
		handler.afterPropertiesSet();
		Message<?> message = new GenericMessage<>("test-äöü&%");
		try {
			handler.handleMessage(message);
		}
		catch (Exception ignored) {
		}
		assertThat(restTemplate.actualUrl.get()).isEqualTo("https://example.com?query=test-%C3%A4%C3%B6%C3%BC%26%25");
	}

	@Test
	public void testUriEncodedDisabled() {
		SpelExpressionParser parser = new SpelExpressionParser();
		MockRestTemplate restTemplate = new MockRestTemplate();

		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"https://example.com?query={query}",
				restTemplate
		);

		handler.setEncodeUri(false);
		handler.setUriVariableExpressions(Collections.singletonMap("query", parser.parseExpression("payload")));
		setBeanFactory(handler);
		handler.afterPropertiesSet();
		Message<?> message = new GenericMessage<>("test-äöü");
		try {
			handler.handleMessage(message);
		}
		catch (Exception ignored) {
		}
		assertThat(restTemplate.actualUrl.get()).isEqualTo("https://example.com?query=test-äöü");
	}

	@Test
	public void testInt2455UriNotEncoded() {
		MockRestTemplate restTemplate = new MockRestTemplate();
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				new SpelExpressionParser().parseExpression("'https://my.RabbitMQ.com/api/' + payload"), restTemplate);
		handler.setEncodeUri(false);
		setBeanFactory(handler);
		handler.afterPropertiesSet();
		Message<?> message = MessageBuilder.withPayload("queues/%2f/si.test.queue?foo#bar").build();
		try {
			handler.handleRequestMessage(message);
		}
		catch (Exception e) {
		}
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

		Message<?> message = MessageBuilder.withPayload("foo").build();
		Exception exception = null;
		try {
			handler.handleMessage(message);
		}
		catch (Exception e) {
			exception = e;
		}
		assertThat(requestHeaders.getAccept()).isNotNull();
		assertThat(requestHeaders.getAccept().size() > 0).isTrue();
		assertThat(exception.getCause().getMessage()).contains("404 Not Found");
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

		Message<?> message = MessageBuilder.withPayload("foo").build();
		Exception exception = null;
		try {
			handler.handleMessage(message);
		}
		catch (Exception e) {
			exception = e;
		}
		assertThat(requestHeaders.getAccept()).isNotNull();
		assertThat(requestHeaders.getAccept().size() > 0).isTrue();
		assertThat(exception.getCause().getMessage()).contains("404 Not Found");
		List<MediaType> accept = requestHeaders.getAccept();
		assertThat(accept.size() > 0).isTrue();
		assertThat(accept.get(0).getType()).isEqualTo("application");
		assertThat(accept.get(0).getSubtype()).isEqualTo("x-java-serialized-object");
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

		@Override
		public String toString() {
			return name;
		}

	}

	private static class MockRestTemplate extends RestTemplate {

		private final AtomicReference<HttpEntity<?>> lastRequestEntity = new AtomicReference<>();

		private final AtomicReference<String> actualUrl = new AtomicReference<>();

		@Override
		public <T> ResponseEntity<T> exchange(URI uri, HttpMethod method, HttpEntity<?> requestEntity,
				Class<T> responseType) throws RestClientException {

			this.actualUrl.set(uri.toString());
			this.lastRequestEntity.set(requestEntity);
			throw new RuntimeException("intentional");
		}

	}

	@SuppressWarnings("unused")
	private static class MockRestTemplate2 extends RestTemplate {

		@Override
		public <T> ResponseEntity<T> exchange(URI uri, HttpMethod method, HttpEntity<?> requestEntity,
				Class<T> responseType) throws RestClientException {
			return new ResponseEntity<T>(HttpStatus.OK);
		}

		@Override
		public <T> ResponseEntity<T> exchange(URI url, HttpMethod method, HttpEntity<?> requestEntity,
				ParameterizedTypeReference<T> responseType) throws RestClientException {
			return new ResponseEntity<T>(HttpStatus.OK);
		}

	}

	private static class Foo implements Serializable {

		private static final long serialVersionUID = 1L;

	}

}
