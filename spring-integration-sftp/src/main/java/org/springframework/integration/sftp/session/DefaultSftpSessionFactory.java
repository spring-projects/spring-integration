/*
 * Copyright 2002-2020 the original author or authors.
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

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.remote.session.SharedSessionCapable;
import org.springframework.integration.util.JavaUtils;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
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
 * @author Pat Turner
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class DefaultSftpSessionFactory implements SessionFactory<LsEntry>, SharedSessionCapable {

	private static final Log LOGGER = LogFactory.getLog(DefaultSftpSessionFactory.class);

	static {
		JSch.setLogger(new JschLogger());
	}

	private final UserInfo userInfoWrapper = new UserInfoWrapper();

	private final JSch jsch;

	private final boolean isSharedSession;

	private final Lock sharedSessionLock;

	private String host;

	private int port = 22; // the default

	private String user;

	private String password;

	private Resource knownHosts;

	private Resource privateKey;

	private String privateKeyPassphrase;

	private Properties sessionConfig;

	private Proxy proxy;

	private SocketFactory socketFactory;

	private Integer timeout;

	private String clientVersion;

	private String hostKeyAlias;

	private Integer serverAliveInterval;

	private Integer serverAliveCountMax;

	private Boolean enableDaemonThread;

	private UserInfo userInfo;

	private boolean allowUnknownKeys = false;

	private Duration channelConnectTimeout;

	private volatile JSchSessionWrapper sharedJschSession;


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
		if (this.isSharedSession) {
			this.sharedSessionLock = new ReentrantLock();
		}
		else {
			this.sharedSessionLock = null;
		}
	}

	/**
	 * The url of the host you want connect to. This is a mandatory property.
	 * @param host The host.
	 * @see JSch#getSession(String, String, int)
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * The port over which the SFTP connection shall be established. If not specified,
	 * this value defaults to <code>22</code>. If specified, this properties must
	 * be a positive number.
	 * @param port The port.
	 * @see JSch#getSession(String, String, int)
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * The remote user to use. This is a mandatory property.
	 * @param user The user.
	 * @see JSch#getSession(String, String, int)
	 */
	public void setUser(String user) {
		this.user = user;
	}

	/**
	 * The password to authenticate against the remote host. If a password is
	 * not provided, then a {@link DefaultSftpSessionFactory#setPrivateKey(Resource) privateKey} is
	 * mandatory.
	 * Not allowed if {@link #setUserInfo(UserInfo) userInfo} is provided - the password is obtained
	 * from that object.
	 * @param password The password.
	 * @see com.jcraft.jsch.Session#setPassword(String)
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Specifies the filename that will be used for a host key repository.
	 * The file has the same format as OpenSSH's known_hosts file.
	 * <p>
	 * <b>Required if {@link #setAllowUnknownKeys(boolean) allowUnknownKeys} is
	 * false (default).</b>
	 * @param knownHosts The known hosts.
	 * @see JSch#setKnownHosts(String)
	 * @deprecated since 5.2.5 in favor of {@link #setKnownHostsResource(Resource)}
	 */
	@Deprecated
	public void setKnownHosts(String knownHosts) {
		setKnownHostsResource(new FileSystemResource(knownHosts));
	}

	/**
	 * Specifies the filename that will be used for a host key repository.
	 * The file has the same format as OpenSSH's known_hosts file.
	 * @param knownHosts the resource for known hosts.
	 * @see JSch#setKnownHosts(java.io.InputStream)
	 * @since 5.2.5
	 */
	public void setKnownHostsResource(Resource knownHosts) {
		this.knownHosts = knownHosts;
	}

	/**
	 * Allows you to set a {@link Resource}, which represents the location of the
	 * private key used for authenticating against the remote host. If the privateKey
	 * is not provided, then the {@link DefaultSftpSessionFactory#setPassword(String) password}
	 * property is mandatory (or {@link #setUserInfo(UserInfo) userInfo} that returns a
	 * password.
	 * @param privateKey The private key.
	 * @see JSch#addIdentity(String)
	 * @see JSch#addIdentity(String, String)
	 */
	public void setPrivateKey(Resource privateKey) {
		this.privateKey = privateKey;
	}

	/**
	 * The password for the private key. Optional.
	 * Not allowed if {@link #setUserInfo(UserInfo) userInfo} is provided - the passphrase is obtained
	 * from that object.
	 * @param privateKeyPassphrase The private key passphrase.
	 * @see JSch#addIdentity(String, String)
	 */
	public void setPrivateKeyPassphrase(String privateKeyPassphrase) {
		this.privateKeyPassphrase = privateKeyPassphrase;
	}

	/**
	 * Using {@link Properties}, you can set additional configuration settings on
	 * the underlying JSch {@link com.jcraft.jsch.Session}.
	 * @param sessionConfig The session configuration properties.
	 * @see com.jcraft.jsch.Session#setConfig(Properties)
	 */
	public void setSessionConfig(Properties sessionConfig) {
		this.sessionConfig = sessionConfig;
	}

	/**
	 * Allows for specifying a JSch-based {@link Proxy}. If set, then the proxy
	 * object is used to create the connection to the remote host.
	 * @param proxy The proxy.
	 * @see com.jcraft.jsch.Session#setProxy(Proxy)
	 */
	public void setProxy(Proxy proxy) {
		this.proxy = proxy;
	}

	/**
	 * Allows you to pass in a {@link SocketFactory}. The socket factory is used
	 * to create a socket to the target host. When a {@link Proxy} is used, the
	 * socket factory is passed to the proxy. By default plain TCP sockets are used.
	 * @param socketFactory The socket factory.
	 * @see com.jcraft.jsch.Session#setSocketFactory(SocketFactory)
	 */
	public void setSocketFactory(SocketFactory socketFactory) {
		this.socketFactory = socketFactory;
	}

	/**
	 * The timeout property is used as the socket timeout parameter, as well as
	 * the default connection timeout. Defaults to <code>0</code>, which means,
	 * that no timeout will occur.
	 * @param timeout The timeout.
	 * @see com.jcraft.jsch.Session#setTimeout(int)
	 */
	public void setTimeout(Integer timeout) {
		this.timeout = timeout;
	}

	/**
	 * Allows you to set the client version property. It's default depends on the
	 * underlying JSch version but it will look like <code>SSH-2.0-JSCH-0.1.45</code>
	 * @param clientVersion The client version.
	 * @see com.jcraft.jsch.Session#setClientVersion(String)
	 */
	public void setClientVersion(String clientVersion) {
		this.clientVersion = clientVersion;
	}

	/**
	 * Sets the host key alias, used when comparing the host key to the known
	 * hosts list.
	 * @param hostKeyAlias The host key alias.
	 * @see com.jcraft.jsch.Session#setHostKeyAlias(String)
	 */
	public void setHostKeyAlias(String hostKeyAlias) {
		this.hostKeyAlias = hostKeyAlias;
	}

	/**
	 * Sets the timeout interval (milliseconds) before a server alive message is
	 * sent, in case no message is received from the server.
	 * @param serverAliveInterval The server alive interval.
	 * @see com.jcraft.jsch.Session#setServerAliveInterval(int)
	 */
	public void setServerAliveInterval(Integer serverAliveInterval) {
		this.serverAliveInterval = serverAliveInterval;
	}

	/**
	 * Specifies the number of server-alive messages, which will be sent without
	 * any reply from the server before disconnecting. If not set, this property
	 * defaults to <code>1</code>.
	 * @param serverAliveCountMax The server alive count max.
	 * @see com.jcraft.jsch.Session#setServerAliveCountMax(int)
	 */
	public void setServerAliveCountMax(Integer serverAliveCountMax) {
		this.serverAliveCountMax = serverAliveCountMax;
	}

	/**
	 * If true, all threads will be daemon threads. If set to <code>false</code>,
	 * normal non-daemon threads will be used. This property will be set on the
	 * underlying {@link com.jcraft.jsch.Session} using
	 * {@link com.jcraft.jsch.Session#setDaemonThread(boolean)}. There, this
	 * property will default to <code>false</code>, if not explicitly set.
	 * @param enableDaemonThread true to enable a daemon thread.
	 * @see com.jcraft.jsch.Session#setDaemonThread(boolean)
	 */
	public void setEnableDaemonThread(Boolean enableDaemonThread) {
		this.enableDaemonThread = enableDaemonThread;
	}

	/**
	 * Provide a {@link UserInfo} which exposes control over dealing with new keys or key
	 * changes. As Spring Integration will not normally allow user interaction, the
	 * implementation must respond to Jsch calls in a suitable way.
	 * <p>
	 * Jsch calls {@link UserInfo#promptYesNo(String)} when connecting to an unknown host,
	 * or when a known host's key has changed (see {@link #setKnownHostsResource(Resource)}
	 * knownHosts}). Generally, it should return false as returning true will accept all
	 * new keys or key changes.
	 * <p>
	 * If no {@link UserInfo} is provided, the behavior is defined by
	 * {@link #setAllowUnknownKeys(boolean) allowUnknownKeys}.
	 * <p>
	 * If {@link #setPassword(String) setPassword} is invoked with a non-null password, it will
	 * override any password in the supplied {@link UserInfo}.
	 * <p>
	 * <b>NOTE: When this is provided, the {@link #setPassword(String) password} and
	 * {@link #setPrivateKeyPassphrase(String) passphrase} are not allowed because those values
	 * will be obtained from the {@link UserInfo}.</b>
	 * @param userInfo the UserInfo.
	 * @see com.jcraft.jsch.Session#setUserInfo(com.jcraft.jsch.UserInfo)
	 * @since 4.1.7
	 */
	public void setUserInfo(UserInfo userInfo) {
		this.userInfo = userInfo;
	}

	/**
	 * When no {@link UserInfo} has been provided, set to true to unconditionally allow
	 * connecting to an unknown host or when a host's key has changed (see
	 * {@link #setKnownHostsResource(Resource) knownHosts}). Default false (since 4.2).
	 * Set to true if a knownHosts file is not provided.
	 * @param allowUnknownKeys true to allow connecting to unknown hosts.
	 * @since 4.1.7
	 */
	public void setAllowUnknownKeys(boolean allowUnknownKeys) {
		this.allowUnknownKeys = allowUnknownKeys;
	}

	/**
	 * Set the connect timeout.
	 * @param timeout the timeout to set.
	 * @since 5.2
	 */
	public void setChannelConnectTimeout(Duration timeout) {
		Assert.notNull(timeout, "'connectTimeout' cannot be null");
		this.channelConnectTimeout = timeout;
	}

	@Override
	public SftpSession getSession() {
		JSchSessionWrapper jschSession = this.sharedJschSession;
		SftpSession sftpSession;
		if (this.sharedSessionLock != null) {
			this.sharedSessionLock.lock();
		}
		try {
			boolean freshJschSession = false;
			if (jschSession == null || !jschSession.isConnected()) {
				jschSession = new JSchSessionWrapper(initJschSession());
				freshJschSession = true;
			}
			sftpSession = new SftpSession(jschSession);
			JavaUtils.INSTANCE.acceptIfNotNull(this.channelConnectTimeout, sftpSession::setChannelConnectTimeout);
			sftpSession.connect();
			if (this.isSharedSession && freshJschSession) {
				this.sharedJschSession = jschSession;
			}
		}
		catch (Exception e) {
			throw new IllegalStateException("failed to create SFTP Session", e);
		}
		finally {
			if (this.sharedSessionLock != null) {
				this.sharedSessionLock.unlock();
			}
		}
		jschSession.addChannel();
		return sftpSession;
	}

	private com.jcraft.jsch.Session initJschSession() throws JSchException, IOException {
		Assert.hasText(this.host, "host must not be empty");
		Assert.hasText(this.user, "user must not be empty");
		Assert.isTrue(StringUtils.hasText(this.userInfoWrapper.getPassword()) || this.privateKey != null,
				"either a password or a private key is required");

		if (this.port <= 0) {
			this.port = 22;
		}
		if (this.knownHosts != null) {
			this.jsch.setKnownHosts(this.knownHosts.getInputStream());
		}

		// private key
		if (this.privateKey != null) {
			byte[] keyByteArray = FileCopyUtils.copyToByteArray(this.privateKey.getInputStream());
			String passphrase = this.userInfoWrapper.getPassphrase();
			if (StringUtils.hasText(passphrase)) {
				this.jsch.addIdentity(this.user, keyByteArray, null, passphrase.getBytes());
			}
			else {
				this.jsch.addIdentity(this.user, keyByteArray, null, null);
			}
		}
		com.jcraft.jsch.Session jschSession = this.jsch.getSession(this.user, this.host, this.port);
		JavaUtils.INSTANCE
				.acceptIfNotNull(this.sessionConfig, jschSession::setConfig)
				.acceptIfHasText(this.userInfoWrapper.getPassword(), jschSession::setPassword);
		jschSession.setUserInfo(this.userInfoWrapper);

		try {
			if (this.timeout != null) {
				jschSession.setTimeout(this.timeout);
			}
			if (this.serverAliveInterval != null) {
				jschSession.setServerAliveInterval(this.serverAliveInterval);
			}
			JavaUtils.INSTANCE
					.acceptIfNotNull(this.proxy, jschSession::setProxy)
					.acceptIfNotNull(this.socketFactory, jschSession::setSocketFactory)
					.acceptIfHasText(this.clientVersion, jschSession::setClientVersion)
					.acceptIfHasText(this.hostKeyAlias, jschSession::setHostKeyAlias)
					.acceptIfNotNull(this.serverAliveCountMax, jschSession::setServerAliveCountMax)
					.acceptIfNotNull(this.enableDaemonThread, jschSession::setDaemonThread);
		}
		catch (Exception e) {
			throw new BeanCreationException("Attempt to set additional properties of " +
					"the com.jcraft.jsch.Session resulted in error: " + e.getMessage(), e);
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
	 * Wrapper class will delegate calls to a configured {@link UserInfo}, providing
	 * sensible defaults if null. As the password is configured in this Factory, the
	 * wrapper will return the factory's configured password and only delegate to the
	 * UserInfo if null.
	 *
	 * @since 4.1.7
	 */
	private class UserInfoWrapper implements UserInfo, UIKeyboardInteractive {

		UserInfoWrapper() {
		}

		/**
		 * Convenience to check whether enclosing factory's UserInfo is configured.
		 * @return true if there's a delegate.
		 */
		private boolean hasDelegate() {
			return getDelegate() != null;
		}

		/**
		 * Convenience to retrieve enclosing factory's UserInfo.
		 * @return the {@link #userInfo} or null if not present.
		 */
		private UserInfo getDelegate() {
			return DefaultSftpSessionFactory.this.userInfo;
		}

		@Override
		public String getPassphrase() {
			if (hasDelegate()) {
				Assert.state(!StringUtils.hasText(DefaultSftpSessionFactory.this.privateKeyPassphrase),
						"When a 'UserInfo' is provided, 'privateKeyPassphrase' is not allowed");
				return getDelegate().getPassphrase();
			}
			else {
				return DefaultSftpSessionFactory.this.privateKeyPassphrase;
			}
		}

		@Override
		public String getPassword() {
			if (hasDelegate()) {
				Assert.state(!StringUtils.hasText(DefaultSftpSessionFactory.this.password),
						"When a 'UserInfo' is provided, 'password' is not allowed");
				return getDelegate().getPassword();
			}
			else {
				return DefaultSftpSessionFactory.this.password;
			}
		}

		@Override
		public boolean promptPassword(String message) {
			if (hasDelegate()) {
				return getDelegate().promptPassword(message);
			}
			else {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("No UserInfo provided - " + message + ", returning: true");
				}
				return true;
			}
		}

		@Override
		public boolean promptPassphrase(String message) {
			if (hasDelegate()) {
				return getDelegate().promptPassphrase(message);
			}
			else {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("No UserInfo provided - " + message + ", returning: true");
				}
				return true;
			}
		}

		@Override
		public boolean promptYesNo(String message) {
			LOGGER.info(message);
			if (hasDelegate()) {
				return getDelegate().promptYesNo(message);
			}
			else {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("No UserInfo provided - " + message + ", returning: "
							+ DefaultSftpSessionFactory.this.allowUnknownKeys);
				}
				return DefaultSftpSessionFactory.this.allowUnknownKeys;
			}
		}

		@Override
		public void showMessage(String message) {
			if (hasDelegate()) {
				getDelegate().showMessage(message);
			}
			else {
				LOGGER.debug(message);
			}
		}

		@Override
		public String[] promptKeyboardInteractive(String destination, String name, String instruction, String[] prompt,
				boolean[] echo) {

			if (hasDelegate() && getDelegate() instanceof UIKeyboardInteractive) {
				return ((UIKeyboardInteractive) getDelegate()).promptKeyboardInteractive(destination, name,
						instruction, prompt, echo);
			}
			else {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("No UIKeyboardInteractive provided - " + destination + ":" + name + ":" + instruction
							+ ":" + Arrays.asList(prompt) + ":" + Arrays.toString(echo));
				}
				return null;
			}
		}

	}

}
