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
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.zip.GZIPInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.integration.core.Message;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.handler.ReplyMessageHolder;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 1.0.2
 */
public class HttpOutboundEndpoint extends AbstractReplyProducingMessageHandler {

	/**
	 * Default content type: "application/x-java-serialized-object"
	 */
	public static final String CONTENT_TYPE_SERIALIZED_OBJECT = "application/x-java-serialized-object";


	protected static final String HTTP_METHOD_POST = "POST";

	protected static final String HTTP_HEADER_ACCEPT_LANGUAGE = "Accept-Language";

	protected static final String HTTP_HEADER_ACCEPT_ENCODING = "Accept-Encoding";

	protected static final String HTTP_HEADER_CONTENT_ENCODING = "Content-Encoding";

	protected static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";

	protected static final String HTTP_HEADER_CONTENT_LENGTH = "Content-Length";

	protected static final String ENCODING_GZIP = "gzip";


	private final Log logger = LogFactory.getLog(getClass());

	private final URL url;

	private volatile String charset = "UTF-8";

	private volatile String contentType = CONTENT_TYPE_SERIALIZED_OBJECT;

	private volatile boolean acceptGzipEncoding = true;


	public HttpOutboundEndpoint(String url) {
		Assert.notNull(url, "url must not be null");
		try {
			this.url = new URL(url);
		}
		catch (MalformedURLException e) {
			throw new IllegalArgumentException(e);
		}
	}


	/**
	 * Specify the charset name to use for converting String-typed
	 * payloads to bytes. The default is 'UTF-8'.
	 */
	public void setCharset(String charset) {
		Assert.isTrue(Charset.isSupported(charset), "unsupported charset '" + charset + "'");
		this.charset = charset;
	}

	/**
	 * Specify the content type to use for sending HTTP requests.
	 * <p>Default is "application/x-java-serialized-object".
	 */
	public void setContentType(String contentType) {
		Assert.notNull(contentType, "'contentType' must not be null");
		this.contentType = contentType;
	}

	/**
	 * Return the content type to use for sending HTTP requests.
	 */
	public String getContentType() {
		return this.contentType;
	}

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
	 * send the HTTP "Accept-Encoding" header with "gzip" as value.
	 */
	public boolean isAcceptGzipEncoding() {
		return this.acceptGzipEncoding;
	}

	@Override
	protected void handleRequestMessage(Message<?> requestMessage, ReplyMessageHolder replyMessageHolder) {
		try {
			HttpURLConnection connection = this.openConnection();
			Object payload = requestMessage.getPayload();
			byte[] bytes = null;
			if (payload instanceof byte[]) {
				bytes = (byte[]) payload;
			}
			else if (payload instanceof String) {
				bytes = ((String) payload).getBytes(this.charset);
			}
			else if (payload instanceof Serializable) {
				ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
				ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
				objectStream.writeObject(payload);
				objectStream.flush();
				objectStream.close();
				bytes = byteStream.toByteArray();
			}
			else {
				throw new IllegalArgumentException("payload must be a byte array, String, or Serializable object");
			}
			ByteArrayOutputStream requestByteStream = new ByteArrayOutputStream();
			requestByteStream.write(bytes);
			this.prepareConnection(connection, requestByteStream.size());
			this.writeRequestBody(connection, requestByteStream);
			this.validateResponse(connection);
			ByteArrayOutputStream responseByteStream = new ByteArrayOutputStream();
			InputStream responseBody = this.readResponseBody(connection);
			FileCopyUtils.copy(responseBody, responseByteStream);
			replyMessageHolder.set(responseByteStream.toByteArray());
		}
		catch (Exception e) {
			throw new MessageHandlingException(requestMessage, "failed to send HTTP request", e);
		}
	}

	/**
	 * Open an HttpURLConnection for this gateway's URL.
	 * @return the HttpURLConnection for the given request
	 * @throws IOException if thrown by I/O methods
	 * @see java.net.URL#openConnection()
	 */
	private HttpURLConnection openConnection() throws IOException {
		URLConnection con = this.url.openConnection();
		if (!(con instanceof HttpURLConnection)) {
			throw new IOException("Service URL [" + this.url + "] is not an HTTP URL");
		}
		return (HttpURLConnection) con;
	}

	/**
	 * Prepare the given HTTP connection.
	 * <p>This implementation specifies POST as method,
	 * "application/x-java-serialized-object" as "Content-Type" header,
	 * and the given content length as "Content-Length" header.
	 * @param con the HTTP connection to prepare
	 * @param contentLength the length of the content to send
	 * @throws IOException if thrown by HttpURLConnection methods
	 * @see java.net.HttpURLConnection#setRequestMethod
	 * @see java.net.HttpURLConnection#setRequestProperty
	 */
	private void prepareConnection(HttpURLConnection con, int contentLength) throws IOException {
		con.setDoOutput(true);
		con.setRequestMethod(HTTP_METHOD_POST);
		con.setRequestProperty(HTTP_HEADER_CONTENT_TYPE, getContentType());
		con.setRequestProperty(HTTP_HEADER_CONTENT_LENGTH, Integer.toString(contentLength));
		LocaleContext locale = LocaleContextHolder.getLocaleContext();
		if (locale != null) {
			con.setRequestProperty(HTTP_HEADER_ACCEPT_LANGUAGE, StringUtils.toLanguageTag(locale.getLocale()));
		}
		if (isAcceptGzipEncoding()) {
			con.setRequestProperty(HTTP_HEADER_ACCEPT_ENCODING, ENCODING_GZIP);
		}
	}

	private void writeRequestBody(HttpURLConnection con, ByteArrayOutputStream baos) throws IOException {
		baos.writeTo(con.getOutputStream());
	}

	private void validateResponse(HttpURLConnection con) throws IOException {
		if (con.getResponseCode() >= 300) {
			throw new IOException(
					"Did not receive successful HTTP response: status code = " + con.getResponseCode() +
					", status message = [" + con.getResponseMessage() + "]");
		}
	}

	/**
	 * Extract the response body from the given executed remote invocation
	 * request.
	 * <p>This implementation simply reads the serialized invocation
	 * from the HttpURLConnection's InputStream. If the response is recognized
	 * as GZIP response, the InputStream will get wrapped in a GZIPInputStream.
	 * @param config the HTTP invoker configuration that specifies the target service
	 * @param con the HttpURLConnection to read the response body from
	 * @return an InputStream for the response body
	 * @throws IOException if thrown by I/O methods
	 * @see #isGzipResponse
	 * @see java.util.zip.GZIPInputStream
	 * @see java.net.HttpURLConnection#getInputStream()
	 * @see java.net.HttpURLConnection#getHeaderField(int)
	 * @see java.net.HttpURLConnection#getHeaderFieldKey(int)
	 */
	private InputStream readResponseBody(HttpURLConnection con) throws IOException {
		if (isGzipResponse(con)) {
			// GZIP response found - need to unzip.
			return new GZIPInputStream(con.getInputStream());
		}
		else {
			// Plain response found.
			return con.getInputStream();
		}
	}

	/**
	 * Determine whether the given response is a GZIP response.
	 * <p>This implementation checks whether the HTTP "Content-Encoding"
	 * header contains "gzip" (in any casing).
	 * @param con the HttpURLConnection to check
	 */
	private boolean isGzipResponse(HttpURLConnection con) {
		String encodingHeader = con.getHeaderField(HTTP_HEADER_CONTENT_ENCODING);
		return (encodingHeader != null && encodingHeader.toLowerCase().indexOf(ENCODING_GZIP) != -1);
	}

}
