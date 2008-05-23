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
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.adapter.ftp.DirectoryContentManager;
import org.springframework.integration.adapter.ftp.FileInfo;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageCreator;
import org.springframework.integration.message.MessageDeliveryAware;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.message.Source;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A messaging source that polls a directory to retrieve files.
 * 
 * @author Mark Fisher
 * @author Marius Bogoevici
 */
public class FileSource implements Source<Object>, InitializingBean, MessageDeliveryAware {
	
	private final Log logger = LogFactory.getLog(this.getClass());

	private final File directory;

	private volatile MessageCreator<File, ?> messageCreator;
	
	private volatile FileFilter fileFilter;

	private volatile FilenameFilter filenameFilter;

	private final DirectoryContentManager directoryContentManager = new DirectoryContentManager();
	
	
	public FileSource(File directory) {
		this(directory, new FileMessageCreator());
	}

	public FileSource(File directory, MessageCreator<File, ?> messageCreator) {
		Assert.notNull(directory, "directory must not be null");
		this.directory = directory;
		Assert.notNull(messageCreator, "MessageCreator must not be null");
		this.messageCreator = messageCreator;
	}

	
	public void setMessageCreator(MessageCreator<File, ?> messageCreator) {
		this.messageCreator = messageCreator;
	}

	public void setFileFilter(FileFilter fileFilter) {
		this.fileFilter = fileFilter;
	}

	public void setFilenameFilter(FilenameFilter filenameFilter) {
		this.filenameFilter = filenameFilter;
	}

	public void afterPropertiesSet() {
		if (null == messageCreator) {
			messageCreator = new FileMessageCreator();
		}
	}

	public Message receive() {
		File[] files = null;
		if (this.fileFilter != null) {
			files = this.directory.listFiles(this.fileFilter);
		}
		else if (this.filenameFilter != null) {
			files = this.directory.listFiles(this.filenameFilter);
		}
		else {
			files = this.directory.listFiles();
		}
		if (files == null) {
			throw new MessagingException("Problem occurred while polling for files. " +
					"Is '" + directory.getAbsolutePath() + "' a directory?");
		}
		HashMap<String, FileInfo> snapshot = new HashMap<String, FileInfo>();
		for (int i = 0; i < files.length; i++) {
			FileInfo fileInfo = new FileInfo(files[i].getName(), files[i].lastModified(), files[i].length());
			snapshot.put(files[i].getName(), fileInfo);
		}
		this.directoryContentManager.processSnapshot(snapshot);
		if (!this.directoryContentManager.getBacklog().isEmpty()) {
			String fileName = this.directoryContentManager.getBacklog().keySet().iterator().next();
			File file = new File(directory, fileName);
			return this.messageCreator.createMessage(file);
		}
		return null;
	}

	public void onSend(Message<?> message) {
		String filename = message.getHeader().getProperty(FileNameGenerator.FILENAME_PROPERTY_KEY);
		if (StringUtils.hasText(filename)) {
			this.directoryContentManager.fileProcessed(filename);
		}
		else if (this.logger.isWarnEnabled()) {
			logger.warn("No filename in Message header, cannot send notification of processing.");
		}
	}

	public void onFailure(MessagingException exception) {
		if (this.logger.isWarnEnabled()) {
			logger.warn("FtpSource received failure notifcation", exception);
		}
	}
	
}
