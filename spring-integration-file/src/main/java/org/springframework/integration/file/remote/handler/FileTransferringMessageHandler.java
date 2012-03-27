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

package org.springframework.integration.file.remote.handler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.springframework.expression.Expression;
import org.springframework.integration.Message;
import org.springframework.integration.MessageDeliveryException;
import org.springframework.integration.MessagingException;
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
public class FileTransferringMessageHandler<F> extends AbstractMessageHandler {
	
	private volatile String temporaryFileSuffix =".writing";

	private final SessionFactory<F> sessionFactory;
	
	private volatile boolean autoCreateDirectory = false;

	private volatile ExpressionEvaluatingMessageProcessor<String> directoryExpressionProcessor;
	
	private volatile ExpressionEvaluatingMessageProcessor<String> temporaryDirectoryExpressionProcessor;

	private volatile FileNameGenerator fileNameGenerator = new DefaultFileNameGenerator();

	private volatile File temporaryDirectory = new File(System.getProperty("java.io.tmpdir"));

	private volatile String charset = "UTF-8";

	private volatile String remoteFileSeparator = "/";


	public FileTransferringMessageHandler(SessionFactory<F> sessionFactory) {
		Assert.notNull(sessionFactory, "sessionFactory must not be null");
		this.sessionFactory = sessionFactory;
	}


	public void setAutoCreateDirectory(boolean autoCreateDirectory) {
		this.autoCreateDirectory = autoCreateDirectory;
	}
	
	public void setRemoteFileSeparator(String remoteFileSeparator) {
		Assert.notNull(remoteFileSeparator, "'remoteFileSeparator' must not be null");
		this.remoteFileSeparator = remoteFileSeparator;
	}

	public void setRemoteDirectoryExpression(Expression remoteDirectoryExpression) {
		Assert.notNull(remoteDirectoryExpression, "remoteDirectoryExpression must not be null");
		this.directoryExpressionProcessor = new ExpressionEvaluatingMessageProcessor<String>(remoteDirectoryExpression, String.class);
	}
	
	public void setTemporaryRemoteDirectoryExpression(Expression temporaryRemoteDirectoryExpression) {
		Assert.notNull(temporaryRemoteDirectoryExpression, "temporaryRemoteDirectoryExpression must not be null");
		this.temporaryDirectoryExpressionProcessor = new ExpressionEvaluatingMessageProcessor<String>(temporaryRemoteDirectoryExpression, String.class);
	}
	
	protected String getTemporaryFileSuffix() {
		return this.temporaryFileSuffix;
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
	
	public void setTemporaryFileSuffix(String temporaryFileSuffix) {
		this.temporaryFileSuffix = temporaryFileSuffix;
	}

	protected void onInit() throws Exception {
		Assert.notNull(this.directoryExpressionProcessor, "remoteDirectoryExpression is required");
		if (this.autoCreateDirectory){
			Assert.hasText(this.remoteFileSeparator, "'remoteFileSeparator' must not be empty when 'autoCreateDirectory' is set to 'true'");
		}
	}
	
	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		File file = this.redeemForStorableFile(message);
		if (file != null && file.exists()) {
			Session<F> session = this.sessionFactory.getSession();
			try {
				String remoteDirectory = this.directoryExpressionProcessor.processMessage(message);
				String temporaryRemoteDirectory = remoteDirectory;
				if (this.temporaryDirectoryExpressionProcessor != null){
					temporaryRemoteDirectory = this.directoryExpressionProcessor.processMessage(message);
				}
				String fileName = this.fileNameGenerator.generateFileName(message);
				this.sendFileToRemoteDirectory(file, temporaryRemoteDirectory, remoteDirectory, fileName, session);
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
						catch (Throwable t) {
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
					bytes = ((String) payload).getBytes(this.charset);
				}
				else {
					bytes = (byte[]) payload;
				}
				FileCopyUtils.copy(bytes, sendableFile);
			}
			else {
				throw new IllegalArgumentException("Unsupported payload type. The only supported payloads are " +
							"java.io.File, java.lang.String, and byte[]");
			}
			return sendableFile;
		}
		catch (Exception e) {
			throw new MessageDeliveryException(message, "Failed to create sendable file.", e);
		}
	}

	private void sendFileToRemoteDirectory(File file, String temporaryRemoteDirectory, String remoteDirectory, String fileName, Session<F> session) 
			throws FileNotFoundException, IOException {
		
		remoteDirectory = this.normalizeDirectoryPath(remoteDirectory);
		temporaryRemoteDirectory = this.normalizeDirectoryPath(temporaryRemoteDirectory);
		
		String remoteFilePath = remoteDirectory + fileName;
		String tempRemoteFilePath = temporaryRemoteDirectory + fileName;
		// write remote file first with .writing extension
		String tempFilePath = tempRemoteFilePath + this.temporaryFileSuffix;
		
		if (this.autoCreateDirectory) {
			try {
				this.makeDirectories(remoteDirectory, session);
			} 
			catch (IllegalStateException e) {
				// Revert to old FTP behavior if recursive mkdir fails, for backwards compatibility
				session.mkdir(remoteDirectory);
			}
		}
		
		FileInputStream fileInputStream = new FileInputStream(file);
		try {
			session.write(fileInputStream, tempFilePath);
			// then rename it to its final name
			session.rename(tempFilePath, remoteFilePath);
		} 
		catch (Exception e) {
			throw new MessagingException("Failed to write to '" + tempFilePath + "' while uploading the file", e);
		}
		finally {
			fileInputStream.close();
		}
	}
	
	private String normalizeDirectoryPath(String directoryPath){
		if (!StringUtils.hasText(directoryPath)) {
			directoryPath = "";
		}
		else if (!directoryPath.endsWith(this.remoteFileSeparator)) {
			directoryPath += this.remoteFileSeparator; 
		}
		return directoryPath;
	}
	
	private void makeDirectories(String path, Session<F> session) throws IOException {
		if (!session.exists(path)){		

			int nextSeparatorIndex = path.lastIndexOf(remoteFileSeparator);
			
			if (nextSeparatorIndex > -1){
				List<String> pathsToCreate = new LinkedList<String>();
				while (nextSeparatorIndex > -1){
					String pathSegment = path.substring(0, nextSeparatorIndex);
					if (session.exists(pathSegment)){
						// no more paths to create
						break;
					}
					else {				
						pathsToCreate.add(0, pathSegment);
						nextSeparatorIndex = pathSegment.lastIndexOf(remoteFileSeparator);
					}
				}
				
				for (String pathToCreate : pathsToCreate) {
					if (logger.isDebugEnabled()){
						logger.debug("Creating '" + pathToCreate + "'");
					}
					session.mkdir(pathToCreate);
				}
			}
			else {
				session.mkdir(path);
			}
		}
	}
}
