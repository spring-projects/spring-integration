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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Base class for {@link HttpRequestExecutor} implementations.
 * 
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 1.0.2
 */
public abstract class AbstractHttpRequestExecutor implements HttpRequestExecutor {

	protected static final String HTTP_HEADER_ACCEPT_LANGUAGE = "Accept-Language";

	protected static final String HTTP_HEADER_ACCEPT_ENCODING = "Accept-Encoding";

	protected static final String HTTP_HEADER_CONTENT_ENCODING = "Content-Encoding";

	protected static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";

	protected static final String HTTP_HEADER_CONTENT_LENGTH = "Content-Length";

	protected static final String ENCODING_GZIP = "gzip";


	protected final Log logger = LogFactory.getLog(getClass());

	private boolean acceptGzipEncoding = true;


	/**
	 * Set whether to accept GZIP encoding, that is, whether to
	 * send the HTTP "Accept-Encoding" header with "gzip" as value.
	 * <p>Default is "true". Turn this flag off if you do not want
	 * GZIP response compression even if enabled on the HTTP server.
	 */
	public void setAcceptGzipEncoding(boolean acceptGzipEncoding) {
		this.acceptGzipEncoding = acceptGzipEncoding;
	}

	/**
	 * Return whether to accept GZIP encoding, that is, whether to
	 * send the HTTP "Accept-Encoding" header with "gzip" as its value.
	 */
	public boolean isAcceptGzipEncoding() {
		return this.acceptGzipEncoding;
	}

	/**
	 * Execute a request to send its content to its target URL.
	 * @param request the request to execute
	 * @return the HttpResponse result
	 * @throws IOException if thrown by I/O operations
	 * @throws Exception in case of general errors
	 */
	public final HttpResponse executeRequest(HttpRequest request) throws Exception {
		if (logger.isDebugEnabled()) {
			StringBuilder sb = new StringBuilder("Sending HTTP request to [" + request.getTargetUrl() + "]");
			Integer contentLength = request.getContentLength();
			if (contentLength != null) {
				sb.append(", with size " + contentLength);
			}
			logger.debug(sb.toString());
		}
		return doExecuteRequest(request);
	}

	/**
	 * Subclasses must implement this method to execute the request.
	 */
	protected abstract HttpResponse doExecuteRequest(HttpRequest request) throws Exception;


	/**
	 * Default implementation of {@link HttpResponse}.
	 */
	class DefaultHttpResponse implements HttpResponse {

		private final InputStream body;

		private final Map<String, List<String>> headers;


		public DefaultHttpResponse(InputStream body, Map<String, List<String>> headers) {
			this.body = body;
			this.headers = (headers != null) ? headers : Collections.<String, List<String>>emptyMap();
		}


		public InputStream getBody() {
			return this.body;
		}

		public String getFirstHeader(String key) {
			List<String> values = this.headers.get(key);
			return (values != null && values.size() > 0) ? values.get(0) : null;
		}

		public Map<String, List<String>> getHeaders() {
			return this.headers;
		}

		public List<String> getHeaders(String key) {
			return this.headers.get(key);
		}
	}

}
