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

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.file.remote.session.Session;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.UserInfo;

/**
 * Default SftpSession implementation.
 *
 * @author Josh Long
 * @author Mario Gray
 * @author Mark Fisher
 * @since 2.0
 */
public class SftpSession implements Session {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final ChannelSftp channel;

	private final com.jcraft.jsch.Session jschSession;

	private String privateKey;

	private String privateKeyPassphrase;

	private volatile UserInfo userInfo;


	/**
	 * @param userName              the name of the account being logged into.
	 * @param hostName              this should be the host. I found values like <code>foo.com</code> work, where
	 *                              <code>http://foo.com</code> don't.
	 * @param userPassword          if you are not using key based authentication, then you are likely being prompted
	 *                              for a password each time you login. This is that password. It is <em>not</em> the
	 *                              passphrase for the private key!
	 * @param port                  the default is 22, and if you specify N<0 for this value we'll default it to 22
	 * @param knownHostsFile        this is the known hosts file. If you don't specify it, jsch does some magic to work
	 *                              without your specification. If you have it in a non well-known location, however,
	 *                              this property is for you. An example: <code>/home/user/.ssh/known_hosts</code>
	 * @param knownHostsInputStream this is the known hosts file. If you don't specify it, jsch does some magic to work
	 *                              without your specification. If you have it in a non well-known location, however,
	 *                              this property is for you. An example: <code>/home/user/.ssh/known_hosts</code>. Note
	 *                              that you may specify this <em>or</em> the #knownHostsFile  - not both!
	 * @param privateKey            this is usually used when you want passwordless automation (obviously, for this
	 *                              integration it's useless since this lets you specify a password once, anyway, but
	 *                              still good to have if required). This file might be ~/.ssh/id_dsa, or a
	 *                              <code>.pem</code> for your remote server (for example, on EC2)
	 * @param pvKeyPassPhrase       sometimes, to be extra secure, the private key itself is extra encrypted. In order
	 *                              to surmount that, we need the private key passphrase. Specify that here.
	 * @throws Exception thrown if any of a myriad of scenarios plays out
	 */
	public SftpSession(String userName, String hostName, String userPassword, int port, String knownHostsFile,
			InputStream knownHostsInputStream, String privateKey, String pvKeyPassPhrase) throws Exception {

		JSch jSch = new JSch();
		if (port <= 0) {
			port = 22;
		}
		this.privateKey = privateKey;
		this.privateKeyPassphrase = pvKeyPassPhrase;
		if (!StringUtils.isEmpty(knownHostsFile)) {
			jSch.setKnownHosts(knownHostsFile);
		}
		else if (null != knownHostsInputStream) {
			jSch.setKnownHosts(knownHostsInputStream);
		}
		// private key
		if (privateKey != null) {
			if (!StringUtils.isEmpty(privateKeyPassphrase)) {
				jSch.addIdentity(this.privateKey, privateKeyPassphrase);
			}
			else {
				jSch.addIdentity(this.privateKey);
			}
		}
		this.jschSession = jSch.getSession(userName, hostName, port);
		if (!StringUtils.isEmpty(userPassword)) {
			this.jschSession.setPassword(userPassword);
		}
		this.userInfo = new OptimisticUserInfoImpl(userPassword);
		this.jschSession.setUserInfo(userInfo);
		this.channel = (ChannelSftp) this.jschSession.openChannel("sftp");
	}

	void connect() {
		try {
			this.jschSession.connect();
		}
		catch (JSchException e) {
			throw new IllegalStateException("failed to connect", e);
		}
	}

	public boolean mkdir(String path) {
		try {
			channel.mkdir(path);
			return true;
		}
		catch (SftpException e) {
			if (logger.isWarnEnabled()) {
				logger.warn("mkdir failed", e);
			}
			return false;
		}
	}

	public boolean rm(String path) {
		try {
			channel.rm(path);
			return true;
		}
		catch (SftpException e) {
			if (logger.isWarnEnabled()) {
				logger.warn("rm failed", e);
			}
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	public <F> Collection<F> ls(String path) {
		try {
			return channel.ls(path);
		}
		catch (SftpException e) {
			if (logger.isWarnEnabled()) {
				logger.warn("ls failed", e);
			}
			return Collections.EMPTY_LIST;
		}
	}

	public InputStream get(String source) {
		try {
			return channel.get(source);
		}
		catch (SftpException e) {
			if (logger.isWarnEnabled()) {
				logger.warn("get failed", e);
			}
			return null;
		}
	}

	public void put(InputStream inputStream, String destination) {
		try {
			channel.put(inputStream, destination);
		}
		catch (SftpException e) {
			if (logger.isWarnEnabled()) {
				logger.warn("put failed", e);
			}
		}
	}

	public void close() {
		if (jschSession.isConnected()) {
			jschSession.disconnect();
		}
	}


	/**
	 * this is a simple, optimistic implementation of this interface. It simply returns in the positive where possible
	 * and handles interactive authentication (ie, 'Please enter your password: ' prompts are dispatched automatically using this)
	 */
	private static class OptimisticUserInfoImpl implements UserInfo {

		private String password;

		public OptimisticUserInfoImpl(String password) {
			this.password = password;
		}

		public String getPassphrase() {
			return null; // pass
		}

		public String getPassword() {
			return password;
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
