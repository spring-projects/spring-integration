/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.ftp.session;

import java.net.InetSocketAddress;
import java.net.Proxy;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPHTTPClient;
import org.junit.jupiter.api.Test;

import org.springframework.integration.test.util.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for the {@link FtpHttpProxySessionFactory}.
 *
 * @author Sharang Gupta
 *
 * @since 7.2
 */
class FtpHttpProxySessionFactoryTests {

	@Test
	void passiveClientModeIsTheDefault() {
		FtpHttpProxySessionFactory sessionFactory = new FtpHttpProxySessionFactory();

		assertThat(TestUtils.<Integer>getPropertyValue(sessionFactory, "clientMode"))
				.isEqualTo(FTPClient.PASSIVE_LOCAL_DATA_CONNECTION_MODE);
	}

	@Test
	void activeClientModeIsRejected() {
		FtpHttpProxySessionFactory sessionFactory = new FtpHttpProxySessionFactory();

		assertThatIllegalArgumentException()
				.isThrownBy(() -> sessionFactory.setClientMode(FTPClient.ACTIVE_LOCAL_DATA_CONNECTION_MODE))
				.withMessageContaining("PASSIVE_LOCAL_DATA_CONNECTION_MODE");
	}

	@Test
	void anAdditionalProxyIsNotSupported() {
		FtpHttpProxySessionFactory sessionFactory = new FtpHttpProxySessionFactory();

		assertThatExceptionOfType(UnsupportedOperationException.class)
				.isThrownBy(() -> sessionFactory.setProxy(
						new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("localhost", 1080))))
				.withMessageContaining("proxyHost");
	}

	@Test
	void proxyHostIsRequired() {
		FtpHttpProxySessionFactory sessionFactory = new FtpHttpProxySessionFactory();
		sessionFactory.setUsername("ftpuser");
		sessionFactory.setProxyPort(3128);

		assertThatIllegalStateException()
				.isThrownBy(sessionFactory::getSession)
				.havingCause()
				.withMessageContaining("'proxyHost' is required");
	}

	@Test
	void proxyPasswordIsRequiredWithTheProxyUsername() {
		FtpHttpProxySessionFactory sessionFactory = new FtpHttpProxySessionFactory();
		sessionFactory.setUsername("ftpuser");
		sessionFactory.setProxyHost("proxy.example.com");
		sessionFactory.setProxyPort(3128);
		sessionFactory.setProxyUsername("squid");

		assertThatIllegalStateException()
				.isThrownBy(sessionFactory::getSession)
				.havingCause()
				.withMessageContaining("'proxyPassword' is required");
	}

	@Test
	void clientTunnelsThroughTheHttpProxy() {
		FtpHttpProxySessionFactory sessionFactory = new FtpHttpProxySessionFactory();
		sessionFactory.setProxyHost("proxy.example.com");
		sessionFactory.setProxyPort(3128);

		assertThat(sessionFactory.createClientInstance()).isInstanceOf(FTPHTTPClient.class);
	}

}
