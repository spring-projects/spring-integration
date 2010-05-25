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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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
 * payload. The map will be an instance of {@link MultiValueMap} where the keys are
 * Strings and the values are Lists of Strings. Those Lists are populated from the
 * String array values of the original request parameter Map as described for the
 * {@link ServletRequest#getParameterMap()} method.</li>
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

	private volatile String multipartCharset = null;

	private volatile boolean copyUploadedFiles; 


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

	/**
	 * Specify whether uploaded multipart files should be copied to a temporary
	 * file on the server. If this is set to 'true', the payload map will
	 * contain a File instance as the value for each multipart file entry.
	 * Otherwise the uploaded file's content will be converted to either a
	 * String or byte array based on the content-type (String for "text/*" and
	 * byte array otherwise). The default value is false.
	 */
	public void setCopyUploadedFiles(boolean copyUploadedFiles) {
		this.copyUploadedFiles = copyUploadedFiles;
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
		Map<String, MultipartFile> fileMap = multipartRequest.getFileMap();
		for (Map.Entry<String, MultipartFile> entry : fileMap.entrySet()) {
			MultipartFile multipartFile = entry.getValue();
			if (multipartFile.isEmpty()) {
				continue;
			}
			try {
				if (this.copyUploadedFiles) {
					File tmpFile = File.createTempFile("si_", null);
					multipartFile.transferTo(tmpFile);
					payloadMap.put(entry.getKey(), tmpFile);
					if (logger.isDebugEnabled()) {
						logger.debug("copied uploaded file [" + multipartFile.getOriginalFilename() +
								"] to temporary file [" + tmpFile.getAbsolutePath() + "]");
					}
				}
				else if (multipartFile.getContentType() != null && multipartFile.getContentType().startsWith("text")) {
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
		return new UnmodifiableRequestParameterMap(request.getParameterMap());
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
		InputStream stream = request.getInputStream();
		int length = request.getContentLength();
		if (length == -1) {
			throw new ResponseStatusCodeException(HttpServletResponse.SC_LENGTH_REQUIRED);
		}
		if (logger.isDebugEnabled()) {
			logger.debug("received " + request.getMethod() + " request, "
					+ "creating byte array payload with content lenth: " + length);
		}
		byte[] bytes = new byte[length];
		stream.read(bytes, 0, length);
		return bytes;
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


	/**
	 * Map class that extends {@link LinkedMultiValueMap} and implements Serializable.
	 * The contents of the map are unmodifiable, so calling any modification operation
	 * (e.g. put, add, or remove) will result in an UnsupportedOperationException. 
	 */
	@SuppressWarnings("serial")
	private static class UnmodifiableRequestParameterMap
			extends LinkedMultiValueMap<String, String> implements Serializable { // TODO: in 3.0.1 LMVM implements Serializable

		UnmodifiableRequestParameterMap(Map<String, String[]> parameters) {
			for (Map.Entry<String, String[]> entry : parameters.entrySet()) {
				super.put(entry.getKey(), Arrays.asList(entry.getValue()));
			}
		}

		@Override
		public void add(String key, String value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<String> put(String key, List<String> value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void putAll(Map<? extends String, ? extends List<String>> m) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<String> remove(Object key) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void set(String key, String value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setAll(Map<String, String> values) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Map<String, String> toSingleValueMap() {
			return Collections.unmodifiableMap(super.toSingleValueMap());
		}

	}

}
