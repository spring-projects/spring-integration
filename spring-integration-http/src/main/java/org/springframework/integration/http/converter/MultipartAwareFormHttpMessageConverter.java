/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.http.converter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.core.ResolvableType;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.multipart.MultipartHttpMessageConverter;
import org.springframework.integration.http.multipart.DefaultMultipartFileReader;
import org.springframework.integration.http.multipart.MultipartFileReader;
import org.springframework.integration.http.multipart.MultipartHttpInputMessage;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

/**
 * An {@link HttpMessageConverter} implementation that delegates to instances of
 * {@link FormHttpMessageConverter} and {@link MultipartHttpMessageConverter} while adding
 * the capability to <i>read</i> <code>multipart/form-data</code> content in an HTTP request.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class MultipartAwareFormHttpMessageConverter implements HttpMessageConverter<MultiValueMap<String, ?>> {

	private static final ResolvableType MULTI_VALUE_MAP_TYPE = ResolvableType.forClass(MultiValueMap.class);

	private final FormHttpMessageConverter formConverter = new FormHttpMessageConverter();

	private final MultipartHttpMessageConverter multipartConverter = new MultipartHttpMessageConverter();

	private MultipartFileReader<?> multipartFileReader = new DefaultMultipartFileReader();

	/**
	 * Set the character set used for writing form data.
	 * @param charset The charset.
	 */
	public void setCharset(Charset charset) {
		this.formConverter.setCharset(charset);
		this.multipartConverter.setCharset(charset);
	}

	/**
	 * Specify the {@link MultipartFileReader} to use when reading {@link MultipartFile} content.
	 * @param multipartFileReader The multipart file reader.
	 */
	public void setMultipartFileReader(MultipartFileReader<?> multipartFileReader) {
		Assert.notNull(multipartFileReader, "'multipartFileReader' must not be null");
		this.multipartFileReader = multipartFileReader;
	}

	@Override
	public List<MediaType> getSupportedMediaTypes() {
		List<MediaType> supportedMediaTypes = new ArrayList<>(this.formConverter.getSupportedMediaTypes());
		supportedMediaTypes.addAll(this.multipartConverter.getSupportedMediaTypes());
		return Collections.unmodifiableList(supportedMediaTypes);
	}

	@Override
	public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
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

	@Override
	public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
		return this.formConverter.canWrite(clazz, mediaType) || this.multipartConverter.canWrite(clazz, mediaType);
	}

	@Override
	@SuppressWarnings("unchecked")
	public MultiValueMap<String, ?> read(Class<? extends MultiValueMap<String, ?>> clazz,
			HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {

		MediaType contentType = inputMessage.getHeaders().getContentType();
		if (!MediaType.MULTIPART_FORM_DATA.includes(contentType)) {
			// The target type is always a MultiValueMap: a byte[] would make the delegate
			// fall back to a single-value Map, which is not what this converter produces.
			return (MultiValueMap<String, ?>) this.formConverter.read(MULTI_VALUE_MAP_TYPE, inputMessage, null);
		}
		Assert.state(inputMessage instanceof MultipartHttpInputMessage,
				"A request with 'multipart/form-data' Content-Type must be a MultipartHttpInputMessage. "
						+ "Be sure to provide a 'multipartResolver' bean in the ApplicationContext.");
		return readMultipart((MultipartHttpInputMessage) inputMessage);
	}

	private MultiValueMap<String, ?> readMultipart(MultipartHttpInputMessage multipartRequest) throws IOException {
		MultiValueMap<String, Object> resultMap = new LinkedMultiValueMap<>();
		MultiValueMap<String, String> parameterMap = multipartRequest.getParameterMap();
		parameterMap.forEach(resultMap::addAll);

		for (Map.Entry<String, List<MultipartFile>> entry : multipartRequest.getMultiFileMap().entrySet()) {
			List<MultipartFile> multipartFiles = entry.getValue();
			for (MultipartFile multipartFile : multipartFiles) {
				if (!multipartFile.isEmpty()) {
					resultMap.add(entry.getKey(), this.multipartFileReader.readMultipartFile(multipartFile));
				}
			}
		}
		return resultMap;
	}

	@Override
	public void write(MultiValueMap<String, ?> map, @Nullable MediaType contentType, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		if (isMultipart(map, contentType)) {
			this.multipartConverter.write(map, contentType, outputMessage);
		}
		else {
			this.formConverter.write(map, contentType, outputMessage);
		}
	}

	private static boolean isMultipart(MultiValueMap<String, ?> map, @Nullable MediaType contentType) {
		if (contentType != null) {
			return contentType.getType().equalsIgnoreCase("multipart");
		}
		for (List<?> values : map.values()) {
			for (Object value : values) {
				if (value != null && !(value instanceof String)) {
					return true;
				}
			}
		}
		return false;
	}

}
