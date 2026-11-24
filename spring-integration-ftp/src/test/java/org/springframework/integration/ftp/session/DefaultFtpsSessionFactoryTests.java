/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.ftp.session;

import javax.net.ssl.X509TrustManager;

import org.apache.commons.net.ftp.FTPSClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Glenn Renfro
 *
 * @since 5.5.23
 */
class DefaultFtpsSessionFactoryTests {

	@Test
	void endpointCheckingIsEnabledByDefault() {
		DefaultFtpsSessionFactory sessionFactory = new DefaultFtpsSessionFactory();
		FTPSClient ftpsClient = new FTPSClient();

		sessionFactory.postProcessClientBeforeConnect(ftpsClient);

		assertThat(ftpsClient.isEndpointCheckingEnabled()).isTrue();
	}

	@Test
	void insecureDefaultTrustManagerIsOverriddenWhenNotConfigured() {
		DefaultFtpsSessionFactory sessionFactory = new DefaultFtpsSessionFactory();
		FTPSClient ftpsClient = new FTPSClient();
		sessionFactory.postProcessClientBeforeConnect(ftpsClient);

		assertThat(ftpsClient.getTrustManager()).isNull();
	}

	@Test
	void explicitlyConfiguredTrustManagerIsHonored() {
		DefaultFtpsSessionFactory sessionFactory = new DefaultFtpsSessionFactory();
		X509TrustManager customTrustManager = mock(X509TrustManager.class);
		when(customTrustManager.getAcceptedIssuers()).thenReturn(new java.security.cert.X509Certificate[0]);
		sessionFactory.setTrustManager(customTrustManager);
		FTPSClient ftpsClient = new FTPSClient();
		sessionFactory.postProcessClientBeforeConnect(ftpsClient);

		assertThat(ftpsClient.getTrustManager()).isSameAs(customTrustManager);
	}

}
