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

package org.springframework.integration.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.util.DefaultLockRegistry;
import org.springframework.integration.util.LockRegistry;
import org.springframework.integration.util.PassThruLockRegistry;
import org.springframework.integration.util.WhileLockedProcessor;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

/**
 * A {@link MessageHandler} implementation that writes the Message payload to a
 * file. If the payload is a File object, it will copy the File to the specified
 * destination directory. If the payload is a byte array or String, it will write
 * it directly. Otherwise, the payload type is unsupported, and an Exception
 * will be thrown.
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
 */
public class FileWritingMessageHandler extends AbstractReplyProducingMessageHandler {

	private volatile String temporaryFileSuffix =".writing";

	private volatile boolean temporaryFileSuffixSet = false;

	private volatile boolean append = false;

	private final Log logger = LogFactory.getLog(this.getClass());

	private volatile FileNameGenerator fileNameGenerator = new DefaultFileNameGenerator();

	private final StandardEvaluationContext evaluationContext = new StandardEvaluationContext();

	private final File destinationDirectory;

	private final Expression destinationDirectoryExpression;

	private volatile boolean autoCreateDirectory = true;

	private volatile boolean deleteSourceFiles;

	private volatile Charset charset = Charset.defaultCharset();

	private volatile boolean expectReply = true;

	private volatile LockRegistry lockRegistry = new PassThruLockRegistry();

	/**
	 * Constructur which sets the {@link #destinationDirectory}.
	 *
	 * @param destinationDirectory Must not be null
	 * @see #FileWritingMessageHandler(Expression)
	 */
	public FileWritingMessageHandler(File destinationDirectory) {
		Assert.notNull(destinationDirectory, "Destination directory must not be null.");
		this.destinationDirectory = destinationDirectory;
		this.destinationDirectoryExpression = null;
	}

	/**
	 * Constructur which sets the {@link #destinationDirectoryExpression}.
	 *
	 * @param destinationDirectoryExpression Must not be null
	 * @see #FileWritingMessageHandler(File)
	 */
	public FileWritingMessageHandler(Expression destinationDirectoryExpression) {

		Assert.notNull(destinationDirectoryExpression, "Destination directory expression must not be null.");

		this.destinationDirectoryExpression = destinationDirectoryExpression;
		this.destinationDirectory = null;

	}

	/**
	 * Specify whether to create the destination directory automatically if it
	 * does not yet exist upon initialization. By default, this value is
	 * <emphasis>true</emphasis>. If set to <emphasis>false</emphasis> and the
	 * destination directory does not exist, an Exception will be thrown upon
	 * initialization.
	 */
	public void setAutoCreateDirectory(boolean autoCreateDirectory) {
		this.autoCreateDirectory = autoCreateDirectory;
	}

	/**
	 * By default, every file that is in the process of being transferred will
	 * appear in the file system with an additional suffix, which by default is
	 * ".writing". This can be changed by setting this property.
	 *
	 * @param temporaryFileSuffix
	 */
	public void setTemporaryFileSuffix(String temporaryFileSuffix) {
		Assert.notNull(temporaryFileSuffix, "'temporaryFileSuffix' must not be null"); // empty string is OK
		this.temporaryFileSuffix = temporaryFileSuffix;
		this.temporaryFileSuffixSet = true;
	}

	/**
	 * Will set 'append' flag which will let his handler to append data to the
	 * existing file rather then creating a new file for each Message.
	 * If 'true' it will also create a real instance of the LockRegistry to ensure
	 * that there is no collisions when multiple threads are writing to the same file.
	 * Otherwise the LockRegistry is set to {@link PassThruLockRegistry} which has no effect.
	 *
	 * @param append
	 */
	public void setAppend(boolean append) {
		this.append = append;
		if (this.append){
			this.lockRegistry = this.lockRegistry instanceof PassThruLockRegistry
					? new DefaultLockRegistry()
			        : this.lockRegistry;
		}
	}

	/**
	 * Specify whether a reply Message is expected. If not, this handler will simply return null for a
	 * successful response or throw an Exception for a non-successful response. The default is true.
	 */
	public void setExpectReply(boolean expectReply) {
		this.expectReply = expectReply;
	}

	protected String getTemporaryFileSuffix() {
		return temporaryFileSuffix;
	}

	/**
	 * Provide the {@link FileNameGenerator} strategy to use when generating
	 * the destination file's name.
	 */
	public void setFileNameGenerator(FileNameGenerator fileNameGenerator) {
		Assert.notNull(fileNameGenerator, "FileNameGenerator must not be null");
		this.fileNameGenerator = fileNameGenerator;
	}

