/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.file.config;

import java.io.File;

import org.springframework.expression.Expression;
import org.springframework.integration.JavaUtils;
import org.springframework.integration.config.AbstractSimpleMessageHandlerFactoryBean;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.file.FileWritingMessageHandler;
import org.springframework.integration.file.FileWritingMessageHandler.MessageFlushPredicate;
import org.springframework.integration.file.support.FileExistsMode;

/**
 * Factory bean used to create {@link FileWritingMessageHandler}s.
 *
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Gunnar Hillert
 * @author Tony Falabella
 *
 * @since 1.0.3
 */
public class FileWritingMessageHandlerFactoryBean
		extends AbstractSimpleMessageHandlerFactoryBean<FileWritingMessageHandler> {

	private File directory;

	private Expression directoryExpression;

	private String charset;

	private FileNameGenerator fileNameGenerator;

	private Boolean deleteSourceFiles;

	private Boolean autoCreateDirectory;

	private Boolean requiresReply;

	private Long sendTimeout;

	private String temporaryFileSuffix;

	private FileExistsMode fileExistsMode;

	private boolean expectReply = true;

	private Integer bufferSize;

	private Boolean appendNewLine;

	private Long flushInterval;

	private Boolean flushWhenIdle;

	private MessageFlushPredicate flushPredicate;

	private String chmod;

	private Boolean preserveTimestamp;

	public void setFileExistsMode(String fileExistsModeAsString) {
		this.fileExistsMode = FileExistsMode.getForString(fileExistsModeAsString);
	}

	public void setDirectory(File directory) {
		this.directory = directory;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	public void setDirectoryExpression(Expression directoryExpression) {
		this.directoryExpression = directoryExpression;
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

	public void setAppendNewLine(Boolean appendNewLine) {
		this.appendNewLine = appendNewLine;
	}

	public void setBufferSize(Integer bufferSize) {
		this.bufferSize = bufferSize;
	}

	public void setFlushInterval(long flushInterval) {
		this.flushInterval = flushInterval;
	}

	public void setFlushWhenIdle(boolean flushWhenIdle) {
		this.flushWhenIdle = flushWhenIdle;
	}

	public void setFlushPredicate(MessageFlushPredicate flushPredicate) {
		this.flushPredicate = flushPredicate;
	}

	public void setChmod(String chmod) {
		this.chmod = chmod;
	}

	public void setPreserveTimestamp(Boolean preserveTimestamp) {
		this.preserveTimestamp = preserveTimestamp;
	}

	@Override
	protected FileWritingMessageHandler createHandler() {

		final FileWritingMessageHandler handler;

		if (this.directory != null && this.directoryExpression != null) {
			throw new IllegalStateException("Cannot set both directory and directoryExpression");
		}
		else if (this.directory != null) {
			handler = new FileWritingMessageHandler(this.directory);
		}
		else if (this.directoryExpression != null) {
			handler = new FileWritingMessageHandler(this.directoryExpression);
		}
		else {
			throw new IllegalStateException("Either directory or directoryExpression must not be null");
		}

		handler.setExpectReply(this.expectReply);
		JavaUtils.INSTANCE
				.acceptIfNotNull(this.charset, handler::setCharset)
				.acceptIfNotNull(this.fileNameGenerator, handler::setFileNameGenerator)
				.acceptIfNotNull(this.deleteSourceFiles, handler::setDeleteSourceFiles)
				.acceptIfNotNull(this.autoCreateDirectory, handler::setAutoCreateDirectory)
				.acceptIfNotNull(this.requiresReply, handler::setRequiresReply)
				.acceptIfNotNull(this.sendTimeout, handler::setSendTimeout)
				.acceptIfNotNull(this.temporaryFileSuffix, handler::setTemporaryFileSuffix)
				.acceptIfNotNull(this.appendNewLine, handler::setAppendNewLine)
				.acceptIfNotNull(this.fileExistsMode, handler::setFileExistsMode)
				.acceptIfNotNull(this.bufferSize, handler::setBufferSize)
				.acceptIfNotNull(this.flushInterval, handler::setFlushInterval)
				.acceptIfNotNull(this.flushWhenIdle, handler::setFlushWhenIdle)
				.acceptIfNotNull(this.flushPredicate, handler::setFlushPredicate)
				.acceptIfNotNull(this.chmod, handler::setChmodOctal)
				.acceptIfNotNull(this.preserveTimestamp, handler::setPreserveTimestamp);

		return handler;
	}

}
