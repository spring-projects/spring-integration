/*
 * Copyright 2002-2015 the original author or authors.
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.support.locks.DefaultLockRegistry;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.integration.support.locks.PassThruLockRegistry;
import org.springframework.integration.util.WhileLockedProcessor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * A {@link MessageHandler} implementation that writes the Message payload to a
 * file. If the payload is a File object, it will copy the File to the specified
 * destination directory. If the payload is a byte array or String, it will write
 * it directly. Otherwise, the payload type is unsupported, and an Exception
 * will be thrown.
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
public class FileWritingMessageHandler extends AbstractReplyProducingMessageHandler {

	private static final String LINE_SEPARATOR = System.getProperty("line.separator");

	private volatile String temporaryFileSuffix =".writing";

	private volatile boolean temporaryFileSuffixSet = false;

	private volatile FileExistsMode fileExistsMode  = FileExistsMode.REPLACE;

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
	 *
	 * If set to {@link FileExistsMode#APPEND}, the adapter will also
	 * create a real instance of the {@link LockRegistry} to ensure that there
	 * is no collisions when multiple threads are writing to the same file.
	 *
	 * Otherwise the LockRegistry is set to {@link PassThruLockRegistry} which
	 * has no effect.
	 *
	 * @param fileExistsMode Must not be null
	 */
	public void setFileExistsMode(FileExistsMode fileExistsMode) {

		Assert.notNull(fileExistsMode, "'fileExistsMode' must not be null.");
		this.fileExistsMode = fileExistsMode;

		if (FileExistsMode.APPEND.equals(fileExistsMode)){
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
	 * @param appendNewLine true if a new-line should be written to the file after payload is written
	 * @since 4.0.7
	 */
	public void setAppendNewLine(boolean appendNewLine) {
		this.appendNewLine = appendNewLine;
	}

	protected String getTemporaryFileSuffix() {
		return temporaryFileSuffix;
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

	@Override
	protected void doInit() {

		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(this.getBeanFactory());

		if (this.destinationDirectoryExpression instanceof LiteralExpression) {
			final File directory = new File(this.destinationDirectoryExpression.getValue(
					this.evaluationContext, null, String.class));
			validateDestinationDirectory(directory, this.autoCreateDirectory);
		}

		if (!this.fileNameGeneratorSet && this.fileNameGenerator instanceof BeanFactoryAware) {
			((BeanFactoryAware) this.fileNameGenerator).setBeanFactory(this.getBeanFactory());
		}
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
		Assert.state(!(this.temporaryFileSuffixSet
						&& FileExistsMode.APPEND.equals(this.fileExistsMode)),
				"'temporaryFileSuffix' can not be set when appending to an existing file");
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		Assert.notNull(requestMessage, "message must not be null");
		Object payload = requestMessage.getPayload();
		Assert.notNull(payload, "message payload must not be null");
		String generatedFileName = this.fileNameGenerator.generateFileName(requestMessage);
		File originalFileFromHeader = this.retrieveOriginalFileFromHeader(requestMessage);

		final File destinationDirectoryToUse = evaluateDestinationDirectoryExpression(requestMessage);

		File tempFile = new File(destinationDirectoryToUse, generatedFileName + temporaryFileSuffix);
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
				if (payload instanceof File) {
					resultFile = this.handleFileMessage((File) payload, tempFile, resultFile);
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
		if (FileExistsMode.APPEND.equals(this.fileExistsMode)) {
			File fileToWriteTo = this.determineFileToWrite(resultFile, tempFile);
			final BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(fileToWriteTo, true));
			final BufferedInputStream bis = new BufferedInputStream(new FileInputStream(sourceFile));
			WhileLockedProcessor whileLockedProcessor = new WhileLockedProcessor(this.lockRegistry, fileToWriteTo.getAbsolutePath()){
				@Override
				protected void whileLocked() throws IOException {
					try {
						byte[] buffer = new byte[StreamUtils.BUFFER_SIZE];
						int bytesRead = -1;
						while ((bytesRead = bis.read(buffer)) != -1) {
							bos.write(buffer, 0, bytesRead);
						}
						if (FileWritingMessageHandler.this.appendNewLine) {
							bos.write(LINE_SEPARATOR.getBytes());
						}
						bos.flush();
					}
					finally {
						try {
							bis.close();
						}
						catch (IOException ex) {
						}
						try {
							bos.close();
						}
						catch (IOException ex) {
						}
					}
				}
			};
			whileLockedProcessor.doWhileLocked();
			this.cleanUpAfterCopy(fileToWriteTo, resultFile, sourceFile);
			return resultFile;
		}
		else {
			if (this.deleteSourceFiles) {
				if (sourceFile.renameTo(resultFile)) {
					return resultFile;
				}
				if (logger.isInfoEnabled()) {
					logger.info(String.format("Failed to move file '%s'. Using copy and delete fallback.",
							sourceFile.getAbsolutePath()));
				}
			}

			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(tempFile));
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(sourceFile));

			try {
				byte[] buffer = new byte[StreamUtils.BUFFER_SIZE];
				int bytesRead = -1;
				while ((bytesRead = bis.read(buffer)) != -1) {
					bos.write(buffer, 0, bytesRead);
				}
				if (this.appendNewLine) {
					bos.write(LINE_SEPARATOR.getBytes());
				}
				bos.flush();
			}
			finally {
				try {
					bis.close();
				}
				catch (IOException ex) {
				}
				try {
					bos.close();
				}
				catch (IOException ex) {
				}
			}
			this.cleanUpAfterCopy(tempFile, resultFile, sourceFile);
			return resultFile;
		}
	}

	private File handleByteArrayMessage(final byte[] bytes, File originalFile, File tempFile, final File resultFile) throws IOException {
		File fileToWriteTo = this.determineFileToWrite(resultFile, tempFile);

		final boolean append = FileExistsMode.APPEND.equals(this.fileExistsMode);

		final BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(fileToWriteTo, append));
		WhileLockedProcessor whileLockedProcessor = new WhileLockedProcessor(this.lockRegistry, fileToWriteTo.getAbsolutePath()){
			@Override
			protected void whileLocked() throws IOException {
				try {
					bos.write(bytes);
					if (FileWritingMessageHandler.this.appendNewLine) {
						bos.write(LINE_SEPARATOR.getBytes());
					}
				}
				finally {
					try {
						bos.close();
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

	private File handleStringMessage(final String content, File originalFile, File tempFile, final File resultFile) throws IOException {
		File fileToWriteTo = this.determineFileToWrite(resultFile, tempFile);

		final boolean append = FileExistsMode.APPEND.equals(this.fileExistsMode);

		final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileToWriteTo, append), this.charset));
		WhileLockedProcessor whileLockedProcessor = new WhileLockedProcessor(this.lockRegistry, fileToWriteTo.getAbsolutePath()){
			@Override
			protected void whileLocked() throws IOException {
				try {
					writer.write(content);
					if (FileWritingMessageHandler.this.appendNewLine) {
						writer.newLine();
					}
				}
				finally {
					try {
						writer.close();
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

	private File determineFileToWrite(File resultFile, File tempFile){

		final File fileToWriteTo;

		switch (this.fileExistsMode) {
			case APPEND:
				fileToWriteTo = resultFile;
				break;
			case FAIL:
			case IGNORE:
			case REPLACE:
				fileToWriteTo = tempFile;
				break;
			default:
				throw new IllegalStateException("Unsupported FileExistsMode "
					+ this.fileExistsMode);
		}
		return fileToWriteTo;
	}

	private void cleanUpAfterCopy(File fileToWriteTo, File resultFile, File originalFile) throws IOException{
		if (!FileExistsMode.APPEND.equals(this.fileExistsMode) && StringUtils.hasText(this.temporaryFileSuffix)) {
			this.renameTo(fileToWriteTo, resultFile);
		}

		if (this.deleteSourceFiles && originalFile != null) {
			originalFile.delete();
		}
	}

	private void renameTo(File tempFile, File resultFile) throws IOException{
		Assert.notNull(resultFile, "'resultFile' must not be null");
		Assert.notNull(tempFile, "'tempFile' must not be null");

		if (resultFile.exists()) {
			if (resultFile.setWritable(true, false) && resultFile.delete()){
				if (!tempFile.renameTo(resultFile)) {
					throw new IOException("Failed to rename file '" + tempFile.getAbsolutePath() + "' to '" + resultFile.getAbsolutePath() + "'");
				}
			}
			else {
				throw new IOException("Failed to rename file '" + tempFile.getAbsolutePath() + "' to '" + resultFile.getAbsolutePath() +
						"' since '" + resultFile.getName() + "' is not writable or can not be deleted");
			}
		}
		else {
			if (!tempFile.renameTo(resultFile)) {
				throw new IOException("Failed to rename file '" + tempFile.getAbsolutePath() + "' to '" + resultFile.getAbsolutePath() + "'");
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
		} else {
			throw new IllegalStateException(String.format("The provided " +
					"destinationDirectoryExpression (%s) must be of type " +
					"java.io.File or be a String.", this.destinationDirectoryExpression.getExpressionString()));
		}

		validateDestinationDirectory(destinationDirectory, this.autoCreateDirectory);
		return destinationDirectory;
	}

}
