/*
 * Copyright 2013-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.expression.Expression;
import org.springframework.integration.file.DefaultFileNameGenerator;
import org.springframework.integration.file.FileNameGenerator;
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
 * A general abstraction for dealing with remote files.
 *
 * @author Iwein Fuld
 * @author Mark Fisher
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @author David Turanski
 * @author Gary Russell
 * @author Artem Bilan
 * @author Alen Turkovic
 *
 * @since 3.0
 *
 */
public class RemoteFileTemplate<F> implements RemoteFileOperations<F>, InitializingBean, BeanFactoryAware {

	private final Log logger = LogFactory.getLog(this.getClass());

	/**
	 * The {@link SessionFactory} for acquiring remote file Sessions.
	 */
	protected final SessionFactory<F> sessionFactory; // NOSONAR

	/*
	 * Not static as normal since we want this TL to be scoped within the template instance.
	 */
	private final ThreadLocal<Session<F>> contextSessions = new ThreadLocal<>();

	private final AtomicInteger activeTemplateCallbacks = new AtomicInteger();

	private String temporaryFileSuffix = ".writing";

	private boolean autoCreateDirectory = false;

	private boolean useTemporaryFileName = true;

	private ExpressionEvaluatingMessageProcessor<String> directoryExpressionProcessor;

	private ExpressionEvaluatingMessageProcessor<String> temporaryDirectoryExpressionProcessor;

	private ExpressionEvaluatingMessageProcessor<String> fileNameProcessor;

	private FileNameGenerator fileNameGenerator = new DefaultFileNameGenerator();

	private boolean fileNameGeneratorSet;

	private Charset charset = StandardCharsets.UTF_8;

	private String remoteFileSeparator = "/";

	private boolean hasExplicitlySetSuffix;

	private BeanFactory beanFactory;

	/**
	 * Construct a {@link RemoteFileTemplate} with the supplied session factory.
	 * @param sessionFactory the session factory.
	 */
	public RemoteFileTemplate(SessionFactory<F> sessionFactory) {
		Assert.notNull(sessionFactory, "sessionFactory must not be null");
		this.sessionFactory = sessionFactory;
	}

	/**
	 * @return this template's {@link SessionFactory}.
	 * @since 4.2
	 */
	public SessionFactory<F> getSessionFactory() {
		return this.sessionFactory;
	}

	/**
	 * Determine whether the remote directory should automatically be created when
	 * sending files to the remote system.
	 * @param autoCreateDirectory true to create the directory.
	 */
	public void setAutoCreateDirectory(boolean autoCreateDirectory) {
		this.autoCreateDirectory = autoCreateDirectory;
	}

	/**
	 * Set the file separator when dealing with remote files; default '/'.
	 * @param remoteFileSeparator the separator.
	 */
	public void setRemoteFileSeparator(String remoteFileSeparator) {
		Assert.notNull(remoteFileSeparator, "'remoteFileSeparator' must not be null");
		this.remoteFileSeparator = remoteFileSeparator;
	}

	/**
	 * @return the remote file separator.
	 */
	public final String getRemoteFileSeparator() {
		return this.remoteFileSeparator;
	}

	/**
	 * Set the remote directory expression used to determine the remote directory to which
	 * files will be sent.
	 * @param remoteDirectoryExpression the remote directory expression.
	 */
	public void setRemoteDirectoryExpression(Expression remoteDirectoryExpression) {
		Assert.notNull(remoteDirectoryExpression, "remoteDirectoryExpression must not be null");
		this.directoryExpressionProcessor =
				new ExpressionEvaluatingMessageProcessor<>(remoteDirectoryExpression, String.class);
	}

	/**
	 * Set a temporary remote directory expression; used when transferring files to the remote
	 * system. After a successful transfer the file is renamed using the
	 * {@link #setRemoteDirectoryExpression(Expression) remoteDirectoryExpression}.
	 * @param temporaryRemoteDirectoryExpression the temporary remote directory expression.
	 */
	public void setTemporaryRemoteDirectoryExpression(Expression temporaryRemoteDirectoryExpression) {
		Assert.notNull(temporaryRemoteDirectoryExpression, "temporaryRemoteDirectoryExpression must not be null");
		this.temporaryDirectoryExpressionProcessor =
				new ExpressionEvaluatingMessageProcessor<>(temporaryRemoteDirectoryExpression, String.class);
	}

	/**
	 * Set the file name expression to determine the full path to the remote file when retrieving
	 * a file using the {@link #get(Message, InputStreamCallback)} method, with the message
	 * being the root object of the evaluation.
	 * @param fileNameExpression the file name expression.
	 */
	public void setFileNameExpression(Expression fileNameExpression) {
		Assert.notNull(fileNameExpression, "fileNameExpression must not be null");
		this.fileNameProcessor = new ExpressionEvaluatingMessageProcessor<>(fileNameExpression, String.class);
	}

