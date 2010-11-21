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

package org.springframework.integration.file.remote.handler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.expression.Expression;
import org.springframework.integration.Message;
import org.springframework.integration.MessageDeliveryException;
import org.springframework.integration.file.DefaultFileNameGenerator;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

/**
 * A {@link org.springframework.integration.core.MessageHandler} implementation that transfers files to a remote server.
 *
 * @author Iwein Fuld
 * @author Mark Fisher
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class FileTransferringMessageHandler extends AbstractMessageHandler {

	private static final String TEMPORARY_FILE_SUFFIX = ".writing";


	private final SessionFactory sessionFactory;

	private volatile ExpressionEvaluatingMessageProcessor<String> directoryExpressionProcessor;

	private volatile FileNameGenerator fileNameGenerator = new DefaultFileNameGenerator();

	private volatile File temporaryBufferFolderFile;

	private volatile Resource temporaryBufferFolder = new FileSystemResource(System.getProperty("java.io.tmpdir"));

	private volatile String charset = Charset.defaultCharset().name();


	public FileTransferringMessageHandler(SessionFactory sessionFactory) {
		Assert.notNull(sessionFactory, "sessionFactory must not be null");
		this.sessionFactory = sessionFactory;
	}


	public void setRemoteDirectoryExpression(Expression remoteDirectoryExpression) {
		this.directoryExpressionProcessor = new ExpressionEvaluatingMessageProcessor<String>(remoteDirectoryExpression);
	}

	public void setTemporaryBufferFolder(Resource temporaryBufferFolder) {
		this.temporaryBufferFolder = temporaryBufferFolder;
	}

	public void setFileNameGenerator(FileNameGenerator fileNameGenerator) {
		this.fileNameGenerator = fileNameGenerator;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	protected void onInit() throws Exception {
		Assert.notNull(this.directoryExpressionProcessor, "remoteDirectoryExpression is required");
		Assert.notNull(this.temporaryBufferFolder, "temporaryBufferFolder must not be null");
		this.temporaryBufferFolderFile = this.temporaryBufferFolder.getFile();
	}

	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		File file = this.redeemForStorableFile(message);
		if (file != null && file.exists()) {
			Session session = this.sessionFactory.getSession();
			boolean sentSuccesfully = false;
			try {
				String targetDirectory = this.directoryExpressionProcessor.processMessage(message);
				sentSuccesfully = this.sendFileToRemoteDirectory(file, targetDirectory, session);
			}
			catch (FileNotFoundException e) {
				throw new MessageDeliveryException(message,
						"File [" + file + "] not found in local working directory; it was moved or deleted unexpectedly.", e);
			}
			catch (IOException e) {
				throw new MessageDeliveryException(message,
						"Failed to transfer file [" + file + "] from local working directory to remote FTP directory.", e);
			}
			catch (Exception e) {
				throw new MessageDeliveryException(message,
						"Error handling message for file [" + file + "]", e);
			}
			finally {
				if (file.exists()) {
					try {
						file.delete();
					}
					catch (Throwable th) {
						// ignore
					}
				}
				if (session != null) {
					session.close();
				}
			}
			if (!sentSuccesfully) {
				throw new MessageDeliveryException(message, "Failed to transfer file '" + file + "'");
			}
		}
	}

	private File handleFileMessage(File sourceFile, File tempFile, File resultFile) throws IOException {
		FileCopyUtils.copy(sourceFile, tempFile);
		tempFile.renameTo(resultFile);
		return resultFile;
	}

	private File handleByteArrayMessage(byte[] bytes, File tempFile, File resultFile) throws IOException {
		FileCopyUtils.copy(bytes, tempFile);
		tempFile.renameTo(resultFile);
		return resultFile;
	}

	private File handleStringMessage(String content, File tempFile, File resultFile, String charset) throws IOException {
		OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(tempFile), charset);
		FileCopyUtils.copy(content, writer);
		tempFile.renameTo(resultFile);
		return resultFile;
	}

	private File redeemForStorableFile(Message<?> message) throws MessageDeliveryException {
		try {
			Object payload = message.getPayload();
			String generateFileName = this.fileNameGenerator.generateFileName(message);
			File tempFile = new File(this.temporaryBufferFolderFile, generateFileName + TEMPORARY_FILE_SUFFIX);
			File resultFile = new File(this.temporaryBufferFolderFile, generateFileName);
			File sendableFile = null;
			if (payload instanceof String) {
				sendableFile = this.handleStringMessage((String) payload, tempFile, resultFile, this.charset);
			}
			else if (payload instanceof File) {
				sendableFile = this.handleFileMessage((File) payload, tempFile, resultFile);
			}
			else if (payload instanceof byte[]) {
				sendableFile = this.handleByteArrayMessage((byte[]) payload, tempFile, resultFile);
			}
			return sendableFile;
		}
		catch (Exception e) {
			throw new MessageDeliveryException(message, "Failed to create sendable file.", e);
		}
	}

	private boolean sendFileToRemoteDirectory(File file, String remoteDirectory, Session session) throws FileNotFoundException, IOException {
		FileInputStream fileInputStream = new FileInputStream(file);
		if (!StringUtils.endsWithIgnoreCase(remoteDirectory, File.separator)) {
			remoteDirectory += File.separatorChar;
		}
		String remoteFilePath = remoteDirectory + file.getName();
		session.put(fileInputStream, remoteFilePath);
		fileInputStream.close();
		return true;
	}

}
