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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.OptionsMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.TraceMethod;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Implementation of {@link HttpRequestExecutor} that uses a commons-http
 * {@link HttpClient} to execute {@link HttpRequest} instances.
 * 
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 1.0.2
 */
public class CommonsHttpRequestExecutor extends AbstractHttpRequestExecutor {

	/**
	 * Default timeout value if no HttpClient is explicitly provided.
	 */
	private static final int DEFAULT_READ_TIMEOUT_MILLISECONDS = (60 * 1000);

	private HttpClient httpClient;


	/**
	 * Create a new CommonsHttpRequestExecutor with a default HttpClient that
	 * uses a default MultiThreadedHttpConnectionManager.
	 * Sets the socket read timeout to {@link #DEFAULT_READ_TIMEOUT_MILLISECONDS}.
	 * @see org.apache.commons.httpclient.HttpClient
	 * @see org.apache.commons.httpclient.MultiThreadedHttpConnectionManager
	 */
	public CommonsHttpRequestExecutor() {
		this.httpClient = new HttpClient(new MultiThreadedHttpConnectionManager());
		this.setReadTimeout(DEFAULT_READ_TIMEOUT_MILLISECONDS);
	}

	/**
	 * Create a new CommonsHttpRequestExecutor with the given HttpClient
	 * instance. The socket read timeout of the provided HttpClient will not be
	 * changed.
	 * @param httpClient the HttpClient instance to use for this request executor
	 */
	public CommonsHttpRequestExecutor(HttpClient httpClient) {
		this.setHttpClient(httpClient);
	}


	/**
	 * Set the HttpClient instance to use for this request executor.
	 */
	public void setHttpClient(HttpClient httpClient) {
		Assert.notNull(httpClient, "httpClient must not be null");
		this.httpClient = httpClient;
	}

	/**
	 * Return the HttpClient instance that this request executor uses.
	 */
	public HttpClient getHttpClient() {
		return this.httpClient;
	}

	/**
	 * Set the socket read timeout for the underlying HttpClient. A value of 0
	 * means <emphasis>never</emphasis> timeout. 
	 * @param timeout the timeout value in milliseconds
	 * @see org.apache.commons.httpclient.params.HttpConnectionManagerParams#setSoTimeout(int)
	 * @see #DEFAULT_READ_TIMEOUT_MILLISECONDS
	 */
	public void setReadTimeout(int timeout) {
		if (timeout < 0) {
			throw new IllegalArgumentException("timeout must be a non-negative value");
		}
		this.httpClient.getHttpConnectionManager().getParams().setSoTimeout(timeout);
	}

	@Override
	protected HttpResponse doExecuteRequest(HttpRequest request) throws Exception {
		HttpMethod httpMethod = createHttpMethod(request);
		try {
			if (httpMethod instanceof EntityEnclosingMethod) {
				setRequestBody((EntityEnclosingMethod) httpMethod, request.getBody(), request.getContentType());
			}
			executeHttpMethod(getHttpClient(), httpMethod);
			validateResponse(httpMethod);
			return new DefaultHttpResponse(readResponseBody(httpMethod), getResponseHeaders(httpMethod));
		}
		finally {
			// Need to explicitly release because it might be pooled.
			httpMethod.releaseConnection();
		}
	}

	/**
	 * Create a HttpMethod for the given {@link HttpRequest}.
	 * <p>This implementation creates an HttpMethod with the request's target
	 * URL as well as the "Accept-Language" and "Accept-Encoding" headers. If
	 * the method is "POST" or "PUT", the "Content-Type" header will also be
	 * set as specified in the given request.
	 * @param request the HTTP request to create a method for
	 * @return the HttpMethod instance
	 */
	private HttpMethod createHttpMethod(HttpRequest request) {
		String url = request.getTargetUrl().toString();
		String methodName = request.getRequestMethod();
		HttpMethod httpMethod = null;
		if ("GET".equals(methodName)) {
			httpMethod = new GetMethod(url);
		}
		else if ("POST".equals(methodName)) {
			httpMethod = new PostMethod(url);
		}
		else if ("PUT".equals(methodName)) {
			httpMethod = new PutMethod(url);
		}
		else if ("DELETE".equals(methodName)) {
			httpMethod = new DeleteMethod(url);
		}
		else if ("TRACE".equals(methodName)) {
			httpMethod = new TraceMethod(url);
		}
		else if ("HEAD".equals(methodName)) {
			httpMethod = new HeadMethod(url);
		}
		else if ("OPTIONS".equals(methodName)) {
			httpMethod = new OptionsMethod(url);
		}
		else {
			throw new UnsupportedOperationException("unsupported request method '" + methodName + "'");
		}
		LocaleContext locale = LocaleContextHolder.getLocaleContext();
		if (locale != null) {
			httpMethod.addRequestHeader(HTTP_HEADER_ACCEPT_LANGUAGE, StringUtils.toLanguageTag(locale.getLocale()));
		}
		if (isAcceptGzipEncoding()) {
			httpMethod.addRequestHeader(HTTP_HEADER_ACCEPT_ENCODING, ENCODING_GZIP);
		}
		if (httpMethod instanceof EntityEnclosingMethod) {
			String contentType = request.getContentType();
			if (contentType != null) {
				httpMethod.addRequestHeader(HTTP_HEADER_CONTENT_TYPE, contentType);
			}
		}
		return httpMethod;
	}