	/**
	 * @return the temporary file suffix.
	 */
	public String getTemporaryFileSuffix() {
		return this.temporaryFileSuffix;
	}

	/**
	 * @return whether a temporary file name is used when sending files to the remote
	 * system.
	 */
	public boolean isUseTemporaryFileName() {
		return this.useTemporaryFileName;
	}

	/**
	 * Set whether a temporary file name is used when sending files to the remote system.
	 * @param useTemporaryFileName true to use a temporary file name.
	 * @see #setTemporaryFileSuffix(String)
	 */
	public void setUseTemporaryFileName(boolean useTemporaryFileName) {
		this.useTemporaryFileName = useTemporaryFileName;
	}

	/**
	 * Set the file name generator used to generate the remote filename to be used when transferring
	 * files to the remote system. Default {@link DefaultFileNameGenerator}.
	 * @param fileNameGenerator the file name generator.
	 */
	public void setFileNameGenerator(FileNameGenerator fileNameGenerator) {
		this.fileNameGenerator = (fileNameGenerator != null) ? fileNameGenerator : new DefaultFileNameGenerator();
		this.fileNameGeneratorSet = fileNameGenerator != null;
	}

	/**
	 * Set the charset to use when converting String payloads to bytes as the content of the
	 * remote file. Default {@code UTF-8}.
	 * @param charset the charset.
	 */
	public void setCharset(String charset) {
		this.charset = Charset.forName(charset);
	}

	/**
	 * Set the temporary suffix to use when transferring files to the remote system.
	 * Default ".writing".
	 * @param temporaryFileSuffix the suffix
	 * @see #setUseTemporaryFileName(boolean)
	 */
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
	public void afterPropertiesSet() {
		if (this.beanFactory != null) {
			if (this.directoryExpressionProcessor != null) {
				this.directoryExpressionProcessor.setBeanFactory(this.beanFactory);
			}
			if (this.temporaryDirectoryExpressionProcessor != null) {
				this.temporaryDirectoryExpressionProcessor.setBeanFactory(this.beanFactory);
			}
			if (!this.fileNameGeneratorSet && this.fileNameGenerator instanceof BeanFactoryAware) {
				((BeanFactoryAware) this.fileNameGenerator).setBeanFactory(this.beanFactory);
			}
			if (this.fileNameProcessor != null) {
				this.fileNameProcessor.setBeanFactory(this.beanFactory);
			}
		}
		if (this.autoCreateDirectory) {
			Assert.hasText(this.remoteFileSeparator,
					"'remoteFileSeparator' must not be empty when 'autoCreateDirectory' is set to 'true'");
		}
		if (this.hasExplicitlySetSuffix && !this.useTemporaryFileName) {
			this.logger.warn("Since 'use-temporary-file-name' is set to 'false' " +
					"the value of 'temporary-file-suffix' has no effect");
		}
	}

	@Override
	public String append(Message<?> message) {
		return append(message, null);
	}

	@Override
	public String append(Message<?> message, String subDirectory) {
		return send(message, subDirectory, FileExistsMode.APPEND);
	}

	@Override
	public String send(Message<?> message, FileExistsMode... mode) {
		return send(message, null, mode);
	}

	@Override
	public String send(Message<?> message, String subDirectory, FileExistsMode... mode) {
		FileExistsMode modeToUse = mode == null || mode.length < 1 || mode[0] == null
				? FileExistsMode.REPLACE
				: mode[0];
		return send(message, subDirectory, modeToUse);
	}

	private String send(Message<?> message, String subDirectory, FileExistsMode mode) {
		Assert.notNull(this.directoryExpressionProcessor, "'remoteDirectoryExpression' is required");
		Assert.isTrue(!FileExistsMode.APPEND.equals(mode) || !this.useTemporaryFileName,
				"Cannot append when using a temporary file name");
		Assert.isTrue(!FileExistsMode.REPLACE_IF_MODIFIED.equals(mode),
				"FilExistsMode.REPLACE_IF_MODIFIED can only be used for local files");
		final StreamHolder inputStreamHolder = payloadToInputStream(message);
		if (inputStreamHolder != null) {
			try {
				return execute(session -> doSend(message, subDirectory, mode, inputStreamHolder, session));
			}
			finally {
				try {
					inputStreamHolder.stream.close();
				}
				catch (@SuppressWarnings("unused") IOException e) {
				}
			}
		}
		else {
			// A null holder means a File payload that does not exist.
			if (this.logger.isWarnEnabled()) {
				this.logger.warn("File " + message.getPayload() + " does not exist");
			}
			return null;
		}
	}

