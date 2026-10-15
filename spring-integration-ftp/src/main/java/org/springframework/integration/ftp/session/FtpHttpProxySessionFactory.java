/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.ftp.session;

import java.net.Proxy;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPHTTPClient;
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * An {@link AbstractFtpSessionFactory} for the connections tunneled through an HTTP proxy
 * with the {@code CONNECT} method, based on the {@link FTPHTTPClient}.
 * <p>
 * Unlike the {@link #setProxy(Proxy)} option of the other factories, this factory sends
 * the credentials of the proxy, when they are provided, with the basic authentication scheme.
 * <p>
 * Both the control connection and the data connections are tunneled through the proxy,
 * therefore only the {@link FTPClient#PASSIVE_LOCAL_DATA_CONNECTION_MODE} is supported,
 * and it is the default client mode of this factory.
 *
 * @author Sharang Gupta
 *
 * @since 7.2
 */
public class FtpHttpProxySessionFactory extends AbstractFtpSessionFactory<FTPHTTPClient> {

	private @Nullable String proxyHost;

	private int proxyPort;

	private @Nullable String proxyUsername;

	private @Nullable String proxyPassword;

	@SuppressWarnings("this-escape")
	public FtpHttpProxySessionFactory() {
		super.setClientMode(FTPClient.PASSIVE_LOCAL_DATA_CONNECTION_MODE);
	}

	/**
	 * Set the host of the HTTP proxy.
	 * @param proxyHost the host of the proxy.
	 */
	public void setProxyHost(String proxyHost) {
		Assert.hasText(proxyHost, "'proxyHost' must not be empty");
		this.proxyHost = proxyHost;
	}

	/**
	 * Set the port of the HTTP proxy.
	 * @param proxyPort the port of the proxy.
	 */
	public void setProxyPort(int proxyPort) {
		Assert.isTrue(proxyPort > 0, "'proxyPort' must be > 0");
		this.proxyPort = proxyPort;
	}

	/**
	 * Set the username for the HTTP proxy, when the proxy requires authentication.
	 * The {@link #setProxyPassword(String) password} is required as well.
	 * @param proxyUsername the username for the proxy.
	 */
	public void setProxyUsername(String proxyUsername) {
		Assert.hasText(proxyUsername, "'proxyUsername' must not be empty");
		this.proxyUsername = proxyUsername;
	}

	/**
	 * Set the password for the HTTP proxy, when the proxy requires authentication.
	 * @param proxyPassword the password for the proxy.
	 */
	public void setProxyPassword(String proxyPassword) {
		Assert.notNull(proxyPassword, "'proxyPassword' must not be null");
		this.proxyPassword = proxyPassword;
	}

	/**
	 * Only the {@link FTPClient#PASSIVE_LOCAL_DATA_CONNECTION_MODE} is supported through an HTTP proxy.
	 * @param clientMode the client mode.
	 */
	@Override
	public void setClientMode(int clientMode) {
		Assert.isTrue(clientMode == FTPClient.PASSIVE_LOCAL_DATA_CONNECTION_MODE,
				"Only 'FTPClient.PASSIVE_LOCAL_DATA_CONNECTION_MODE' is supported through an HTTP proxy. Was: "
						+ clientMode);
		super.setClientMode(clientMode);
	}

	/**
	 * This factory connects to the HTTP proxy directly, so an additional proxy is not supported.
	 * @param proxy the proxy.
	 */
	@Override
	public void setProxy(Proxy proxy) {
		throw new UnsupportedOperationException(
				"The HTTP proxy is configured with the 'proxyHost' and 'proxyPort' properties");
	}

	@Override
	protected FTPHTTPClient createClientInstance() {
		String host = this.proxyHost;
		if (host == null) {
			throw new IllegalStateException("'proxyHost' is required");
		}
		if (this.proxyPort <= 0) {
			throw new IllegalStateException("'proxyPort' is required");
		}
		String username = this.proxyUsername;
		if (username == null) {
			return new FTPHTTPClient(host, this.proxyPort);
		}
		String password = this.proxyPassword;
		if (password == null) {
			throw new IllegalStateException("'proxyPassword' is required when the 'proxyUsername' is provided");
		}
		return new FTPHTTPClient(host, this.proxyPort, username, password);
	}

}
