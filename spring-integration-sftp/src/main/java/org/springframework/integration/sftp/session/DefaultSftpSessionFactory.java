/*
 * Copyright 2002-2014 the original author or authors.
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
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.core.io.Resource;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.remote.session.SharedSessionCapable;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Proxy;
import com.jcraft.jsch.SocketFactory;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

/**
 * Factory for creating {@link SftpSession} instances.
 *
 * @author Josh Long
 * @author Mario Gray
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author David Liu
 *
 * @since 2.0
 */
public class DefaultSftpSessionFactory implements SessionFactory<LsEntry>, SharedSessionCapable {

	private volatile String host;

	private volatile int port = 22; // the default

	private volatile String user;

	private volatile String password;

	private volatile String knownHosts;

	private volatile Resource privateKey;

	private volatile String privateKeyPassphrase;

	private volatile Properties sessionConfig;

	private volatile Proxy proxy;

	private volatile SocketFactory socketFactory;

	private volatile Integer timeout;

	private volatile String clientVersion;

	private volatile String hostKeyAlias;

	private volatile Integer serverAliveInterval;

	private volatile Integer serverAliveCountMax;

	private volatile Boolean enableDaemonThread;

	private final JSch jsch;

	private final boolean isSharedSession;

	private volatile JSchSessionWrapper sharedJschSession;

	private final ReentrantReadWriteLock sharedSessionLock = new ReentrantReadWriteLock();


	public DefaultSftpSessionFactory() {
		this(false);
	}

	/**
	 * @param isSharedSession true if the session is to be shared.
	 */
	public DefaultSftpSessionFactory(boolean isSharedSession) {
		this(new JSch(), isSharedSession);
	}

	/**
	 * Intended for use in tests so the jsch can be mocked.
	 * @param jsch The jsch instance.
	 * @param isSharedSession true if the session is to be shared.
	 */
	public DefaultSftpSessionFactory(JSch jsch, boolean isSharedSession) {
		this.jsch = jsch;
		this.isSharedSession = isSharedSession;
	}

	/**
	 * The url of the host you want connect to. This is a mandatory property.
	 *
	 * @param host The host.
	 *
	 * @see JSch#getSession(String, String, int)
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * The port over which the SFTP connection shall be established. If not specified,
	 * this value defaults to <code>22</code>. If specified, this properties must
	 * be a positive number.
	 *
	 * @param port The port.
	 *
	 * @see JSch#getSession(String, String, int)
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * The remote user to use. This is a mandatory property.
	 *
	 * @param user The user.
	 *
	 * @see JSch#getSession(String, String, int)
	 */
	public void setUser(String user) {
		this.user = user;
	}

	/**
	 * The password to authenticate against the remote host. If a password is
	 * not provided, then the {@link DefaultSftpSessionFactory#privateKey} is
	 * mandatory.
	 *
	 * @param password The password.
	 *
	 * @see com.jcraft.jsch.Session#setPassword(String)
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Specifies the filename that will be used to create a host key repository.
	 * The resulting file has the same format as OpenSSH's known_hosts file.
	 *
	 * @param knownHosts The known hosts.
	 *
	 * @see JSch#setKnownHosts(String)
	 */
	public void setKnownHosts(String knownHosts) {
		this.knownHosts = knownHosts;
	}

	/**
	 * Allows you to set a {@link Resource}, which represents the location of the
	 * private key used for authenticating against the remote host. If the privateKey
	 * is not provided, then the {@link DefaultSftpSessionFactory#setPassword(String)}
	 * property is mandatory.
	 *
	 * @param privateKey The private key.
	 *
	 * @see JSch#addIdentity(String)
	 * @see JSch#addIdentity(String, String)
	 *
	 */
	public void setPrivateKey(Resource privateKey) {
		this.privateKey = privateKey;
	}

	/**
	 * The password for the private key. Optional.
	 *
	 * @param privateKeyPassphrase The private key passphrase.
	 *
	 * @see JSch#addIdentity(String, String)
	 */
	public void setPrivateKeyPassphrase(String privateKeyPassphrase) {
		this.privateKeyPassphrase = privateKeyPassphrase;
	}

