/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.integration.ftp.session;

import java.io.IOException;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import org.springframework.integration.file.remote.ClientCallback;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.SessionCallback;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.messaging.MessagingException;

/**
 * FTP version of {@code RemoteFileTemplate} providing type-safe access to
 * the underlying FTPClient object.
 *
 * @author Gary Russell
 * @since 4.1
 *
 */
public class FtpRemoteFileTemplate extends RemoteFileTemplate<FTPFile> {

	public FtpRemoteFileTemplate(SessionFactory<FTPFile> sessionFactory) {
		super(sessionFactory);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T, C> T executeWithClient(final ClientCallback<C, T> callback) {
		return doExecuteWithClient((ClientCallback<FTPClient, T>) callback);
	}

	protected <T> T doExecuteWithClient(final ClientCallback<FTPClient, T> callback) {
		return execute(new SessionCallback<FTPFile, T>() {

			@Override
			public T doInSession(Session<FTPFile> session) throws IOException {
				return callback.doWithClient((FTPClient) session.getClientInstance());
			}
		});
	}

	@Override
	public boolean exists(final String path) {
		return executeWithClient(new ClientCallback<FTPClient, Boolean>() {

			@Override
			public Boolean doWithClient(FTPClient client) {
				try {
					return client.getStatus(path) != null;
				}
				catch (IOException e) {
					throw new MessagingException("Failed to stat " + path, e);
				}
			}
		});
	}


}
