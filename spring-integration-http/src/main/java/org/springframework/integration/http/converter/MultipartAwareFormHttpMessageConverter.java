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
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

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
 * An {@link HttpMessageConverter} implementation that delegates URL-encoded form data
 * to a {@link FormHttpMessageConverter} and multipart data to a
 * {@link MultipartHttpMessageConverter}, while adding the capability to <i>read</i>
 * {@code multipart/form-data} content in an HTTP request.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 * @author Dongliang Xie
 *
 * @since 2.0
 */
public class MultipartAwareFormHttpMessageConverter implements HttpMessageConverter<MultiValueMap<String, ?>> {

	private static final ResolvableType FORM_DATA_TYPE =
			ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class);

	private final FormHttpMessageConverter formConverter = new FormHttpMessageConverter();

	private final MultipartHttpMessageConverter multipartConverter = new MultipartHttpMessageConverter();

	private final List<MediaType> supportedMediaTypes = Stream
			.concat(this.formConverter.getSupportedMediaTypes().stream(),
					this.multipartConverter.getSupportedMediaTypes().stream())
			.distinct()
			.toList();

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
		return this.supportedMediaTypes;
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
		if (!MultiValueMap.class.isAssignableFrom(clazz)) {
			return false;
		}
		if (mediaType == null) {
			return true;
		}
		if (MediaType.APPLICATION_FORM_URLENCODED.includes(mediaType)) {
			return this.formConverter.canWrite(clazz, mediaType);
		}
		return this.multipartConverter.canWrite(clazz, mediaType);
	}

	@Override
	public MultiValueMap<String, ?> read(Class<? extends MultiValueMap<String, ?>> clazz,
			HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {

		MediaType contentType = inputMessage.getHeaders().getContentType();
		if (!MediaType.MULTIPART_FORM_DATA.includes(contentType)) {
			return readForm(inputMessage);
		}
		Assert.state(inputMessage instanceof MultipartHttpInputMessage,
				"A request with 'multipart/form-data' Content-Type must be a MultipartHttpInputMessage. "
						+ "Be sure to provide a 'multipartResolver' bean in the ApplicationContext.");
		return readMultipart((MultipartHttpInputMessage) inputMessage);
	}

	@SuppressWarnings("unchecked")
	private MultiValueMap<String, ?> readForm(HttpInputMessage inputMessage) throws IOException {
		return (MultiValueMap<String, ?>) this.formConverter.read(FORM_DATA_TYPE, inputMessage, Map.of());
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
		else if (contentType == null || MediaType.APPLICATION_FORM_URLENCODED.includes(contentType)) {
			this.formConverter.write(toFormData(map), contentType, outputMessage);
		}
		else {
			throw new HttpMessageNotWritableException("Unsupported Content-Type: " + contentType);
		}
	}

	private boolean isMultipart(MultiValueMap<String, ?> map, @Nullable MediaType contentType) {
		if (contentType != null) {
			return this.multipartConverter.canWrite(MultiValueMap.class, contentType);
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

	private static MultiValueMap<String, String> toFormData(MultiValueMap<String, ?> map) {
		MultiValueMap<String, String> formData = new LinkedMultiValueMap<>(map.size());
		map.forEach((name, values) -> values.forEach((value) ->
				formData.add(name, value != null ? String.valueOf(value) : null)));
		return formData;
	}

}
