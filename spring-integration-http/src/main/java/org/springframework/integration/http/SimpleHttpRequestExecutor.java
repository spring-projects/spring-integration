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
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

/**
 * Implementation of {@link HttpRequestExecutor} that uses {@link HttpURLConnection}
 * directly. This version has limited functionality but no additional dependencies.
 * For more features, see {@link CommonsHttpRequestExecutor}.
 * 
 * @author Juergen Hoeller
 * @author Iwein Fuld
 * @author Mark Fisher
 * @since 1.0.2
 */
public class SimpleHttpRequestExecutor extends AbstractHttpRequestExecutor {

	@Override
	protected HttpResponse doExecuteRequest(HttpRequest request) throws Exception {
		HttpURLConnection connection = this.openConnection(request.getTargetUrl());
		this.prepareConnection(connection, request);
		this.writeRequestBody(connection, request.getBody());
		this.validateResponse(connection);
		InputStream responseBody = this.readResponseBody(connection);
		return new DefaultHttpResponse(responseBody, this.getResponseHeaders(connection));
	}

	/**
	 * Open an HttpURLConnection for the given request URL.
	 * @return the HttpURLConnection for the given request
	 * @throws IOException if thrown by I/O methods
	 * @see java.net.URL#openConnection()
	 */
	private HttpURLConnection openConnection(URL url) throws IOException {
		URLConnection connection = url.openConnection();
		if (!(connection instanceof HttpURLConnection)) {
			throw new IOException("target URL [" + url + "] is not an HTTP URL");
		}
		return (HttpURLConnection) connection;
	}

	/**
	 * Prepare the given HTTP connection.
	 * <p>
	 * The request method (e.g. "POST), "Content-Type" header, and content
	 * length will be determined from the provided {@link HttpRequest}. 
	 * @param connection HttpURLConnection the connection to prepare
	 * @param request HttpRequest for which the connection should be prepared
	 * @throws IOException if thrown by HttpURLConnection methods
	 * @see java.net.HttpURLConnection#setRequestMethod
	 * @see java.net.HttpURLConnection#setRequestProperty
	 */
	private void prepareConnection(HttpURLConnection connection, HttpRequest request) throws IOException {
		connection.setDoInput(true);
		String requestMethod = request.getRequestMethod();
		if ("PUT".equals(requestMethod) || "POST".equals(requestMethod)) {
			connection.setDoOutput(true);
		}
		else {
			connection.setDoOutput(false);
		}
		connection.setRequestMethod(request.getRequestMethod());
		String contentType = request.getContentType();
		if (contentType != null) {
			connection.setRequestProperty(HTTP_HEADER_CONTENT_TYPE, contentType);
		}
		Integer contentLength = request.getContentLength();
		if (contentLength != null) {
			connection.setRequestProperty(HTTP_HEADER_CONTENT_LENGTH, contentLength.toString());
		}
		LocaleContext locale = LocaleContextHolder.getLocaleContext();
		if (locale != null) {
			connection.setRequestProperty(HTTP_HEADER_ACCEPT_LANGUAGE,
					StringUtils.toLanguageTag(locale.getLocale()));
		}
		if (isAcceptGzipEncoding()) {
			connection.setRequestProperty(HTTP_HEADER_ACCEPT_ENCODING, ENCODING_GZIP);
		}
	}

	private void writeRequestBody(HttpURLConnection connection, ByteArrayOutputStream body) throws IOException {
		if (body != null) {
			byte[] bytes = body.toByteArray();
			if (bytes.length > 0) {
				FileCopyUtils.copy(bytes, connection.getOutputStream());
			}
		}
	}

	private void validateResponse(HttpURLConnection connection) throws IOException {
		if (connection.getResponseCode() >= 300) {
			throw new IOException(
					"Did not receive successful HTTP response from [" + connection.getURL() +
							"]: status code = " + connection.getResponseCode() +
							", status message = [" + connection.getResponseMessage() + "]");
		}
	}

	/**
	 * Extract the response body from the connection after the request has
	 * been successfully executed.
	 * <p>
	 * This implementation simply reads the HttpURLConnection's InputStream.
	 * If the response is recognized as GZIP response, the InputStream will be
	 * wrapped in a GZIPInputStream.
	 * @param connection the HttpURLConnection to read the response body from
	 * @return an InputStream for the response body
	 * @throws IOException if thrown by I/O methods
	 * @see #isGzipResponse
	 * @see java.util.zip.GZIPInputStream
	 * @see java.net.HttpURLConnection#getInputStream()
	 */
	private InputStream readResponseBody(HttpURLConnection connection) throws IOException {
		if (isGzipResponse(connection)) {
			// GZIP response found - need to unzip.
			return new GZIPInputStream(connection.getInputStream());
		}
		else {
			// Plain response found.
			return connection.getInputStream();
		}
	}

	private Map<String, List<String>> getResponseHeaders(HttpURLConnection connection) {
		return connection.getHeaderFields();
	}

	/**
	 * Determine whether the given response is a GZIP response.
	 * <p>
	 * This implementation checks whether the HTTP "Content-Encoding" header
	 * contains "gzip" (in any casing).
	 * @param connection the HttpURLConnection to check
	 */
	private boolean isGzipResponse(HttpURLConnection connection) {
		String encodingHeader = connection.getHeaderField(HTTP_HEADER_CONTENT_ENCODING);
		return (encodingHeader != null
				&& encodingHeader.toLowerCase().indexOf(ENCODING_GZIP) != -1);
	}

}
