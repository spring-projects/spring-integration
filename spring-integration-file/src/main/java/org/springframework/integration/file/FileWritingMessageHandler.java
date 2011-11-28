/*
 * Copyright 2002-2010 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

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
 */
public class FileWritingMessageHandler extends AbstractReplyProducingMessageHandler {

	private volatile String temporaryFileSuffix =".writing";

	private final Log logger = LogFactory.getLog(this.getClass());

	private volatile FileNameGenerator fileNameGenerator = new DefaultFileNameGenerator();

	private final File destinationDirectory;

	private volatile boolean autoCreateDirectory = true;

	private volatile boolean deleteSourceFiles;

	private volatile Charset charset = Charset.defaultCharset();


	public FileWritingMessageHandler(File destinationDirectory) {
		Assert.notNull(destinationDirectory, "Destination directory must not be null.");
		this.destinationDirectory = destinationDirectory;
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

	public void setTemporaryFileSuffix(String temporaryFileSuffix) {
		this.temporaryFileSuffix = temporaryFileSuffix;
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
		if (!this.destinationDirectory.exists() && this.autoCreateDirectory) {
			this.destinationDirectory.mkdirs();
		}
		Assert.isTrue(destinationDirectory.exists(),
				"Destination directory [" + destinationDirectory + "] does not exist.");
		Assert.isTrue(this.destinationDirectory.isDirectory(),
				"Destination path [" + this.destinationDirectory + "] does not point to a directory.");
		Assert.isTrue(this.destinationDirectory.canWrite(),
				"Destination directory [" + this.destinationDirectory + "] is not writable.");
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		Assert.notNull(requestMessage, "message must not be null");
		Object payload = requestMessage.getPayload();
		Assert.notNull(payload, "message payload must not be null");
		String generatedFileName = this.fileNameGenerator.generateFileName(requestMessage);
		File originalFileFromHeader = this.retrieveOriginalFileFromHeader(requestMessage);
		File tempFile = new File(this.destinationDirectory, generatedFileName + temporaryFileSuffix);
		File resultFile = new File(this.destinationDirectory, generatedFileName);
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

	private File handleFileMessage(File sourceFile, File tempFile, File resultFile) throws IOException {
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
		this.renameTo(tempFile, resultFile);
		if (this.deleteSourceFiles) {
			sourceFile.delete();
		}
		return resultFile;
	}

	private File handleByteArrayMessage(byte[] bytes, File originalFile, File tempFile, File resultFile) throws IOException {
		FileCopyUtils.copy(bytes, tempFile);
		this.renameTo(tempFile, resultFile);
		if (this.deleteSourceFiles && originalFile != null) {
			originalFile.delete();
		}
		return resultFile;
	}

	private File handleStringMessage(String content, File originalFile, File tempFile, File resultFile) throws IOException {
		OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(tempFile), this.charset);
		FileCopyUtils.copy(content, writer);
		this.renameTo(tempFile, resultFile);
		if (this.deleteSourceFiles && originalFile != null) {
			originalFile.delete();
		}
		return resultFile;
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

}
