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
import java.io.FileReader;
import java.io.FileWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.message.AbstractMessageMapper;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

/**
 * Base class providing common behavior for file-based message mappers.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractFileMapper<T> extends AbstractMessageMapper<T, File> {

	protected Log logger = LogFactory.getLog(this.getClass());

	private File parentDirectory;

	private File backupDirectory;

	private FileNameGenerator fileNameGenerator = new DefaultFileNameGenerator();


	public AbstractFileMapper(File parentDirectory) {
		this.parentDirectory = parentDirectory;
	}

	public void setBackupDirectory(File backupDirectory) {
		this.backupDirectory = backupDirectory;
	}

	public void setFileNameGenerator(FileNameGenerator fileNameGenerator) {
		Assert.notNull(fileNameGenerator, "'fileNameGenerator' must not be null");
		this.fileNameGenerator = fileNameGenerator;
	}

	public File fromMessage(Message<T> message) {
		try {
			File file = new File(parentDirectory, this.fileNameGenerator.generateFileName(message));
			this.writeToFile(file, message.getPayload());
			return file;
		}
		catch (Exception e) {
			throw new MessageHandlingException("failure occurred mapping file to message", e);
		}
	}

	public Message<T> toMessage(File file) {
		try {
			T payload = this.readMessagePayload(file);
			if (payload == null) {
				return null;
			}
			Message<T> message = new GenericMessage<T>(this.getUidGenerator().generateUid(), payload);
			if (this.backupDirectory != null) {
				FileWriter writer = new FileWriter(this.backupDirectory.getAbsolutePath() +
						File.separator + file.getName());
				FileCopyUtils.copy(new FileReader(file), writer);
			}
			file.delete();
			return message;
		}
		catch (Exception e) {
			String errorMessage = "failure occurred mapping file to message";
			if (logger.isWarnEnabled()) {
				logger.warn(errorMessage, e);
			}
			throw new MessageHandlingException(errorMessage, e);
		}
	}

	protected abstract T readMessagePayload(File file) throws Exception;

	protected abstract void writeToFile(File file, T payload) throws Exception;

}
