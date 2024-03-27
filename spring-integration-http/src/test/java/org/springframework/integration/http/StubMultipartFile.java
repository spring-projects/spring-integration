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

package org.springframework.integration.http;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.web.multipart.MultipartFile;

/**
 * @author Mark Fisher
 */
public class StubMultipartFile implements MultipartFile {

	private final String parameterName;

	private final String filename;

	private final byte[] bytes;

	private final String text;

	public StubMultipartFile(String parameterName, String filename, String text) {
		this.parameterName = parameterName;
		this.filename = filename;
		if (text != null) {
			this.bytes = text.getBytes();
		}
		else {
			this.bytes = null;
		}
		this.text = text;
	}

	public StubMultipartFile(String parameterName, String filename, byte[] bytes) {
		this.parameterName = parameterName;
		this.filename = filename;
		this.bytes = bytes;
		this.text = null;
	}

	public byte[] getBytes() throws IOException {
		return this.bytes;
	}

	public String getContentType() {
		return (this.text != null) ? "text" : null;
	}

	public InputStream getInputStream() throws IOException {
		return new ByteArrayInputStream(this.bytes);
	}

	public String getName() {
		return this.parameterName;
	}

	public String getOriginalFilename() {
		return this.filename;
	}

	public long getSize() {
		return this.bytes.length;
	}

	public boolean isEmpty() {
		return this.bytes == null || this.bytes.length == 0;
	}

	public void transferTo(File dest) throws IOException, IllegalStateException {
		FileOutputStream fos = new FileOutputStream(dest);
		fos.write(this.bytes);
		fos.close();
	}

}
