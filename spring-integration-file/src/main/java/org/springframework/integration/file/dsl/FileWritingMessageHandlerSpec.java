/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.file.dsl;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

import org.springframework.expression.Expression;
import org.springframework.integration.dsl.ComponentsRegistration;
import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.file.DefaultFileNameGenerator;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.file.FileWritingMessageHandler;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.messaging.Message;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;

/**
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class FileWritingMessageHandlerSpec
		extends MessageHandlerSpec<FileWritingMessageHandlerSpec, FileWritingMessageHandler>
		implements ComponentsRegistration {

	private FileNameGenerator fileNameGenerator;

	private DefaultFileNameGenerator defaultFileNameGenerator;

	FileWritingMessageHandlerSpec(File destinationDirectory) {
		this.target = new FileWritingMessageHandler(destinationDirectory);
	}

	FileWritingMessageHandlerSpec(String directoryExpression) {
		this(PARSER.parseExpression(directoryExpression));
	}

	<P> FileWritingMessageHandlerSpec(Function<Message<P>, ?> directoryFunction) {
		this(new FunctionExpression<>(directoryFunction));
	}

	FileWritingMessageHandlerSpec(Expression directoryExpression) {
		this.target = new FileWritingMessageHandler(directoryExpression);
	}

	FileWritingMessageHandlerSpec expectReply(boolean expectReply) {
		this.target.setExpectReply(expectReply);
		if (expectReply) {
			this.target.setRequiresReply(true);
		}
		return _this();
	}

	public FileWritingMessageHandlerSpec autoCreateDirectory(boolean autoCreateDirectory) {
		this.target.setAutoCreateDirectory(autoCreateDirectory);
		return _this();
	}

	public FileWritingMessageHandlerSpec temporaryFileSuffix(String temporaryFileSuffix) {
		this.target.setTemporaryFileSuffix(temporaryFileSuffix);
		return _this();
	}

	public FileWritingMessageHandlerSpec fileExistsMode(FileExistsMode fileExistsMode) {
		this.target.setFileExistsMode(fileExistsMode);
		return _this();
	}

	public FileWritingMessageHandlerSpec fileNameGenerator(FileNameGenerator fileNameGenerator) {
		this.fileNameGenerator = fileNameGenerator;
		this.target.setFileNameGenerator(fileNameGenerator);
		return _this();
	}

	public FileWritingMessageHandlerSpec fileNameExpression(String fileNameExpression) {
		Assert.isNull(this.fileNameGenerator,
				"'fileNameGenerator' and 'fileNameGeneratorExpression' are mutually exclusive.");
		this.defaultFileNameGenerator = new DefaultFileNameGenerator();
		this.defaultFileNameGenerator.setExpression(fileNameExpression);
		return fileNameGenerator(this.defaultFileNameGenerator);
	}


	public FileWritingMessageHandlerSpec deleteSourceFiles(boolean deleteSourceFiles) {
		this.target.setDeleteSourceFiles(deleteSourceFiles);
		return _this();
	}

	public FileWritingMessageHandlerSpec charset(String charset) {
		this.target.setCharset(charset);
		return _this();
	}

	/**
	 * @param appendNewLine true if a new-line should be written to the file after payload is written.
	 * @return the spec.
	 * @see FileWritingMessageHandler#setAppendNewLine(boolean)
	 */
	public FileWritingMessageHandlerSpec appendNewLine(boolean appendNewLine) {
		this.target.setAppendNewLine(appendNewLine);
		return this;
	}

	/**
	 * Set the buffer size to use while writing to files; default 8192.
	 * @param bufferSize the buffer size.
	 * @return the spec.
	 * @see FileWritingMessageHandler#setBufferSize(int)
	 */
	public FileWritingMessageHandlerSpec bufferSize(int bufferSize) {
		this.target.setBufferSize(bufferSize);
		return this;
	}

	/**
	 * Set the frequency to flush buffers when {@link FileExistsMode#APPEND_NO_FLUSH} is
	 * being used.
	 * @param flushInterval the interval.
	 * @return the spec.
	 * @see FileWritingMessageHandler#setBufferSize(int)
	 */
	public FileWritingMessageHandlerSpec flushInterval(long flushInterval) {
		this.target.setFlushInterval(flushInterval);
		return this;
	}

	/**
	 * Specify a {@link TaskScheduler} for flush task when the {@link FileExistsMode#APPEND_NO_FLUSH} is in use.
	 * @param taskScheduler the {@link TaskScheduler} to use.
	 * @return the spec.
	 * @see FileWritingMessageHandler#setTaskScheduler(TaskScheduler)
	 */
	public FileWritingMessageHandlerSpec taskScheduler(TaskScheduler taskScheduler) {
		this.target.setTaskScheduler(taskScheduler);
		return this;
	}

	/**
	 * Specify a {@link FileWritingMessageHandler.MessageFlushPredicate} for flush task
	 * when the {@link FileExistsMode#APPEND_NO_FLUSH} is in use.
	 * @param flushPredicate the {@link FileWritingMessageHandler.MessageFlushPredicate} to use.
	 * @return the spec.
	 * @see FileWritingMessageHandler#setFlushPredicate(FileWritingMessageHandler.MessageFlushPredicate)
	 */
	public FileWritingMessageHandlerSpec flushPredicate(
			FileWritingMessageHandler.MessageFlushPredicate flushPredicate) {
		this.target.setFlushPredicate(flushPredicate);
		return this;
	}

	/**
	 * Set to true to preserve the destination file timestamp. If true and
	 * the payload is a {@link File}, the payload's {@code lastModified} time will be
	 * transferred to the destination file.
	 * @param preserveTimestamp the {@code boolean} flag to use.
	 * @return the spec.
	 * @see FileWritingMessageHandler#setPreserveTimestamp(boolean)
	 */
	public FileWritingMessageHandlerSpec preserveTimestamp(boolean preserveTimestamp) {
		this.target.setPreserveTimestamp(preserveTimestamp);
		return this;
	}

	@Override
	public Collection<Object> getComponentsToRegister() {
		if (this.defaultFileNameGenerator != null) {
			return Collections.singletonList(this.defaultFileNameGenerator);
		}
		return null;
	}

}
