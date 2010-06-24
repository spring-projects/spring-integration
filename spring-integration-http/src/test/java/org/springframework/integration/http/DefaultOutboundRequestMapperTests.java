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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageBuilder;

/**
 * @author Mark Fisher
 */
public class DefaultOutboundRequestMapperTests {

	@Test
	public void simpleStringValueFormData() throws Exception {
		DefaultOutboundRequestMapper mapper = new DefaultOutboundRequestMapper();
		mapper.setHttpMethod(HttpMethod.POST);
		Map<String, String> form = new LinkedHashMap<String, String>();
		form.put("a", "1");
		form.put("b", "2");
		form.put("c", "3");
		Message<?> message = MessageBuilder.withPayload(form).build();
		HttpEntity<?> request = mapper.fromMessage(message);
		Object body = request.getBody();
		assertTrue(body instanceof Map<?, ?>);
		Map<?, ?> map = (Map <?, ?>) body;
		assertEquals("1", map.get("a"));
		assertEquals("2", map.get("b"));
		assertEquals("3", map.get("c"));
		assertEquals(MediaType.APPLICATION_FORM_URLENCODED, request.getHeaders().getContentType());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void stringArrayValueFormData() throws Exception {
		DefaultOutboundRequestMapper mapper = new DefaultOutboundRequestMapper();
		mapper.setHttpMethod(HttpMethod.POST);
		Map form = new LinkedHashMap();
		form.put("a", new String[] { "1", "2", "3" });
		form.put("b", "4");
		form.put("c", new String[] { "5" });
		form.put("d", "6");
		Message<?> message = MessageBuilder.withPayload(form).build();
		HttpEntity<?> request = mapper.fromMessage(message);
		Object body = request.getBody();
		assertTrue(body instanceof Map<?, ?>);
		Map<?, ?> map = (Map <?, ?>) body;
		Object entryA = map.get("a");
		assertEquals(String[].class, entryA.getClass());
		String[] resultA = (String[]) entryA;
		assertEquals(3, resultA.length);
		assertEquals("1", resultA[0]);
		assertEquals("2", resultA[1]);
		assertEquals("3", resultA[2]);
		Object entryB = map.get("b");
		assertEquals(String.class, entryB.getClass());
		assertEquals("4", entryB);
		Object entryC = map.get("c");
		assertEquals(String[].class, entryC.getClass());
		String[] resultC = (String[]) entryC;
		assertEquals(1, resultC.length);		
		assertEquals("5", resultC[0]);
		Object entryD = map.get("d");
		assertEquals(String.class, entryD.getClass());
		assertEquals("6", entryD);
		assertEquals(MediaType.APPLICATION_FORM_URLENCODED, request.getHeaders().getContentType());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void listValueFormData() throws Exception {
		DefaultOutboundRequestMapper mapper = new DefaultOutboundRequestMapper();
		mapper.setHttpMethod(HttpMethod.POST);
		Map form = new LinkedHashMap();
		List<String> listA = new ArrayList<String>();
		listA.add("1");
		listA.add("2");
		form.put("a", listA);
		form.put("b", Collections.EMPTY_LIST);
		form.put("c", Collections.singletonList("3"));
		Message<?> message = MessageBuilder.withPayload(form).build();
		HttpEntity<?> request = mapper.fromMessage(message);
		Object body = request.getBody();
		assertTrue(body instanceof Map<?, ?>);
		Map<?, ?> map = (Map <?, ?>) body;
		Object entryA = map.get("a");
		assertTrue(entryA instanceof List<?>);
		List<?> resultA = (List<?>) entryA;
		assertEquals(2, resultA.size());
		assertEquals("1", resultA.get(0));
		assertEquals("2", resultA.get(1));
		Object entryB = map.get("b");
		assertTrue(entryB instanceof List<?>);
		List<?> resultB = (List<?>) entryB;
		assertEquals(0, resultB.size());
		Object entryC = map.get("c");
		assertTrue(entryC instanceof List<?>);
		List<?> resultC = (List<?>) entryC;
		assertEquals(1, resultC.size());		
		assertEquals("3", resultC.get(0));
		assertEquals(MediaType.APPLICATION_FORM_URLENCODED, request.getHeaders().getContentType());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void nameOnlyWithNullValues() throws Exception {
		DefaultOutboundRequestMapper mapper = new DefaultOutboundRequestMapper();
		mapper.setHttpMethod(HttpMethod.POST);
		Map form = new LinkedHashMap();
		form.put("a", null);
		form.put("b", "foo");
		form.put("c", null);
		Message<?> message = MessageBuilder.withPayload(form).build();
		HttpEntity<?> request = mapper.fromMessage(message);
		Object body = request.getBody();
		assertTrue(body instanceof Map<?, ?>);
		Map<?, ?> map = (Map<?, ?>) body;
		assertTrue(map.containsKey("a"));
		assertNull(map.get("a"));
		Object entryB = map.get("b");
		assertEquals("foo", entryB);
		assertTrue(map.containsKey("c"));
		assertNull(map.get("c"));
		assertEquals(MediaType.APPLICATION_FORM_URLENCODED, request.getHeaders().getContentType());
	}

	@Test
	public void nonFormDataInMap() throws Exception {
		DefaultOutboundRequestMapper mapper = new DefaultOutboundRequestMapper();
		mapper.setHttpMethod(HttpMethod.POST);
		Map<String, TestBean> form = new LinkedHashMap<String, TestBean>();
		form.put("A", new TestBean());
		form.put("B", new TestBean());
		Message<?> message = MessageBuilder.withPayload(form).build();
		HttpEntity<?> request = mapper.fromMessage(message);
		Map<?, ?> map = (Map<?, ?>) request.getBody();
		assertEquals(2, map.size());
		assertEquals(TestBean.class, map.get("A").getClass());
		assertEquals(TestBean.class, map.get("B").getClass());
	}


	@SuppressWarnings("serial")
	private static class TestBean implements Serializable {
	}

}