	private String doSend(Message<?> message, String subDirectory, FileExistsMode mode,
			StreamHolder inputStreamHolder, Session<F> session) {

		String fileName = inputStreamHolder.name;
		try {
			String remoteDirectory = this.directoryExpressionProcessor.processMessage(message);
			remoteDirectory = normalizeDirectoryPath(remoteDirectory);
			if (StringUtils.hasText(subDirectory)) {
				if (subDirectory.startsWith(this.remoteFileSeparator)) {
					remoteDirectory += subDirectory.substring(1);
				}
				else {
					remoteDirectory += normalizeDirectoryPath(subDirectory);
				}
			}
			String temporaryRemoteDirectory = remoteDirectory;
			if (this.temporaryDirectoryExpressionProcessor != null) {
				temporaryRemoteDirectory = this.temporaryDirectoryExpressionProcessor.processMessage(message);
			}
			fileName = this.fileNameGenerator.generateFileName(message);
			sendFileToRemoteDirectory(inputStreamHolder.stream, temporaryRemoteDirectory, remoteDirectory, fileName,
					session, mode);
			return remoteDirectory + fileName;
		}
		catch (FileNotFoundException e) {
			throw new MessageDeliveryException(message, "File [" + inputStreamHolder.name
					+ "] not found in local working directory; it was moved or deleted unexpectedly.", e);
		}
		catch (IOException e) {
			throw new MessageDeliveryException(message, "Failed to transfer file ["
					+ inputStreamHolder.name + " -> " + fileName
					+ "] from local directory to remote directory.", e);
		}
		catch (Exception e) {
			throw new MessageDeliveryException(message, "Error handling message for file ["
					+ inputStreamHolder.name + " -> " + fileName + "]", e);
		}
	}

	@Override
	public boolean exists(String path) {
		return execute(session -> session.exists(path));
	}

	@Override
	public boolean remove(String path) {
		return execute(session -> session.remove(path));
	}

	@Override
	public void rename(String fromPath, String toPath) {
		Assert.hasText(fromPath, "Old filename cannot be null or empty");
		Assert.hasText(toPath, "New filename cannot be null or empty");

		this.execute((SessionCallbackWithoutResult<F>) session -> {
			int lastSeparator = toPath.lastIndexOf(RemoteFileTemplate.this.remoteFileSeparator);
			if (lastSeparator > 0) {
				String remoteFileDirectory = toPath.substring(0, lastSeparator + 1);
				RemoteFileUtils.makeDirectories(remoteFileDirectory, session,
						RemoteFileTemplate.this.remoteFileSeparator, RemoteFileTemplate.this.logger);
			}
			session.rename(fromPath, toPath);
		});
	}

	/**
	 * @see #setFileNameExpression(Expression)
	 */
	@Override
	public boolean get(Message<?> message, InputStreamCallback callback) {
		Assert.notNull(this.fileNameProcessor, "A 'fileNameExpression' is needed to use get");
		String remotePath = this.fileNameProcessor.processMessage(message);
		return get(remotePath, callback);
	}

	@Override
	public boolean get(String remotePath, InputStreamCallback callback) {
		Assert.notNull(remotePath, "'remotePath' cannot be null");
		return execute(session -> {
			try (InputStream inputStream = session.readRaw(remotePath)) {
				callback.doWithInputStream(inputStream);
				return session.finalizeRaw();
			}
		});
	}


	@Override
	public F[] list(String path) {
		return execute(session -> session.list(path));
	}

	@Override
	public Session<F> getSession() {
		if (this.activeTemplateCallbacks.get() > 0) {
			Session<F> session = this.contextSessions.get();
			// If no session in the ThreadLocal, no {@code invoke()} in this call stack
			if (session != null) {
				return session;
			}
		}

		return this.sessionFactory.getSession();
	}

	@Override
	public <T> T execute(SessionCallback<F, T> callback) {
		Session<F> session = null;
		boolean invokeScope = false;
		if (this.activeTemplateCallbacks.get() > 0) {
			session = this.contextSessions.get();
		}
		try {
			if (session == null) {
				session = this.sessionFactory.getSession();
			}
			else {
				invokeScope = true;
			}
			return callback.doInSession(session);
		}
		catch (Exception e) {
			if (session != null) {
				session.dirty();
			}
			if (e instanceof MessagingException) { // NOSONAR
				throw (MessagingException) e;
			}
			throw new MessagingException("Failed to execute on session", e);
		}
		finally {
			if (!invokeScope && session != null) {
				try {
					session.close();
				}
				catch (Exception ignored) {
					this.logger.debug("failed to close Session", ignored);
				}
			}
		}
	}

	@Override
	public <T> T invoke(OperationsCallback<F, T> action) {
		Session<F> contextSession = this.contextSessions.get();
		if (contextSession == null) {
			this.contextSessions.set(this.sessionFactory.getSession());
		}
		this.activeTemplateCallbacks.incrementAndGet();

		try {
			return action.doInOperations(this);
		}
		finally {
			this.activeTemplateCallbacks.decrementAndGet();
			if (contextSession == null) {
				Session<F> session = this.contextSessions.get();
				if (session != null) {
					session.close();
				}
				this.contextSessions.remove();
			}
		}
	}

