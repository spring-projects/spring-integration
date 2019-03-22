/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.integration.ip.tcp.connection;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link TcpSSLContextSupport}; uses a
 * 'TLS' (by default) {@link SSLContext}, initialized with 'JKS'
 * keystores, managed by 'SunX509' Key and Trust managers.
 *
 * @author Gary Russell
 *
 * @since 2.1
 *
 */
public class DefaultTcpSSLContextSupport implements TcpSSLContextSupport {

	private static final String DEFAULT_KEY_STORE_TYPE = "JKS";

	private static final String DEFAULT_TRUST_STORE_TYPE = "JKS";

	private final Resource keyStore;

	private final Resource trustStore;

	private final char[] keyStorePassword;

	private final char[] trustStorePassword;

	private String protocol = "TLS";

	private String keyStoreType = DEFAULT_KEY_STORE_TYPE;

	private String trustStoreType = DEFAULT_TRUST_STORE_TYPE;

	/**
	 * Prepares for the creation of an SSLContext using the supplied
	 * key/trust stores and passwords.
	 * @param keyStore A {@link Resource} pattern pointing to the keyStore.
	 * @param trustStore A {@link Resource} pattern pointing to the trustStore.
	 * @param keyStorePassword The password for the keyStore.
	 * @param trustStorePassword The password for the trustStore.
	 */
	public DefaultTcpSSLContextSupport(String keyStore, String trustStore,
			String keyStorePassword, String trustStorePassword) {
		Assert.notNull(keyStore, "keyStore cannot be null");
		Assert.notNull(trustStore, "trustStore cannot be null");
		Assert.notNull(keyStorePassword, "keyStorePassword cannot be null");
		Assert.notNull(trustStorePassword, "trustStorePassword cannot be null");
		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		this.keyStore = resolver.getResource(keyStore);
		this.trustStore = resolver.getResource(trustStore);
		this.keyStorePassword = keyStorePassword.toCharArray();
		this.trustStorePassword = trustStorePassword.toCharArray();
	}

	/**
	 * Set the key store type. Default JKS.
	 * @param keyStoreType the type.
	 * @since 5.0.8
	 */
	public void setKeyStoreType(String keyStoreType) {
		Assert.hasText(keyStoreType, "'keyStoreType' cannot be empty");
		this.keyStoreType = keyStoreType;
	}

	/**
	 * Set the trust store type. Default JKS.
	 * @param trustStoreType the type.
	 * @since 5.0.8
	 */
	public void setTrustStoreType(String trustStoreType) {
		Assert.hasText(trustStoreType, "'trustStoreType' cannot be empty");
		this.trustStoreType = trustStoreType;
	}

	@Override
	public SSLContext getSSLContext() throws GeneralSecurityException, IOException {
		KeyStore ks = KeyStore.getInstance(this.keyStoreType);
		KeyStore ts = KeyStore.getInstance(this.trustStoreType);

		ks.load(this.keyStore.getInputStream(), this.keyStorePassword);
		ts.load(this.trustStore.getInputStream(), this.trustStorePassword);

		KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
		kmf.init(ks, this.keyStorePassword);

		TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
		tmf.init(ts);

		SSLContext sslContext = SSLContext.getInstance(this.protocol);

		sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

		return sslContext;

	}

	/**
	 * The protocol used in {@link SSLContext#getInstance(String)}; default "TLS".
	 * @param protocol The protocol.
	 */
	public void setProtocol(String protocol) {
		Assert.notNull(protocol, "protocol must not be null");
		this.protocol = protocol;
	}

}
