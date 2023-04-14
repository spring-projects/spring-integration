/*
 * Copyright 2016-2023 the original author or authors.
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

package org.springframework.integration.file.dsl;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import org.springframework.expression.Expression;
import org.springframework.integration.dsl.ComponentsRegistration;
import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.file.DefaultFileNameGenerator;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.file.FileWritingMessageHandler;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;

/**
 * The {@link MessageHandlerSpec} for the {@link FileWritingMessageHandler}.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
public class FileWritingMessageHandlerSpec
		extends MessageHandlerSpec<FileWritingMessageHandlerSpec, FileWritingMessageHandler>
		implements ComponentsRegistration {

	@Nullable
	private FileNameGenerator fileNameGenerator;

	@Nullable
	private DefaultFileNameGenerator defaultFileNameGenerator;

	protected FileWritingMessageHandlerSpec(File destinationDirectory) {
		this.target = new FileWritingMessageHandler(destinationDirectory);
	}

	protected FileWritingMessageHandlerSpec(String directoryExpression) {
		this(PARSER.parseExpression(directoryExpression));
	}

	protected <P> FileWritingMessageHandlerSpec(Function<Message<P>, ?> directoryFunction) {
		this(new FunctionExpression<>(directoryFunction));
	}

	protected FileWritingMessageHandlerSpec(Expression directoryExpression) {
		this.target = new FileWritingMessageHandler(directoryExpression);
	}

	FileWritingMessageHandlerSpec expectReply(boolean expectReply) {
		this.target.setExpectReply(expectReply);
		if (expectReply) {
			this.target.setRequiresReply(true);
		}
		return _this();
	}

	/**
	 * Specify whether to create the destination directory automatically if it
	 * does not yet exist upon initialization. By default, this value is
	 * <em>true</em>. If set to <em>false</em> and the
	 * destination directory does not exist, an Exception will be thrown upon
	 * initialization.
	 * @param autoCreateDirectory true to create the directory if needed.
	 * @return the current Spec
	 */
	public FileWritingMessageHandlerSpec autoCreateDirectory(boolean autoCreateDirectory) {
		this.target.setAutoCreateDirectory(autoCreateDirectory);
		return _this();
	}

	/**
	 * By default, every file that is in the process of being transferred will
	 * appear in the file system with an additional suffix, which by default is {@code .writing}.
	 * @param temporaryFileSuffix The temporary file suffix.
	 * @return the current Spec
	 */
	public FileWritingMessageHandlerSpec temporaryFileSuffix(String temporaryFileSuffix) {
		this.target.setTemporaryFileSuffix(temporaryFileSuffix);
		return _this();
	}

	/**
	 * Set the {@link FileExistsMode} that specifies what will happen in
	 * case the destination exists.
	 * @param fileExistsMode the {@link FileExistsMode} to consult.
	 * @return the current Spec
	 */
	public FileWritingMessageHandlerSpec fileExistsMode(FileExistsMode fileExistsMode) {
		this.target.setFileExistsMode(fileExistsMode);
		return _this();
	}

	/**
	 * Set the file name generator used to generate the target file name.
	 * Default {@link DefaultFileNameGenerator}.
	 * @param fileNameGenerator the file name generator.
	 * @return the current Spec
	 */
	public FileWritingMessageHandlerSpec fileNameGenerator(FileNameGenerator fileNameGenerator) {
		this.fileNameGenerator = fileNameGenerator;
		this.target.setFileNameGenerator(fileNameGenerator);
		return _this();
	}

	/**
	 * Set the {@link DefaultFileNameGenerator} based on the provided SpEL expression.
	 * @param fileNameExpression the SpEL expression for file names generation.
	 * @return the current Spec
	 */
	public FileWritingMessageHandlerSpec fileNameExpression(String fileNameExpression) {
		Assert.isNull(this.fileNameGenerator,
				"'fileNameGenerator' and 'fileNameGeneratorExpression' are mutually exclusive.");
		this.defaultFileNameGenerator = new DefaultFileNameGenerator();
		this.defaultFileNameGenerator.setExpression(fileNameExpression);
		return fileNameGenerator(this.defaultFileNameGenerator);
	}

	/**
	 * Specify whether to delete source Files after writing to the destination directory.
	 * The default is <em>false</em>. When set to <em>true</em>, it will only have an
	 * effect if the inbound Message has a File payload or a
	 * {@link org.springframework.integration.file.FileHeaders#ORIGINAL_FILE} header value
	 * containing either a File instance or a String representing the original file path.
	 * @param deleteSourceFiles true to delete the source files.
	 * @return the current Spec
	 */
	public FileWritingMessageHandlerSpec deleteSourceFiles(boolean deleteSourceFiles) {
		this.target.setDeleteSourceFiles(deleteSourceFiles);
		return _this();
	}

	/**
	 * Set the charset to use when converting String payloads to bytes as the content of the file.
	 * Default {@code UTF-8}.
	 * @param charset the charset.
	 * @return the current Spec
	 */
	public FileWritingMessageHandlerSpec charset(String charset) {
		this.target.setCharset(charset);
		return _this();
	}

	/**
	 * If {@code true} will append a new-line after each write.
	 * Defaults to {@code false}.
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
	 * @see #flushWhenIdle(boolean)
	 */
	public FileWritingMessageHandlerSpec flushInterval(long flushInterval) {
		this.target.setFlushInterval(flushInterval);
		return this;
	}

	/**
	 * Set the flush when idle flag to false if you wish the interval to apply to when
	 * the file was opened rather than when the file was last written.
	 * @param flushWhenIdle false to flush if the interval since the file was opened has elapsed.
	 * @return the spec.
	 * @see FileWritingMessageHandler#setFlushWhenIdle(boolean)
	 * @see #flushInterval(long)
	 */
	public FileWritingMessageHandlerSpec flushWhenIdle(boolean flushWhenIdle) {
		this.target.setFlushWhenIdle(flushWhenIdle);
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

	/**
	 * Set the file permissions after uploading, e.g. 0600 for
	 * owner read/write. Only applies to file systems that support posix
	 * file permissions.
	 * @param chmod the permissions.
	 * @return the spec.
	 * @throws IllegalArgumentException if the value is higher than 0777.
	 * @see FileWritingMessageHandler#setChmod(int)
	 */
	public FileWritingMessageHandlerSpec chmod(int chmod) {
		this.target.setChmod(chmod);
		return this;
	}

	@Override
	public Map<Object, String> getComponentsToRegister() {
		if (this.defaultFileNameGenerator != null) {
			return Collections.singletonMap(this.defaultFileNameGenerator, null);
		}
		return Collections.emptyMap();
	}

}
