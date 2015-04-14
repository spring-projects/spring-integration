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
 * @author Iwein Fuld
 * @author Mark Fisher
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @author David Turanski
 * @author Gary Russell
 * @since 2.0
 */
public class FileTransferringMessageHandler<F> extends AbstractMessageHandler {

	private final RemoteFileTemplate<F> remoteFileTemplate;

	private final FileExistsMode mode;

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
	 * @param autoCreateDirectory true to automatically create the direcotory.
	 * @see RemoteFileTemplate#setAutoCreateDirectory(boolean)
	 */
	public void setAutoCreateDirectory(boolean autoCreateDirectory) {
		this.remoteFileTemplate.setAutoCreateDirectory(autoCreateDirectory);
	}

	/**
	 * @param remoteFileSeparator the remote file separator.
	 * @see RemoteFileTemplate#setRemoteFileSeparator(String)
	 */
	public void setRemoteFileSeparator(String remoteFileSeparator) {
		this.remoteFileTemplate.setRemoteFileSeparator(remoteFileSeparator);
	}

	/**
	 * @param remoteDirectoryExpression the remote directory expression
	 * @see RemoteFileTemplate#setRemoteDirectoryExpression(Expression)
	 */
	public void setRemoteDirectoryExpression(Expression remoteDirectoryExpression) {
		this.remoteFileTemplate.setRemoteDirectoryExpression(remoteDirectoryExpression);
	}

	/**
	 * @param temporaryRemoteDirectoryExpression the temporary remote directory expression
	 * @see RemoteFileTemplate#setTemporaryRemoteDirectoryExpression(Expression)
	 */
	public void setTemporaryRemoteDirectoryExpression(Expression temporaryRemoteDirectoryExpression) {
		this.remoteFileTemplate.setTemporaryRemoteDirectoryExpression(temporaryRemoteDirectoryExpression);
	}

	protected String getTemporaryFileSuffix() {
		return this.remoteFileTemplate.getTemporaryFileSuffix();
	}

	protected boolean isUseTemporaryFileName() {
		return this.remoteFileTemplate.isUseTemporaryFileName();
	}

	/**
	 * @param useTemporaryFileName true to use a temporary file name.
	 * @see RemoteFileTemplate#setUseTemporaryFileName(boolean)
	 */
	public void setUseTemporaryFileName(boolean useTemporaryFileName) {
		this.remoteFileTemplate.setUseTemporaryFileName(useTemporaryFileName);
	}

	/**
	 * @param fileNameGenerator the file name generator.
	 * @see RemoteFileTemplate#setFileNameGenerator(FileNameGenerator)
	 */
	public void setFileNameGenerator(FileNameGenerator fileNameGenerator) {
		this.remoteFileTemplate.setFileNameGenerator(fileNameGenerator);
	}

	/**
	 * @param charset the charset.
	 * @see RemoteFileTemplate#setCharset(String)
	 */
	public void setCharset(String charset) {
		this.remoteFileTemplate.setCharset(charset);
	}

	/**
	 * @param temporaryFileSuffix the temporary file suffix.
	 * @see RemoteFileTemplate#setTemporaryFileSuffix(String)
	 */
	public void setTemporaryFileSuffix(String temporaryFileSuffix) {
		this.remoteFileTemplate.setTemporaryFileSuffix(temporaryFileSuffix);
	}

	@Override
	protected void onInit() throws Exception {
		this.remoteFileTemplate.setBeanFactory(this.getBeanFactory());
		this.remoteFileTemplate.afterPropertiesSet();
	}

	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		this.remoteFileTemplate.send(message, this.mode);
	}

}
