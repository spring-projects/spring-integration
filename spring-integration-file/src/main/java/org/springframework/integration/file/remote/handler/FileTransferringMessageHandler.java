/*
 * Copyright 2002-2013 the original author or authors.
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

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.expression.Expression;
import org.springframework.integration.Message;
import org.springframework.integration.MessageDeliveryException;
import org.springframework.integration.MessagingException;
import org.springframework.integration.file.DefaultFileNameGenerator;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.file.remote.RemoteFileUtils;
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
 * @author David Turanski
 * @author Gary Russell
 * @since 2.0
 */
public class FileTransferringMessageHandler<F> extends AbstractMessageHandler {

	private volatile String temporaryFileSuffix =".writing";

	private final SessionFactory<F> sessionFactory;

	private volatile boolean autoCreateDirectory = false;

	private volatile boolean useTemporaryFileName = true;

	private volatile ExpressionEvaluatingMessageProcessor<String> directoryExpressionProcessor;

	private volatile ExpressionEvaluatingMessageProcessor<String> temporaryDirectoryExpressionProcessor;

	private volatile FileNameGenerator fileNameGenerator = new DefaultFileNameGenerator();

	private volatile boolean fileNameGeneratorSet;

	private volatile File temporaryDirectory = new File(System.getProperty("java.io.tmpdir"));

	private volatile String charset = "UTF-8";

	private volatile String remoteFileSeparator = "/";

	private volatile boolean hasExplicitlySetSuffix;


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

	protected boolean isUseTemporaryFileName() {
		return useTemporaryFileName;
	}


	public void setUseTemporaryFileName(boolean useTemporaryFileName) {
		this.useTemporaryFileName = useTemporaryFileName;
	}


	public void setFileNameGenerator(FileNameGenerator fileNameGenerator) {
		this.fileNameGenerator = (fileNameGenerator != null) ? fileNameGenerator : new DefaultFileNameGenerator();
		this.fileNameGeneratorSet = fileNameGenerator != null;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	public void setTemporaryFileSuffix(String temporaryFileSuffix) {
		Assert.notNull(temporaryFileSuffix, "'temporaryFileSuffix' must not be null");
		this.hasExplicitlySetSuffix = true;
		this.temporaryFileSuffix = temporaryFileSuffix;
	}

	@Override
	protected void onInit() throws Exception {
		Assert.notNull(this.directoryExpressionProcessor, "remoteDirectoryExpression is required");
		BeanFactory beanFactory = this.getBeanFactory();
		if (beanFactory != null) {
			this.directoryExpressionProcessor.setBeanFactory(beanFactory);
			if (this.temporaryDirectoryExpressionProcessor != null) {
				this.temporaryDirectoryExpressionProcessor.setBeanFactory(beanFactory);
			}
			if (!this.fileNameGeneratorSet && this.fileNameGenerator instanceof BeanFactoryAware) {
				((BeanFactoryAware) this.fileNameGenerator).setBeanFactory(beanFactory);
			}
		}
		if (this.autoCreateDirectory){
			Assert.hasText(this.remoteFileSeparator, "'remoteFileSeparator' must not be empty when 'autoCreateDirectory' is set to 'true'");
		}
		if (hasExplicitlySetSuffix && !useTemporaryFileName){
			this.logger.warn("Since 'use-temporary-file-name' is set to 'false' the value of 'temporary-file-suffix' has no effect");
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
					temporaryRemoteDirectory = this.temporaryDirectoryExpressionProcessor.processMessage(message);
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
		// write remote file first with temporary file extension if enabled

		String tempFilePath = tempRemoteFilePath + (useTemporaryFileName ? this.temporaryFileSuffix : "");

		if (this.autoCreateDirectory) {
			try {
				RemoteFileUtils.makeDirectories(remoteDirectory, session, this.remoteFileSeparator, this.logger);
			}
			catch (IllegalStateException e) {
				// Revert to old FTP behavior if recursive mkdir fails, for backwards compatibility
				session.mkdir(remoteDirectory);
			}
		}

		FileInputStream fileInputStream = new FileInputStream(file);
		try {
			session.write(fileInputStream, tempFilePath);
			// then rename it to its final name if necessary
			if (useTemporaryFileName){
			   session.rename(tempFilePath, remoteFilePath);
			}
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

}