	@Override
	public <T, C> T executeWithClient(ClientCallback<C, T> callback) {
		throw new UnsupportedOperationException("executeWithClient() is not supported by the generic template");
	}

	private StreamHolder payloadToInputStream(Message<?> message) throws MessageDeliveryException {
		Object payload = message.getPayload();
		try {
			if (payload instanceof File) {
				File inputFile = (File) payload;
				if (inputFile.exists()) {
					return new StreamHolder(
							new BufferedInputStream(new FileInputStream(inputFile)), inputFile.getAbsolutePath());
				}
			}
			else if (payload instanceof byte[] || payload instanceof String) {
				byte[] bytes;
				String name;
				if (payload instanceof String) {
					bytes = ((String) payload).getBytes(this.charset);
					name = "String payload";
				}
				else {
					bytes = (byte[]) payload;
					name = "byte[] payload";
				}
				return new StreamHolder(new ByteArrayInputStream(bytes), name);
			}
			else if (payload instanceof InputStream) {
				return new StreamHolder((InputStream) payload, "InputStream payload");
			}
			else if (payload instanceof Resource) {
				Resource resource = (Resource) payload;
				String filename = resource.getFilename();
				return new StreamHolder(resource.getInputStream(), filename != null ? filename : "Resource payload");
			}
			else {
				throw new IllegalArgumentException("Unsupported payload type ["
						+ payload.getClass().getName()
						+ "]. The only supported payloads are " +
						"java.io.File, java.lang.String, byte[], and InputStream");
			}
		}
		catch (Exception e) {
			throw new MessageDeliveryException(message, "Failed to create sendable file.", e);
		}
		return null;
	}

	private void sendFileToRemoteDirectory(InputStream inputStream, String temporaryRemoteDirectoryArg,
			String remoteDirectoryArg, String fileName, Session<F> session, FileExistsMode mode) throws IOException {

		String remoteDirectory = normalizeDirectoryPath(remoteDirectoryArg);
		String temporaryRemoteDirectory = normalizeDirectoryPath(temporaryRemoteDirectoryArg);

		String remoteFilePath = remoteDirectory + fileName;
		String tempRemoteFilePath = temporaryRemoteDirectory + fileName;
		// write remote file first with temporary file extension if enabled

		String tempFilePath = tempRemoteFilePath + (this.useTemporaryFileName ? this.temporaryFileSuffix : "");

		if (this.autoCreateDirectory) {
			try {
				RemoteFileUtils.makeDirectories(remoteDirectory, session, this.remoteFileSeparator, this.logger);
			}
			catch (@SuppressWarnings("unused") IllegalStateException e) {
				// Revert to old FTP behavior if recursive mkdir fails, for backwards compatibility
				session.mkdir(remoteDirectory);
			}
		}

		try (InputStream stream = inputStream) {
			doSend(session, mode, remoteFilePath, tempFilePath, stream);
		}
		catch (Exception e) {
			throw new MessagingException("Failed to write to '" + tempFilePath + "' while uploading the file", e);
		}
	}

	private void doSend(Session<F> session, FileExistsMode mode, String remoteFilePath, String tempFilePath,
			InputStream stream) throws IOException {

		boolean rename = this.useTemporaryFileName;
		if (FileExistsMode.REPLACE.equals(mode)) {
			session.write(stream, tempFilePath);
		}
		else if (FileExistsMode.APPEND.equals(mode)) {
			session.append(stream, tempFilePath);
		}
		else {
			if (session.exists(remoteFilePath)) {
				if (FileExistsMode.FAIL.equals(mode)) {
					throw new MessagingException(
							"The destination file already exists at '" + remoteFilePath + "'.");
				}
				else {
					if (this.logger.isDebugEnabled()) {
						this.logger.debug("File not transferred to '" + remoteFilePath + "'; already exists.");
					}
				}
				rename = false;
			}
			else {
				session.write(stream, tempFilePath);
			}
		}
		// then rename it to its final name if necessary
		if (rename) {
			session.rename(tempFilePath, remoteFilePath);
		}
	}

	private String normalizeDirectoryPath(String directoryPath) {
		if (!StringUtils.hasText(directoryPath)) {
			return "";
		}
		else if (!directoryPath.endsWith(this.remoteFileSeparator)) {
			return directoryPath + this.remoteFileSeparator;
		}
		else {
			return directoryPath;
		}
	}

	private static final class StreamHolder {

		private final InputStream stream;

		private final String name;

		StreamHolder(InputStream stream, String name) {
			this.stream = stream;
			this.name = name;
		}

	}

}
