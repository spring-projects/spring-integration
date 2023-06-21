/*
 * Copyright 2002-2023 the original author or authors.
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
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.auth.keyboard.UserInteraction;
import org.apache.sshd.client.auth.password.PasswordIdentityProvider;
import org.apache.sshd.client.config.hosts.HostConfigEntry;
import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier;
import org.apache.sshd.client.keyverifier.RejectAllServerKeyVerifier;
import org.apache.sshd.client.keyverifier.ServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.SshConstants;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.keyprovider.KeyIdentityProvider;
import org.apache.sshd.common.util.io.resource.AbstractIoResource;
import org.apache.sshd.common.util.io.resource.IoResource;
import org.apache.sshd.common.util.net.SshdSocketAddress;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.apache.sshd.sftp.client.SftpVersionSelector;

import org.springframework.core.io.Resource;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.remote.session.SharedSessionCapable;
import org.springframework.util.Assert;

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
 * @author Krzysztof Debski
 * @author Auke Zaaiman
 * @author Christian Tzolov
 *
 * @since 2.0
 */
public class DefaultSftpSessionFactory implements SessionFactory<SftpClient.DirEntry>, SharedSessionCapable {

	private final Lock lock = new ReentrantLock();

	private final SshClient sshClient;

	private volatile boolean initialized;

	private final boolean isSharedSession;

	private final Lock sharedSessionLock;

	private boolean isInnerClient = false;

	private String host;

	private int port = SshConstants.DEFAULT_PORT;

	private String user;

	private String password;

	private HostConfigEntry hostConfig;

	private Resource knownHosts;

	private Resource privateKey;

	private String privateKeyPassphrase;

	private UserInteraction userInteraction;

	private boolean allowUnknownKeys = false;

	private Integer timeout;

	private SftpVersionSelector sftpVersionSelector = SftpVersionSelector.CURRENT;

	private volatile SftpClient sharedSftpClient;

	public DefaultSftpSessionFactory() {
		this(false);
	}

	/**
	 * @param isSharedSession true if the session is to be shared.
	 */
	public DefaultSftpSessionFactory(boolean isSharedSession) {
		this(SshClient.setUpDefaultClient(), isSharedSession);
		this.isInnerClient = true;
	}

	/**
	 * Instantiate based on the provided {@link SshClient}, e.g. some extension for HTTP/SOCKS.
	 * @param sshClient the {@link SshClient} instance.
	 * @param isSharedSession true if the session is to be shared.
	 */
	public DefaultSftpSessionFactory(SshClient sshClient, boolean isSharedSession) {
		Assert.notNull(sshClient, "'sshClient' must not be null");
		this.sshClient = sshClient;
		this.isSharedSession = isSharedSession;
		if (this.isSharedSession) {
			this.sharedSessionLock = new ReentrantLock();
		}
		else {
			this.sharedSessionLock = null;
		}
	}

	/**
	 * The url of the host you want to connect to. This is a mandatory property.
	 * @param host The host.
	 * @see SshClient#connect(String, String, int)
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * The port over which the SFTP connection shall be established. If not specified,
	 * this value defaults to <code>22</code>. If specified, this properties must
	 * be a positive number.
	 * @param port The port.
	 * @see SshClient#connect(String, String, int)
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * The remote user to use. This is a mandatory property.
	 * @param user The user.
	 * @see SshClient#connect(String, String, int)
	 */
	public void setUser(String user) {
		this.user = user;
	}

	/**
	 * The password to authenticate against the remote host. If a password is
	 * not provided, then a {@link DefaultSftpSessionFactory#setPrivateKey(Resource) privateKey} is
	 * mandatory.
	 * @param password The password.
	 * @see SshClient#setPasswordIdentityProvider(PasswordIdentityProvider)
	 */
	public void setPassword(String password) {
		Assert.state(this.isInnerClient,
				"A password must be configured on the externally provided SshClient instance");
		this.password = password;
	}

	/**
	 * Provide a {@link HostConfigEntry} as an alternative for the user/host/port options.
	 * Can be configured with a proxy jump property.
	 * @param hostConfig the {@link HostConfigEntry} for connection.
	 * @since 6.0
	 * @see SshClient#connect(HostConfigEntry)
	 */
	public void setHostConfig(HostConfigEntry hostConfig) {
		this.hostConfig = hostConfig;
	}

	/**
	 * Specifies a {@link Resource} that will be used for a host key repository.
	 * The data has to have the same format as OpenSSH's known_hosts file.
	 * @param knownHosts the resource for known hosts.
	 * @since 5.2.5
	 * @see SshClient#setServerKeyVerifier(ServerKeyVerifier)
	 */
	public void setKnownHostsResource(Resource knownHosts) {
		Assert.state(this.isInnerClient,
				"Known hosts must be configured on the externally provided SshClient instance");
		this.knownHosts = knownHosts;
	}

	/**
	 * Allows you to set a {@link Resource}, which represents the location of the
	 * private key used for authenticating against the remote host. If the privateKey
	 * is not provided, then the {@link DefaultSftpSessionFactory#setPassword(String) password}
	 * property is mandatory.
	 * @param privateKey The private key.
	 * @see SshClient#setKeyIdentityProvider(KeyIdentityProvider)
	 */
	public void setPrivateKey(Resource privateKey) {
		Assert.state(this.isInnerClient,
				"A private key auth must be configured on the externally provided SshClient instance");
		this.privateKey = privateKey;
	}

	/**
	 * The password for the private key. Optional.
	 * @param privateKeyPassphrase The private key passphrase.
	 * @see SshClient#setKeyIdentityProvider(KeyIdentityProvider)
	 */
	public void setPrivateKeyPassphrase(String privateKeyPassphrase) {
		Assert.state(this.isInnerClient,
				"A private key auth must be configured on the externally provided SshClient instance");
		this.privateKeyPassphrase = privateKeyPassphrase;
	}

