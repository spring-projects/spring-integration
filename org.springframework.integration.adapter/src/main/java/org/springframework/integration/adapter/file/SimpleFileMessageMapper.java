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
import java.io.FileWriter;
import java.io.IOException;

import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.MessageMapper;
import org.springframework.util.FileCopyUtils;

/**
 * A default {@link MessageMapper} for {@link FileTarget}, converting payloads of the
 * {@link File}, {@code byte[]} and {@link String} types to files. The name of the newly
 * created file is defined by the {@link FileNameGenerator} instance configured with it.
 * By default, it uses a {@link DefaultFileNameGenerator}.
 * 
 * @author Marius Bogoevici
 */
public class SimpleFileMessageMapper implements MessageMapper<Object, File> {

	private volatile FileNameGenerator fileNameGenerator = new DefaultFileNameGenerator();

	private final File parentDirectory;


	public SimpleFileMessageMapper(String parentDirectoryPath) {
		this(new File(parentDirectoryPath));
	}

	public SimpleFileMessageMapper(File parentDirectory) {
		this.parentDirectory = parentDirectory;
	}


	public void setFileNameGenerator(FileNameGenerator fileNameGenerator) {
		this.fileNameGenerator = fileNameGenerator;
	}

	public File mapMessage(Message<Object> message) {
		try {
			File file = new File(parentDirectory, this.fileNameGenerator.generateFileName(message));
			this.writeToFile(file, message.getPayload());
			return file;
		}
		catch (Exception e) {
			throw new MessageHandlingException(message, "failure occurred mapping file to message", e);
		}
	}

	public void writeToFile(File file, Object payload) throws IOException {
		if (payload instanceof byte[]) {
			FileCopyUtils.copy((byte[]) payload, file);
		}
		else if (payload instanceof String) {
			FileCopyUtils.copy((String) payload, new FileWriter(file));
		}
		else if (payload instanceof File) {
			FileCopyUtils.copy((File) payload, file);
		}
	}

}
