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

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageBuilder;

/**
 * @author Mark Fisher
 */
public class DefaultOutboundRequestMapperTests {

	@Test
	public void simpleStringValueFormData() throws Exception {
		DefaultOutboundRequestMapper mapper = new DefaultOutboundRequestMapper(new URL("http://example.org"));
		Map<String, String> form = new LinkedHashMap<String, String>();
		form.put("a", "1");
		form.put("b", "2");
		form.put("c", "3");
		Message<?> message = MessageBuilder.withPayload(form).build();
		HttpRequest request = mapper.fromMessage(message);
		String bodyText = request.getBody().toString("UTF-8");
		assertEquals("a=1&b=2&c=3", bodyText);
		assertEquals("application/x-www-form-urlencoded", request.getContentType());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void stringArrayValueFormData() throws Exception {
		DefaultOutboundRequestMapper mapper = new DefaultOutboundRequestMapper(new URL("http://example.org"));
		Map form = new LinkedHashMap();
		form.put("a", new String[] { "1", "2", "3" });
		form.put("b", "4");
		form.put("c", new String[] { "5" });
		form.put("d", "6");
		Message<?> message = MessageBuilder.withPayload(form).build();
		HttpRequest request = mapper.fromMessage(message);
		String bodyText = request.getBody().toString("UTF-8");
		assertEquals("a=1&a=2&a=3&b=4&c=5&d=6", bodyText);
		assertEquals("application/x-www-form-urlencoded", request.getContentType());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void stringListValueFormData() throws Exception {
		DefaultOutboundRequestMapper mapper = new DefaultOutboundRequestMapper(new URL("http://example.org"));
		Map form = new LinkedHashMap();
		List<String> listA = new ArrayList<String>();
		listA.add("1");
		listA.add("2");
		form.put("a", listA);
		form.put("b", Collections.EMPTY_LIST);
		form.put("c", Collections.singletonList("3"));
		Message<?> message = MessageBuilder.withPayload(form).build();
		HttpRequest request = mapper.fromMessage(message);
		String bodyText = request.getBody().toString("UTF-8");
		assertEquals("a=1&a=2&b&c=3", bodyText);
		assertEquals("application/x-www-form-urlencoded", request.getContentType());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void nameOnlyWithNullValues() throws Exception {
		DefaultOutboundRequestMapper mapper = new DefaultOutboundRequestMapper(new URL("http://example.org"));
		Map form = new LinkedHashMap();
		form.put("a", null);
		form.put("b", "foo");
		form.put("c", null);
		Message<?> message = MessageBuilder.withPayload(form).build();
		HttpRequest request = mapper.fromMessage(message);
		String bodyText = request.getBody().toString("UTF-8");
		assertEquals("a&b=foo&c", bodyText);
		assertEquals("application/x-www-form-urlencoded", request.getContentType());
	}

	@Test
	public void encodedFormData() throws Exception {
		DefaultOutboundRequestMapper mapper = new DefaultOutboundRequestMapper(new URL("http://example.org"));
		Map<String, String> form = new LinkedHashMap<String, String>();
		form.put("a", "1 + 2 + 3");
		form.put("b", "4+5");
		form.put("c", "97%");
		Message<?> message = MessageBuilder.withPayload(form).build();
		HttpRequest request = mapper.fromMessage(message);
		String bodyText = request.getBody().toString("UTF-8");
		assertEquals("a=1+%2B+2+%2B+3&b=4%2B5&c=97%25", bodyText);
		assertEquals("application/x-www-form-urlencoded", request.getContentType());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void nonFormDataInMap() throws Exception {
		DefaultOutboundRequestMapper mapper = new DefaultOutboundRequestMapper(new URL("http://example.org"));
		Map<String, TestBean> form = new LinkedHashMap<String, TestBean>();
		form.put("A", new TestBean());
		form.put("B", new TestBean());
		Message<?> message = MessageBuilder.withPayload(form).build();
		HttpRequest request = mapper.fromMessage(message);
		byte[] body = request.getBody().toByteArray();
		ByteArrayInputStream byteStream = new ByteArrayInputStream(body);
		Object result = new ObjectInputStream(byteStream).readObject();
		assertEquals(LinkedHashMap.class, result.getClass());
		Map<String, TestBean> resultMap = (Map<String, TestBean>) result; 
		assertEquals(2, resultMap.size());
		assertEquals(TestBean.class, resultMap.get("A").getClass());
		assertEquals(TestBean.class, resultMap.get("B").getClass());
	}


	@SuppressWarnings("serial")
	private static class TestBean implements Serializable {
	}

}