	/**
	 * Using {@link Properties}, you can set additional configuration settings on
	 * the underlying JSch {@link com.jcraft.jsch.Session}.
	 *
	 * @param sessionConfig The session configuration properties.
	 *
	 * @see com.jcraft.jsch.Session#setConfig(Properties)
	 */
	public void setSessionConfig(Properties sessionConfig) {
		this.sessionConfig = sessionConfig;
	}

	/**
	 * Allows for specifying a JSch-based {@link Proxy}. If set, then the proxy
	 * object is used to create the connection to the remote host.
	 *
	 * @param proxy The proxy.
	 *
	 * @see com.jcraft.jsch.Session#setProxy(Proxy)
	 */
	public void setProxy(Proxy proxy){
		this.proxy = proxy;
	}

	/**
	 * Allows you to pass in a {@link SocketFactory}. The socket factory is used
	 * to create a socket to the target host. When a {@link Proxy} is used, the
	 * socket factory is passed to the proxy. By default plain TCP sockets are used.
	 *
	 * @param socketFactory The socket factory.
	 *
	 * @see com.jcraft.jsch.Session#setSocketFactory(SocketFactory)
	 */
	public void setSocketFactory(SocketFactory socketFactory){
		this.socketFactory = socketFactory;
	}

	/**
	 * The timeout property is used as the socket timeout parameter, as well as
	 * the default connection timeout. Defaults to <code>0</code>, which means,
	 * that no timeout will occur.
	 *
	 * @param timeout The timeout.
	 *
	 * @see com.jcraft.jsch.Session#setTimeout(int)
	 */
	public void setTimeout(Integer timeout) {
		this.timeout = timeout;
	}

	/**
	 * Allows you to set the client version property. It's default depends on the
	 * underlying JSch version but it will look like <code>SSH-2.0-JSCH-0.1.45</code>
	 *
	 * @param clientVersion The client version.
	 *
	 * @see com.jcraft.jsch.Session#setClientVersion(String)
	 */
	public void setClientVersion(String clientVersion){
		this.clientVersion = clientVersion;
	}

	/**
	 * Sets the host key alias, used when comparing the host key to the known
	 * hosts list.
	 *
	 * @param hostKeyAlias The host key alias.
	 *
	 * @see com.jcraft.jsch.Session#setHostKeyAlias(String)
	 */
	public void setHostKeyAlias(String hostKeyAlias){
		this.hostKeyAlias = hostKeyAlias;
	}

	/**
	 * Sets the timeout interval (milliseconds) before a server alive message is
	 * sent, in case no message is received from the server.
	 *
	 * @param serverAliveInterval The server alive interval.
	 *
	 * @see com.jcraft.jsch.Session#setServerAliveInterval(int)
	 */
	public void setServerAliveInterval(Integer serverAliveInterval){
		this.serverAliveInterval = serverAliveInterval;
	}

	/**
	 * Specifies the number of server-alive messages, which will be sent without
	 * any reply from the server before disconnecting. If not set, this property
	 * defaults to <code>1</code>.
	 *
	 * @param serverAliveCountMax The server alive count max.
	 *
	 * @see com.jcraft.jsch.Session#setServerAliveCountMax(int)
	 */
	public void setServerAliveCountMax(Integer serverAliveCountMax){
		this.serverAliveCountMax = serverAliveCountMax;
	}

	/**
	 * If true, all threads will be daemon threads. If set to <code>false</code>,
	 * normal non-daemon threads will be used. This property will be set on the
	 * underlying {@link com.jcraft.jsch.Session} using
	 * {@link com.jcraft.jsch.Session#setDaemonThread(boolean)}. There, this
	 * property will default to <code>false</code>, if not explicitly set.
	 *
	 * @param enableDaemonThread true to enable a daemon thread.
	 *
	 * @see com.jcraft.jsch.Session#setDaemonThread(boolean)
	 */
	public void setEnableDaemonThread(Boolean enableDaemonThread){
		this.enableDaemonThread = enableDaemonThread;
	}


