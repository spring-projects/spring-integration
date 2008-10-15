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

package org.springframework.integration.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageConsumer;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

/**
 * A {@link MessageConsumer} implementation that writes the Message payload to a
 * file. If the payload is a File object, it will copy the File to this
 * consumer's directory. If the payload is a byte array or String, it will
 * write it directly. Otherwise, it will invoke toString on the payload Object.
 * 
 * @author Mark Fisher
 */
public class FileWritingMessageConsumer implements MessageConsumer {

	private volatile FileNameGenerator fileNameGenerator = new DefaultFileNameGenerator();

	private final File parentDirectory;

	private volatile Charset charset = Charset.defaultCharset(); 


	public FileWritingMessageConsumer(String parentDirectoryPath) {
		this(new File(parentDirectoryPath));
	}

	public FileWritingMessageConsumer(File parentDirectory) {
		this.parentDirectory = parentDirectory;
	}


	public void setFileNameGenerator(FileNameGenerator fileNameGenerator) {
		this.fileNameGenerator = fileNameGenerator;
	}

	/**
	 * Set the charset name to use when writing a File from a
	 * String-based Message payload.
	 */
	public void setCharset(String charset) {
		Assert.notNull(charset, "charset must not be null");
		Assert.isTrue(Charset.isSupported(charset), "Charset '" + charset + "' is not supported.");
		this.charset = Charset.forName(charset);
	}

	public void onMessage(Message<?> message) {
		Assert.notNull(message, "message must not be null");
		Object payload = message.getPayload();
		Assert.notNull(payload, "message payload must not be null");
		File file = new File(parentDirectory, this.fileNameGenerator.generateFileName(message));
		try {
			if (payload instanceof byte[]) {
				FileCopyUtils.copy((byte[]) payload, file);
			}
			else if (payload instanceof File) {
				FileCopyUtils.copy((File) payload, file);
			}
			else {
				OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), this.charset);
				FileCopyUtils.copy(payload.toString(), writer);
			}
		}
		catch (Exception e) {
			throw new MessageHandlingException(message, "failed to write Message payload to file", e);
		}
	}

}
