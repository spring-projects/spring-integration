/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.ftp.session;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.apache.commons.net.ftp.FTPFile;
import org.junit.jupiter.api.Test;

import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.ftp.FtpTestSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests the FTP session factories against a real FTP server, through real proxies.
 *
 * @author Sharang Gupta
 *
 * @since 7.2
 */
public class FtpProxySessionFactoryTests extends FtpTestSupport {

	private static final String REMOTE_FILE = "ftpTarget/proxied.txt";

	private static final String CONTENT = "through the proxy";

	@Test
	void sessionIsEstablishedThroughASocksProxy() throws IOException {
		try (LocalProxyServer proxy = LocalProxyServer.socks5()) {
			DefaultFtpSessionFactory sessionFactory = rawSessionFactory();
			sessionFactory.setProxy(new Proxy(Proxy.Type.SOCKS, proxy.address()));

			assertFileRoundTrip(sessionFactory);

			assertControlAndDataConnectionsWereTunneled(proxy);
		}
	}

	@Test
	void sessionIsEstablishedThroughAnHttpProxyOfTheJvm() throws IOException {
		try (LocalProxyServer proxy = LocalProxyServer.httpConnect(null, null)) {
			DefaultFtpSessionFactory sessionFactory = rawSessionFactory();
			sessionFactory.setProxy(new Proxy(Proxy.Type.HTTP, proxy.address()));

			assertFileRoundTrip(sessionFactory);

			assertControlAndDataConnectionsWereTunneled(proxy);
		}
	}

	@Test
	void sessionIsEstablishedThroughAnHttpProxyWithCredentials() throws IOException {
		try (LocalProxyServer proxy = LocalProxyServer.httpConnect("squid", "secret")) {
			FtpHttpProxySessionFactory sessionFactory = httpProxySessionFactory(proxy);
			sessionFactory.setProxyUsername("squid");
			sessionFactory.setProxyPassword("secret");

			assertFileRoundTrip(sessionFactory);

			assertControlAndDataConnectionsWereTunneled(proxy);
		}
	}

	@Test
	void sessionIsNotEstablishedWithWrongProxyCredentials() throws IOException {
		try (LocalProxyServer proxy = LocalProxyServer.httpConnect("squid", "secret")) {
			FtpHttpProxySessionFactory sessionFactory = httpProxySessionFactory(proxy);
			sessionFactory.setProxyUsername("squid");
			sessionFactory.setProxyPassword("wrong");

			assertThatIllegalStateException()
					.isThrownBy(sessionFactory::getSession)
					.withStackTraceContaining("407 Proxy Authentication Required");

			assertThat(proxy.tunnels()).isEmpty();
		}
	}

	private static FtpHttpProxySessionFactory httpProxySessionFactory(LocalProxyServer proxy) {
		FtpHttpProxySessionFactory sessionFactory = new FtpHttpProxySessionFactory();
		sessionFactory.setHost("localhost");
		sessionFactory.setPort(port);
		sessionFactory.setUsername("ftpuser");
		sessionFactory.setPassword("ftppassword");
		sessionFactory.setProxyHost(proxy.address().getHostString());
		sessionFactory.setProxyPort(proxy.address().getPort());
		return sessionFactory;
	}

	private static void assertFileRoundTrip(SessionFactory<FTPFile> sessionFactory) throws IOException {
		try (Session<FTPFile> session = sessionFactory.getSession()) {
			session.write(new ByteArrayInputStream(CONTENT.getBytes(StandardCharsets.UTF_8)), REMOTE_FILE);

			assertThat(Arrays.stream(session.list("ftpTarget")).map(FTPFile::getName)).contains("proxied.txt");

			ByteArrayOutputStream content = new ByteArrayOutputStream();
			session.read(REMOTE_FILE, content);
			assertThat(content.toString(StandardCharsets.UTF_8)).isEqualTo(CONTENT);
		}
	}

	private static void assertControlAndDataConnectionsWereTunneled(LocalProxyServer proxy) {
		assertThat(proxy.tunnels())
				.as("the control connection and the data connections of the write, the list and the read")
				.hasSizeGreaterThanOrEqualTo(4);
		assertThat(proxy.tunnels().get(0)).as("the control connection").endsWith(":" + port);
	}

}
