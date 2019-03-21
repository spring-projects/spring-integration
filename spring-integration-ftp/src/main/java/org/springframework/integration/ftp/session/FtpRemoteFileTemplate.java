/*
 * Copyright 2014-2019 the original author or authors.
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

package org.springframework.integration.ftp.session;

import java.io.IOException;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import org.springframework.integration.file.remote.ClientCallback;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * FTP version of {@code RemoteFileTemplate} providing type-safe access to
 * the underlying FTPClient object.
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @since 4.1
 *
 */
public class FtpRemoteFileTemplate extends RemoteFileTemplate<FTPFile> {

	private ExistsMode existsMode = ExistsMode.STAT;

	public FtpRemoteFileTemplate(SessionFactory<FTPFile> sessionFactory) {
		super(sessionFactory);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T, C> T executeWithClient(final ClientCallback<C, T> callback) {
		return doExecuteWithClient((ClientCallback<FTPClient, T>) callback);
	}

	/**
	 * Specify an {@link ExistsMode} for {@link #exists(String)} operation.
	 * Defaults to {@link ExistsMode#STAT}.
	 * When used internally by framework components for file operation,
	 * switched to {@link ExistsMode#NLST}.
	 * @param existsMode the {@link ExistsMode} to use.
	 * @since 4.1.9
	 */
	public void setExistsMode(ExistsMode existsMode) {
		Assert.notNull(existsMode, "'existsMode' must not be null.");
		this.existsMode = existsMode;
	}

	protected <T> T doExecuteWithClient(final ClientCallback<FTPClient, T> callback) {
		return execute(session -> callback.doWithClient((FTPClient) session.getClientInstance()));
	}

	/**
	 * This particular FTP implementation is based on the {@link FTPClient#getStatus(String)}
	 * by default, but since not all FTP servers properly implement the {@code STAT} command,
	 * the framework internal {@link FtpRemoteFileTemplate} instances are switched to the
	 * {@link FTPClient#listNames(String)} for only files operations.
	 * <p> The mode can be switched with the {@link #setExistsMode(ExistsMode)} property.
	 * <p> Any custom implementation can be done in an extension of the {@link FtpRemoteFileTemplate}.
	 * @param path the remote file path to check.
	 * @return true or false if remote file exists or not.
	 */
	@Override
	public boolean exists(final String path) {
		return doExecuteWithClient(client -> {
			try {
				switch (FtpRemoteFileTemplate.this.existsMode) {

					case STAT:
						return client.getStatus(path) != null;

					case NLST:
						String[] names = client.listNames(path);
						return !ObjectUtils.isEmpty(names);

					case NLST_AND_DIRS:
						return FtpRemoteFileTemplate.super.exists(path);

					default:
						throw new IllegalStateException("Unsupported 'existsMode': " +
								FtpRemoteFileTemplate.this.existsMode);
				}
			}
			catch (IOException e) {
				throw new MessagingException("Failed to check the remote path for " + path, e);
			}
		});
	}

	/**
	 * The {@link #exists(String)} operation mode.
	 * @since 4.1.9
	 */
	public enum ExistsMode {

		/**
		 * Perform the {@code STAT} FTP command.
		 * Default.
		 */
		STAT,

		/**
		 * Perform the {@code NLST} FTP command.
		 * Used as default internally by framework components for files only operations.
		 */
		NLST,

		/**
		 * Perform the {@code NLST} FTP command and fall back to
		 * {@link FTPClient#changeWorkingDirectory(String)}.
		 * <p> This technique is required when you want to check if a directory exists
		 * and the server does not support {@code STAT} - it requires 4 requests/replies.
		 * <p> If you are only checking for an existing file, {@code NLST} is preferred
		 * (unless {@code STAT} is supported).
		 * @see FtpSession#exists(String)
		 */
		NLST_AND_DIRS
	}

}
