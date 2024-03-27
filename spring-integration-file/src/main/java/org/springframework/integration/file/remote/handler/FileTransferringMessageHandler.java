/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.file.remote.handler;

import org.springframework.expression.Expression;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * A {@link org.springframework.messaging.MessageHandler} implementation that transfers files to a remote server.
 *
 * @param <F> the file type.
 *
 * @author Iwein Fuld
 * @author Mark Fisher
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @author David Turanski
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class FileTransferringMessageHandler<F> extends AbstractMessageHandler {

	protected final RemoteFileTemplate<F> remoteFileTemplate; // NOSONAR

	private final FileExistsMode mode;

	private Integer chmod;

	public FileTransferringMessageHandler(SessionFactory<F> sessionFactory) {
		Assert.notNull(sessionFactory, "sessionFactory must not be null");
		this.remoteFileTemplate = new RemoteFileTemplate<F>(sessionFactory);
		this.mode = FileExistsMode.REPLACE;
	}

	public FileTransferringMessageHandler(RemoteFileTemplate<F> remoteFileTemplate) {
		this(remoteFileTemplate, FileExistsMode.REPLACE);
	}

	public FileTransferringMessageHandler(RemoteFileTemplate<F> remoteFileTemplate, FileExistsMode mode) {
		Assert.notNull(remoteFileTemplate, "remoteFileTemplate must not be null");
		this.remoteFileTemplate = remoteFileTemplate;
		this.mode = mode;
	}

	/**
	 * A {@code boolean} flag to indicate automatically create the directory or not.
	 * @param autoCreateDirectory true to automatically create the directory.
	 * @see RemoteFileTemplate#setAutoCreateDirectory(boolean)
	 */
	public void setAutoCreateDirectory(boolean autoCreateDirectory) {
		this.remoteFileTemplate.setAutoCreateDirectory(autoCreateDirectory);
	}

	/**
	 * Specify a remote file separator symbol.
	 * @param remoteFileSeparator the remote file separator.
	 * @see RemoteFileTemplate#setRemoteFileSeparator(String)
	 */
	public void setRemoteFileSeparator(String remoteFileSeparator) {
		this.remoteFileTemplate.setRemoteFileSeparator(remoteFileSeparator);
	}

	/**
	 * Specify a remote directory path SpEL expression.
	 * @param remoteDirectoryExpression the remote directory expression
	 * @see RemoteFileTemplate#setRemoteDirectoryExpression(Expression)
	 */
	public void setRemoteDirectoryExpression(Expression remoteDirectoryExpression) {
		this.remoteFileTemplate.setRemoteDirectoryExpression(remoteDirectoryExpression);
	}

	/**
	 * Specify a remote directory path SpEL expression.
	 * @param remoteDirectoryExpression the remote directory expression
	 * @since 4.3.13
	 * @see #setRemoteDirectoryExpression(Expression)
	 */
	public void setRemoteDirectoryExpressionString(String remoteDirectoryExpression) {
		setRemoteDirectoryExpression(EXPRESSION_PARSER.parseExpression(remoteDirectoryExpression));
	}

	/**
	 * Specify a remote directory path SpEL expression.
	 * @param temporaryRemoteDirectoryExpression the temporary remote directory expression
	 * @see RemoteFileTemplate#setTemporaryRemoteDirectoryExpression(Expression)
	 */
	public void setTemporaryRemoteDirectoryExpression(Expression temporaryRemoteDirectoryExpression) {
		this.remoteFileTemplate.setTemporaryRemoteDirectoryExpression(temporaryRemoteDirectoryExpression);
	}

	/**
	 * Specify a remote directory path SpEL expression.
	 * @param temporaryRemoteDirectoryExpression the temporary remote directory expression
	 * @since 4.3.13
	 * @see #setTemporaryRemoteDirectoryExpression(Expression)
	 */
	public void setTemporaryRemoteDirectoryExpressionString(String temporaryRemoteDirectoryExpression) {
		setTemporaryRemoteDirectoryExpression(EXPRESSION_PARSER.parseExpression(temporaryRemoteDirectoryExpression));
	}

	protected String getTemporaryFileSuffix() {
		return this.remoteFileTemplate.getTemporaryFileSuffix();
	}

	protected boolean isUseTemporaryFileName() {
		return this.remoteFileTemplate.isUseTemporaryFileName();
	}

	/**
	 * A {@code boolean} flag to use temporary files names or not.
	 * Defaults to {@code true}.
	 * @param useTemporaryFileName true to use a temporary file name.
	 * @see RemoteFileTemplate#setUseTemporaryFileName(boolean)
	 */
	public void setUseTemporaryFileName(boolean useTemporaryFileName) {
		this.remoteFileTemplate.setUseTemporaryFileName(useTemporaryFileName);
	}

	/**
	 * Set the file name generator used to generate the remote filename to be used when
	 * transferring files to the remote system. Default
	 * {@link org.springframework.integration.file.DefaultFileNameGenerator}.
	 * @param fileNameGenerator the file name generator.
	 * @see RemoteFileTemplate#setFileNameGenerator(FileNameGenerator)
	 */
	public void setFileNameGenerator(FileNameGenerator fileNameGenerator) {
		this.remoteFileTemplate.setFileNameGenerator(fileNameGenerator);
	}

	/**
	 * Set the charset to use when converting String payloads to bytes as the content of the
	 * remote file. Default {@code UTF-8}.
	 * @param charset the charset.
	 * @see RemoteFileTemplate#setCharset(String)
	 */
	public void setCharset(String charset) {
		this.remoteFileTemplate.setCharset(charset);
	}

	/**
	 * Set the temporary suffix to use when transferring files to the remote system.
	 * Default ".writing".
	 * @param temporaryFileSuffix the temporary file suffix.
	 * @see RemoteFileTemplate#setTemporaryFileSuffix(String)
	 */
	public void setTemporaryFileSuffix(String temporaryFileSuffix) {
		this.remoteFileTemplate.setTemporaryFileSuffix(temporaryFileSuffix);
	}

	/**
	 * String setter for Spring XML convenience.
	 * @param chmod permissions as an octal string e.g "600";
	 * @since 4.3
	 * @see #setChmod(int)
	 */
	public void setChmodOctal(String chmod) {
		Assert.notNull(chmod, "'chmod' cannot be null");
		setChmod(Integer.parseInt(chmod, 8)); // NOSONAR
	}

	/**
	 * Set the file permissions after uploading, e.g. 0600 for
	 * owner read/write.
	 * @param chmod the permissions.
	 * @since 4.3
	 */
	public void setChmod(int chmod) {
		Assert.isTrue(isChmodCapable(), "chmod operations not supported");
		this.chmod = chmod;
	}

	public boolean isChmodCapable() {
		return false;
	}

	@Override
	protected void onInit() {
		this.remoteFileTemplate.setBeanFactory(this.getBeanFactory());
		this.remoteFileTemplate.afterPropertiesSet();
	}

	@Override
	protected void handleMessageInternal(Message<?> message) {
		String path = this.remoteFileTemplate.send(message, this.mode);
		if (this.chmod != null && isChmodCapable()) {
			doChmod(this.remoteFileTemplate, path, this.chmod);
		}
	}

	/**
	 * Set the mode on the remote file after transfer; the default implementation does
	 * nothing.
	 * @param remoteFileTemplate the remote file template.
	 * @param path the path.
	 * @param chmod the chmod to set.
	 * @since 4.3
	 */
	protected void doChmod(RemoteFileTemplate<F> remoteFileTemplate, String path, int chmod) {
		// no-op
	}

}
