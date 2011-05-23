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

import java.util.Properties;

import org.springframework.core.io.Resource;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.UserInfo;

/**
 * Factory for creating {@link SftpSession} instances.
 *
 * @author Josh Long
 * @author Mario Gray
 * @author Oleg Zhurakousky
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
	
	private volatile Properties sessionConfig;

	private final JSch jsch = new JSch();


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
	
	public void setSessionConfig(Properties sessionConfig) {
		this.sessionConfig = sessionConfig;
	}

	public Session getSession() {
		Assert.hasText(this.host, "host must not be empty");
		Assert.hasText(this.user, "user must not be empty");
		Assert.isTrue(this.port >= 0, "port must be a positive number");
		Assert.isTrue(StringUtils.hasText(this.password) || this.privateKey != null,
				"either a password or a private key is required");
		try {
			com.jcraft.jsch.Session jschSession = this.initJschSession();
			SftpSession sftpSession = new SftpSession(jschSession);
			sftpSession.connect();
			return sftpSession;
		}
		catch (Exception e) {
			throw new IllegalStateException("failed to create SFTP Session", e);
		}
	}

	private com.jcraft.jsch.Session initJschSession() throws Exception { 
		JSch.setLogger(new JschLogger());
		
		if (this.port <= 0) {
			this.port = 22;
		}
		if (StringUtils.hasText(this.knownHosts)) {
			this.jsch.setKnownHosts(this.knownHosts);
		}

		// private key
		if (this.privateKey != null) {
			String privateKeyFilePath = this.privateKey.getFile().getAbsolutePath();
			if (StringUtils.hasText(this.privateKeyPassphrase)) {
				this.jsch.addIdentity(privateKeyFilePath, this.privateKeyPassphrase);
			}
			else {
				this.jsch.addIdentity(privateKeyFilePath);
			}
		}
		com.jcraft.jsch.Session jschSession = this.jsch.getSession(this.user, this.host, this.port);
		if (this.sessionConfig != null){
			jschSession.setConfig(this.sessionConfig);
		}		
		if (StringUtils.hasText(this.password)) {
			jschSession.setPassword(this.password);
		}
		jschSession.setUserInfo(new OptimisticUserInfoImpl(this.password));
		return jschSession;
	}


	/**
	 * this is a simple, optimistic implementation of the UserInfo interface.
	 * It returns in the positive where possible and handles interactive authentication
	 * (i.e. 'Please enter your password: ' prompts are dispatched automatically).
	 */
	private static class OptimisticUserInfoImpl implements UserInfo {

		private final String password;

		public OptimisticUserInfoImpl(String password) {
			this.password = password;
		}

		public String getPassphrase() {
			return null; // pass
		}

		public String getPassword() {
			return this.password;
		}

		public boolean promptPassphrase(String string) {
			return true;
		}

		public boolean promptPassword(String string) {
			return true;
		}

		public boolean promptYesNo(String string) {
			return true;
		}

		public void showMessage(String string) {
		}
	}

}
