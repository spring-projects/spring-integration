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

package org.springframework.integration.http.multipart;

import java.io.IOException;
import java.nio.charset.Charset;

import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.WebUtils;

/**
 * {@link MultipartFileReader} implementation that does not maintain metadata from
 * the original {@link MultipartFile} instance. Instead this simply reads the file
 * content directly as either a String or byte array depending on the Content-Type.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
public class SimpleMultipartFileReader implements MultipartFileReader<Object> {

	private volatile Charset defaultCharset = Charset.forName(WebUtils.DEFAULT_CHARACTER_ENCODING);

	/**
	 * Specify the default charset name to use when converting multipart file
	 * content into Strings if the multipart itself does not provide a charset.
	 *
	 * @param defaultCharset The default charset.
	 */
	public void setDefaultMultipartCharset(String defaultCharset) {
		this.defaultCharset = Charset.forName(
				defaultCharset != null ? defaultCharset : WebUtils.DEFAULT_CHARACTER_ENCODING);
	}

	@Override
	public Object readMultipartFile(MultipartFile multipartFile) throws IOException {
		String mpContentType = multipartFile.getContentType();
		if (mpContentType != null && mpContentType.startsWith("text")) {
			MediaType contentType = MediaType.parseMediaType(mpContentType);
			Charset charset = contentType.getCharset();
			if (charset == null) {
				charset = this.defaultCharset;
			}
			return new String(multipartFile.getBytes(), charset.name());
		}
		else {
			return multipartFile.getBytes();
		}
	}

}
