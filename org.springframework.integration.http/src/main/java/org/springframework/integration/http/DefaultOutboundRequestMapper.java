/*
 * Copyright 2002-2009 the original author or authors.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.springframework.integration.core.Message;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link OutboundRequestMapper}.
 * 
 * @author Mark Fisher
 * @since 1.0.2
 */
public class DefaultOutboundRequestMapper implements OutboundRequestMapper {

	private final URL defaultUrl;

	private volatile String charset = "UTF-8";


	public DefaultOutboundRequestMapper(URL defaultUrl) {
		Assert.notNull(defaultUrl, "default URL must not be null");
		this.defaultUrl = defaultUrl;
	}


	/**
	 * Specify the charset name to use for converting String-typed payloads to
	 * bytes. The default is 'UTF-8'.
	 */
	public void setCharset(String charset) {
		Assert.isTrue(Charset.isSupported(charset), "unsupported charset '" + charset + "'");
		this.charset = charset;
	}

	public HttpRequest fromMessage(Message<?> message) throws Exception {
		Assert.notNull(message, "message must not be null");
		Object payload = message.getPayload();
		Assert.notNull(payload, "payload must not be null");
		String contentType = null;
		byte[] bytes = null;
		Map<String, String[]> parameterMap = null;
		if (payload instanceof Map) {
			parameterMap = this.createParameterMap((Map<?,?>) payload);
			if (parameterMap == null) {
				Assert.isInstanceOf(Serializable.class, payload);
				bytes = this.serializePayload((Serializable) payload);
				contentType = "application/x-java-serialized-object";
			}
		}
		else if (payload instanceof byte[]) {
			bytes = (byte[]) payload;
		}
		else if (payload instanceof String) {
			bytes = ((String) payload).getBytes(this.charset);
			contentType = "text/plain";
		}
		else if (payload instanceof Serializable) {
			bytes = this.serializePayload((Serializable) payload);
			contentType = "application/x-java-serialized-object";
		}
		else {
			throw new IllegalArgumentException(
					"payload must be a byte array, String, Serializable object, or a " +
					"Map with String typed keys and String or String array typed values.");
		}
		URL url = this.resolveUrl(message);
		String method = (parameterMap != null) ? "GET" : "POST";
		if (method.equals("GET")) {
			url = this.addQueryParameters(url, parameterMap);
		}
		return new DefaultHttpRequest(url, method, bytes, contentType);
	}

	/**
	 * Creates a parameter map with String keys and String array values from
	 * the provided map if possible. If the provided map contains any keys that
	 * are not String typed, or any values that are not String or String array
	 * typed, then this method will return <code>null</code>.
	 */
	private Map<String, String[]> createParameterMap(Map<?,?> map) {
		Map<String, String[]> parameterMap = new HashMap<String, String[]>();
		for (Object key : map.keySet()) {
			if (!(key instanceof String)) {
				return null;
			}
			String[] stringArrayValue = null;
			Object value = map.get(key);
			if (value instanceof String) {
				stringArrayValue = new String[] { (String) value };
			}
			else if (value instanceof String[]) {
				stringArrayValue = (String[]) value;
			}
			else {
				return null;
			}
			parameterMap.put((String) key, stringArrayValue);
		}
		return parameterMap;
	}

	private byte[] serializePayload(Serializable payload) throws IOException {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
		objectStream.writeObject(payload);
		objectStream.flush();
		objectStream.close();
		return byteStream.toByteArray();
	}

	/**
	 * Resolve the request URL for the given Message. This implementation
	 * simply returns the default URL as provided to the constructor.  
	 */
	protected URL resolveUrl(Message<?> message) {
		return this.defaultUrl;
	}

	private URL addQueryParameters(URL url, Map<String, String[]> parameterMap) throws Exception {
		if (parameterMap == null || parameterMap.size() == 0) {
			return url;
		}
		String urlString = url.toExternalForm();
		String fragment = "";
		int fragmentStartIndex = urlString.indexOf('#');
		if (fragmentStartIndex != -1) {
			fragment = urlString.substring(fragmentStartIndex);
			urlString = urlString.substring(0, fragmentStartIndex);
		}
		StringBuilder sb = new StringBuilder(urlString);
		if (urlString.indexOf('?') == -1) {
			sb.append('?');
		}
		for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
			String[] values = entry.getValue();
			for (String value : values) {
				char lastChar = sb.charAt(sb.length() -1);
				if (lastChar != '?' && lastChar != '&') {
					sb.append('&');
				}
				sb.append(URLEncoder.encode(entry.getKey(), this.charset) + "=");
				sb.append(URLEncoder.encode(value, this.charset));
			}
		}
		sb.append(fragment);
		return new URL(sb.toString());
	}


	class DefaultHttpRequest implements HttpRequest {

		private final URL targetUrl;

		private final String requestMethod;

		private final String contentType;

		private volatile ByteArrayOutputStream requestBody;


		DefaultHttpRequest(URL targetUrl, String requestMethod, byte[] content, String contentType) throws IOException {
			Assert.notNull(targetUrl, "target url must not be null");
			this.targetUrl = targetUrl;
			if (content != null && content.length > 0) {
				this.requestBody = new ByteArrayOutputStream();
				this.requestBody.write(content);
			}
			this.contentType = contentType;
			this.requestMethod = (requestMethod != null) ? requestMethod : "POST";
		}


		public URL getTargetUrl() {
			return this.targetUrl;
		}

		public String getRequestMethod() {
			return this.requestMethod;
		}

		public String getContentType() {
			return this.contentType;
		}

		public Integer getContentLength() {
			return (this.requestBody != null) ? this.requestBody.size() : null;
		}

		public ByteArrayOutputStream getBody() {
			return this.requestBody;
		}

	}

}
