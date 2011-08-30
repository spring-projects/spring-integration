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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.ConversionServiceFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;
import org.springframework.http.HttpHeaders;
import org.springframework.integration.Message;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.http.MockHttpServletRequest;
import org.springframework.integration.http.inbound.HttpRequestHandlingEndpointSupport.SmartServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.CollectionUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
	/**
	 * This is a temporary test which simply shows what would happen inside of the gateway which has an internal 
	 * capability for SpEL based extraction of data such as UriVariabe mappings, HttpHeaders, Body and Request Parameters.
	 * 
	 * @throws Exception
	 */
	@Test
	public void withExpression() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("text/plain");
		request.setParameter("foo", "bar");
		request.setContent("hello".getBytes());
		request.setRequestURI("/fname/bill/lname/clinton");
		
		HttpRequestHandlingMessagingGateway gateway = new HttpRequestHandlingMessagingGateway(true);
		gateway.setPath("/fname/{f}/lname/{l}");
		
		SmartServletServerHttpRequest smartRequest = gateway.new SmartServletServerHttpRequest(request);
		
		StandardEvaluationContext context = new StandardEvaluationContext();
		ConversionService conversionService = ConversionServiceFactory.createDefaultConversionService();
		
		context.setTypeConverter(new StandardTypeConverter(conversionService));
			
		ExpressionParser parser = new SpelExpressionParser();
		
		Expression bodyExpression = parser.parseExpression("#this.body");
		InputStream body = bodyExpression.getValue(context, smartRequest, InputStream.class);
		assertNotNull(body);
		assertEquals("hello", this.convertToString(body));
		
		Expression headersExpression = parser.parseExpression("#this.headers['Content-Type']");
		List<?> headers = headersExpression.getValue(context, smartRequest, List.class);
		assertNotNull(headers);
		assertEquals("text/plain", headers.get(0));
		
		Expression uriVarsExpression = parser.parseExpression("#this.uriVars");
		Map uriVars = uriVarsExpression.getValue(context, smartRequest, Map.class);
		assertNotNull(uriVars);
		assertTrue(!CollectionUtils.isEmpty(uriVars));
		assertEquals("bill", "f");
		assertEquals("bill", "f");
		
	}
	
	public String convertToString(InputStream in) throws IOException {
	    StringBuffer out = new StringBuffer();
	    byte[] b = new byte[4096];
	    for (int n; (n = in.read(b)) != -1;) {
	        out.append(new String(b, 0, n));
	    }
	    return out.toString();
	}
	
}
