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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
//import org.springframework.http.converter.feed.AtomFeedHttpMessageConverter;
//import org.springframework.http.converter.feed.RssChannelHttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.http.converter.xml.XmlAwareFormHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageHeaders;
import org.springframework.integration.core.MessagingException;
import org.springframework.integration.gateway.AbstractMessagingGateway;
import org.springframework.integration.message.HeaderMapper;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.HttpRequestHandler;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class HttpRequestHandlingMessagingGateway extends AbstractMessagingGateway implements HttpRequestHandler {

	private static final boolean jaxb2Present =
			ClassUtils.isPresent("javax.xml.bind.Binder", HttpRequestHandlingMessagingGateway.class.getClassLoader());

	private static final boolean jacksonPresent =
			ClassUtils.isPresent("org.codehaus.jackson.map.ObjectMapper", HttpRequestHandlingMessagingGateway.class.getClassLoader()) &&
					ClassUtils.isPresent("org.codehaus.jackson.JsonGenerator", HttpRequestHandlingMessagingGateway.class.getClassLoader());

	private static boolean romePresent =
			ClassUtils.isPresent("com.sun.syndication.feed.WireFeed", HttpRequestHandlingMessagingGateway.class.getClassLoader());


	private volatile Class<?> expectedType = byte[].class;

	private final List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();

	private volatile HeaderMapper<HttpHeaders> headerMapper = new DefaultHeaderMapper();

	private final boolean expectReply;


	public HttpRequestHandlingMessagingGateway() {
		this(true);
	}

	@SuppressWarnings("unchecked")
	public HttpRequestHandlingMessagingGateway(boolean expectReply) {
		this.expectReply = expectReply;
		this.messageConverters.add(new SerializingHttpMessageConverter());
		this.messageConverters.add(new ByteArrayHttpMessageConverter());
		this.messageConverters.add(new StringHttpMessageConverter());
		this.messageConverters.add(new ResourceHttpMessageConverter());
		this.messageConverters.add(new SourceHttpMessageConverter());
		this.messageConverters.add(new XmlAwareFormHttpMessageConverter());
		if (jaxb2Present) {
			this.messageConverters.add(new Jaxb2RootElementHttpMessageConverter());
		}
		if (jacksonPresent) {
			this.messageConverters.add(new MappingJacksonHttpMessageConverter());
		}
		if (romePresent) {
			// TODO add deps for:
			//this.messageConverters.add(new AtomFeedHttpMessageConverter());
			//this.messageConverters.add(new RssChannelHttpMessageConverter());
		}
	}


	public void setExpectedType(Class<?> expectedType) {
		Assert.notNull(expectedType, "expectedType must not be null");
		this.expectedType = expectedType;
	}

	public final void handleRequest(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws ServletException, IOException {
		this.doHandleRequest(new ServletServerHttpRequest(servletRequest), new ServletServerHttpResponse(servletResponse));
	}

	private void doHandleRequest(ServletServerHttpRequest request, ServletServerHttpResponse response) throws IOException {
		Object payload = this.readPayload(request);
		Map<String, ?> headers = this.headerMapper.toHeaders(request.getHeaders());
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		if (this.expectReply) {
			Message<?> reply = this.sendAndReceiveMessage(message);
			this.headerMapper.fromHeaders(reply.getHeaders(), response.getHeaders());
			if (reply.getPayload() != null) {
				this.writePayload(reply.getPayload(), response, request.getHeaders().getAccept());
			}
		}
		else {
			this.send(message);
			// will be a status response for now... add an optional ResponseGenerator strategy?
		}
	}

	@SuppressWarnings("unchecked")
	private Object readPayload(ServletServerHttpRequest request) throws IOException {
		MediaType contentType = request.getHeaders().getContentType();
		for (HttpMessageConverter<?> converter : this.messageConverters) {
			if (converter.canRead(this.expectedType, contentType)) {
				return converter.read((Class) this.expectedType, request);
			}
		}
		throw new MessagingException("Could not convert request: no suitable HttpMessageConverter found for expected type [" +
				this.expectedType.getName() + "] and content type [" + contentType + "]");
	}

	@SuppressWarnings("unchecked")
	private void writePayload(Object payload, ServletServerHttpResponse response, List<MediaType> acceptTypes) throws IOException {
		for (HttpMessageConverter converter : this.messageConverters) {
			for (MediaType acceptType : acceptTypes) {
				if (converter.canWrite(payload.getClass(), acceptType)) {
					converter.write(payload, acceptType, response);
					return;
				}
			}
		}
		throw new MessagingException("Could not convert reply: no suitable HttpMessageConverter found for result type [" +
				payload.getClass().getName() + "] and content types [" + acceptTypes + "]");
	}


	private static class DefaultHeaderMapper implements HeaderMapper<HttpHeaders> {

		public void fromHeaders(MessageHeaders headers, HttpHeaders target) {
			for (String name : headers.keySet()) {
				Object value = headers.get(name);
				if (value instanceof String) {
					target.add(name, (String) value);
				}
			}
		}

		public Map<String, ?> toHeaders(HttpHeaders source) {
			return source;
		}
	}

}
