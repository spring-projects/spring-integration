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
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import org.springframework.core.io.Resource;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageHandler;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

/**
 * A {@link MessageHandler} implementation that writes the Message payload to a
 * file. If the payload is a File object, it will copy the File to this
 * consumer's directory. If the payload is a byte array or String, it will write
 * it directly. Otherwise, the payload type is unsupported, and an Exception
 * will be thrown.
 * <p>
 * Other transformers may be useful to precede this handler. For example, any
 * Serializable object payload can be converted into a byte array by the
 * {@link org.springframework.integration.transformer.PayloadSerializingTransformer}
 * . Likewise, any Object can be converted to a String based on its
 * <code>toString()</code> method by the
 * {@link org.springframework.integration.transformer.ObjectToStringTransformer}.
 * 
 * @author Mark Fisher
 * @author Iwein Fuld
 */
public class FileWritingMessageHandler implements MessageHandler {

	private static final String TEMPORARY_FILE_SUFFIX =".writing";
	
	private volatile FileNameGenerator fileNameGenerator = new DefaultFileNameGenerator();

	private final File parentDirectory;

	private volatile Charset charset = Charset.defaultCharset();
	
	public FileWritingMessageHandler(Resource parentDirectory) {
		try {
			Assert.isTrue(parentDirectory.exists(), "Output directory [" + parentDirectory + "] does not exist");
			this.parentDirectory = parentDirectory.getFile();
			Assert.isTrue(this.parentDirectory.isDirectory(), "[" + this.parentDirectory + "] is not a directory");
			Assert.isTrue(this.parentDirectory.canWrite(), "[" + this.parentDirectory + "] should be writable");			
		}
		catch (IOException e) {
			throw new IllegalArgumentException("Inaccessable output directory", e);
		}
	}

	public void setFileNameGenerator(FileNameGenerator fileNameGenerator) {
		this.fileNameGenerator = fileNameGenerator;
	}

	/**
	 * Set the charset name to use when writing a File from a String-based
	 * Message payload.
	 */
	public void setCharset(String charset) {
		Assert.notNull(charset, "charset must not be null");
		Assert.isTrue(Charset.isSupported(charset), "Charset '" + charset + "' is not supported.");
		this.charset = Charset.forName(charset);
	}

	public void handleMessage(Message<?> message) {
		Assert.notNull(message, "message must not be null");
		Object payload = message.getPayload();
		Assert.notNull(payload, "message payload must not be null");
		String generatedFileName = this.fileNameGenerator.generateFileName(message);
		File file = new File(parentDirectory, generatedFileName+TEMPORARY_FILE_SUFFIX);
		try {
			if (payload instanceof File) {
				FileCopyUtils.copy((File) payload, file);
			}
			else if (payload instanceof byte[]) {
				FileCopyUtils.copy((byte[]) payload, file);
			}
			else if (payload instanceof String) {
				OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), this.charset);
				FileCopyUtils.copy((String) payload, writer);
			}
			else {
				throw new IllegalArgumentException("unsupported Message payload type [" + payload.getClass().getName()
						+ "]");
			}
			file.renameTo(new File(parentDirectory, generatedFileName));
		}
		catch (Exception e) {
			throw new MessageHandlingException(message, "failed to write Message payload to file", e);
		}
	}

}
