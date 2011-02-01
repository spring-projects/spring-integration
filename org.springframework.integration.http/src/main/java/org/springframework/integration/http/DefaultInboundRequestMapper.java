/*
 * Copyright 2002-2011 the original author or authors.
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
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;

/**
 * Default implementation of {@link InboundRequestMapper} for inbound HttpServletRequests.
 * The request will be mapped according to the following rules:
 * <ul>
 * <li>For a GET request or a POST request with a Content-Type of
 * "application/x-www-form-urlencoded", the parameter Map will be copied as the
 * payload. The map's keys will be Strings, and the values will be String arrays
 * as described for {@link ServletRequest#getParameterMap()}.</li>
 * <li>If a MultipartResolver has been provided, and a multipart request is
 * detected, the multipart file content will be converted to String for any
 * "text" content type, or byte arrays otherwise.</li>
 * <li>For other request types, the request body will be used as the payload
 * and the type will depend on the Content-Type header value. If it begins with
 * "text", a String will be created. If the Content-Type is
 * "application/x-java-serialized-object", the request body will be expected to
 * contain a Serializable Object, and that will be used as the message payload.
 * Otherwise, the payload will be a byte array.</li>
 * </ul>
 * In all cases, the original request headers will be passed in the
 * MessageHeaders. Likewise, the following headers will be added:
 * <ul>
 *   <li>{@link HttpHeaders#REQUEST_URL}</li>
 *   <li>{@link HttpHeaders#REQUEST_METHOD}</li>
 *   <li>{@link HttpHeaders#USER_PRINCIPAL} (if available)</li>
 * </ul>
 * 
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @since 1.0.2
 */
public class DefaultInboundRequestMapper implements InboundRequestMapper {

	private final Log logger = LogFactory.getLog(getClass());

	private volatile MultipartResolver multipartResolver;

	private String multipartCharset = null;


	/**
	 * Specify the {@link MultipartResolver} to use when checking requests.
	 * If no resolver is provided, this mapper will not support multipart
	 * requests. 
	 */
	public void setMultipartResolver(MultipartResolver multipartResolver) {
		this.multipartResolver = multipartResolver;
	}

	/**
	 * Specify the charset name to use when converting multipart file content
	 * into Strings.
	 */
	public void setMultipartCharset(String multipartCharset) {
		this.multipartCharset = multipartCharset;
	}


	public Message<?> toMessage(HttpServletRequest request) throws Exception {
		try {
			request = this.checkMultipart(request);
			Object payload = createPayloadFromRequest(request);
			MessageBuilder<?> builder = MessageBuilder.withPayload(payload);
			this.populateHeaders(request, builder);
			return builder.build();
		}
		finally {
			this.cleanupMultipart(request);
		}
	}

	/**
	 * Convert the request into a multipart request to make multiparts available.
	 * If no multipart resolver is set, simply use the existing request.
	 * @param request current HTTP request
	 * @return the processed request (multipart wrapper if necessary)
	 * @see MultipartResolver#resolveMultipart
	 */
	private HttpServletRequest checkMultipart(HttpServletRequest request) throws MultipartException {
		if (this.multipartResolver != null && this.multipartResolver.isMultipart(request)) {
			if (request instanceof MultipartHttpServletRequest) {
				logger.debug("Request is already a MultipartHttpServletRequest");
			}
			else {
				return this.multipartResolver.resolveMultipart(request);
			}
		}
		return request;
	}

	/**
	 * Clean up any resources used by the given multipart request (if any).
	 * @param request current HTTP request
	 * @see MultipartResolver#cleanupMultipart
	 */
	private void cleanupMultipart(HttpServletRequest request) {
		if (this.multipartResolver != null && request instanceof MultipartHttpServletRequest) {
			this.multipartResolver.cleanupMultipart((MultipartHttpServletRequest) request);
		}
	}

