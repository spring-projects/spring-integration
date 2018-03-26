/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.integration.http.outbound;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
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
 */
public class HttpRequestExecutingMessageHandlerTests {

	public static ParameterizedTypeReference<List<String>> testParameterizedTypeReference() {
		return new ParameterizedTypeReference<List<String>>() {

		};
	}

	@Test
	public void simpleStringKeyStringValueFormData() throws Exception {
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"http://www.springsource.org/spring-integration");
		MockRestTemplate template = new MockRestTemplate();
		new DirectFieldAccessor(handler).setPropertyValue("restTemplate", template);
		handler.setHttpMethod(HttpMethod.POST);
		setBeanFactory(handler);
		handler.afterPropertiesSet();
		Map<String, String> form = new LinkedHashMap<String, String>();
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
		assertEquals("intentional", exception.getCause().getMessage());
		HttpEntity<?> request = template.lastRequestEntity.get();
		Object body = request.getBody();
		assertNotNull(request.getHeaders().getContentType());
		assertTrue(body instanceof MultiValueMap<?, ?>);
		MultiValueMap<?, ?> map = (MultiValueMap<?, ?>) body;
		assertEquals("1", map.get("a").iterator().next());
		assertEquals("2", map.get("b").iterator().next());
		assertEquals("3", map.get("c").iterator().next());
		assertEquals(MediaType.APPLICATION_FORM_URLENCODED, request.getHeaders().getContentType());
	}

	@Test
	public void simpleStringKeyObjectValueFormData() throws Exception {
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"http://www.springsource.org/spring-integration");
		MockRestTemplate template = new MockRestTemplate();
		new DirectFieldAccessor(handler).setPropertyValue("restTemplate", template);
		handler.setHttpMethod(HttpMethod.POST);
		setBeanFactory(handler);
		handler.afterPropertiesSet();
		Map<String, Object> form = new LinkedHashMap<String, Object>();
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
		assertEquals("intentional", exception.getCause().getMessage());
		HttpEntity<?> request = template.lastRequestEntity.get();
		Object body = request.getBody();
		assertTrue(body instanceof MultiValueMap<?, ?>);
		MultiValueMap<?, ?> map = (MultiValueMap<?, ?>) body;
		assertEquals("Philadelphia", map.get("a").get(0).toString());
		assertEquals("Ambler", map.get("b").get(0).toString());
		assertEquals("Mohnton", map.get("c").get(0).toString());
		assertEquals(MediaType.MULTIPART_FORM_DATA, request.getHeaders().getContentType());
	}

	@Test
	public void simpleObjectKeyObjectValueFormData() throws Exception {
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"http://www.springsource.org/spring-integration");
		MockRestTemplate template = new MockRestTemplate();
		new DirectFieldAccessor(handler).setPropertyValue("restTemplate", template);
		handler.setHttpMethod(HttpMethod.POST);
		setBeanFactory(handler);
		handler.afterPropertiesSet();
		Map<Object, Object> form = new LinkedHashMap<Object, Object>();
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
		assertEquals("intentional", exception.getCause().getMessage());
		HttpEntity<?> request = template.lastRequestEntity.get();
		Object body = request.getBody();
		assertTrue(body instanceof Map<?, ?>);
		Map<?, ?> map = (Map<?, ?>) body;
		assertEquals("Philadelphia", map.get(1).toString());
		assertEquals("Ambler", map.get(2).toString());
		assertEquals("Mohnton", map.get(3).toString());
		assertEquals("application", request.getHeaders().getContentType().getType());
		assertEquals("x-java-serialized-object", request.getHeaders().getContentType().getSubtype());
	}

	@Test
	public void stringKeyStringArrayValueFormData() throws Exception {
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"http://www.springsource.org/spring-integration");
		MockRestTemplate template = new MockRestTemplate();
		new DirectFieldAccessor(handler).setPropertyValue("restTemplate", template);
		handler.setHttpMethod(HttpMethod.POST);
		setBeanFactory(handler);
		handler.afterPropertiesSet();
		Map<String, Object> form = new LinkedHashMap<String, Object>();
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
		assertEquals("intentional", exception.getCause().getMessage());
		HttpEntity<?> request = template.lastRequestEntity.get();
		Object body = request.getBody();
		assertTrue(body instanceof MultiValueMap<?, ?>);
		MultiValueMap<?, ?> map = (MultiValueMap<?, ?>) body;

		List<?> aValue = map.get("a");
		assertEquals(3, aValue.size());
		assertEquals("1", aValue.get(0));
		assertEquals("2", aValue.get(1));
		assertEquals("3", aValue.get(2));

		List<?> bValue = map.get("b");
		assertEquals(1, bValue.size());
		assertEquals("4", bValue.get(0));

		List<?> cValue = map.get("c");
		assertEquals(1, cValue.size());
		assertEquals("5", cValue.get(0));

		List<?> dValue = map.get("d");
		assertEquals(1, dValue.size());
		assertEquals("6", dValue.get(0));
		assertEquals(MediaType.APPLICATION_FORM_URLENCODED, request.getHeaders().getContentType());
	}

	@Test
	public void stringKeyPrimitiveArrayValueMixedFormData() throws Exception {
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"http://www.springsource.org/spring-integration");
		MockRestTemplate template = new MockRestTemplate();
		new DirectFieldAccessor(handler).setPropertyValue("restTemplate", template);
		handler.setHttpMethod(HttpMethod.POST);
		setBeanFactory(handler);
		handler.afterPropertiesSet();
		Map<String, Object> form = new LinkedHashMap<String, Object>();
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
		assertEquals("intentional", exception.getCause().getMessage());
		HttpEntity<?> request = template.lastRequestEntity.get();
		Object body = request.getBody();
		assertTrue(body instanceof MultiValueMap<?, ?>);
		MultiValueMap<?, ?> map = (MultiValueMap<?, ?>) body;

		List<?> aValue = map.get("a");
		assertEquals(1, aValue.size());
		Object value = aValue.get(0);
		assertTrue(value.getClass().isArray());
		int[] y = (int[]) value;
		assertEquals(1, y[0]);
		assertEquals(2, y[1]);
		assertEquals(3, y[2]);

		List<?> bValue = map.get("b");
		assertEquals(1, bValue.size());
		assertEquals("4", bValue.get(0));

		List<?> cValue = map.get("c");
		assertEquals(1, cValue.size());
		assertEquals("5", cValue.get(0));

		List<?> dValue = map.get("d");
		assertEquals(1, dValue.size());
		assertEquals("6", dValue.get(0));
		assertEquals(MediaType.MULTIPART_FORM_DATA, request.getHeaders().getContentType());
	}

	@Test
	public void stringKeyNullArrayValueMixedFormData() throws Exception {
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"http://www.springsource.org/spring-integration");
		MockRestTemplate template = new MockRestTemplate();
		new DirectFieldAccessor(handler).setPropertyValue("restTemplate", template);
		handler.setHttpMethod(HttpMethod.POST);
		setBeanFactory(handler);
		handler.afterPropertiesSet();
		Map<String, Object> form = new LinkedHashMap<String, Object>();
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
		assertEquals("intentional", exception.getCause().getMessage());
		HttpEntity<?> request = template.lastRequestEntity.get();
		Object body = request.getBody();
		assertTrue(body instanceof MultiValueMap<?, ?>);
		MultiValueMap<?, ?> map = (MultiValueMap<?, ?>) body;

		List<?> aValue = map.get("a");
		assertEquals(3, aValue.size());
		assertNull(aValue.get(0));
		assertEquals(4, aValue.get(1));
		assertNull(aValue.get(2));

		List<?> bValue = map.get("b");
		assertEquals(1, bValue.size());
		assertEquals("4", bValue.get(0));

		assertEquals(MediaType.MULTIPART_FORM_DATA, request.getHeaders().getContentType());
	}

	/**
	 * This test and the one below might look identical, but they are not. This test
	 * injected "5" into the list as String resulting in the Content-TYpe being
	 * application/x-www-form-urlencoded
	 */
	@Test
	public void stringKeyNullCollectionValueMixedFormDataString() throws Exception {
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"http://www.springsource.org/spring-integration");
		MockRestTemplate template = new MockRestTemplate();
		new DirectFieldAccessor(handler).setPropertyValue("restTemplate", template);
		handler.setHttpMethod(HttpMethod.POST);
		setBeanFactory(handler);
		handler.afterPropertiesSet();
		Map<String, Object> form = new LinkedHashMap<String, Object>();
		List<Object> list = new ArrayList<Object>();
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
		assertEquals("intentional", exception.getCause().getMessage());
		HttpEntity<?> request = template.lastRequestEntity.get();
		Object body = request.getBody();
		assertTrue(body instanceof MultiValueMap<?, ?>);
		MultiValueMap<?, ?> map = (MultiValueMap<?, ?>) body;

		List<?> aValue = map.get("a");
		assertEquals(3, aValue.size());
		assertNull(aValue.get(0));
		assertEquals("5", aValue.get(1));
		assertNull(aValue.get(2));

		List<?> bValue = map.get("b");
		assertEquals(1, bValue.size());
		assertEquals("4", bValue.get(0));

		assertEquals(MediaType.APPLICATION_FORM_URLENCODED, request.getHeaders().getContentType());
	}

	/**
	 * This test and the one above might look identical, but they are not. This test
	 * injected 5 into the list as int resulting in Content-type being multipart/form-data
	 */
	@Test
	public void stringKeyNullCollectionValueMixedFormDataObject() throws Exception {
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"http://www.springsource.org/spring-integration");
		MockRestTemplate template = new MockRestTemplate();
		new DirectFieldAccessor(handler).setPropertyValue("restTemplate", template);
		handler.setHttpMethod(HttpMethod.POST);
		setBeanFactory(handler);
		handler.afterPropertiesSet();
		Map<String, Object> form = new LinkedHashMap<String, Object>();
		List<Object> list = new ArrayList<Object>();
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
		assertEquals("intentional", exception.getCause().getMessage());
		HttpEntity<?> request = template.lastRequestEntity.get();
		Object body = request.getBody();
		assertTrue(body instanceof MultiValueMap<?, ?>);
		MultiValueMap<?, ?> map = (MultiValueMap<?, ?>) body;

		List<?> aValue = map.get("a");
		assertEquals(3, aValue.size());
		assertNull(aValue.get(0));
		assertEquals(5, aValue.get(1));
		assertNull(aValue.get(2));

		List<?> bValue = map.get("b");
		assertEquals(1, bValue.size());
		assertEquals("4", bValue.get(0));

		assertEquals(MediaType.MULTIPART_FORM_DATA, request.getHeaders().getContentType());
	}

	@Test
	public void stringKeyStringCollectionValueFormData() throws Exception {
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"http://www.springsource.org/spring-integration");
		MockRestTemplate template = new MockRestTemplate();
		new DirectFieldAccessor(handler).setPropertyValue("restTemplate", template);
		handler.setHttpMethod(HttpMethod.POST);
		setBeanFactory(handler);
		handler.afterPropertiesSet();
		Map<String, Object> form = new LinkedHashMap<String, Object>();
		List<String> listA = new ArrayList<String>();
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
		assertEquals("intentional", exception.getCause().getMessage());
		HttpEntity<?> request = template.lastRequestEntity.get();
		Object body = request.getBody();
		assertTrue(body instanceof MultiValueMap<?, ?>);
		MultiValueMap<?, ?> map = (MultiValueMap<?, ?>) body;

		List<?> aValue = map.get("a");
		assertEquals(2, aValue.size());
		assertEquals("1", aValue.get(0));
		assertEquals("2", aValue.get(1));

		List<?> bValue = map.get("b");
		assertEquals(0, bValue.size());

		List<?> cValue = map.get("c");
		assertEquals(1, cValue.size());
		assertEquals("3", cValue.get(0));

		assertEquals(MediaType.APPLICATION_FORM_URLENCODED, request.getHeaders().getContentType());
	}

	@Test
	public void stringKeyObjectCollectionValueFormData() throws Exception {
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"http://www.springsource.org/spring-integration");
		MockRestTemplate template = new MockRestTemplate();
		new DirectFieldAccessor(handler).setPropertyValue("restTemplate", template);
		handler.setHttpMethod(HttpMethod.POST);
		setBeanFactory(handler);
		handler.afterPropertiesSet();
		Map<String, Object> form = new LinkedHashMap<String, Object>();
		List<Object> listA = new ArrayList<Object>();
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
		assertEquals("intentional", exception.getCause().getMessage());
		HttpEntity<?> request = template.lastRequestEntity.get();
		Object body = request.getBody();
		assertTrue(body instanceof MultiValueMap<?, ?>);
		MultiValueMap<?, ?> map = (MultiValueMap<?, ?>) body;

		List<?> aValue = map.get("a");
		assertEquals(2, aValue.size());
		assertEquals("Philadelphia", aValue.get(0).toString());
		assertEquals("Ambler", aValue.get(1).toString());

		List<?> bValue = map.get("b");
		assertEquals(0, bValue.size());

		List<?> cValue = map.get("c");
		assertEquals(1, cValue.size());
		assertEquals("Mohnton", cValue.get(0).toString());

		assertEquals(MediaType.MULTIPART_FORM_DATA, request.getHeaders().getContentType());
	}

	@Test
	public void nameOnlyWithNullValues() throws Exception {
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"http://www.springsource.org/spring-integration");
		MockRestTemplate template = new MockRestTemplate();
		new DirectFieldAccessor(handler).setPropertyValue("restTemplate", template);
		handler.setHttpMethod(HttpMethod.POST);
		setBeanFactory(handler);
		handler.afterPropertiesSet();
		Map<String, Object> form = new LinkedHashMap<String, Object>();
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
		assertEquals("intentional", exception.getCause().getMessage());
		HttpEntity<?> request = template.lastRequestEntity.get();
		Object body = request.getBody();
		assertTrue(body instanceof MultiValueMap<?, ?>);
		MultiValueMap<?, ?> map = (MultiValueMap<?, ?>) body;
		assertTrue(map.containsKey("a"));
		assertTrue(map.get("a").size() == 1);
		assertNull(map.get("a").get(0));
		List<?> entryB = map.get("b");
		assertEquals("foo", entryB.get(0));
		assertTrue(map.containsKey("c"));
		assertTrue(map.get("c").size() == 1);
		assertNull(map.get("c").get(0));
		assertEquals(MediaType.APPLICATION_FORM_URLENCODED, request.getHeaders().getContentType());
	}

	@SuppressWarnings("cast")
	@Test
	public void contentAsByteArray() throws Exception {
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"http://www.springsource.org/spring-integration");
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
		assertEquals("intentional", exception.getCause().getMessage());
		HttpEntity<?> request = template.lastRequestEntity.get();
		Object body = request.getBody();
		assertTrue(body instanceof byte[]);
		assertEquals("Hello World", new String(bytes));
		assertEquals(MediaType.APPLICATION_OCTET_STREAM, request.getHeaders().getContentType());
	}

	@Test
	public void contentAsXmlSource() throws Exception {
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"http://www.springsource.org/spring-integration");
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
		assertEquals("intentional", exception.getCause().getMessage());
		HttpEntity<?> request = template.lastRequestEntity.get();
		Object body = request.getBody();
		assertTrue(body instanceof Source);
		assertEquals(MediaType.TEXT_XML, request.getHeaders().getContentType());
	}

	@Test // no assertions just a warn message in a log
	public void testWarnMessageForNonPostPutAndExtractPayload() throws Exception {
		// should see a warn message

		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"http://www.springsource.org/spring-integration");
		MockRestTemplate template = new MockRestTemplate();
		new DirectFieldAccessor(handler).setPropertyValue("restTemplate", template);
		handler.setHttpMethod(HttpMethod.GET);
		handler.setExtractPayload(true);
		setBeanFactory(handler);
		handler.afterPropertiesSet();

		// should not see a warn message since 'setExtractPayload' is not set explicitly

		handler = new HttpRequestExecutingMessageHandler("http://www.springsource.org/spring-integration");
		template = new MockRestTemplate();
		new DirectFieldAccessor(handler).setPropertyValue("restTemplate", template);
		handler.setHttpMethod(HttpMethod.GET);
		setBeanFactory(handler);
		handler.afterPropertiesSet();

		// should not see a warn message since HTTP method is not GET

		handler = new HttpRequestExecutingMessageHandler("http://www.springsource.org/spring-integration");
		template = new MockRestTemplate();
		new DirectFieldAccessor(handler).setPropertyValue("restTemplate", template);
		handler.setHttpMethod(HttpMethod.POST);
		handler.setExtractPayload(true);
		setBeanFactory(handler);
		handler.afterPropertiesSet();
	}

	@Test
	public void contentTypeIsNotSetForGetAndHeadRequest() throws Exception {
		// GET
		HttpRequestExecutingMessageHandler handler =
				new HttpRequestExecutingMessageHandler("http://www.springsource.org/spring-integration");
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
			assertEquals("intentional", e.getCause().getMessage());
			assertNull(template.lastRequestEntity.get().getHeaders().getContentType());
		}

		//HEAD
		handler.setHttpMethod(HttpMethod.HEAD);

		message = MessageBuilder.withPayload(mock(Source.class)).build();
		try {
			handler.handleMessage(message);
			fail("An Exception expected");
		}
		catch (Exception e) {
			assertEquals("intentional", e.getCause().getMessage());
			assertNull(template.lastRequestEntity.get().getHeaders().getContentType());
		}


		//DELETE
		handler.setHttpMethod(HttpMethod.DELETE);

		message = MessageBuilder.withPayload(mock(Source.class)).build();
		try {
			handler.handleMessage(message);
			fail("An Exception expected");
		}
		catch (Exception e) {
			assertEquals("intentional", e.getCause().getMessage());
			assertEquals(MediaType.TEXT_XML, template.lastRequestEntity.get().getHeaders().getContentType());
		}

		//TRACE
		handler.setHttpMethod(HttpMethod.TRACE);

		message = MessageBuilder.withPayload(mock(Source.class)).build();
		try {
			handler.handleMessage(message);
			fail("An Exception expected");
		}
		catch (Exception e) {
			assertEquals("intentional", e.getCause().getMessage());
			assertNull(template.lastRequestEntity.get().getHeaders().getContentType());
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
		assertThat(TestUtils.getPropertyValue(handler, "trustedSpel"), equalTo(Boolean.TRUE));
		ctx.close();
	}

	@Test // INT-1029
	public void testHttpOutboundGatewayWithinChain() throws IOException, URISyntaxException {
		ConfigurableApplicationContext ctx = new ClassPathXmlApplicationContext(
				"HttpOutboundWithinChainTests-context.xml", this.getClass());
		MessageChannel channel = ctx.getBean("httpOutboundGatewayWithinChain", MessageChannel.class);
		RestTemplate restTemplate = ctx.getBean("restTemplate", RestTemplate.class);
		channel.send(MessageBuilder.withPayload("test").build());

		PollableChannel output = ctx.getBean("replyChannel", PollableChannel.class);
		Message<?> receive = output.receive();
		assertEquals(HttpStatus.OK, ((ResponseEntity<?>) receive.getPayload()).getStatusCode());
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
		String theURL = "http://bar/baz?foo#bar";
		Message<?> message = MessageBuilder.withPayload("").setHeader("foo", theURL).build();
		try {
			handler.handleRequestMessage(message);
		}
		catch (Exception e) {
		}
		assertEquals(theURL, restTemplate.actualUrl.get());
	}

	@Test
	public void testInt2455UriNotEncoded() {
		MockRestTemplate restTemplate = new MockRestTemplate();
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				new SpelExpressionParser().parseExpression("'http://my.RabbitMQ.com/api/' + payload"), restTemplate);
		handler.setEncodeUri(false);
		setBeanFactory(handler);
		handler.afterPropertiesSet();
		Message<?> message = MessageBuilder.withPayload("queues/%2f/si.test.queue?foo#bar").build();
		try {
			handler.handleRequestMessage(message);
		}
		catch (Exception e) {
		}
		assertEquals("http://my.RabbitMQ.com/api/queues/%2f/si.test.queue?foo#bar", restTemplate.actualUrl.get());
	}

	@Test
	public void acceptHeaderForSerializableResponse() throws Exception {
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"http://www.springsource.org/spring-integration");
		handler.setHttpMethod(HttpMethod.GET);
		handler.setExpectedResponseType(Foo.class);

		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
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
		assertTrue(requestHeaders.getAccept() != null);
		assertTrue(requestHeaders.getAccept().size() > 0);
		assertEquals("404 Not Found", exception.getCause().getMessage());
		List<MediaType> accept = requestHeaders.getAccept();
		assertTrue(accept != null && accept.size() > 0);
		assertEquals("application", accept.get(0).getType());
		assertEquals("x-java-serialized-object", accept.get(0).getSubtype());
	}

	@Test
	public void acceptHeaderForSerializableResponseMessageExchange() throws Exception {
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler(
				"http://www.springsource.org/spring-integration");

		handler.setHttpMethod(HttpMethod.GET);
		handler.setExtractPayload(false);
		handler.setExpectedResponseType(GenericMessage.class);

		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
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
		assertTrue(requestHeaders.getAccept() != null);
		assertTrue(requestHeaders.getAccept().size() > 0);
		assertEquals("404 Not Found", exception.getCause().getMessage());
		List<MediaType> accept = requestHeaders.getAccept();
		assertTrue(accept != null && accept.size() > 0);
		assertEquals("application", accept.get(0).getType());
		assertEquals("x-java-serialized-object", accept.get(0).getSubtype());
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

		private final AtomicReference<HttpEntity<?>> lastRequestEntity = new AtomicReference<HttpEntity<?>>();

		private final AtomicReference<String> actualUrl = new AtomicReference<String>();

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
