/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.ftp.session;

import java.io.IOException;
import java.util.Arrays;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;

import org.apache.commons.net.ftp.FTPSClient;

import org.springframework.integration.JavaUtils;
import org.springframework.util.StringUtils;

/**
 * SessionFactory for FTPS.
 *
 * @author Josh Long
 * @author Iwein Fuld
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
public class DefaultFtpsSessionFactory extends AbstractFtpSessionFactory<FTPSClient> {

	private Boolean useClientMode;

	private Boolean sessionCreation;

	private String authValue;

	private TrustManager trustManager;

	private String[] cipherSuites;

	private String[] protocols;

	private KeyManager keyManager;

	private Boolean needClientAuth;

	private Boolean wantsClientAuth;

	private boolean implicit = false;

	private String prot = "P";

	private String protocol;

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public void setUseClientMode(Boolean useClientMode) {
		this.useClientMode = useClientMode;
	}

	public void setSessionCreation(Boolean sessionCreation) {
		this.sessionCreation = sessionCreation;
	}

	public void setAuthValue(String authValue) {
		this.authValue = authValue;
	}

	public void setTrustManager(TrustManager trustManager) {
		this.trustManager = trustManager;
	}

	public void setCipherSuites(String[] cipherSuites) {
		this.cipherSuites = Arrays.copyOf(cipherSuites, cipherSuites.length);
	}

	public void setProtocols(String[] protocols) {
		this.protocols = Arrays.copyOf(protocols, protocols.length);
	}

	public void setKeyManager(KeyManager keyManager) {
		this.keyManager = keyManager;
	}

	public void setNeedClientAuth(Boolean needClientAuth) {
		this.needClientAuth = needClientAuth;
	}

	public void setWantsClientAuth(Boolean wantsClientAuth) {
		this.wantsClientAuth = wantsClientAuth;
	}

	public void setProt(String prot) {
		this.prot = prot;
	}

	public void setImplicit(boolean implicit) {
		this.implicit = implicit;
	}

	@Override
	protected FTPSClient createClientInstance() {
		try {
			if (StringUtils.hasText(this.protocol)) {
				return new FTPSClient(this.protocol, this.implicit);
			}
			return new FTPSClient(this.implicit);
		}
		catch (Exception e) {

			/*
			 This catch block is technically not necessary but it allows users
			 to use the older Commons Net 2.0 if necessary, which requires you
			 to catch a NoSuchAlgorithmException.
			 */

			if (e instanceof RuntimeException) { //NOSONAR false positive
				throw (RuntimeException) e;
			}

			throw new IllegalStateException("Failed to create FTPS client.", e);
		}
	}

	@Override
	protected void postProcessClientAfterConnect(FTPSClient ftpsClient) throws IOException {
		ftpsClient.execPBSZ(0);
		ftpsClient.execPROT(this.prot);
	}

	@Override
	protected void postProcessClientBeforeConnect(FTPSClient ftpsClient) {
		JavaUtils.INSTANCE
				.acceptIfHasText(this.authValue, ftpsClient::setAuthValue)
				.acceptIfNotNull(this.trustManager, ftpsClient::setTrustManager)
				.acceptIfNotNull(this.cipherSuites, ftpsClient::setEnabledCipherSuites)
				.acceptIfNotNull(this.protocols, ftpsClient::setEnabledProtocols)
				.acceptIfNotNull(this.sessionCreation, ftpsClient::setEnabledSessionCreation)
				.acceptIfNotNull(this.useClientMode, ftpsClient::setUseClientMode)
				.acceptIfNotNull(this.keyManager, ftpsClient::setKeyManager)
				.acceptIfNotNull(this.needClientAuth, ftpsClient::setNeedClientAuth)
				.acceptIfNotNull(this.wantsClientAuth, ftpsClient::setWantClientAuth);
	}

}