	private Object createPayloadFromRequest(HttpServletRequest request) throws Exception {
		Object payload = null;
		String contentType = request.getContentType() != null ? request.getContentType() : "";
		if (request instanceof MultipartHttpServletRequest) {
			payload = this.createPayloadFromMultipartRequest((MultipartHttpServletRequest) request);
		}
		else if (contentType.startsWith("multipart/form-data")) {
			throw new IllegalArgumentException("Content-Type of 'multipart/form-data' requires a MultipartResolver." +
						" Try configuring a MultipartResolver within the ApplicationContext.");
		}
		else if (request.getMethod().equals("GET")) {
			if (logger.isDebugEnabled()) {
				logger.debug("received GET request, using parameter map as payload");
			}
			payload = this.createPayloadFromParameterMap(request);
		}
		else if (contentType.startsWith("application/x-www-form-urlencoded")) {
			if (logger.isDebugEnabled()) {
				logger.debug("received " + request.getMethod()
						+ " request with form data, using parameter map as payload");
			}
			payload = createPayloadFromParameterMap(request);
		}
		else if (contentType.startsWith("text")) {
			if (logger.isDebugEnabled()) {
				logger.debug("received " + request.getMethod()
						+ " request, creating payload with text content");
			}
			payload = createPayloadFromTextContent(request);
		}
		else if (contentType.startsWith("application/x-java-serialized-object")) {
			payload = createPayloadFromSerializedObject(request);
		}
		else {
			payload = createPayloadFromInputStream(request);
		}
		return payload;
	}

	@SuppressWarnings("unchecked")
	private Object createPayloadFromMultipartRequest(MultipartHttpServletRequest multipartRequest) {
		Map<String, Object> payloadMap = new HashMap<String, Object>(multipartRequest.getParameterMap());
		Map<String, MultipartFile> fileMap = (Map<String, MultipartFile>) multipartRequest.getFileMap();
		for (Map.Entry<String, MultipartFile> entry : fileMap.entrySet()) {
			MultipartFile multipartFile = entry.getValue();
			try {
				if (multipartFile.getContentType() != null && multipartFile.getContentType().startsWith("text")) {
					String multipartFileAsString = this.multipartCharset != null ?
							new String(multipartFile.getBytes(), this.multipartCharset) :
							new String(multipartFile.getBytes());
					payloadMap.put(entry.getKey(), multipartFileAsString);
				}
				else {
					payloadMap.put(entry.getKey(), multipartFile.getBytes());
				}
			}
			catch (IOException e) {
				throw new IllegalArgumentException("Cannot read contents of multipart file", e);
			}
		}
		return Collections.unmodifiableMap(payloadMap);
	}

	@SuppressWarnings("unchecked")
	private Object createPayloadFromParameterMap(HttpServletRequest request) {
		Map<String, String[]> parameterMap = new HashMap<String, String[]>(request.getParameterMap());
		return Collections.unmodifiableMap(parameterMap);
	}

	private Object createPayloadFromTextContent(HttpServletRequest request) throws IOException {
		String charset = request.getCharacterEncoding() != null ? request.getCharacterEncoding() : "utf-8";
		return new String(FileCopyUtils.copyToByteArray(request.getInputStream()), charset);
	}

	private Object createPayloadFromSerializedObject(HttpServletRequest request) {
		try {
			return new ObjectInputStream(request.getInputStream()).readObject();
		}
		catch (Exception e) {
			throw new IllegalArgumentException("failed to deserialize Object in request", e);
		}
	}

	private byte[] createPayloadFromInputStream(HttpServletRequest request) throws Exception {
		int bufferSize = 4096;
		InputStream in = request.getInputStream();
		int length = request.getContentLength();
		if (length == -1) {
			throw new ResponseStatusCodeException(HttpServletResponse.SC_LENGTH_REQUIRED);
		}
		if (logger.isDebugEnabled()) {
			logger.debug("received " + request.getMethod() + " request, "
					+ "creating byte array payload with content lenth: " + length);
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream(bufferSize);
		byte[] buffer = new byte[bufferSize];
		int bytesRead = -1;
		while ((bytesRead = in.read(buffer)) != -1) {
			out.write(buffer, 0, bytesRead);
		}
		return out.toByteArray();
	}

	private void populateHeaders(HttpServletRequest request, MessageBuilder<?> builder) {
		Enumeration<?> headerNames = request.getHeaderNames();
		if (headerNames != null) {
			while (headerNames.hasMoreElements()) {
				String headerName = (String) headerNames.nextElement();
				Enumeration<?> headerEnum = request.getHeaders(headerName);
				if (headerEnum != null) {
					List<Object> headers = new ArrayList<Object>();
					while (headerEnum.hasMoreElements()) {
						headers.add(headerEnum.nextElement());
					}
					if (headers.size() == 1) {
						builder.setHeader(headerName, headers.get(0));
					}
					else if (headers.size() > 1) {
						builder.setHeader(headerName, headers);
					}
				}
			}
		}
		builder.setHeader(HttpHeaders.REQUEST_URL, request.getRequestURL().toString());
		builder.setHeader(HttpHeaders.REQUEST_METHOD, request.getMethod());
		builder.setHeader(HttpHeaders.USER_PRINCIPAL, request.getUserPrincipal());
	}

}
