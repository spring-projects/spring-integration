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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpHeaders;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.http.inbound.HttpRequestHandlingController;
import org.springframework.integration.http.inbound.HttpRequestHandlingMessagingGateway;
import org.springframework.integration.http.support.DefaultHttpHeaderMapper;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import static org.springframework.integration.test.util.TestUtils.getPropertyValue;
import static org.springframework.integration.test.util.TestUtils.handlerExpecting;

/**
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @author Gary Russell
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class HttpInboundGatewayParserTests {
	
	@Autowired
	@Qualifier("inboundGateway")
	private HttpRequestHandlingMessagingGateway gateway;
	
	@Autowired
	@Qualifier("withMappedHeaders")
	private HttpRequestHandlingMessagingGateway withMappedHeaders;
	
	@Autowired
	@Qualifier("withMappedHeadersAndConverter")
	private HttpRequestHandlingMessagingGateway withMappedHeadersAndConverter;

	@Autowired
	private HttpRequestHandlingController inboundController;

	@Autowired
	private SubscribableChannel requests;

	@Autowired
	private PollableChannel responses;


	@Test
	public void checkConfig() {
		assertNotNull(gateway);
		assertThat((Boolean) getPropertyValue(gateway, "expectReply"), is(true));
		assertThat((Boolean) getPropertyValue(gateway, "convertExceptions"), is(true));
		assertThat((PollableChannel) getPropertyValue(gateway, "replyChannel"), is(responses));
		assertNotNull(TestUtils.getPropertyValue(gateway, "errorChannel"));
		MessagingTemplate messagingTemplate = TestUtils.getPropertyValue(
				gateway, "messagingTemplate", MessagingTemplate.class);
		assertEquals(Long.valueOf(1234), TestUtils.getPropertyValue(messagingTemplate, "sendTimeout"));
		assertEquals(Long.valueOf(4567), TestUtils.getPropertyValue(messagingTemplate, "receiveTimeout"));
	}
	
	@Test(timeout=1000)
	public void checkFlow() throws Exception {
		requests.subscribe(handlerExpecting(any(Message.class)));
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.addHeader("Accept", "application/x-java-serialized-object");
		request.setParameter("foo", "bar");
		MockHttpServletResponse response = new MockHttpServletResponse();
		gateway.handleRequest(request, response);
		assertThat(response.getStatus(), is(HttpServletResponse.SC_OK));
		assertThat(response.getContentType(), is("application/x-java-serialized-object"));
	}

	@Test
	public void testController() throws Exception {
		DirectFieldAccessor accessor = new DirectFieldAccessor(inboundController);
		String errorCode =  (String) accessor.getPropertyValue("errorCode");
		assertEquals("oops", errorCode);
	}
	
	@Test
	public void requestWithHeaders() throws Exception {
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
		
		Map<String, Object> mapOfHeaders = new HashMap<String, Object>();
		mapOfHeaders.put("abc", "abc");
		MessageHeaders mh = new MessageHeaders(mapOfHeaders);
		headers = new HttpHeaders();
		headerMapper.fromHeaders(mh, headers);
		assertTrue(headers.size() == 1);
		List<String> abc = headers.get("X-abc");
		assertEquals("abc", abc.get(0));
	}
	
	@Test
	public void requestWithHeadersWithConversionService() throws Exception {
		DefaultHttpHeaderMapper headerMapper = 
			(DefaultHttpHeaderMapper) TestUtils.getPropertyValue(withMappedHeadersAndConverter, "headerMapper");
		
		HttpHeaders headers = new HttpHeaders();
		headers.set("foo", "foo");
		headers.set("bar", "bar");
		headers.set("baz", "baz");
		Map<String, Object> map = headerMapper.toHeaders(headers);
		assertTrue(map.size() == 2);
		assertEquals("foo", map.get("foo"));
		assertEquals("bar", map.get("bar"));
		
		Map<String, Object> mapOfHeaders = new HashMap<String, Object>();
		mapOfHeaders.put("abc", "abc");
		Person person = new Person();
		person.setName("Oleg");
		mapOfHeaders.put("person", person);
		MessageHeaders mh = new MessageHeaders(mapOfHeaders);
		headers = new HttpHeaders();
		headerMapper.fromHeaders(mh, headers);
		assertTrue(headers.size() == 2);
		List<String> abc = headers.get("X-abc");
		assertEquals("abc", abc.get(0));
		List<String> personHeaders = headers.get("X-person");
		assertEquals("Oleg", personHeaders.get(0));
	}
	
	public static class Person{
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
	
	public static class PersonConverter implements Converter<Person, String>{

		public String convert(Person source) {
			return source.getName();
		}
		
	}

}
