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

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Map;

import javax.xml.transform.Source;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.integration.core.Message;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link OutboundRequestMapper}.
 * 
 * @author Mark Fisher
 * @since 1.0.2
 */
public class DefaultOutboundRequestMapper implements OutboundRequestMapper {

	private volatile boolean extractPayload = true;

	private volatile ContentTypeResolver contentTypeResolver = new DefaultContentTypeResolver();

	private volatile String charset = "UTF-8";


	/**
	 * Specify whether the outbound message's payload should be extracted
	 * when preparing the request body. Otherwise the Message instance itself
	 * will be serialized. The default value is <code>true</code>.
	 */
	public void setExtractPayload(boolean extractPayload) {
		this.extractPayload = extractPayload;
	}

	/**
	 * Specify the charset name to use for converting String-typed payloads to
	 * bytes. The default is 'UTF-8'.
	 */
	public void setCharset(String charset) {
		Assert.isTrue(Charset.isSupported(charset), "unsupported charset '" + charset + "'");
		this.charset = charset;
	}

	public HttpEntity<?> fromMessage(Message<?> message) throws Exception {
		Assert.notNull(message, "message must not be null");
		return (this.extractPayload) ? this.createHttpEntityWithPayloadAsBody(message)
				: this.createHttpEntityWithMessageAsBody(message);
	}

	@SuppressWarnings("unchecked")
	private HttpEntity<?> createHttpEntityWithPayloadAsBody(Message<?> requestMessage) {
		if (requestMessage.getPayload() instanceof HttpEntity<?>) {
			return (HttpEntity<?>) requestMessage.getPayload();
		}
		// TODO: provide more fine-grained control over header mapping
		HttpHeaders httpHeaders = new HttpHeaders();
		for (String headerName : requestMessage.getHeaders().keySet()) {
			Object value = requestMessage.getHeaders().get(headerName);
			if (value instanceof String) {
				httpHeaders.add(headerName, (String) value);
			}
		}
		Object payload = requestMessage.getPayload();
		MediaType contentType = (payload instanceof String) ? this.contentTypeResolver.resolveContentType((String) payload, this.charset)
				: this.contentTypeResolver.resolveContentType(payload);
		httpHeaders.setContentType(contentType);
		return new HttpEntity(requestMessage.getPayload(), httpHeaders);
	}

	private HttpEntity<Object> createHttpEntityWithMessageAsBody(Message<?> requestMessage) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(new MediaType("application", "x-java-serialized-object"));
		return new HttpEntity<Object>(requestMessage, headers);
	}


	private static class DefaultContentTypeResolver implements ContentTypeResolver {

		@SuppressWarnings("unchecked")
		public MediaType resolveContentType(Object content) {
			MediaType contentType = null;
			if (content instanceof byte[]) {
				contentType = MediaType.APPLICATION_OCTET_STREAM;
			}
			else if (content instanceof Source) {
				contentType = MediaType.TEXT_XML;
			}
			else {
				if (content instanceof Map && isFormData((Map) content)) {
					contentType = MediaType.APPLICATION_FORM_URLENCODED;
				}
				if (contentType == null && content instanceof Serializable) {
					contentType = new MediaType("application", "x-java-serialized-object");
				}
			}
			if (contentType == null) {
				throw new IllegalArgumentException("payload must be a byte array, " +
						"String, Map, Source, or Serializable object, received: " + content.getClass());
			}
			return contentType;
		}

		public MediaType resolveContentType(String content, String charset) {
			return new MediaType("text", "plain", Charset.forName(charset));
		}

		/**
		 * If all keys are Strings, we'll consider the Map to be form data.
		 */
		private boolean isFormData(Map<?, ?> map) {
			for (Object key : map.keySet()) {
				if (!(key instanceof String)) {
					return false;
				}
			}
			return true;
		}
	}

}