	/**
	 * Provide a {@link UserInteraction} which exposes control over dealing with new keys or key
	 * changes. As Spring Integration will not normally allow user interaction, the
	 * implementation must respond to SSH protocol calls in a suitable way.
	 * @param userInteraction the UserInteraction.
	 * @since 4.1.7
	 * @see SshClient#setUserInteraction(UserInteraction)
	 */
	public void setUserInteraction(UserInteraction userInteraction) {
		Assert.state(this.isInnerClient,
				"A `UserInteraction` must be configured on the externally provided SshClient instance");
		this.userInteraction = userInteraction;
	}

	/**
	 * When no {@link #knownHosts} has been provided, set to true to unconditionally allow
	 * connecting to an unknown host or when a host's key has changed (see
	 * {@link #setKnownHostsResource(Resource) knownHosts}). Default false (since 4.2).
	 * Set to true if a knownHosts file is not provided.
	 * @param allowUnknownKeys true to allow connecting to unknown hosts.
	 * @since 4.1.7
	 */
	public void setAllowUnknownKeys(boolean allowUnknownKeys) {
		Assert.state(this.isInnerClient,
				"An `AcceptAllServerKeyVerifier` must be configured on the externally provided SshClient instance");
		this.allowUnknownKeys = allowUnknownKeys;
	}

	/**
	 * The timeout property is used as the socket timeout parameter, as well as
	 * the default connection timeout. Defaults to <code>0</code>, which means,
	 * that no timeout will occur.
	 * @param timeout The timeout.
	 * @see org.apache.sshd.client.future.ConnectFuture#verify(long)
	 */
	public void setTimeout(Integer timeout) {
		this.timeout = timeout;
	}

	public void setSftpVersionSelector(SftpVersionSelector sftpVersionSelector) {
		Assert.notNull(sftpVersionSelector, "'sftpVersionSelector' must noy be null");
		this.sftpVersionSelector = sftpVersionSelector;
	}

	@Override
	public SftpSession getSession() {
		SftpSession sftpSession;
		if (this.sharedSessionLock != null) {
			this.sharedSessionLock.lock();
		}
		SftpClient sftpClient = this.sharedSftpClient;
		try {
			boolean freshSftpClient = false;
			if (sftpClient == null || !sftpClient.isOpen()) {
				sftpClient =
						SftpClientFactory.instance()
								.createSftpClient(initClientSession(), this.sftpVersionSelector);
				freshSftpClient = true;
			}
			sftpSession = new SftpSession(sftpClient);
			sftpSession.connect();
			if (this.isSharedSession && freshSftpClient) {
				this.sharedSftpClient = sftpClient;
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
		return sftpSession;
	}

	private ClientSession initClientSession() throws IOException {
		Assert.hasText(this.host, "host must not be empty");
		Assert.hasText(this.user, "user must not be empty");

		initClient();

		Duration verifyTimeout = this.timeout != null ? Duration.ofMillis(this.timeout) : null;
		HostConfigEntry config = this.hostConfig;
		if (config == null) {
			config = new HostConfigEntry(SshdSocketAddress.isIPv6Address(this.host) ? "" : this.host, this.host,
					this.port, this.user);
		}
		ClientSession clientSession =
				this.sshClient.connect(config)
						.verify(verifyTimeout)
						.getSession();

		clientSession.auth().verify(verifyTimeout);

		return clientSession;
	}

	private void initClient() throws IOException {
		if (!this.initialized) {
			this.lock.lock();
			try {
				if (!this.initialized) {
					doInitClient();
					this.initialized = true;
				}
			}
			finally {
				this.lock.unlock();
			}
		}
	}

	private void doInitClient() throws IOException {
		if (this.port <= 0) {
			this.port = SshConstants.DEFAULT_PORT;
		}

		doInitInnerClient();

		this.sshClient.start();
	}

	private void doInitInnerClient() throws IOException {
		if (this.isInnerClient) {
			ServerKeyVerifier serverKeyVerifier =
					this.allowUnknownKeys ? AcceptAllServerKeyVerifier.INSTANCE : RejectAllServerKeyVerifier.INSTANCE;
			if (this.knownHosts != null) {
				serverKeyVerifier = new ResourceKnownHostsServerKeyVerifier(this.knownHosts);
			}
			this.sshClient.setServerKeyVerifier(serverKeyVerifier);

			this.sshClient.setPasswordIdentityProvider(PasswordIdentityProvider.wrapPasswords(this.password));
			if (this.privateKey != null) {
				IoResource<Resource> privateKeyResource =
						new AbstractIoResource<>(Resource.class, this.privateKey) {

							@Override
							public InputStream openInputStream() throws IOException {
								return getResourceValue().getInputStream();
							}

						};
				try {
					Collection<KeyPair> keys =
							SecurityUtils.getKeyPairResourceParser()
									.loadKeyPairs(null, privateKeyResource,
											FilePasswordProvider.of(this.privateKeyPassphrase));
					this.sshClient.setKeyIdentityProvider(KeyIdentityProvider.wrapKeyPairs(keys));
				}
				catch (GeneralSecurityException ex) {
					throw new IOException("Cannot load private key: " + this.privateKey.getFilename(), ex);
				}
			}
			this.sshClient.setUserInteraction(this.userInteraction);
		}
	}

	@Override
	public final boolean isSharedSession() {
		return this.isSharedSession;
	}

	@Override
	public void resetSharedSession() {
		Assert.state(this.isSharedSession, "Shared sessions are not being used");
		this.sharedSftpClient = null;
	}

}
