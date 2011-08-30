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

package org.springframework.integration.http.inbound;

import java.util.Collections;
import java.util.Map;

import org.junit.Test;
import org.springframework.integration.Message;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.http.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Oleg Zhurakousky
 */
public class HttpRequestHandlingMessagingGatewayWithPathMappingTests {
	
	@Test
	public void defaultUriVariableMappingWithPOST() throws Exception {
		QueueChannel requestChannel = new QueueChannel();
		HttpRequestHandlingMessagingGateway gateway = new HttpRequestHandlingMessagingGateway(false);
		gateway.setPath("/fname/{f}/lname/{l}");
		gateway.setRequestChannel(requestChannel);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("text/plain");
		request.setParameter("foo", "bar");
		request.setContent("hello".getBytes());
		request.setRequestURI("/fname/bill/lname/clinton");
		MockHttpServletResponse response = new MockHttpServletResponse();
		gateway.handleRequest(request, response);
		Message<?> message = requestChannel.receive(0);
		assertNotNull(message);
		assertEquals("bill", message.getHeaders().get("f"));
		assertEquals("clinton", message.getHeaders().get("l"));
	}
	
	@SuppressWarnings("rawtypes")
	@Test
	public void defaultUriVariableMappingWithGET() throws Exception {
		QueueChannel requestChannel = new QueueChannel();
		HttpRequestHandlingMessagingGateway gateway = new HttpRequestHandlingMessagingGateway(false);
		gateway.setPath("/fname/{f}/lname/{l}");
		gateway.setRequestChannel(requestChannel);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.setParameter("foo", "bar");
		request.setRequestURI("/fname/bill/lname/clinton");
		MockHttpServletResponse response = new MockHttpServletResponse();
		gateway.handleRequest(request, response);
		Message<?> message = requestChannel.receive(0);
		assertNotNull(message);
		assertNull(message.getHeaders().get("f"));
		assertNull(message.getHeaders().get("l"));
		Map payload = (Map) message.getPayload();
		assertEquals(Collections.singletonList("bill"), payload.get("f"));
		assertEquals(Collections.singletonList("clinton"), payload.get("l"));
	}
	

}
