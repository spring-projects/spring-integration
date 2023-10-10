/*
 * Copyright 2014-2022 the original author or authors.
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

package org.springframework.integration.sftp.session;

import java.util.List;

import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.common.SftpConstants;
import org.apache.sshd.sftp.common.SftpException;

import org.springframework.integration.file.remote.ClientCallback;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.lang.Nullable;

/**
 * SFTP version of {@code RemoteFileTemplate} providing type-safe access to
 * the underlying ChannelSftp object.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.1
 *
 */
public class SftpRemoteFileTemplate extends RemoteFileTemplate<SftpClient.DirEntry> {

	protected static final List<Integer> NOT_DIRTY_STATUSES =
			List.of(
					SftpConstants.SSH_FX_NO_SUCH_FILE,
					SftpConstants.SSH_FX_NO_SUCH_PATH,
					SftpConstants.SSH_FX_INVALID_FILENAME,
					SftpConstants.SSH_FX_INVALID_HANDLE,
					SftpConstants.SSH_FX_FILE_ALREADY_EXISTS,
					SftpConstants.SSH_FX_DIR_NOT_EMPTY,
					SftpConstants.SSH_FX_NOT_A_DIRECTORY,
					SftpConstants.SSH_FX_EOF,
					SftpConstants.SSH_FX_INVALID_FILENAME,
					SftpConstants.SSH_FX_INVALID_FILENAME
			);

	public SftpRemoteFileTemplate(SessionFactory<SftpClient.DirEntry> sessionFactory) {
		super(sessionFactory);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T, C> T executeWithClient(final ClientCallback<C, T> callback) {
		return doExecuteWithClient((ClientCallback<SftpClient, T>) callback);
	}

	protected <T> T doExecuteWithClient(final ClientCallback<SftpClient, T> callback) {
		return execute(session -> callback.doWithClient((SftpClient) session.getClientInstance()));
	}

	@Override
	protected boolean shouldMarkSessionAsDirty(Exception ex) {
		SftpException sftpException = findSftpException(ex);
		if (sftpException != null) {
			return isStatusDirty(sftpException.getStatus());
		}
		else {
			return super.shouldMarkSessionAsDirty(ex);
		}
	}

	/**
	 * Check if {@link SftpException#getStatus()} is treated as fatal.
	 * @param status the value from {@link SftpException#getStatus()}.
	 * @return true if {@link SftpException#getStatus()} is treated as fatal.
	 * @since 6.0.8
	 */
	protected boolean isStatusDirty(int status) {
		return !NOT_DIRTY_STATUSES.contains(status);
	}

	@Nullable
	private static SftpException findSftpException(Throwable ex) {
		if (ex == null || ex instanceof SftpException) {
			return (SftpException) ex;
		}
		else {
			return findSftpException(ex.getCause());
		}
	}

}
