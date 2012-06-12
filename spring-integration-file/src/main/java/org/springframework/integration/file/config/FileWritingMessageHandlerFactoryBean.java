/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.integration.file.config;

import java.io.File;

import org.springframework.integration.config.AbstractSimpleMessageHandlerFactoryBean;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.file.FileWritingMessageHandler;

/**
 * Factory bean used to create {@link FileWritingMessageHandler}s.
 *
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @since 1.0.3
 */
public class FileWritingMessageHandlerFactoryBean extends AbstractSimpleMessageHandlerFactoryBean<FileWritingMessageHandler>{

	private volatile File directory;

	private volatile String charset;

	private volatile FileNameGenerator fileNameGenerator;

	private volatile Boolean deleteSourceFiles;

	private volatile Boolean autoCreateDirectory;

	private volatile Boolean requiresReply;

	private volatile Long sendTimeout;

	private volatile String temporaryFileSuffix;

	private volatile boolean appendIfExists;

	private volatile boolean expectReply = true;

	public void setAppendIfExists(boolean appendIfExists) {
		this.appendIfExists = appendIfExists;
	}

	public void setDirectory(File directory) {
		this.directory = directory;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	public void setFileNameGenerator(FileNameGenerator fileNameGenerator) {
		this.fileNameGenerator = fileNameGenerator;
	}

	public void setDeleteSourceFiles(Boolean deleteSourceFiles) {
		this.deleteSourceFiles = deleteSourceFiles;
	}

	public void setAutoCreateDirectory(Boolean autoCreateDirectory) {
		this.autoCreateDirectory = autoCreateDirectory;
	}

	public void setRequiresReply(Boolean requiresReply) {
		this.requiresReply = requiresReply;
	}

	public void setSendTimeout(Long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	public void setTemporaryFileSuffix(String temporaryFileSuffix) {
		this.temporaryFileSuffix = temporaryFileSuffix;
	}

	public void setExpectReply(boolean expectReply) {
		this.expectReply = expectReply;
	}

	@Override
	protected FileWritingMessageHandler createHandler() {
		FileWritingMessageHandler handler = new FileWritingMessageHandler(this.directory);
		if (this.charset != null) {
			handler.setCharset(this.charset);
		}
		if (this.fileNameGenerator != null) {
			handler.setFileNameGenerator(this.fileNameGenerator);
		}
		if (this.deleteSourceFiles != null) {
			handler.setDeleteSourceFiles(this.deleteSourceFiles);
		}
		if (this.autoCreateDirectory != null) {
			handler.setAutoCreateDirectory(this.autoCreateDirectory);
		}
		if (this.requiresReply != null) {
			handler.setRequiresReply(this.requiresReply);
		}
		if (this.sendTimeout != null) {
			handler.setSendTimeout(this.sendTimeout);
		}
		if (this.temporaryFileSuffix != null) {
			handler.setTemporaryFileSuffix(this.temporaryFileSuffix);
		}
		handler.setExpectReply(this.expectReply);
		handler.setAppendIfExists(this.appendIfExists);
		return handler;
	}
}
