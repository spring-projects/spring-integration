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

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.integration.http.multipart.MultipartHttpInputMessage;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dongliang Xie
 *
 * @since 7.2
 */
public class MultipartAwareFormHttpMessageConverterTests {

	private final MultipartAwareFormHttpMessageConverter converter =
			new MultipartAwareFormHttpMessageConverter();

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void readUrlEncodedFormForByteArrayTarget() throws Exception {
		MockHttpInputMessage inputMessage =
				new MockHttpInputMessage("name=J%FCrgen&tag=first&tag=second".getBytes(StandardCharsets.US_ASCII));
		inputMessage.getHeaders().setContentType(
				new MediaType(MediaType.APPLICATION_FORM_URLENCODED, StandardCharsets.ISO_8859_1));

		Object result = ((HttpMessageConverter) this.converter).read(byte[].class, inputMessage);

		assertThat(result).isInstanceOf(MultiValueMap.class);
		MultiValueMap<String, String> form = (MultiValueMap<String, String>) result;
		assertThat(form.getFirst("name")).isEqualTo("Jürgen");
		assertThat(form.get("tag")).containsExactly("first", "second");
	}

	@Test
	public void exposeFormAndMultipartMediaTypes() {
		assertThat(this.converter.getSupportedMediaTypes()).containsExactly(
				MediaType.APPLICATION_FORM_URLENCODED,
				MediaType.MULTIPART_FORM_DATA,
				MediaType.MULTIPART_MIXED,
				MediaType.MULTIPART_RELATED);
	}

	@Test
	public void readAndWriteCapabilitiesMatchSupportedConversions() {
		assertThat(this.converter.canRead(byte[].class, MediaType.APPLICATION_FORM_URLENCODED)).isTrue();
		assertThat(this.converter.canRead(MultiValueMap.class, MediaType.MULTIPART_FORM_DATA)).isTrue();
		assertThat(this.converter.canRead(byte[].class, null)).isTrue();
		assertThat(this.converter.canRead(MultiValueMap.class, MediaType.APPLICATION_JSON)).isFalse();
		assertThat(this.converter.canRead(String.class, MediaType.APPLICATION_FORM_URLENCODED)).isFalse();
		assertThat(this.converter.canWrite(MultiValueMap.class, MediaType.APPLICATION_FORM_URLENCODED)).isTrue();
		assertThat(this.converter.canWrite(MultiValueMap.class, MediaType.MULTIPART_FORM_DATA)).isTrue();
		assertThat(this.converter.canWrite(MultiValueMap.class, null)).isTrue();
		assertThat(this.converter.canWrite(MultiValueMap.class, MediaType.APPLICATION_JSON)).isFalse();
		assertThat(this.converter.canWrite(Map.class, MediaType.APPLICATION_FORM_URLENCODED)).isFalse();
	}

	@Test
	public void writeUrlEncodedFormWithExplicitCharset() throws Exception {
		MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
		form.add("name", "Jürgen");
		form.add("tag", "first");
		form.add("tag", "second");
		form.add("count", 42);
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		MediaType contentType =
				new MediaType(MediaType.APPLICATION_FORM_URLENCODED, StandardCharsets.ISO_8859_1);

		this.converter.write(form, contentType, outputMessage);

		assertThat(outputMessage.getHeaders().getContentType()).isEqualTo(contentType);
		assertThat(outputMessage.getBodyAsString(StandardCharsets.ISO_8859_1))
				.isEqualTo("name=J%FCrgen&tag=first&tag=second&count=42");
	}

	@Test
	public void useConfiguredCharsetForFormWithNoContentType() throws Exception {
		this.converter.setCharset(StandardCharsets.ISO_8859_1);
		MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
		form.add("name", "Jürgen");
		form.add("empty", null);
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();

		this.converter.write(form, null, outputMessage);

		assertThat(outputMessage.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_FORM_URLENCODED);
		assertThat(outputMessage.getBodyAsString(StandardCharsets.ISO_8859_1)).isEqualTo("name=J%FCrgen&empty");
	}

	@Test
	public void writeMultipartForNonStringValueWithNoContentType() throws Exception {
		MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
		form.add("file", "test".getBytes(StandardCharsets.UTF_8));
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();

		this.converter.write(form, null, outputMessage);

		MediaType contentType = outputMessage.getHeaders().getContentType();
		assertThat(contentType).isNotNull();
		assertThat(contentType.isCompatibleWith(MediaType.MULTIPART_FORM_DATA)).isTrue();
		assertThat(contentType.getParameter("boundary")).isNotBlank();
		assertThat(outputMessage.getBodyAsString()).contains("name=\"file\"").contains("test");
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void retainMultipartParametersAndCustomFileReader() throws Exception {
		MockMultipartHttpServletRequest servletRequest = new MockMultipartHttpServletRequest();
		servletRequest.setContentType(MediaType.MULTIPART_FORM_DATA_VALUE);
		servletRequest.addParameter("tag", "first", "second");
		servletRequest.addFile(
				new MockMultipartFile("file", "first.txt", "text/plain", "one".getBytes(StandardCharsets.UTF_8)));
		servletRequest.addFile(new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]));
		servletRequest.addFile(
				new MockMultipartFile("file", "second.txt", "text/plain", "two".getBytes(StandardCharsets.UTF_8)));
		this.converter.setMultipartFileReader(multipartFile -> multipartFile.getOriginalFilename());

		Object result = ((HttpMessageConverter) this.converter)
				.read(byte[].class, new MultipartHttpInputMessage(servletRequest));

		MultiValueMap<String, Object> form = (MultiValueMap<String, Object>) result;
		assertThat(form.get("tag")).containsExactly("first", "second");
		assertThat(form.get("file")).containsExactly("first.txt", "second.txt");
	}

}
