/*
 * Copyright 2002-2016 the original author or authors.
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.Lifecycle;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.handler.MessageTriggerAction;
import org.springframework.integration.support.locks.DefaultLockRegistry;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.integration.support.locks.PassThruLockRegistry;
import org.springframework.integration.util.WhileLockedProcessor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * A {@link MessageHandler} implementation that writes the Message payload to a
 * file. If the payload is a File object, it will copy the File to the specified
 * destination directory. If the payload is a byte array, a String or an
 * InputStream it will be written directly. Otherwise, the payload type is
 * unsupported, and an Exception will be thrown.
 * <p>
 * To append a new-line after each write, set the
 * {@link #setAppendNewLine(boolean) appendNewLine} flag to 'true'. It is 'false' by default.
 * <p>
 * If the 'deleteSourceFiles' flag is set to true, the original Files will be
 * deleted. The default value for that flag is <em>false</em>. See the
 * {@link #setDeleteSourceFiles(boolean)} method javadoc for more information.
 * <p>
 * Other transformers may be useful to precede this handler. For example, any
 * Serializable object payload can be converted into a byte array by the
 * {@link org.springframework.integration.transformer.PayloadSerializingTransformer}.
 * Likewise, any Object can be converted to a String based on its
 * <code>toString()</code> method by the
 * {@link org.springframework.integration.transformer.ObjectToStringTransformer}.
 * <p>
 * {@link FileExistsMode#APPEND} adds content to an existing file; the file is closed after
 * each write.
 * {@link FileExistsMode#APPEND_NO_FLUSH} adds content to an existing file and the file
 * is left open without flushing any data. Data will be flushed based on the
 * {@link #setFlushInterval(long) flushInterval} or when a message is sent to the
 * {@link #trigger(Message)} method, or a
 * {@link #flushIfNeeded(MessageFlushPredicate, Message) flushIfNeeded}
 * method is called.
 *
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Alex Peters
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Tony Falabella
 */
