/*
 * Copyright 2026-present the original author or authors.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the {@link MultipartAwareFormHttpMessageConverter}.
 *
 * @since 7.2
 */
public class MultipartAwareFormHttpMessageConverterTests {

	private final MultipartAwareFormHttpMessageConverter converter = new MultipartAwareFormHttpMessageConverter();

	@Test
	public void supportedMediaTypesCoverFormAndMultipart() {
		assertThat(this.converter.getSupportedMediaTypes())
				.contains(MediaType.APPLICATION_FORM_URLENCODED, MediaType.MULTIPART_FORM_DATA);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void readsFormUrlEncodedBody() throws Exception {
		MultiValueMap<String, Object> result =
				(MultiValueMap<String, Object>) this.converter.read(
						(Class<? extends MultiValueMap<String, ?>>) (Class<?>) LinkedMultiValueMap.class,
						formInputMessage());

		assertThat(result).containsOnlyKeys("name", "other");
		assertThat(result.get("name")).containsExactly("foo", "bar");
	}

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void readsFormUrlEncodedBodyForByteArrayTargetType() throws Exception {
		// The inbound endpoint falls back to a 'byte[]' target type when no request payload type is configured.
		Object result = ((HttpMessageConverter) this.converter).read(byte[].class, formInputMessage());

		assertThat(result).isInstanceOf(MultiValueMap.class);
		assertThat((MultiValueMap<String, Object>) result).containsOnlyKeys("name", "other");
	}

	@Test
	public void writesFormUrlEncodedBody() throws Exception {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("name", "foo");
		form.add("name", "bar");

		ByteArrayOutputStream body = new ByteArrayOutputStream();
		HttpHeaders headers = new HttpHeaders();
		this.converter.write(form, MediaType.APPLICATION_FORM_URLENCODED, new HttpOutputMessage() {

			@Override
			public OutputStream getBody() {
				return body;
			}

			@Override
			public HttpHeaders getHeaders() {
				return headers;
			}

		});

		MediaType contentType = headers.getContentType();
		assertThat(contentType).isNotNull();
		assertThat(contentType.isCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED)).isTrue();
		assertThat(body.toString(StandardCharsets.UTF_8)).isEqualTo("name=foo&name=bar");
	}

	private static HttpInputMessage formInputMessage() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		byte[] body = "name=foo&name=bar&other=baz".getBytes(StandardCharsets.UTF_8);
		return new HttpInputMessage() {

			@Override
			public InputStream getBody() {
				return new ByteArrayInputStream(body);
			}

			@Override
			public HttpHeaders getHeaders() {
				return headers;
			}

		};
	}

}
