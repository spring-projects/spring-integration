/*
 * Copyright 2002-2017 the original author or authors.
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
import java.nio.file.attribute.PosixFilePermission;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;
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
import org.springframework.integration.file.support.FileUtils;
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

	private volatile boolean flushWhenIdle = true;

	private volatile ScheduledFuture<?> flushTask;

	private volatile MessageFlushPredicate flushPredicate = new DefaultFlushPredicate();

	private volatile boolean preserveTimestamp;

	private Set<PosixFilePermission> permissions;

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
	 * <p>
	 * With {@link FileExistsMode#REPLACE_IF_MODIFIED}, if the file exists,
	 * it is only replaced if its last modified timestamp is different to the
	 * source; otherwise, the write is ignored. For {@link File} payloads,
	 * the actual timestamp of the {@link File} is compared; for other payloads,
	 * the {@link FileHeaders#SET_MODIFIED} is compared to the existing file.
	 * If the header is missing, or its value is not a {@link Number}, the file
	 * is always replaced. This mode will typically only make sense if
	 * {@link #setPreserveTimestamp(boolean) preserveTimestamp} is true.
	 *
	 * @param fileExistsMode Must not be null
	 * @see #setPreserveTimestamp(boolean)
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
	 * being used. The interval is approximate; the actual interval will be between
	 * {@code flushInterval} and {@code flushInterval * 1.33} with an average of
	 * {@code flushInterval * 1.167}.
	 * @param flushInterval the interval.
	 * @see #setFlushWhenIdle(boolean)
	 * @since 4.3
	 */
	public void setFlushInterval(long flushInterval) {
		this.flushInterval = flushInterval;
	}

	/**
	 * Determine whether the {@link #setFlushInterval(long) flushInterval} applies only
	 * to idle files (default) or whether to flush on that interval after the first
	 * write to a previously flushed or new file.
	 * @param flushWhenIdle false to flush on the interval after the first write
	 * to a closed file.
	 * @see #setFlushInterval(long)
	 * @see #setBufferSize(int)
	 * @since 4.3.7
	 */
	public void setFlushWhenIdle(boolean flushWhenIdle) {
		this.flushWhenIdle = flushWhenIdle;
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

	/**
	 * String setter for Spring XML convenience.
	 * @param chmod permissions as an octal string e.g "600";
	 * @see #setChmod(int)
	 * @since 5.0
	 */
	public void setChmodOctal(String chmod) {
		Assert.notNull(chmod, "'chmod' cannot be null");
		setChmod(Integer.parseInt(chmod, 8));
	}

	/**
	 * Set the file permissions after uploading, e.g. 0600 for
	 * owner read/write. Only applies to file systems that support posix
	 * file permissions.
	 * @param chmod the permissions.
	 * @throws IllegalArgumentException if the value is higher than 0777.
	 * @since 5.0
	 */
	public void setChmod(int chmod) {
		Assert.isTrue(chmod >= 0 && chmod <= 0777, "'chmod' must be between 0 and 0777 (octal)");
		if (!FileUtils.IS_POSIX) {
			this.logger.error("'chmod' setting ignored - the file system does not support Posix attributes");
			return;
		}
		/*
		 * Bitset.valueOf(byte[]) takes a little-endian array of bytes to create a BitSet.
		 * Since we are interested in 9 bits, we construct an array with the low-order byte
		 * (bits 0-7) followed by the second order byte (bit 8).
		 * BitSet.stream() returns a stream of ints representing those bits that are set.
		 * We use that stream with a switch to create the set of PosixFilePermissions
		 * representing the bits that were set in the chmod value.
		 */
		BitSet bits = BitSet.valueOf(new byte[] { (byte) chmod, (byte) (chmod >> 8) });
		final Set<PosixFilePermission> permissions = new HashSet<>();
		bits.stream().forEach(b -> {
			switch (b) {
				case 0:
					permissions.add(PosixFilePermission.OTHERS_EXECUTE);
					break;
				case 1:
					permissions.add(PosixFilePermission.OTHERS_WRITE);
					break;
				case 2:
					permissions.add(PosixFilePermission.OTHERS_READ);
					break;
				case 3:
					permissions.add(PosixFilePermission.GROUP_EXECUTE);
					break;
				case 4:
					permissions.add(PosixFilePermission.GROUP_WRITE);
					break;
				case 5:
					permissions.add(PosixFilePermission.GROUP_READ);
					break;
				case 6:
					permissions.add(PosixFilePermission.OWNER_EXECUTE);
					break;
				case 7:
					permissions.add(PosixFilePermission.OWNER_WRITE);
					break;
				case 8:
					permissions.add(PosixFilePermission.OWNER_READ);
					break;
			}
		});
		this.permissions = permissions;
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
		synchronized (this) {
			if (this.flushTask != null) {
				this.flushTask.cancel(true);
				this.flushTask = null;
			}
		}
		Flusher flusher = new Flusher();
		flusher.run();
		boolean needInterrupt = this.fileStates.size() > 0;
		int n = 0;
		while (n++ < 10 && this.fileStates.size() > 0) {
			try {
				Thread.sleep(1);
			}
			catch (InterruptedException e) {
				// cancel the interrupt
			}
			flusher.run();
		}
		if (this.fileStates.size() > 0) {
			this.logger.error("Failed to flush after multiple attempts, while stopping: " + this.fileStates.keySet());
		}
		if (needInterrupt) {
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public boolean isRunning() {
		return this.flushTask != null;
	}

	private void validateDestinationDirectory(File destinationDirectory, boolean autoCreateDirectory) {
		if (!destinationDirectory.exists() && autoCreateDirectory) {
			Assert.isTrue(destinationDirectory.mkdirs(),
					() -> "Destination directory [" + destinationDirectory + "] could not be created.");
		}
		Assert.isTrue(destinationDirectory.exists(),
				() -> "Destination directory [" + destinationDirectory + "] does not exist.");
		Assert.isTrue(destinationDirectory.isDirectory(),
				() -> "Destination path [" + destinationDirectory + "] does not point to a directory.");
		Assert.isTrue(Files.isWritable(destinationDirectory.toPath()),
				() -> "Destination directory [" + destinationDirectory + "] is not writable.");
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

		boolean exists = resultFile.exists();
		if (exists && FileExistsMode.FAIL.equals(this.fileExistsMode)) {
			throw new MessageHandlingException(requestMessage,
					"The destination file already exists at '" + resultFile.getAbsolutePath() + "'.");
		}

		Object timestamp = requestMessage.getHeaders().get(FileHeaders.SET_MODIFIED);
		if (payload instanceof File) {
			timestamp = ((File) payload).lastModified();
		}
		boolean ignore = (FileExistsMode.IGNORE.equals(this.fileExistsMode)
				&& (exists || (StringUtils.hasText(this.temporaryFileSuffix) && tempFile.exists())))
				|| ((exists && FileExistsMode.REPLACE_IF_MODIFIED.equals(this.fileExistsMode))
				&& (timestamp instanceof Number
				&& ((Number) timestamp).longValue() == resultFile.lastModified()));
		if (!ignore) {
			try {
				if (!exists &&
						generatedFileName.replaceAll("/", Matcher.quoteReplacement(File.separator))
								.contains(File.separator)) {
					resultFile.getParentFile().mkdirs(); //NOSONAR - will fail on the writing below
				}
				if (payload instanceof File) {
					resultFile = handleFileMessage((File) payload, tempFile, resultFile);
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
									+ " must be a Number, not " + (timestamp == null ? "null" : timestamp.getClass()));
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
			rename(sourceFile, resultFile);
			return resultFile;
		}
		else {
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(sourceFile));
			return handleInputStreamMessage(bis, sourceFile, tempFile, resultFile);
		}
	}

	private File handleInputStreamMessage(final InputStream sourceFileInputStream, File originalFile, File tempFile,
			final File resultFile) throws IOException {
		final boolean append = FileExistsMode.APPEND.equals(this.fileExistsMode)
				|| FileExistsMode.APPEND_NO_FLUSH.equals(this.fileExistsMode);

		if (append) {
			final File fileToWriteTo = this.determineFileToWrite(resultFile, tempFile);

			WhileLockedProcessor whileLockedProcessor = new WhileLockedProcessor(this.lockRegistry,
					fileToWriteTo.getAbsolutePath()) {

				@Override
				protected void whileLocked() throws IOException {
					FileState state = getFileState(fileToWriteTo, false);
					BufferedOutputStream bos = null;
					try {
						bos = state != null ? state.stream : createOutputStream(fileToWriteTo, true);
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
								if (bos != null) {
									bos.close();
								}
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

			BufferedOutputStream bos = null;
			try {
				bos = new BufferedOutputStream(new FileOutputStream(tempFile), this.bufferSize);
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
					if (bos != null) {
						bos.close();
					}
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

		final boolean append = FileExistsMode.APPEND.equals(this.fileExistsMode);

		WhileLockedProcessor whileLockedProcessor = new WhileLockedProcessor(this.lockRegistry,
				fileToWriteTo.getAbsolutePath()) {

			@Override
			protected void whileLocked() throws IOException {
				FileState state = getFileState(fileToWriteTo, false);
				BufferedOutputStream bos = null;
				try {
					bos = state != null ? state.stream : createOutputStream(fileToWriteTo, append);
					bos.write(bytes);
					if (FileWritingMessageHandler.this.appendNewLine) {
						bos.write(LINE_SEPARATOR.getBytes());
					}
				}
				finally {
					try {
						if (state == null || FileWritingMessageHandler.this.flushTask == null) {
							if (bos != null) {
								bos.close();
							}
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

		final boolean append = FileExistsMode.APPEND.equals(this.fileExistsMode);

		WhileLockedProcessor whileLockedProcessor = new WhileLockedProcessor(this.lockRegistry,
				fileToWriteTo.getAbsolutePath()) {

			@Override
			protected void whileLocked() throws IOException {
				FileState state = getFileState(fileToWriteTo, true);
				BufferedWriter writer = null;
				try {
					writer = state != null ? state.writer : createWriter(fileToWriteTo, append);
					writer.write(content);
					if (FileWritingMessageHandler.this.appendNewLine) {
						writer.newLine();
					}
				}
				finally {
					try {
						if (state == null || FileWritingMessageHandler.this.flushTask == null) {
							if (writer != null) {
								writer.close();
							}
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
			case REPLACE_IF_MODIFIED:
				fileToWriteTo = tempFile;
				break;
			default:
				throw new IllegalStateException("Unsupported FileExistsMode: " + this.fileExistsMode);
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

		setPermissions(resultFile);
	}

	/**
	 * Set permissions on newly written files.
	 * @param resultFile the file.
	 * @throws IOException any exception.
	 * @since 5.0
	 */
	protected void setPermissions(File resultFile) throws IOException {
		if (this.permissions != null) {
			Files.setPosixFilePermissions(resultFile.toPath(), this.permissions);
		}
	}

	private void renameTo(File tempFile, File resultFile) throws IOException {
		Assert.notNull(resultFile, "'resultFile' must not be null");
		Assert.notNull(tempFile, "'tempFile' must not be null");

		if (resultFile.exists()) {
			if (resultFile.setWritable(true, false) && resultFile.delete()) {
				rename(tempFile, resultFile);
			}
			else {
				throw new IOException("Failed to rename file '" + tempFile.getAbsolutePath() +
						"' to '" + resultFile.getAbsolutePath() +
						"' since '" + resultFile.getName() + "' is not writable or can not be deleted");
			}
		}
		else {
			rename(tempFile, resultFile);
		}
	}

	private File evaluateDestinationDirectoryExpression(Message<?> message) {
		final File destinationDirectory = ExpressionUtils.expressionToFile(this.destinationDirectoryExpression,
				this.evaluationContext, message, "Destination Directory");
		validateDestinationDirectory(destinationDirectory, this.autoCreateDirectory);
		return destinationDirectory;
	}

	private synchronized FileState getFileState(final File fileToWriteTo, boolean isString)
			throws FileNotFoundException {
		FileState state;
		boolean appendNoFlush = FileExistsMode.APPEND_NO_FLUSH.equals(this.fileExistsMode);
		if (appendNoFlush) {
			String absolutePath = fileToWriteTo.getAbsolutePath();
			state = this.fileStates.get(absolutePath);
			if (state != null && ((isString && state.stream != null) || (!isString && state.writer != null))) {
				state.close();
				state = null;
				this.fileStates.remove(absolutePath);
			}
			if (state == null) {
				if (isString) {
					state = new FileState(createWriter(fileToWriteTo, true),
							this.lockRegistry.obtain(fileToWriteTo.getAbsolutePath()));
				}
				else {
					state = new FileState(createOutputStream(fileToWriteTo, true),
							this.lockRegistry.obtain(fileToWriteTo.getAbsolutePath()));
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

	/**
	 * Create a buffered writer for the file, for String payloads.
	 * @param fileToWriteTo the file.
	 * @param append true if we are appending.
	 * @return the writer.
	 * @throws FileNotFoundException if the file does not exist.
	 * @since 4.3.8
	 */
	protected BufferedWriter createWriter(final File fileToWriteTo, final boolean append) throws FileNotFoundException {
		return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileToWriteTo, append), this.charset),
				this.bufferSize);
	}

	/**
	 * Create a buffered output stream for the file.
	 * @param fileToWriteTo the file.
	 * @param append true if we are appending.
	 * @return the stream.
	 * @throws FileNotFoundException if not found.
	 * @since 4.3.8
	 */
	protected BufferedOutputStream createOutputStream(File fileToWriteTo, final boolean append)
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
	 * selectively flush and close open files. For each open file the supplied
	 * {@link MessageFlushPredicate#shouldFlush(String, long, long, Message)}
	 * method is invoked and if true is returned, the file is flushed.
	 * @param flushPredicate the {@link FlushPredicate}.
	 * @since 4.3
	 */
	public void flushIfNeeded(FlushPredicate flushPredicate) {
		flushIfNeeded((fileAbsolutePath, firstWrite, lastWrite, filterMessage) ->
						flushPredicate.shouldFlush(fileAbsolutePath, firstWrite, lastWrite),
				null);
	}

	/**
	 * When using {@link FileExistsMode#APPEND_NO_FLUSH} you can invoke this method to
	 * selectively flush and close open files. For each open file the supplied
	 * {@link MessageFlushPredicate#shouldFlush(String, long, long, Message)}
	 * method is invoked and if true is returned, the file is flushed.
	 * @param flushPredicate the {@link MessageFlushPredicate}.
	 * @param filterMessage an optional message passed into the predicate.
	 * @since 4.3
	 */
	public void flushIfNeeded(MessageFlushPredicate flushPredicate, Message<?> filterMessage) {
		doFlush(findFilesToFlush(flushPredicate, filterMessage));
	}

	private Map<String, FileState> findFilesToFlush(MessageFlushPredicate flushPredicate, Message<?> filterMessage) {
		Map<String, FileState> toRemove = new HashMap<>();
		synchronized (this) {
			Iterator<Entry<String, FileState>> iterator = this.fileStates.entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<String, FileState> entry = iterator.next();
				FileState state = entry.getValue();
				if (flushPredicate.shouldFlush(entry.getKey(), state.firstWrite, state.lastWrite, filterMessage)) {
					iterator.remove();
					toRemove.put(entry.getKey(), state);
				}
			}
		}
		return toRemove;
	}

	private synchronized void clearState(final File fileToWriteTo, final FileState state) {
		if (state != null) {
			this.fileStates.remove(fileToWriteTo.getAbsolutePath());
		}
	}

	private void doFlush(Map<String, FileState> toRemove) {
		Map<String, FileState> toRestore = new HashMap<>();
		boolean interrupted = false;
		for (Entry<String, FileState> entry : toRemove.entrySet()) {
			if (!interrupted && entry.getValue().close()) {
				if (FileWritingMessageHandler.this.logger.isDebugEnabled()) {
					FileWritingMessageHandler.this.logger.debug("Flushed: " + entry.getKey());
				}
			}
			else { // interrupted (stop), re-add
				interrupted = true;
				toRestore.put(entry.getKey(), entry.getValue());
			}
		}
		if (interrupted) {
			if (FileWritingMessageHandler.this.logger.isDebugEnabled()) {
				FileWritingMessageHandler.this.logger
						.debug("Interrupted during flush; not flushed: " + toRestore.keySet());
			}
			synchronized (this) {
				for (Entry<String, FileState> entry : toRestore.entrySet()) {
					this.fileStates.putIfAbsent(entry.getKey(), entry.getValue());
				}
			}
		}
	}

	private static void rename(File source, File target) throws IOException {
		Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
	}

	private static final class FileState {

		private final BufferedWriter writer;

		private final BufferedOutputStream stream;

		private final Lock lock;

		private final long firstWrite = System.currentTimeMillis();

		private volatile long lastWrite;

		FileState(BufferedWriter writer, Lock lock) {
			this.writer = writer;
			this.stream = null;
			this.lock = lock;
		}

		FileState(BufferedOutputStream stream, Lock lock) {
			this.writer = null;
			this.stream = stream;
			this.lock = lock;
		}

		private boolean close() {
			try {
				this.lock.lockInterruptibly();
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
				return true;
			}
			catch (InterruptedException e1) {
				Thread.currentThread().interrupt();
				return false;
			}
			finally {
				this.lock.unlock();
			}
		}
	}

	private final class Flusher implements Runnable {

		Flusher() {
			super();
		}

		@Override
		public void run() {
			Map<String, FileState> toRemove = new HashMap<>();
			synchronized (FileWritingMessageHandler.this) {
				long expired = FileWritingMessageHandler.this.flushTask == null ? Long.MAX_VALUE
						: (System.currentTimeMillis() - FileWritingMessageHandler.this.flushInterval);
				Iterator<Entry<String, FileState>> iterator =
						FileWritingMessageHandler.this.fileStates.entrySet().iterator();
				while (iterator.hasNext()) {
					Entry<String, FileState> entry = iterator.next();
					FileState state = entry.getValue();
					if (state.lastWrite < expired ||
							(!FileWritingMessageHandler.this.flushWhenIdle && state.firstWrite < expired)) {
						toRemove.put(entry.getKey(), state);
						iterator.remove();
					}
				}
			}
			doFlush(toRemove);
		}

	}

	/**
	 * When using {@link FileExistsMode#APPEND_NO_FLUSH}, an implementation of this
	 * interface is called for each file that has pending data to flush and close when
	 * {@link FileWritingMessageHandler#flushIfNeeded(FlushPredicate)} is invoked.
	 * @since 4.3
	 *
	 */
	@FunctionalInterface
	public interface FlushPredicate {

		/**
		 * Return true to cause the file to be flushed and closed.
		 * @param fileAbsolutePath the path to the file.
		 * @param firstWrite the time of the first write to a new or previously closed
		 * file.
		 * @param lastWrite the time of the last write -
		 * {@link System#currentTimeMillis()}.
		 * @return true if the file should be flushed and closed.
		 */
		boolean shouldFlush(String fileAbsolutePath, long firstWrite, long lastWrite);

	}

	/**
	 * When using {@link FileExistsMode#APPEND_NO_FLUSH}
	 * an implementation of this interface is called for each file that has pending data
	 * to flush when a trigger message is received.
	 * @see FileWritingMessageHandler#trigger(Message)
	 * @since 4.3
	 *
	 */
	@FunctionalInterface
	public interface MessageFlushPredicate {

		/**
		 * Return true to cause the file to be flushed and closed.
		 * @param fileAbsolutePath the path to the file.
		 * @param firstWrite the time of the first write to a new or previously closed
		 * file.
		 * @param lastWrite the time of the last write - {@link System#currentTimeMillis()}.
		 * @param filterMessage an optional message to be used in the decision process.
		 * @return true if the file should be flushed and closed.
		 */
		boolean shouldFlush(String fileAbsolutePath, long firstWrite, long lastWrite, Message<?> filterMessage);

	}

	/**
	 * Flushes files where the path matches a pattern, regardless of last write time.
	 */
	private static final class DefaultFlushPredicate implements MessageFlushPredicate {

		DefaultFlushPredicate() {
			super();
		}

		@Override
		public boolean shouldFlush(String fileAbsolutePath, long firstWrite, long lastWrite,
				Message<?> triggerMessage) {
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