public class FileWritingMessageHandler extends AbstractReplyProducingMessageHandler
		implements Lifecycle, MessageTriggerAction {

	private static final boolean nioFilesPresent = ClassUtils.isPresent("java.nio.file.Files",
			FileWritingMessageHandler.class.getClassLoader());

	private static final String LINE_SEPARATOR = System.getProperty("line.separator");

	private static final int DEFAULT_BUFFER_SIZE = 8192;

	private static final long DEFAULT_FLUSH_INTERVAL = 30000L;

	private final Map<String, FileState> fileStates = new HashMap<String, FileState>();

	private volatile String temporaryFileSuffix = ".writing";

	private volatile boolean temporaryFileSuffixSet = false;

	private volatile FileExistsMode fileExistsMode = FileExistsMode.REPLACE;

	private final Log logger = LogFactory.getLog(this.getClass());

	private volatile FileNameGenerator fileNameGenerator = new DefaultFileNameGenerator();

	private volatile boolean fileNameGeneratorSet;

	private volatile StandardEvaluationContext evaluationContext;

	private final Expression destinationDirectoryExpression;

	private volatile boolean autoCreateDirectory = true;

	private volatile boolean deleteSourceFiles;

	private volatile Charset charset = Charset.defaultCharset();

	private volatile boolean expectReply = true;

	private volatile boolean appendNewLine = false;

	private volatile LockRegistry lockRegistry = new PassThruLockRegistry();

	private volatile int bufferSize = DEFAULT_BUFFER_SIZE;

	private volatile long flushInterval = DEFAULT_FLUSH_INTERVAL;

	private volatile ScheduledFuture<?> flushTask;

	private volatile MessageFlushPredicate flushPredicate = new DefaultFlushPredicate();

	private volatile boolean preserveTimestamp;

	/**
	 * Constructor which sets the {@link #destinationDirectoryExpression} using
	 * a {@link LiteralExpression}.
	 *
	 * @param destinationDirectory Must not be null
	 * @see #FileWritingMessageHandler(Expression)
	 */
	public FileWritingMessageHandler(File destinationDirectory) {
		Assert.notNull(destinationDirectory, "Destination directory must not be null.");
		this.destinationDirectoryExpression = new LiteralExpression(destinationDirectory.getPath());
	}

	/**
	 * Constructor which sets the {@link #destinationDirectoryExpression}.
	 *
	 * @param destinationDirectoryExpression Must not be null
	 * @see #FileWritingMessageHandler(File)
	 */
	public FileWritingMessageHandler(Expression destinationDirectoryExpression) {
		Assert.notNull(destinationDirectoryExpression, "Destination directory expression must not be null.");
		this.destinationDirectoryExpression = destinationDirectoryExpression;
	}

	/**
	 * Specify whether to create the destination directory automatically if it
	 * does not yet exist upon initialization. By default, this value is
	 * <em>true</em>. If set to <em>false</em> and the
	 * destination directory does not exist, an Exception will be thrown upon
	 * initialization.
	 *
	 * @param autoCreateDirectory true to create the directory if needed.
	 */
	public void setAutoCreateDirectory(boolean autoCreateDirectory) {
		this.autoCreateDirectory = autoCreateDirectory;
	}

	/**
	 * By default, every file that is in the process of being transferred will
	 * appear in the file system with an additional suffix, which by default is
	 * ".writing". This can be changed by setting this property.
	 *
	 * @param temporaryFileSuffix The temporary file suffix.
	 */
	public void setTemporaryFileSuffix(String temporaryFileSuffix) {
		Assert.notNull(temporaryFileSuffix, "'temporaryFileSuffix' must not be null"); // empty string is OK
		this.temporaryFileSuffix = temporaryFileSuffix;
		this.temporaryFileSuffixSet = true;
	}


	/**
	 * Will set the {@link FileExistsMode} that specifies what will happen in
	 * case the destination exists. For example {@link FileExistsMode#APPEND}
	 * instructs this handler to append data to the existing file rather then
	 * creating a new file for each {@link Message}.
	 * <p>
	 * If set to {@link FileExistsMode#APPEND}, the adapter will also
	 * create a real instance of the {@link LockRegistry} to ensure that there
	 * is no collisions when multiple threads are writing to the same file.
	 * <p>
	 * Otherwise the LockRegistry is set to {@link PassThruLockRegistry} which
	 * has no effect.
	 *
	 * @param fileExistsMode Must not be null
	 */
	public void setFileExistsMode(FileExistsMode fileExistsMode) {

		Assert.notNull(fileExistsMode, "'fileExistsMode' must not be null.");
		this.fileExistsMode = fileExistsMode;

		if (FileExistsMode.APPEND.equals(fileExistsMode)
				|| FileExistsMode.APPEND_NO_FLUSH.equals(this.fileExistsMode)) {
			this.lockRegistry = this.lockRegistry instanceof PassThruLockRegistry
					? new DefaultLockRegistry()
					: this.lockRegistry;
		}
	}

	/**
	 * Specify whether a reply Message is expected. If not, this handler will simply return null for a
	 * successful response or throw an Exception for a non-successful response. The default is true.
	 *
	 * @param expectReply true if a reply is expected.
	 */
	public void setExpectReply(boolean expectReply) {
		this.expectReply = expectReply;
	}

	/**
	 * If 'true' will append a new-line after each write. It is 'false' by default.
	 *
	 * @param appendNewLine true if a new-line should be written to the file after payload is written
	 * @since 4.0.7
	 */
	public void setAppendNewLine(boolean appendNewLine) {
		this.appendNewLine = appendNewLine;
	}

	protected String getTemporaryFileSuffix() {
		return this.temporaryFileSuffix;
	}

	/**
	 * Provide the {@link FileNameGenerator} strategy to use when generating
	 * the destination file's name.
	 *
	 * @param fileNameGenerator The file name generator.
	 */
	public void setFileNameGenerator(FileNameGenerator fileNameGenerator) {
		Assert.notNull(fileNameGenerator, "FileNameGenerator must not be null");
		this.fileNameGenerator = fileNameGenerator;
		this.fileNameGeneratorSet = true;
	}

	/**
	 * Specify whether to delete source Files after writing to the destination
	 * directory. The default is <em>false</em>. When set to <em>true</em>, it
	 * will only have an effect if the inbound Message has a File payload or
	 * a {@link FileHeaders#ORIGINAL_FILE} header value containing either a
	 * File instance or a String representing the original file path.
	 *
	 * @param deleteSourceFiles true to delete the source files.
	 */
	public void setDeleteSourceFiles(boolean deleteSourceFiles) {
		this.deleteSourceFiles = deleteSourceFiles;
	}

	/**
	 * Set the charset name to use when writing a File from a String-based
	 * Message payload.
	 *
	 * @param charset The charset.
	 */
	public void setCharset(String charset) {
		Assert.notNull(charset, "charset must not be null");
		Assert.isTrue(Charset.isSupported(charset), "Charset '" + charset + "' is not supported.");
		this.charset = Charset.forName(charset);
	}

	/**
	 * Set the buffer size to use while writing to files; default 8192.
	 * @param bufferSize the buffer size.
	 * @since 4.3
	 */
	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	/**
	 * Set the frequency to flush buffers when {@link FileExistsMode#APPEND_NO_FLUSH} is
	 * being used.
	 * @param flushInterval the interval.
	 * @since 4.3
	 */
	public void setFlushInterval(long flushInterval) {
		this.flushInterval = flushInterval;
	}

	@Override
	public void setTaskScheduler(TaskScheduler taskScheduler) {
		super.setTaskScheduler(taskScheduler);
	}

	/**
	 * Set a {@link MessageFlushPredicate} to use when flushing files when
	 * {@link FileExistsMode#APPEND_NO_FLUSH} is being used.
	 * See {@link #trigger(Message)}.
	 * @param flushPredicate the predicate.
	 * @since 4.3
	 */
	public void setFlushPredicate(MessageFlushPredicate flushPredicate) {
		Assert.notNull(flushPredicate, "'flushPredicate' cannot be null");
		this.flushPredicate = flushPredicate;
	}

	/**
	 * Set to true to preserve the destination file timestamp. If true and
	 * the payload is a {@link File}, the payload's {@code lastModified} time will be
	 * transferred to the destination file. For other payloads, the
	 * {@link FileHeaders#SET_MODIFIED} header {@value FileHeaders#SET_MODIFIED}
	 * will be used if present and it's a {@link Number}.
	 * @param preserveTimestamp the preserveTimestamp to set.
	 * @since 4.3
	 */
	public void setPreserveTimestamp(boolean preserveTimestamp) {
		this.preserveTimestamp = preserveTimestamp;
	}

	@Override
	protected void doInit() {
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());

		if (this.destinationDirectoryExpression instanceof LiteralExpression) {
			final File directory = new File(this.destinationDirectoryExpression.getValue(
					this.evaluationContext, null, String.class));
			validateDestinationDirectory(directory, this.autoCreateDirectory);
		}

		Assert.state(!(this.temporaryFileSuffixSet
				&& (FileExistsMode.APPEND.equals(this.fileExistsMode)
						|| FileExistsMode.APPEND_NO_FLUSH.equals(this.fileExistsMode))),
				"'temporaryFileSuffix' can not be set when appending to an existing file");

		if (!this.fileNameGeneratorSet && this.fileNameGenerator instanceof BeanFactoryAware) {
			((BeanFactoryAware) this.fileNameGenerator).setBeanFactory(getBeanFactory());
		}

	}

	@Override
	public void start() {
		if (this.flushTask == null && FileExistsMode.APPEND_NO_FLUSH.equals(this.fileExistsMode)) {
			TaskScheduler taskScheduler = getTaskScheduler();
			Assert.state(taskScheduler != null, "'taskScheduler' is required for FileExistsMode.APPEND_NO_FLUSH");
			this.flushTask = taskScheduler.scheduleAtFixedRate(new Flusher(), this.flushInterval / 3);
		}
	}

	@Override
	public void stop() {
		if (this.flushTask != null) {
			this.flushTask.cancel(true);
			this.flushTask = null;
		}
		new Flusher().run();
	}

	@Override
	public boolean isRunning() {
		return this.flushTask != null;
	}

	private void validateDestinationDirectory(File destinationDirectory, boolean autoCreateDirectory) {
		if (!destinationDirectory.exists() && autoCreateDirectory) {
			Assert.isTrue(destinationDirectory.mkdirs(),
					"Destination directory [" + destinationDirectory + "] could not be created.");
		}
		Assert.isTrue(destinationDirectory.exists(),
				"Destination directory [" + destinationDirectory + "] does not exist.");
		Assert.isTrue(destinationDirectory.isDirectory(),
				"Destination path [" + destinationDirectory + "] does not point to a directory.");
		Assert.isTrue(destinationDirectory.canWrite(),
				"Destination directory [" + destinationDirectory + "] is not writable.");
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		Assert.notNull(requestMessage, "message must not be null");
		Object payload = requestMessage.getPayload();
		Assert.notNull(payload, "message payload must not be null");
		String generatedFileName = this.fileNameGenerator.generateFileName(requestMessage);
		File originalFileFromHeader = retrieveOriginalFileFromHeader(requestMessage);

		final File destinationDirectoryToUse = evaluateDestinationDirectoryExpression(requestMessage);

		File tempFile = new File(destinationDirectoryToUse, generatedFileName + this.temporaryFileSuffix);
		File resultFile = new File(destinationDirectoryToUse, generatedFileName);

		if (FileExistsMode.FAIL.equals(this.fileExistsMode) && resultFile.exists()) {
			throw new MessageHandlingException(requestMessage,
					"The destination file already exists at '" + resultFile.getAbsolutePath() + "'.");
		}

		final boolean ignore = FileExistsMode.IGNORE.equals(this.fileExistsMode) &&
				(resultFile.exists() ||
						(StringUtils.hasText(this.temporaryFileSuffix) && tempFile.exists()));

		if (!ignore) {
			try {
				Object timestamp = requestMessage.getHeaders().get(FileHeaders.SET_MODIFIED);
				if (!resultFile.exists() &&
						generatedFileName.replaceAll("/", Matcher.quoteReplacement(File.separator))
								.contains(File.separator)) {
					resultFile.getParentFile().mkdirs(); //NOSONAR - will fail on the writing below
				}
				if (payload instanceof File) {
					resultFile = handleFileMessage((File) payload, tempFile, resultFile);
					timestamp = ((File) payload).lastModified();
				}
				else if (payload instanceof InputStream) {
					resultFile = handleInputStreamMessage((InputStream) payload, originalFileFromHeader, tempFile,
							resultFile);
				}
				else if (payload instanceof byte[]) {
					resultFile = this.handleByteArrayMessage(
							(byte[]) payload, originalFileFromHeader, tempFile, resultFile);
				}
				else if (payload instanceof String) {
					resultFile = this.handleStringMessage(
							(String) payload, originalFileFromHeader, tempFile, resultFile);
				}
				else {
					throw new IllegalArgumentException(
							"unsupported Message payload type [" + payload.getClass().getName() + "]");
				}
				if (this.preserveTimestamp) {
					if (timestamp instanceof Number) {
						resultFile.setLastModified(((Number) timestamp).longValue());
					}
					else {
						if (this.logger.isWarnEnabled()) {
							this.logger.warn("Could not set lastModified, header " + FileHeaders.SET_MODIFIED
									+ " must be a Number, not " + timestamp.getClass());
						}
					}
				}
			}
			catch (Exception e) {
				throw new MessageHandlingException(requestMessage, "failed to write Message payload to file", e);
			}

		}

		if (!this.expectReply) {
			return null;
		}

		if (resultFile != null) {
			if (originalFileFromHeader == null && payload instanceof File) {
				return this.getMessageBuilderFactory().withPayload(resultFile)
						.setHeader(FileHeaders.ORIGINAL_FILE, payload);
			}
		}
		return resultFile;
	}

	/**
	 * Retrieves the File instance from the {@link FileHeaders#ORIGINAL_FILE}
	 * header if available. If the value is not a File instance or a String
	 * representation of a file path, this will return <code>null</code>.
	 */
	private File retrieveOriginalFileFromHeader(Message<?> message) {
		Object value = message.getHeaders().get(FileHeaders.ORIGINAL_FILE);
		if (value instanceof File) {
			return (File) value;
		}
		if (value instanceof String) {
			return new File((String) value);
		}
		return null;
	}

	private File handleFileMessage(final File sourceFile, File tempFile, final File resultFile) throws IOException {
		if (!FileExistsMode.APPEND.equals(this.fileExistsMode) && this.deleteSourceFiles) {
			if (rename(sourceFile, resultFile)) {
				return resultFile;
			}
			if (this.logger.isInfoEnabled()) {
				this.logger.info(String.format("Failed to move file '%s'. Using copy and delete fallback.",
						sourceFile.getAbsolutePath()));
			}
		}
		final BufferedInputStream bis = new BufferedInputStream(new FileInputStream(sourceFile));
		return handleInputStreamMessage(bis, sourceFile, tempFile, resultFile);
	}

	private File handleInputStreamMessage(final InputStream sourceFileInputStream, File originalFile, File tempFile,
										  final File resultFile) throws IOException {
		final boolean append = FileExistsMode.APPEND.equals(this.fileExistsMode)
				|| FileExistsMode.APPEND_NO_FLUSH.equals(this.fileExistsMode);

		if (append) {
			final File fileToWriteTo = this.determineFileToWrite(resultFile, tempFile);

			final FileState state = getFileState(fileToWriteTo, false);

			WhileLockedProcessor whileLockedProcessor = new WhileLockedProcessor(this.lockRegistry,
					fileToWriteTo.getAbsolutePath()) {

				@Override
				protected void whileLocked() throws IOException {
					BufferedOutputStream bos = state != null ? state.stream : createOutputStream(fileToWriteTo, true);
					try {
						byte[] buffer = new byte[StreamUtils.BUFFER_SIZE];
						int bytesRead = -1;
						while ((bytesRead = sourceFileInputStream.read(buffer)) != -1) {
							bos.write(buffer, 0, bytesRead);
						}
						if (FileWritingMessageHandler.this.appendNewLine) {
							bos.write(LINE_SEPARATOR.getBytes());
						}
					}
					finally {
						try {
							sourceFileInputStream.close();
						}
						catch (IOException ex) {
						}
						try {
							if (state == null || FileWritingMessageHandler.this.flushTask == null) {
								bos.close();
								clearState(fileToWriteTo, state);
							}
							else {
								state.lastWrite = System.currentTimeMillis();
							}
						}
						catch (IOException ex) {
						}
					}
				}

			};
			whileLockedProcessor.doWhileLocked();
			cleanUpAfterCopy(fileToWriteTo, resultFile, originalFile);
			return resultFile;
		}
		else {

			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(tempFile), this.bufferSize);

			try {
				byte[] buffer = new byte[StreamUtils.BUFFER_SIZE];
				int bytesRead = -1;
				while ((bytesRead = sourceFileInputStream.read(buffer)) != -1) {
					bos.write(buffer, 0, bytesRead);
				}
				if (this.appendNewLine) {
					bos.write(LINE_SEPARATOR.getBytes());
				}
				bos.flush();
			}
			finally {
				try {
					sourceFileInputStream.close();
				}
				catch (IOException ex) {
				}
				try {
					bos.close();
				}
				catch (IOException ex) {
				}
			}
			cleanUpAfterCopy(tempFile, resultFile, originalFile);
			return resultFile;
		}
	}

	private File handleByteArrayMessage(final byte[] bytes, File originalFile, File tempFile, final File resultFile)
			throws IOException {
		final File fileToWriteTo = this.determineFileToWrite(resultFile, tempFile);

		final FileState state = getFileState(fileToWriteTo, false);

		final boolean append = FileExistsMode.APPEND.equals(this.fileExistsMode);

		WhileLockedProcessor whileLockedProcessor = new WhileLockedProcessor(this.lockRegistry,
				fileToWriteTo.getAbsolutePath()) {

			@Override
			protected void whileLocked() throws IOException {
				BufferedOutputStream bos = state != null ? state.stream : createOutputStream(fileToWriteTo, append);
				try {
					bos.write(bytes);
					if (FileWritingMessageHandler.this.appendNewLine) {
						bos.write(LINE_SEPARATOR.getBytes());
					}
				}
				finally {
					try {
						if (state == null || FileWritingMessageHandler.this.flushTask == null) {
							bos.close();
							clearState(fileToWriteTo, state);
						}
						else {
							state.lastWrite = System.currentTimeMillis();
						}
					}
					catch (IOException ex) {
					}
				}
			}

		};
		whileLockedProcessor.doWhileLocked();
		this.cleanUpAfterCopy(fileToWriteTo, resultFile, originalFile);
		return resultFile;
	}

	private File handleStringMessage(final String content, File originalFile, File tempFile, final File resultFile)
			throws IOException {
		final File fileToWriteTo = this.determineFileToWrite(resultFile, tempFile);

		final FileState state = getFileState(fileToWriteTo, true);

		final boolean append = FileExistsMode.APPEND.equals(this.fileExistsMode);

		WhileLockedProcessor whileLockedProcessor = new WhileLockedProcessor(this.lockRegistry,
				fileToWriteTo.getAbsolutePath()) {

			@Override
			protected void whileLocked() throws IOException {
				BufferedWriter writer = state != null ? state.writer : createWriter(fileToWriteTo, append);
				try {
					writer.write(content);
					if (FileWritingMessageHandler.this.appendNewLine) {
						writer.newLine();
					}
				}
				finally {
					try {
						if (state == null || FileWritingMessageHandler.this.flushTask == null) {
							writer.close();
							clearState(fileToWriteTo, state);
						}
						else {
							state.lastWrite = System.currentTimeMillis();
						}
					}
					catch (IOException ex) {
					}
				}

			}

		};
		whileLockedProcessor.doWhileLocked();

		this.cleanUpAfterCopy(fileToWriteTo, resultFile, originalFile);
		return resultFile;
	}

	private File determineFileToWrite(File resultFile, File tempFile) {

		final File fileToWriteTo;

		switch (this.fileExistsMode) {
			case APPEND:
			case APPEND_NO_FLUSH:
				fileToWriteTo = resultFile;
				break;
			case FAIL:
			case IGNORE:
			case REPLACE:
				fileToWriteTo = tempFile;
				break;
			default:
				throw new IllegalStateException("Unsupported FileExistsMode " + this.fileExistsMode);
		}
		return fileToWriteTo;
	}

	private void cleanUpAfterCopy(File fileToWriteTo, File resultFile, File originalFile) throws IOException {
		if (!FileExistsMode.APPEND.equals(this.fileExistsMode)
				&& !FileExistsMode.APPEND_NO_FLUSH.equals(this.fileExistsMode)
				&& StringUtils.hasText(this.temporaryFileSuffix)) {
			this.renameTo(fileToWriteTo, resultFile);
		}

		if (this.deleteSourceFiles && originalFile != null) {
			originalFile.delete();
		}
	}

	private void renameTo(File tempFile, File resultFile) throws IOException {
		Assert.notNull(resultFile, "'resultFile' must not be null");
		Assert.notNull(tempFile, "'tempFile' must not be null");

		if (resultFile.exists()) {
			if (resultFile.setWritable(true, false) && resultFile.delete()) {
				if (!rename(tempFile, resultFile)) {
					throw new IOException("Failed to rename file '" + tempFile.getAbsolutePath() +
							"' to '" + resultFile.getAbsolutePath() + "'");
				}
			}
			else {
				throw new IOException("Failed to rename file '" + tempFile.getAbsolutePath() +
						"' to '" + resultFile.getAbsolutePath() +
						"' since '" + resultFile.getName() + "' is not writable or can not be deleted");
			}
		}
		else {
			if (!rename(tempFile, resultFile)) {
				throw new IOException("Failed to rename file '" + tempFile.getAbsolutePath() +
						"' to '" + resultFile.getAbsolutePath() + "'");
			}
		}
	}

	private File evaluateDestinationDirectoryExpression(Message<?> message) {

		final File destinationDirectory;

		final Object destinationDirectoryToUse = this.destinationDirectoryExpression.getValue(
				this.evaluationContext, message);

		if (destinationDirectoryToUse == null) {
			throw new IllegalStateException(String.format("The provided " +
							"destinationDirectoryExpression (%s) must not resolve to null.",
					this.destinationDirectoryExpression.getExpressionString()));
		}
		else if (destinationDirectoryToUse instanceof String) {

			final String destinationDirectoryPath = (String) destinationDirectoryToUse;

			Assert.hasText(destinationDirectoryPath, String.format(
					"Unable to resolve destination directory name for the provided Expression '%s'.",
					this.destinationDirectoryExpression.getExpressionString()));
			destinationDirectory = new File(destinationDirectoryPath);
		}
		else if (destinationDirectoryToUse instanceof File) {
			destinationDirectory = (File) destinationDirectoryToUse;
		}
		else {
			throw new IllegalStateException(String.format("The provided " +
					"destinationDirectoryExpression (%s) must be of type " +
					"java.io.File or be a String.", this.destinationDirectoryExpression.getExpressionString()));
		}

		validateDestinationDirectory(destinationDirectory, this.autoCreateDirectory);
		return destinationDirectory;
	}

	private synchronized FileState getFileState(final File fileToWriteTo, boolean isString)
			throws FileNotFoundException {
		String absolutePath = fileToWriteTo.getAbsolutePath();
		FileState state;
		boolean appendNoFlush = FileExistsMode.APPEND_NO_FLUSH.equals(this.fileExistsMode);
		if (appendNoFlush) {
			state = this.fileStates.get(absolutePath);
			if (state != null && ((isString && state.stream != null) || (!isString && state.writer != null))) {
				state.close();
				state = null;
				this.fileStates.remove(absolutePath);
			}
			if (state == null) {
				if (isString) {
					state = new FileState(createWriter(fileToWriteTo, true));
				}
				else {
					state = new FileState(createOutputStream(fileToWriteTo, true));
				}
				this.fileStates.put(absolutePath, state);
			}
			state.lastWrite = Long.MAX_VALUE; // prevent flush while we write
		}
		else {
			state = null;
		}
		return state;
	}

	private BufferedWriter createWriter(final File fileToWriteTo, final boolean append) throws FileNotFoundException {
		return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileToWriteTo, append), this.charset),
				this.bufferSize);
	}

	private BufferedOutputStream createOutputStream(File fileToWriteTo, final boolean append)
			throws FileNotFoundException {
		return new BufferedOutputStream(new FileOutputStream(fileToWriteTo, append), this.bufferSize);
	}

	/**
	 * When using {@link FileExistsMode#APPEND_NO_FLUSH}, you can send a message to this
	 * method to flush any file(s) that needs it. By default, the payload must be a regular
	 * expression ({@link String} or {@link Pattern}) that matches the absolutePath
	 * of any in-process files. However, if a custom {@link MessageFlushPredicate} is provided,
	 * the payload can be of any type supported by that implementation.
	 * @since 4.3
	 */
	@Override
	public void trigger(Message<?> message) {
		flushIfNeeded(this.flushPredicate, message);
	}

	/**
	 * When using {@link FileExistsMode#APPEND_NO_FLUSH} you can invoke this method to
	 * selectively flush open files. For each open file the supplied
	 * {@link MessageFlushPredicate#shouldFlush(String, long, Message)}
	 * method is invoked and if true is returned, the file is flushed.
	 * @param flushPredicate the {@link FlushPredicate}.
	 * @since 4.3
	 */
	public synchronized void flushIfNeeded(FlushPredicate flushPredicate) {
		Iterator<Entry<String, FileState>> iterator = FileWritingMessageHandler.this.fileStates.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, FileState> entry = iterator.next();
			FileState state = entry.getValue();
			if (flushPredicate.shouldFlush(entry.getKey(), state.lastWrite)) {
				iterator.remove();
				state.close();
			}
		}
	}

	/**
	 * When using {@link FileExistsMode#APPEND_NO_FLUSH} you can invoke this method to
	 * selectively flush open files. For each open file the supplied
	 * {@link MessageFlushPredicate#shouldFlush(String, long, Message)}
	 * method is invoked and if true is returned, the file is flushed.
	 * @param flushPredicate the {@link MessageFlushPredicate}.
	 * @param filterMessage an optional message passed into the predicate.
	 * @since 4.3
	 */
	public synchronized void flushIfNeeded(MessageFlushPredicate flushPredicate, Message<?> filterMessage) {
		Iterator<Entry<String, FileState>> iterator = FileWritingMessageHandler.this.fileStates.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, FileState> entry = iterator.next();
			FileState state = entry.getValue();
			if (flushPredicate.shouldFlush(entry.getKey(), state.lastWrite, filterMessage)) {
				iterator.remove();
				state.close();
			}
		}
	}

	private synchronized void clearState(final File fileToWriteTo, final FileState state) {
		if (state != null) {
			this.fileStates.remove(fileToWriteTo.getAbsolutePath());
		}
	}

	private static boolean rename(File source, File target) throws IOException {
		return (nioFilesPresent && filesMove(source, target)) || source.renameTo(target);
	}

	private static boolean filesMove(File source, File target) throws IOException {
		Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
		return true;
	}

	private static final class FileState {

		private final BufferedWriter writer;

		private final BufferedOutputStream  stream;

		private volatile long lastWrite;

		private FileState(BufferedWriter writer) {
			this.writer = writer;
			this.stream = null;
		}

		private FileState(BufferedOutputStream stream) {
			this.writer = null;
			this.stream = stream;
		}

		private void close() {
			try {
				if (this.writer != null) {
					this.writer.close();
				}
				else {
					this.stream.close();
				}
			}
			catch (IOException e) {
				// ignore
			}
		}
	}

	private final class Flusher implements Runnable {

		@Override
		public void run() {
			synchronized (FileWritingMessageHandler.this) {
				long expired = FileWritingMessageHandler.this.flushTask == null ? Long.MAX_VALUE
						: (System.currentTimeMillis() - FileWritingMessageHandler.this.flushInterval);
				Iterator<Entry<String, FileState>> iterator = FileWritingMessageHandler.this.fileStates.entrySet().iterator();
				while (iterator.hasNext()) {
					Entry<String, FileState> entry = iterator.next();
					FileState state = entry.getValue();
					if (state.lastWrite < expired) {
						iterator.remove();
						state.close();
						if (FileWritingMessageHandler.this.logger.isDebugEnabled()) {
							FileWritingMessageHandler.this.logger.debug("Flushed: " + entry.getKey());
						}
					}
				}
			}
		}

	}

	/**
	 * When using {@link FileExistsMode#APPEND_NO_FLUSH}
	 * an implementation of this interface is called for each file that has pending data
	 * to flush when {@link FileWritingMessageHandler#flushIfNeeded(FlushPredicate)}
	 * is invoked.
	 * @since 4.3
	 *
	 */
	public interface FlushPredicate {

		/**
		 * @param fileAbsolutePath the path to the file.
		 * @param lastWrite the time of the last write - {@link System#currentTimeMillis()}.
		 * @return true if the file should be flushed.
		 */
		boolean shouldFlush(String fileAbsolutePath, long lastWrite);

	}

	/**
	 * When using {@link FileExistsMode#APPEND_NO_FLUSH}
	 * an implementation of this interface is called for each file that has pending data
	 * to flush.
	 * @see FileWritingMessageHandler#trigger(Message)
	 * @since 4.3
	 *
	 */
	public interface MessageFlushPredicate {

		/**
		 * @param fileAbsolutePath the path to the file.
		 * @param lastWrite the time of the last write - {@link System#currentTimeMillis()}.
		 * @param filterMessage an optional message to be used in the decision process.
		 * @return true if the file should be flushed.
		 */
		boolean shouldFlush(String fileAbsolutePath, long lastWrite, Message<?> filterMessage);

	}

	/**
	 * Flushes files where the path matches a pattern, regardless of last write time.
	 */
	private static final class DefaultFlushPredicate implements MessageFlushPredicate {

		@Override
		public boolean shouldFlush(String fileAbsolutePath, long lastWrite, Message<?> triggerMessage) {
			Pattern pattern;
			if (triggerMessage.getPayload() instanceof String) {
				pattern = Pattern.compile((String) triggerMessage.getPayload());
			}
			else if (triggerMessage.getPayload() instanceof Pattern) {
				pattern = (Pattern) triggerMessage.getPayload();
			}
			else {
				throw new IllegalArgumentException("Invalid payload type, must be a String or Pattern");
			}
			return pattern.matcher(fileAbsolutePath).matches();
		}

	}

}
