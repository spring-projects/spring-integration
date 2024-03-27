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

package org.springframework.integration.file.transformer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

/**
 * A payload transformer that copies a File's contents to a String.
 *
 * @author Mark Fisher
 * @author Gary Russell
 */
public class FileToStringTransformer extends AbstractFilePayloadTransformer<String> {

	private volatile Charset charset = Charset.defaultCharset();

	/**
	 * Set the charset name to use when copying the File to a String.
	 *
	 * @param charset The charset.
	 */
	public void setCharset(String charset) {
		Assert.notNull(charset, "charset must not be null");
		Assert.isTrue(Charset.isSupported(charset), "Charset '" + charset + "' is not supported.");
		this.charset = Charset.forName(charset);
	}

	@Override
	protected final String transformFile(File file) throws IOException {
		Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), this.charset));
		return FileCopyUtils.copyToString(reader);
	}

}
