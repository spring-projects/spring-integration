/*
 * Copyright 2002-2024 the original author or authors.
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

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.integration.http.multipart.DefaultMultipartFileReader;
import org.springframework.integration.http.multipart.MultipartFileReader;
import org.springframework.integration.http.multipart.MultipartHttpInputMessage;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

/**
 * An {@link HttpMessageConverter} implementation that delegates to an instance of
 * {@link AllEncompassingFormHttpMessageConverter} while adding the capability to <i>read</i>
 * <code>multipart/form-data</code> content in an HTTP request.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class MultipartAwareFormHttpMessageConverter implements HttpMessageConverter<MultiValueMap<String, ?>> {

	private final FormHttpMessageConverter wrappedConverter = new AllEncompassingFormHttpMessageConverter();

	private MultipartFileReader<?> multipartFileReader = new DefaultMultipartFileReader();

	/**
	 * Set the character set used for writing form data.
	 * @param charset The charset.
	 */
	public void setCharset(Charset charset) {
		this.wrappedConverter.setCharset(charset);
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
		return this.wrappedConverter.getSupportedMediaTypes();
	}

	@Override
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

	@Override
	public boolean canWrite(Class<?> clazz, MediaType mediaType) {
		return this.wrappedConverter.canWrite(clazz, mediaType);
	}

	@Override
	public MultiValueMap<String, ?> read(Class<? extends MultiValueMap<String, ?>> clazz,
			HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {

		MediaType contentType = inputMessage.getHeaders().getContentType();
		if (!MediaType.MULTIPART_FORM_DATA.includes(contentType)) {
			return this.wrappedConverter.read(clazz, inputMessage);
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
	public void write(MultiValueMap<String, ?> map, MediaType contentType, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		this.wrappedConverter.write(map, contentType, outputMessage);
	}

}
