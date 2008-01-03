/*
 * Copyright 2002-2007 the original author or authors.
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

import org.springframework.integration.adapter.DefaultTargetAdapter;
import org.springframework.integration.adapter.Target;
import org.springframework.integration.message.MessageMapper;
import org.springframework.util.Assert;

/**
 * A convenience adapter for writing files. The actual file writing occurs in
 * the message mapper ({@link TextFileMapper} or {@link ByteArrayFileMapper}).
 * 
 * @author Mark Fisher
 */
public class FileTargetAdapter extends DefaultTargetAdapter {

	public FileTargetAdapter(File directory) {
		this(directory, true);
	}

	public FileTargetAdapter(File directory, boolean isTextBased) {
		super(new FileTarget());
		if (isTextBased) {
			this.setMessageMapper(new TextFileMapper(directory));
		}
		else {
			this.setMessageMapper(new ByteArrayFileMapper(directory));
		}
	}

	public void setFileNameGenerator(FileNameGenerator fileNameGenerator) {
		Assert.notNull(fileNameGenerator, "'fileNameGenerator' must not be null");
		MessageMapper<?,?> mapper = this.getMessageMapper();
		if (mapper instanceof AbstractFileMapper<?>) {
			((AbstractFileMapper<?>) mapper).setFileNameGenerator(fileNameGenerator);
		}
	}


	private static class FileTarget implements Target<File> {

		public boolean send(File file) {
			return file.exists();
		}
	}

}
