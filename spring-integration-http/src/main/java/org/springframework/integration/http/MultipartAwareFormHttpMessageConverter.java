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
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.xml.XmlAwareFormHttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.WebUtils;

/**
 * An extension of {@link XmlAwareFormHttpMessageConverter} that adds the capability
 * to <i>read</i> <code>multipart/form-data</code> content in an HTTP request.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
public class MultipartAwareFormHttpMessageConverter implements HttpMessageConverter<MultiValueMap<String, ?>> {

	private static final Log logger = LogFactory.getLog(MultipartAwareFormHttpMessageConverter.class);


	private volatile Charset defaultMultipartCharset = Charset.forName(WebUtils.DEFAULT_CHARACTER_ENCODING);

	private volatile boolean uploadMultipartFiles = true;

	private final XmlAwareFormHttpMessageConverter wrappedConverter = new XmlAwareFormHttpMessageConverter();


	/**
	 * Specify the default charset name to use when converting multipart file
	 * content into Strings if the multipart itself does not provide a charset.
	 */
	public void setDefaultMultipartCharset(String defaultMultipartCharset) {
		this.defaultMultipartCharset = Charset.forName(
				defaultMultipartCharset != null ? defaultMultipartCharset : WebUtils.DEFAULT_CHARACTER_ENCODING);
		this.wrappedConverter.setCharset(this.defaultMultipartCharset);
	}

	/**
	 * Specify whether files in a multipart request should be "uploaded"
	 * to the temporary directory instead of being read directly into
	 * a value in the payload map. By default this is <code>true</code>.
	 */
	public void setUploadMultipartFiles(boolean uploadMultipartFiles) {
		this.uploadMultipartFiles = uploadMultipartFiles;
	}

	public List<MediaType> getSupportedMediaTypes() {
		return this.wrappedConverter.getSupportedMediaTypes();
	}

	public boolean canRead(Class<?> clazz, MediaType mediaType) {
		if (!(MultiValueMap.class.isAssignableFrom(clazz) || byte[].class.isAssignableFrom(clazz))) {
			return false;
		}
		if (mediaType != null) {
			return MediaType.APPLICATION_FORM_URLENCODED.includes(mediaType)
					|| MediaType.MULTIPART_FORM_DATA.includes(mediaType);
		}
		else {
			return true;
		}
	}

	public boolean canWrite(Class<?> clazz, MediaType mediaType) {
		return this.wrappedConverter.canWrite(clazz, mediaType);
	}

	public MultiValueMap<String, ?> read(Class<? extends MultiValueMap<String, ?>> clazz,
			HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {

		MediaType contentType = inputMessage.getHeaders().getContentType();
		if (!MediaType.MULTIPART_FORM_DATA.includes(contentType)) {
			return this.wrappedConverter.read(clazz, inputMessage);
		}
		Assert.state(inputMessage instanceof MultipartHttpInputMessage, "request must be a MultipartHttpInputMessage");
		MultipartHttpInputMessage multipartInputMessage = (MultipartHttpInputMessage) inputMessage;
		return this.readMultipart(multipartInputMessage);
	}

	@SuppressWarnings("unchecked")
	private MultiValueMap<String, ?> readMultipart(MultipartHttpInputMessage multipartRequest) {
		MultiValueMap<String, Object> payloadMap = new LinkedMultiValueMap<String, Object>();
		Map parameterMap = multipartRequest.getParameterMap();
		for (Object key : parameterMap.keySet()) {
			payloadMap.add((String) key, parameterMap.get(key));
		}
		Map<String, MultipartFile> fileMap = multipartRequest.getFileMap();
		for (Map.Entry<String, MultipartFile> entry : fileMap.entrySet()) {
			MultipartFile multipartFile = entry.getValue();
			if (multipartFile.isEmpty()) {
				continue;
			}
			try {
				if (this.uploadMultipartFiles) {
					//payloadMap.add(entry.getKey(), multipartFile);
					String filename = multipartFile.getOriginalFilename();
					// TODO: add filename post-processor, also consider names with path separators (e.g. from Opera)?
					String tmpdir = System.getProperty("java.io.tmpdir");
					File upload = (filename == null) ? File.createTempFile("si_", ".tmp") : new File(tmpdir, filename);
					multipartFile.transferTo(upload);
					payloadMap.add(entry.getKey(), upload);
					if (logger.isDebugEnabled()) {
						logger.debug("copied uploaded file [" + multipartFile.getOriginalFilename() +
								"] to [" + upload.getAbsolutePath() + "]");
					}
				}
				else if (multipartFile.getContentType() != null && multipartFile.getContentType().startsWith("text")) {
					// TODO: use FileCopyUtils?
					MediaType contentType = MediaType.parseMediaType(multipartFile.getContentType());
					Charset charset = contentType.getCharSet();
					if (charset == null) {
						charset = defaultMultipartCharset;
					}
					payloadMap.add(entry.getKey(), new String(multipartFile.getBytes(), charset));
				}
				else {
					// TODO: use FileCopyUtils?
					payloadMap.add(entry.getKey(), multipartFile.getBytes());
				}
			}
			catch (IOException e) {
				throw new IllegalArgumentException("Cannot read contents of multipart file", e);
			}
		}
		return payloadMap;
	}

	public void write(MultiValueMap<String, ?> map, MediaType contentType, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {
		this.wrappedConverter.write(map, contentType, outputMessage);
	}

}
