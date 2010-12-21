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
import java.io.IOException;

import org.springframework.expression.Expression;
import org.springframework.integration.Message;
import org.springframework.integration.MessageDeliveryException;
import org.springframework.integration.file.DefaultFileNameGenerator;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.file.FileWritingMessageHandler;
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

	private final SessionFactory sessionFactory;

	private volatile ExpressionEvaluatingMessageProcessor<String> directoryExpressionProcessor;

	private volatile FileNameGenerator fileNameGenerator = new DefaultFileNameGenerator();

	private volatile File temporaryDirectory = new File(System.getProperty("java.io.tmpdir"));

	private volatile String charset = "UTF-8";
	

	public FileTransferringMessageHandler(SessionFactory sessionFactory) {
		Assert.notNull(sessionFactory, "sessionFactory must not be null");
		this.sessionFactory = sessionFactory;
	}


	public void setRemoteDirectoryExpression(Expression remoteDirectoryExpression) {
		this.directoryExpressionProcessor = new ExpressionEvaluatingMessageProcessor<String>(remoteDirectoryExpression);
	}

	public void setTemporaryDirectory(File temporaryDirectory) {
		Assert.notNull(temporaryDirectory, "temporaryDirectory must not be null");
		this.temporaryDirectory = temporaryDirectory;
	}

	public void setFileNameGenerator(FileNameGenerator fileNameGenerator) {
		this.fileNameGenerator = (fileNameGenerator != null) ? fileNameGenerator : new DefaultFileNameGenerator();
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	protected void onInit() throws Exception {
		Assert.notNull(this.directoryExpressionProcessor, "remoteDirectoryExpression is required");
	}

	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		File file = this.redeemForStorableFile(message);
		if (file != null && file.exists()) {
			Session session = this.sessionFactory.getSession();
			try {
				String targetDirectory = this.directoryExpressionProcessor.processMessage(message);
				String fileName = this.fileNameGenerator.generateFileName(message);
				this.sendFileToRemoteDirectory(file, targetDirectory, fileName, session);
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
				if (!(message.getPayload() instanceof File)) {
					// we created the File, so we need to delete it
					if (file.exists()) {
						try {
							file.delete();
						}
						catch (Throwable th) {
							// ignore
						}
					}
				}
				if (session != null) {
					session.close();
				}
			}
		}
	}

	private File redeemForStorableFile(Message<?> message) throws MessageDeliveryException {
		try {
			Object payload = message.getPayload();
			File sendableFile = null;
			if (payload instanceof File) {
				sendableFile = (File) payload;
			}
			else if (payload instanceof byte[] || payload instanceof String) { 
				String tempFileName = this.fileNameGenerator.generateFileName(message) + ".tmp";
				sendableFile = new File(this.temporaryDirectory, tempFileName); // will only create temp file for String/byte[]
				byte[] bytes = null;
				if (payload instanceof String) {
					bytes = ((String) payload).getBytes(charset);
				}
				else {
					bytes = (byte[]) payload;
				}
				FileCopyUtils.copy(bytes, sendableFile);
			}
			else {
				throw new IllegalArgumentException("Unsupported payload type. The only supported payloads are " +
							"java.io.File, java.lang.String and byte[]");
			}
			return sendableFile;
		}
		catch (Exception e) {
			throw new MessageDeliveryException(message, "Failed to create sendable file.", e);
		}
	}

	private void sendFileToRemoteDirectory(File file, String remoteDirectory, String fileName, Session session) 
											throws FileNotFoundException, IOException {
		
		FileInputStream fileInputStream = new FileInputStream(file);
		if (!StringUtils.endsWithIgnoreCase(remoteDirectory, File.separator)) {
			remoteDirectory += File.separatorChar; 
		}
		String remoteFilePath = remoteDirectory + file.getName() + FileWritingMessageHandler.TEMPORARY_FILE_SUFFIX;
		// write remote file first with .writing extension
		session.write(fileInputStream, remoteFilePath);
		fileInputStream.close();
		// then rename it to its final name
		session.rename(remoteFilePath, remoteDirectory + fileName);
	}

}
