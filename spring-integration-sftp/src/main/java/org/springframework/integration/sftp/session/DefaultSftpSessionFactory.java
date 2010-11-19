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

package org.springframework.integration.sftp.session;

import org.springframework.core.io.Resource;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Factory for creating {@link SftpSession} instances.
 *
 * @author Josh Long
 * @author Mario Gray
 * @since 2.0
 */
public class DefaultSftpSessionFactory implements SessionFactory {

	private volatile String host;

	private volatile int port = 22; // the default

	private volatile String user;

	private volatile String password;

	private volatile String knownHosts;

	private volatile Resource privateKey;

	private volatile String privateKeyPassphrase;


	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setKnownHosts(String knownHosts) {
		this.knownHosts = knownHosts;
	}

	public void setPrivateKey(Resource privateKey) {
		this.privateKey = privateKey;
	}

	public void setPrivateKeyPassphrase(String privateKeyPassphrase) {
		this.privateKeyPassphrase = privateKeyPassphrase;
	}

	public Session getSession() {
		Assert.hasText(this.host, "host must not be empty");
		Assert.hasText(this.user, "user must not be empty");
		Assert.isTrue(this.port >= 0, "port must be a positive number");
		Assert.isTrue(StringUtils.hasText(this.password) || privateKey != null || StringUtils.hasText(this.privateKeyPassphrase),
				"either a password or a private key and/or a private key passphrase is required");
		String privateKeyToPass = null;
		try {
			if (privateKey != null){
				privateKeyToPass = privateKey.getFile().getAbsolutePath();
			}
			SftpSession session = new SftpSession(
					this.user, this.host, this.password, this.port, this.knownHosts, null, privateKeyToPass, this.privateKeyPassphrase);
			session.connect();
			return session;
		}
		catch (Exception e) {
			throw new IllegalStateException("failed to create SFTP Session", e);
		}
	}

}