	/**
	 * Set the given byte stream as the request body.
	 * <p>This implementation simply sets the byte stream as the
	 * EntityEnclosingMethod's request body. This can be overridden, for
	 * example, to write a specific encoding and potentially set appropriate
	 * HTTP request headers.
	 * @param httpMethod the EntityEnclosingMethod on which to set the request body
	 * @param baos the ByteArrayOutputStream that contains the content
	 * @param contentType the request body's content type
	 * @throws IOException if thrown by I/O methods
	 * @see org.apache.commons.httpclient.methods.PostMethod#setRequestBody(java.io.InputStream)
	 * @see org.apache.commons.httpclient.methods.PostMethod#setRequestEntity
	 * @see org.apache.commons.httpclient.methods.InputStreamRequestEntity
	 */
	private void setRequestBody(
			EntityEnclosingMethod httpMethod, ByteArrayOutputStream baos, String contentType)
			throws IOException {
		httpMethod.setRequestEntity(new ByteArrayRequestEntity(baos.toByteArray(), contentType));
	}

	/**
	 * Execute the given HttpMethod instance.
	 * @param httpClient the HttpClient responsible for execution
	 * @param httpMethod the HttpMethod to be executed
	 * @throws IOException if thrown by I/O methods
	 * @see org.apache.commons.httpclient.HttpClient#executeMethod(org.apache.commons.httpclient.HttpMethod)
	 */
	private void executeHttpMethod(HttpClient httpClient, HttpMethod httpMethod) throws IOException {
		httpClient.executeMethod(httpMethod);
	}

	/**
	 * Validate the given response as contained in the HttpMethod object,
	 * throwing an exception if it does not correspond to a successful HTTP response.
	 * <p>This implementation rejects any HTTP status code beyond 2xx, to avoid
	 * parsing the response body and trying to read from a corrupted stream.
	 * @param httpMethod the executed HttpMethod to validate
	 * @throws IOException if validation failed
	 * @see org.apache.commons.httpclient.methods.PostMethod#getStatusCode()
	 * @see org.apache.commons.httpclient.HttpException
	 */
	private void validateResponse(HttpMethod httpMethod) throws IOException {
		if (httpMethod.getStatusCode() >= 300) {
			throw new HttpException(
 					"Did not receive successful HTTP response from [" + httpMethod.getURI() +
 							"]: status code = " + httpMethod.getStatusCode() +
 							", status message = [" + httpMethod.getStatusText() + "]");
		}
	}

	/**
	 * Extract the response body from the given executed request.
	 * <p>This implementation simply fetches the HttpMethod's response
	 * body stream. If the response is recognized as a GZIP response, the
	 * InputStream will be wrapped in a GZIPInputStream.
	 * @param httpMethod the HttpMethod from which to read the response body
	 * @return an InputStream for the response body, or <code>null</code> if no response stream is available
	 * @throws IOException if thrown by I/O methods
	 * @see #isGzipResponse
	 * @see java.util.zip.GZIPInputStream
	 * @see org.apache.commons.httpclient.HttpMethod#getResponseBodyAsStream()
	 */
	private InputStream readResponseBody(HttpMethod httpMethod) throws IOException {
		byte[] responseBody = httpMethod.getResponseBody();
		InputStream responseStream = null;
		if (responseBody != null) {
			responseStream = new ByteArrayInputStream(responseBody);
			if (isGzipResponse(httpMethod)) {
				responseStream = new GZIPInputStream(responseStream);
			}
		}
		return responseStream;
	}

	private Map<String, List<String>> getResponseHeaders(HttpMethod httpMethod) {
		Map<String, List<String>> headers = new HashMap<String, List<String>>();
		for (Header header : httpMethod.getResponseHeaders()) {
			String name = header.getName();
			String value = header.getValue();
			List<String> values = headers.get(name);
			if (values == null) {
				values = new ArrayList<String>();
			}
			values.add(value);
			headers.put(name, values);
		}
		return Collections.unmodifiableMap(headers);
	}

	/**
	 * Determine whether the given response indicates a GZIP response.
	 * <p>This implementation checks whether the HTTP "Content-Encoding"
	 * header contains "gzip" (in any casing).
	 * @param httpMethod the HttpMethod to check
	 * @return whether the given response indicates a GZIP response
	 * @see org.apache.commons.httpclient.HttpMethod#getResponseHeader(String)
	 */
	private boolean isGzipResponse(HttpMethod httpMethod) {
		Header encodingHeader = httpMethod.getResponseHeader(HTTP_HEADER_CONTENT_ENCODING);
		return (encodingHeader != null && encodingHeader.getValue() != null
				&& encodingHeader.getValue().toLowerCase().indexOf(ENCODING_GZIP) != -1);
	}

}