	@Override
	public SftpSession getSession() {
		Assert.hasText(this.host, "host must not be empty");
		Assert.hasText(this.user, "user must not be empty");
		Assert.isTrue(this.port >= 0, "port must be a positive number");
		Assert.isTrue(StringUtils.hasText(this.password) || this.privateKey != null,
				"either a password or a private key is required");
		try {
			JSchSessionWrapper jschSession;
			if (this.isSharedSession) {
				this.sharedSessionLock.readLock().lock();
				try {
					if (this.sharedJschSession == null || !this.sharedJschSession.isConnected()) {
						this.sharedSessionLock.readLock().unlock();
						this.sharedSessionLock.writeLock().lock();
						try {
							if (this.sharedJschSession == null || !this.sharedJschSession.isConnected()) {
								this.sharedJschSession = new JSchSessionWrapper(initJschSession());
							}
						}
						finally {
							this.sharedSessionLock.readLock().lock();
							this.sharedSessionLock.writeLock().unlock();
						}
					}
				}
				finally {
					this.sharedSessionLock.readLock().unlock();
				}
				jschSession = this.sharedJschSession;
			}
			else {
				jschSession = new JSchSessionWrapper(initJschSession());
			}
			SftpSession sftpSession = new SftpSession(jschSession);
			sftpSession.connect();
			jschSession.addChannel();
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
			byte[] keyByteArray = StreamUtils.copyToByteArray(this.privateKey.getInputStream());
			if (StringUtils.hasText(this.privateKeyPassphrase)) {
				this.jsch.addIdentity(this.user, keyByteArray, null, this.privateKeyPassphrase.getBytes());
			}
			else {
				this.jsch.addIdentity(this.user, keyByteArray, null, null);
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

		try {
			if (proxy != null){
				jschSession.setProxy(proxy);
			}
			if (socketFactory != null){
				jschSession.setSocketFactory(socketFactory);
			}
			if (timeout != null){
				jschSession.setTimeout(timeout);
			}
			if (StringUtils.hasText(clientVersion)){
				jschSession.setClientVersion(clientVersion);
			}
			if (StringUtils.hasText(hostKeyAlias)){
				jschSession.setHostKeyAlias(hostKeyAlias);
			}
			if (serverAliveInterval != null){
				jschSession.setServerAliveInterval(serverAliveInterval);
			}
			if (serverAliveCountMax != null){
				jschSession.setServerAliveCountMax(serverAliveCountMax);
			}
			if (enableDaemonThread != null){
				jschSession.setDaemonThread(enableDaemonThread);
			}
		} catch (Exception e) {
			throw new BeanCreationException("Attempt to set additional properties of the com.jcraft.jsch.Session resulted in error: " + e.getMessage(), e);
		}
		return jschSession;
	}

	@Override
	public final boolean isSharedSession() {
		return this.isSharedSession;
	}

	@Override
	public void resetSharedSession() {
		Assert.state(this.isSharedSession, "Shared sessions are not being used");
		this.sharedJschSession = null;
	}

	/**
	 * this is a simple, optimistic implementation of the UserInfo interface.
	 * It returns in the positive where possible and handles interactive authentication
	 * (i.e. 'Please enter your password: ' prompts are dispatched automatically).
	 */
	private static class OptimisticUserInfoImpl implements UserInfo, UIKeyboardInteractive {

		private final String password;

		public OptimisticUserInfoImpl(String password) {
			this.password = password;
		}

		@Override
		public String getPassphrase() {
			return null; // pass
		}

		@Override
		public String getPassword() {
			return this.password;
		}

		@Override
		public boolean promptPassphrase(String string) {
			return true;
		}

		@Override
		public boolean promptPassword(String string) {
			return true;
		}

		@Override
		public boolean promptYesNo(String string) {
			return true;
		}

		@Override
		public void showMessage(String string) {
		}

		@Override
		public String[] promptKeyboardInteractive(String destination,
				String name, String instruction, String[] prompt, boolean[] echo) {
			return null;
		}
	}

}
