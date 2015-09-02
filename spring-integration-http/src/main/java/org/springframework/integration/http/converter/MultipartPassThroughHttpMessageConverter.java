/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.integration.http.converter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.FileCopyUtils;

/**
 * A simple {@link HttpMessageConverter} that passes a multipart/form-data request
 * as a byte[]. Do not include a {@code MultipartResolver} in the context.
 * Used for inbound only.
 *
 * @author Gary Russell
 * @since 4.2
 *
 */
public class MultipartPassThroughHttpMessageConverter implements HttpMessageConverter<byte[]> {

	@Override
	public boolean canRead(Class<?> clazz, MediaType mediaType) {
		if (!(byte[].class.isAssignableFrom(clazz))) {
			return false;
		}
		if (mediaType != null) {
			return MediaType.APPLICATION_FORM_URLENCODED.includes(mediaType)
					|| MediaType.MULTIPART_FORM_DATA.includes(mediaType);
		}
		else {
			return false;
		}
	}

	@Override
	public boolean canWrite(Class<?> clazz, MediaType mediaType) {
		return false;
	}

	@Override
	public List<MediaType> getSupportedMediaTypes() {
		return Collections.singletonList(MediaType.MULTIPART_FORM_DATA);
	}

	@Override
	public byte[] read(Class<? extends byte[]> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		FileCopyUtils.copy(inputMessage.getBody(), baos);
		return baos.toByteArray();
	}

	@Override
	public void write(byte[] t, MediaType contentType, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {
		throw new UnsupportedOperationException();
	}

}
