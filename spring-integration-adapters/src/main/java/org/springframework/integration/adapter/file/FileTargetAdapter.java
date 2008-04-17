/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.adapter.file;

import java.io.File;

import org.springframework.integration.message.Message;
import org.springframework.integration.message.Target;
import org.springframework.util.Assert;

/**
 * A convenience adapter for writing files. The actual file writing occurs in
 * the message mapper ({@link TextFileMapper} or {@link ByteArrayFileMapper}).
 * 
 * @author Mark Fisher
 */
public class FileTargetAdapter implements Target {

	private AbstractFileMapper<?> mapper;


	public FileTargetAdapter(File directory) {
		this(directory, true);
	}

	public FileTargetAdapter(File directory, boolean isTextBased) {
		if (isTextBased) {
			this.mapper = new TextFileMapper(directory);
		}
		else {
			this.mapper = new ByteArrayFileMapper(directory);
		}
	}

	public void setFileNameGenerator(FileNameGenerator fileNameGenerator) {
		Assert.notNull(fileNameGenerator, "'fileNameGenerator' must not be null");
		if (mapper instanceof AbstractFileMapper<?>) {
			((AbstractFileMapper<?>) mapper).setFileNameGenerator(fileNameGenerator);
		}
	}

	public boolean send(Message message) {
		File file = this.mapper.mapMessage(message);
		return file.exists();
	}

}
