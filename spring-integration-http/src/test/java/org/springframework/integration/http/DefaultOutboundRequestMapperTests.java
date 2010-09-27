/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.Message;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * @author Mark Fisher
 */
public class DefaultOutboundRequestMapperTests {

	@Test
	public void simpleStringValueFormData() throws Exception {
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler("http://www.springsource.org/spring-integration");
		MockRestTemplate template = new MockRestTemplate();
		new DirectFieldAccessor(handler).setPropertyValue("restTemplate", template);
		handler.setHttpMethod(HttpMethod.POST);
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
		assertTrue(body instanceof MultiValueMap<?, ?>);
		MultiValueMap<?, ?> map = (MultiValueMap <?, ?>) body;
		assertEquals("1", map.get("a").iterator().next());
		assertEquals("2", map.get("b").iterator().next());
		assertEquals("3", map.get("c").iterator().next());
		assertEquals(MediaType.APPLICATION_FORM_URLENCODED, request.getHeaders().getContentType());
	}

	@Test
	public void stringArrayValueFormData() throws Exception {
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler("http://www.springsource.org/spring-integration");
		MockRestTemplate template = new MockRestTemplate();
		new DirectFieldAccessor(handler).setPropertyValue("restTemplate", template);
		handler.setHttpMethod(HttpMethod.POST);
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
		MultiValueMap<?, ?> map = (MultiValueMap <?, ?>) body;
		
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
	public void listValueFormData() throws Exception {
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler("http://www.springsource.org/spring-integration");
		MockRestTemplate template = new MockRestTemplate();
		new DirectFieldAccessor(handler).setPropertyValue("restTemplate", template);
		handler.setHttpMethod(HttpMethod.POST);
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
		MultiValueMap<?, ?> map = (MultiValueMap <?, ?>) body;
		
		
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
	public void nameOnlyWithNullValues() throws Exception {
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler("http://www.springsource.org/spring-integration");
		MockRestTemplate template = new MockRestTemplate();
		new DirectFieldAccessor(handler).setPropertyValue("restTemplate", template);
		handler.setHttpMethod(HttpMethod.POST);
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

	@Test
	public void nonFormDataInMap() throws Exception {
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler("http://www.springsource.org/spring-integration");
		MockRestTemplate template = new MockRestTemplate();
		new DirectFieldAccessor(handler).setPropertyValue("restTemplate", template);
		handler.setHttpMethod(HttpMethod.POST);
		Map<String, TestBean> form = new LinkedHashMap<String, TestBean>();
		form.put("A", new TestBean());
		form.put("B", new TestBean());
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
		MultiValueMap<?, ?> map = (MultiValueMap<?, ?>) request.getBody();
		assertEquals(2, map.size());
		assertEquals(TestBean.class, map.get("A").get(0).getClass());
		assertEquals(TestBean.class, map.get("B").get(0).getClass());
		assertEquals(MediaType.MULTIPART_FORM_DATA, request.getHeaders().getContentType());
	}
	@Test
	public void nonFormAndNonMultipartDataInMap() throws Exception {
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler("http://www.springsource.org/spring-integration");
		MockRestTemplate template = new MockRestTemplate();
		new DirectFieldAccessor(handler).setPropertyValue("restTemplate", template);
		handler.setHttpMethod(HttpMethod.POST);
		Map<Object, TestBean> form = new LinkedHashMap<Object, TestBean>();
		form.put(1, new TestBean());
		form.put(2, new TestBean());
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
		MultiValueMap<?, ?> map = (MultiValueMap<?, ?>) request.getBody();
		assertEquals(2, map.size());
		assertEquals(TestBean.class, map.get(1).get(0).getClass());
		assertEquals(TestBean.class, map.get(2).get(0).getClass());
		System.out.println(request.getHeaders().getContentType());
		assertEquals("application", request.getHeaders().getContentType().getType());
		assertEquals("x-java-serialized-object", request.getHeaders().getContentType().getSubtype());
	}


	@SuppressWarnings("serial")
	private static class TestBean implements Serializable {
	}


	private static class MockRestTemplate extends RestTemplate {

		private final AtomicReference<HttpEntity<?>> lastRequestEntity = new AtomicReference<HttpEntity<?>>();

		@Override
		public <T> ResponseEntity<T> exchange(String url, HttpMethod method, HttpEntity<?> requestEntity,
				Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException {
			this.lastRequestEntity.set(requestEntity);
			throw new RuntimeException("intentional");
		}
	}

}
