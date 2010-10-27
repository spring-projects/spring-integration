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

package org.springframework.integration.sftp;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import org.apache.commons.lang.StringUtils;

import java.io.InputStream;

/**
 * There are many ways to create a {@link SftpSession} just as there are many ways to SSH into a remote system.
 * You may use a username and password, you may use a username and private key, you may use a username and a private key with a password, etc.
 * <p/>
 * This object represents the connection to the remote server, and to use it you must provide it with all the components you'd normally provide an
 * incantation of the <code>ssh</code> command.
 *
 * @author Josh Long
 * @author Mario Gray
 */
public class SftpSession {

	private volatile ChannelSftp channel;

	private volatile Session session;

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
		if (!StringUtils.isEmpty(this.privateKey)) {
			if (!StringUtils.isEmpty(privateKeyPassphrase)) {
				jSch.addIdentity(this.privateKey, privateKeyPassphrase);
			}
			else {
				jSch.addIdentity(this.privateKey);
			}
		}
		this.session = jSch.getSession(userName, hostName, port);
		if (!StringUtils.isEmpty(userPassword)) {
			this.session.setPassword(userPassword);
		}
		this.userInfo = new OptimisticUserInfoImpl(userPassword);
		this.session.setUserInfo(userInfo);
		this.session.connect();
		this.channel = (ChannelSftp) this.session.openChannel("sftp");
	}


	public ChannelSftp getChannel() {
		return channel;
	}

	public Session getSession() {
		return session;
	}

	public void start() throws Exception {
		if (!channel.isConnected()) {
			channel.connect();
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
