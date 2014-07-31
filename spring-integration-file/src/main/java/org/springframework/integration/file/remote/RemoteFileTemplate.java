/*
 * Copyright 2013-2014 the original author or authors.
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
package org.springframework.integration.file.remote;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.expression.Expression;
import org.springframework.integration.file.DefaultFileNameGenerator;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Iwein Fuld
 * @author Mark Fisher
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @author David Turanski
 * @author Gary Russell
 * @since 3.0
 *
 */
public class RemoteFileTemplate<F> implements RemoteFileOperations<F>, InitializingBean, BeanFactoryAware {

	private final Log logger = LogFactory.getLog(this.getClass());

	/**
	 * the {@link SessionFactory} for acquiring remote file Sessions.
	 */
	protected final SessionFactory<F> sessionFactory;

	private volatile String temporaryFileSuffix =".writing";

	private volatile boolean autoCreateDirectory = false;

	private volatile boolean useTemporaryFileName = true;

	private volatile ExpressionEvaluatingMessageProcessor<String> directoryExpressionProcessor;

	private volatile ExpressionEvaluatingMessageProcessor<String> temporaryDirectoryExpressionProcessor;

	private volatile ExpressionEvaluatingMessageProcessor<String> fileNameProcessor;

	private volatile FileNameGenerator fileNameGenerator = new DefaultFileNameGenerator();

	private volatile boolean fileNameGeneratorSet;

	private volatile String charset = "UTF-8";

	private volatile String remoteFileSeparator = "/";

	private volatile boolean hasExplicitlySetSuffix;

	private volatile BeanFactory beanFactory;

