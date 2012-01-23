/*
 * Copyright 2002-2012 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.http.MockHttpServletRequest;
import org.springframework.integration.http.inbound.HttpRequestHandlingController;
import org.springframework.integration.http.inbound.HttpRequestHandlingMessagingGateway;
import org.springframework.integration.http.support.DefaultHttpHeaderMapper;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.ExpectedException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.MultiValueMap;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class HttpInboundChannelAdapterParserTests {

	@Autowired
	private PollableChannel requests;

	@Autowired
	private HttpRequestHandlingMessagingGateway defaultAdapter;

	@Autowired
	private HttpRequestHandlingMessagingGateway postOnlyAdapter;

	@Autowired
	private HttpRequestHandlingMessagingGateway putOrDeleteAdapter;
	
	@Autowired
	private HttpRequestHandlingMessagingGateway withMappedHeaders;
	
	@Autowired
	private HttpRequestHandlingMessagingGateway inboundAdapterWithExpressions;
	
	@Autowired
	@Qualifier("/fname/{blah}/lname/{boo}")
	private HttpRequestHandlingMessagingGateway inboundAdapterWithNameAndExpressions;
	
	@Autowired
	@Qualifier("/fname/{f}/lname/{l}")
	private HttpRequestHandlingMessagingGateway inboundAdapterWithNameNoPath;
	
	@Autowired
	private HttpRequestHandlingController inboundController;

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
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
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
		request.setRequestURI("/fname/bill/lname/clinton");
		
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

	@Test // ensure that 'path' takes priority over name
	// INT-1677
	public void withNameAndExpressionsAndPath() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("text/plain");
		request.setParameter("foo", "bar");
		request.setContent("hello".getBytes());
		request.setRequestURI("/fname/bill/lname/clinton");
		
		MockHttpServletResponse response = new MockHttpServletResponse();
		inboundAdapterWithNameAndExpressions.handleRequest(request, response);
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		Message<?> message = requests.receive(0);
		assertNotNull(message);
		Object payload = message.getPayload();
		assertTrue(payload instanceof String);
		assertEquals("bill", payload);
		assertEquals("clinton", message.getHeaders().get("lname"));
	}
	
	@Test
	// INT-1677
	@ExpectedException(SpelEvaluationException.class)
	public void withNameAndExpressionsNoPath() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("text/plain");
		request.setParameter("foo", "bar");
		request.setContent("hello".getBytes());
		request.setRequestURI("/fname/bill/lname/clinton");
		
		MockHttpServletResponse response = new MockHttpServletResponse();
		inboundAdapterWithNameNoPath.handleRequest(request, response);
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		Message<?> message = requests.receive(0);
		assertNotNull(message);
		Object payload = message.getPayload();
		assertTrue(payload instanceof String);
		assertEquals("hello", payload); // default payload
		assertNull(message.getHeaders().get("lname"));
	}
	
	

	@Test
	public void getRequestNotAllowed() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.setParameter("foo", "bar");
		MockHttpServletResponse response = new MockHttpServletResponse();
		postOnlyAdapter.handleRequest(request, response);
		assertEquals(HttpServletResponse.SC_METHOD_NOT_ALLOWED, response.getStatus());
		Message<?> message = requests.receive(0);
		assertNull(message);
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

	@Test
	public void postRequestWithSerializedObjectContentOk() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		Object obj = new TestObject("testObject");
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		new ObjectOutputStream(byteStream).writeObject(obj);
		request.setContent(byteStream.toByteArray());
		request.setContentType("application/x-java-serialized-object");
		MockHttpServletResponse response = new MockHttpServletResponse();
		postOnlyAdapter.handleRequest(request, response);
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		Message<?> message = requests.receive(0);
		assertNotNull(message);
		assertTrue(message.getPayload() instanceof TestObject);
		assertEquals("testObject", ((TestObject) message.getPayload()).text);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void putOrDeleteMethodsSupported() throws Exception {
		DirectFieldAccessor accessor = new DirectFieldAccessor(putOrDeleteAdapter);
		List<String> supportedMethods = (List<String>) accessor.getPropertyValue("supportedMethods");
		assertEquals(2, supportedMethods.size());
		assertTrue(supportedMethods.contains(HttpMethod.PUT));
		assertTrue(supportedMethods.contains(HttpMethod.DELETE));
	}

	@Test
	public void testController() throws Exception {
		DirectFieldAccessor accessor = new DirectFieldAccessor(inboundController);
		String errorCode =  (String) accessor.getPropertyValue("errorCode");
		assertEquals("oops", errorCode);
	}

	@Test
	public void testAutoChannel() {
		assertSame(autoChannel, TestUtils.getPropertyValue(autoChannelAdapter, "requestChannel"));
	}

	@SuppressWarnings("serial")
	private static class TestObject implements Serializable {

		String text;

		TestObject(String text) {
			this.text = text;
		}
	}

}