	/**
	 * Specify whether to delete source Files after writing to the destination
	 * directory. The default is <em>false</em>. When set to <em>true</em>, it
	 * will only have an effect if the inbound Message has a File payload or
	 * a {@link FileHeaders#ORIGINAL_FILE} header value containing either a
	 * File instance or a String representing the original file path.
	 */
	public void setDeleteSourceFiles(boolean deleteSourceFiles) {
		this.deleteSourceFiles = deleteSourceFiles;
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

	@Override
	public final void onInit() {

		if (this.destinationDirectory != null) {
			validateDestinationDirectory(this.destinationDirectory, this.autoCreateDirectory);
		} else {
			this.evaluationContext.addPropertyAccessor(new MapAccessor());

			final BeanFactory beanFactory = this.getBeanFactory();

			if (beanFactory != null) {
				this.evaluationContext.setBeanResolver(new BeanFactoryResolver(beanFactory));
			}
		}

	}

	private void validateDestinationDirectory(File destinationDirectory, boolean autoCreateDirectory) {

		if (!destinationDirectory.exists() && autoCreateDirectory) {
			destinationDirectory.mkdirs();
		}

		Assert.isTrue(destinationDirectory.exists(),
				"Destination directory [" + destinationDirectory + "] does not exist.");
		Assert.isTrue(destinationDirectory.isDirectory(),
				"Destination path [" + destinationDirectory + "] does not point to a directory.");
		Assert.isTrue(destinationDirectory.canWrite(),
				"Destination directory [" + destinationDirectory + "] is not writable.");
		Assert.state(!(this.temporaryFileSuffixSet && this.append),
				"'temporaryFileSuffix' can not be set when appending to an existing file");;
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		Assert.notNull(requestMessage, "message must not be null");
		Object payload = requestMessage.getPayload();
		Assert.notNull(payload, "message payload must not be null");
		String generatedFileName = this.fileNameGenerator.generateFileName(requestMessage);
		File originalFileFromHeader = this.retrieveOriginalFileFromHeader(requestMessage);

		File tempFile;
		File resultFile;

		if (this.destinationDirectory != null) {
			tempFile = new File(this.destinationDirectory, generatedFileName + temporaryFileSuffix);
			resultFile = new File(this.destinationDirectory, generatedFileName);
		} else {
			final File destinationDirectoryToUse = evaluateDestinationDirectoryExpression(requestMessage);
			validateDestinationDirectory(destinationDirectoryToUse, this.autoCreateDirectory);

			tempFile = new File(destinationDirectoryToUse, generatedFileName + temporaryFileSuffix);
			resultFile = new File(destinationDirectoryToUse, generatedFileName);
		}

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

		if (!this.expectReply) {
			return null;
		}

		if (resultFile != null) {
			if (originalFileFromHeader == null && payload instanceof File) {
				return MessageBuilder.withPayload(resultFile)
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
		if (this.append){
			File fileToWriteTo = this.determineFileToWrite(resultFile, tempFile);
			final FileOutputStream fos = new FileOutputStream(fileToWriteTo, this.append);
			final FileInputStream fis = new FileInputStream(sourceFile);
			WhileLockedProcessor whileLockedProcessor = new WhileLockedProcessor(this.lockRegistry, fileToWriteTo.getAbsolutePath()){
				@Override
				protected void whileLocked() throws IOException {
					FileCopyUtils.copy(fis, fos);
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
			FileCopyUtils.copy(sourceFile, tempFile);
			this.cleanUpAfterCopy(tempFile, resultFile, sourceFile);
			return resultFile;
		}
	}

	private File handleByteArrayMessage(final byte[] bytes, File originalFile, File tempFile, final File resultFile) throws IOException {
		File fileToWriteTo = this.determineFileToWrite(resultFile, tempFile);
		final FileOutputStream fos = new FileOutputStream(fileToWriteTo, this.append);
		WhileLockedProcessor whileLockedProcessor = new WhileLockedProcessor(this.lockRegistry, fileToWriteTo.getAbsolutePath()){
			@Override
			protected void whileLocked() throws IOException {
				FileCopyUtils.copy(bytes, fos);
			}

		};
		whileLockedProcessor.doWhileLocked();
		this.cleanUpAfterCopy(fileToWriteTo, resultFile, originalFile);
		return resultFile;
	}

	private File handleStringMessage(final String content, File originalFile, File tempFile, final File resultFile) throws IOException {
		File fileToWriteTo = this.determineFileToWrite(resultFile, tempFile);
		final OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(fileToWriteTo, this.append), this.charset);
		WhileLockedProcessor whileLockedProcessor = new WhileLockedProcessor(this.lockRegistry, fileToWriteTo.getAbsolutePath()){
			@Override
			protected void whileLocked() throws IOException {
				FileCopyUtils.copy(content, writer);
			}

		};
		whileLockedProcessor.doWhileLocked();

		this.cleanUpAfterCopy(fileToWriteTo, resultFile, originalFile);
		return resultFile;
	}

	private File determineFileToWrite(File resultFile, File tempFile){
		File fileToWriteTo = null;
		if (this.append){
			fileToWriteTo  = resultFile;
		}
		else {
			fileToWriteTo  = tempFile;
		}
		return fileToWriteTo;
	}

	private void cleanUpAfterCopy(File fileToWriteTo, File resultFile, File originalFile) throws IOException{
		if (!this.append){
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
		final String destinationDirectoryToUse = this.destinationDirectoryExpression.getValue(
								this.evaluationContext, message, String.class);

		Assert.hasText(destinationDirectoryToUse, String.format(
				"Unable to resolve destination directory name for the provided Expression '%s'.",
				this.destinationDirectoryExpression.getExpressionString()));

		return new File(destinationDirectoryToUse);

	}

}