	public RemoteFileTemplate(SessionFactory<F> sessionFactory) {
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

	public final String getRemoteFileSeparator() {
		return remoteFileSeparator;
	}

	public void setRemoteDirectoryExpression(Expression remoteDirectoryExpression) {
		Assert.notNull(remoteDirectoryExpression, "remoteDirectoryExpression must not be null");
		this.directoryExpressionProcessor = new ExpressionEvaluatingMessageProcessor<String>(remoteDirectoryExpression, String.class);
	}

	public void setTemporaryRemoteDirectoryExpression(Expression temporaryRemoteDirectoryExpression) {
		Assert.notNull(temporaryRemoteDirectoryExpression, "temporaryRemoteDirectoryExpression must not be null");
		this.temporaryDirectoryExpressionProcessor = new ExpressionEvaluatingMessageProcessor<String>(temporaryRemoteDirectoryExpression, String.class);
	}

	public void setFileNameExpression(Expression fileNameExpression) {
		Assert.notNull(fileNameExpression, "fileNameExpression must not be null");
		this.fileNameProcessor = new ExpressionEvaluatingMessageProcessor<String>(fileNameExpression, String.class);
	}

	public String getTemporaryFileSuffix() {
		return this.temporaryFileSuffix;
	}

	public boolean isUseTemporaryFileName() {
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
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		BeanFactory beanFactory = this.beanFactory;
		if (beanFactory != null) {
			if (this.directoryExpressionProcessor != null) {
				this.directoryExpressionProcessor.setBeanFactory(beanFactory);
			}
			if (this.temporaryDirectoryExpressionProcessor != null) {
				this.temporaryDirectoryExpressionProcessor.setBeanFactory(beanFactory);
			}
			if (!this.fileNameGeneratorSet && this.fileNameGenerator instanceof BeanFactoryAware) {
				((BeanFactoryAware) this.fileNameGenerator).setBeanFactory(beanFactory);
			}
			if (this.fileNameProcessor != null) {
				this.fileNameProcessor.setBeanFactory(beanFactory);
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
	public String append(final Message<?> message) {
		return append(message, null);
	}

	@Override
	public String append(final Message<?> message, String subDirectory) {
		return send(message, subDirectory, FileExistsMode.APPEND);
	}

	@Override
	public String send(Message<?> message, FileExistsMode... mode) {
		return send(message, null, mode);
	}

	@Override
	public String send(final Message<?> message, String subDirectory, FileExistsMode... mode) {
		FileExistsMode modeToUse = mode == null || mode.length < 1 ? FileExistsMode.REPLACE : mode[0];
		return send(message, subDirectory, modeToUse);
	}

	private String send(final Message<?> message, final String subDirectory, final FileExistsMode mode) {
		Assert.notNull(this.directoryExpressionProcessor, "'remoteDirectoryExpression' is required");
		Assert.isTrue(!FileExistsMode.APPEND.equals(mode) || !this.useTemporaryFileName,
				"Cannot append when using a temporary file name");
		final StreamHolder inputStreamHolder = this.payloadToInputStream(message);
		if (inputStreamHolder != null) {
			return this.execute(new SessionCallback<F, String>() {

				@Override
				public String doInSession(Session<F> session) throws IOException {
					String fileName = inputStreamHolder.getName();
					try {
						String remoteDirectory = RemoteFileTemplate.this.directoryExpressionProcessor
								.processMessage(message);
						remoteDirectory = RemoteFileTemplate.this.normalizeDirectoryPath(remoteDirectory);
						if (StringUtils.hasText(subDirectory)) {
							if (subDirectory.startsWith(RemoteFileTemplate.this.remoteFileSeparator)) {
								remoteDirectory += subDirectory.substring(1);
							}
							else {
								remoteDirectory += RemoteFileTemplate.this.normalizeDirectoryPath(subDirectory);
							}
						}
						String temporaryRemoteDirectory = remoteDirectory;
						if (RemoteFileTemplate.this.temporaryDirectoryExpressionProcessor != null) {
							temporaryRemoteDirectory = RemoteFileTemplate.this.temporaryDirectoryExpressionProcessor
									.processMessage(message);
						}
						fileName = RemoteFileTemplate.this.fileNameGenerator.generateFileName(message);
						RemoteFileTemplate.this.sendFileToRemoteDirectory(inputStreamHolder.getStream(),
								temporaryRemoteDirectory, remoteDirectory, fileName, session, mode);
						return remoteDirectory + fileName;
					}
					catch (FileNotFoundException e) {
						throw new MessageDeliveryException(message, "File [" + inputStreamHolder.getName()
								+ "] not found in local working directory; it was moved or deleted unexpectedly.", e);
					}
					catch (IOException e) {
						throw new MessageDeliveryException(message, "Failed to transfer file ["
								+ inputStreamHolder.getName() + " -> " + fileName
								+ "] from local directory to remote directory.", e);
					}
					catch (Exception e) {
						throw new MessageDeliveryException(message, "Error handling message for file ["
								+ inputStreamHolder.getName() + " -> " + fileName + "]", e);
					}
				}
			});
		}
		else {
			// A null holder means a File payload that does not exist.
			if (logger.isWarnEnabled()) {
				logger.warn("File " + message.getPayload() + " does not exist");
			}
			return null;
		}
	}

	@Override
	public boolean exists(String path) {
		throw new UnsupportedOperationException("exists() is not supported by the generic template");
	}

	@Override
	public boolean remove(final String path) {
		return this.execute(new SessionCallback<F, Boolean>() {

			@Override
			public Boolean doInSession(Session<F> session) throws IOException {
				return session.remove(path);
			}
		});
	}

	@Override
	public void rename(final String fromPath, final String toPath) {
		Assert.hasText(fromPath, "Old filename cannot be null or empty");
		Assert.hasText(toPath, "New filename cannot be null or empty");

		this.execute(new SessionCallbackWithoutResult<F>() {

			@Override
			public void doInSessionWithoutResult(Session<F> session) throws IOException {
				int lastSeparator = toPath.lastIndexOf(RemoteFileTemplate.this.remoteFileSeparator);
				if (lastSeparator > 0) {
					String remoteFileDirectory = toPath.substring(0, lastSeparator + 1);
					RemoteFileUtils.makeDirectories(remoteFileDirectory, session,
							RemoteFileTemplate.this.remoteFileSeparator, RemoteFileTemplate.this.logger);
				}
				session.rename(fromPath, toPath);
			}
		});
	}

	@Override
	public boolean get(Message<?> message, InputStreamCallback callback) {
		Assert.notNull(this.fileNameProcessor, "A 'fileNameExpression' is needed to use get");
		String remotePath = this.fileNameProcessor.processMessage(message);
		return this.get(remotePath, callback);
	}

	@Override
	public boolean get(final String remotePath, final InputStreamCallback callback) {
		Assert.notNull(remotePath, "'remotePath' cannot be null");
		return this.execute(new SessionCallback<F, Boolean>() {

			@Override
			public Boolean doInSession(Session<F> session) throws IOException {
				InputStream inputStream = session.readRaw(remotePath);
				callback.doWithInputStream(inputStream);
				inputStream.close();
				return session.finalizeRaw();
			}
		});
	}

	@SuppressWarnings("rawtypes")
	@Override
	public <T> T execute(SessionCallback<F, T> callback) {
		Session<F> session = null;
		try {
			session = this.sessionFactory.getSession();
			Assert.notNull(session, "failed to acquire a Session");
			return callback.doInSession(session);
		}
		catch (Exception e) {
			if (session instanceof CachingSessionFactory<?>.CachedSession) {
				((CachingSessionFactory.CachedSession) session).dirty();
			}
			if (e instanceof MessagingException) {
				throw (MessagingException) e;
			}
			throw new MessagingException("Failed to execute on session", e);
		}
		finally {
			if (session != null) {
				try {
					session.close();
				}
				catch (Exception ignored) {
					if (logger.isDebugEnabled()) {
						logger.debug("failed to close Session", ignored);
					}
				}
			}
		}
	}

	@Override
	public <T, C> T executeWithClient(ClientCallback<C, T> callback) {
		throw new UnsupportedOperationException("executeWithClient() is not supported by the generic template");
	}

	private StreamHolder payloadToInputStream(Message<?> message) throws MessageDeliveryException {
		try {
			Object payload = message.getPayload();
			InputStream dataInputStream = null;
			String name = null;
			if (payload instanceof File) {
				File inputFile = (File) payload;
				if (inputFile.exists()) {
					dataInputStream = new BufferedInputStream(new FileInputStream(inputFile));
					name = inputFile.getAbsolutePath();
				}
			}
			else if (payload instanceof byte[] || payload instanceof String) {
				byte[] bytes = null;
				if (payload instanceof String) {
					bytes = ((String) payload).getBytes(this.charset);
					name = "String payload";
				}
				else {
					bytes = (byte[]) payload;
					name = "byte[] payload";
				}
				dataInputStream = new ByteArrayInputStream(bytes);
			}
			else {
				throw new IllegalArgumentException("Unsupported payload type. The only supported payloads are " +
							"java.io.File, java.lang.String, and byte[]");
			}
			if (dataInputStream == null) {
				return null;
			}
			else {
				return new StreamHolder(dataInputStream, name);
			}
		}
		catch (Exception e) {
			throw new MessageDeliveryException(message, "Failed to create sendable file.", e);
		}
	}

	private void sendFileToRemoteDirectory(InputStream inputStream, String temporaryRemoteDirectory,
			String remoteDirectory, String fileName, Session<F> session, FileExistsMode mode) throws IOException {

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

		try {
			boolean rename = this.useTemporaryFileName;
			if (FileExistsMode.REPLACE.equals(mode)) {
				session.write(inputStream, tempFilePath);
			}
			else if (FileExistsMode.APPEND.equals(mode)) {
				session.append(inputStream, tempFilePath);
			}
			else {
				if (exists(remoteFilePath)) {
					if (FileExistsMode.FAIL.equals(mode)) {
						throw new MessagingException(
								"The destination file already exists at '" + remoteFilePath + "'.");
					}
					else {
						if (logger.isDebugEnabled()) {
							logger.debug("File not transferred to '" + remoteFilePath + "'; already exists.");
						}
					}
					rename = false;
				}
			}
			// then rename it to its final name if necessary
			if (rename) {
			   session.rename(tempFilePath, remoteFilePath);
			}
		}
		catch (Exception e) {
			throw new MessagingException("Failed to write to '" + tempFilePath + "' while uploading the file", e);
		}
		finally {
			inputStream.close();
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

	private class StreamHolder {

		private final InputStream stream;

		private final String name;

		private StreamHolder(InputStream stream, String name) {
			this.stream = stream;
			this.name = name;
		}

		public InputStream getStream() {
			return stream;
		}

		public String getName() {
			return name;
		}

	}

}
