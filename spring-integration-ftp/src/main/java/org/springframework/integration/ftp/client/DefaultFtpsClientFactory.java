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

package org.springframework.integration.ftp.client;

import java.io.IOException;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;

import org.apache.commons.net.ftp.FTPSClient;

import org.springframework.util.StringUtils;

/**
 * provides a working FTPS implementation. Based heavily on {@link org.springframework.integration.ftp.client.DefaultFtpClientFactory}
 * 
 * @author Josh Long
 * @author Iwein Fuld
 */
public class DefaultFtpsClientFactory extends AbstractFtpClientFactory<FTPSClient> {

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
		this.cipherSuites = cipherSuites;
	}

	public void setProtocols(String[] protocols) {
		this.protocols = protocols;
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
	protected void onAfterConnect(FTPSClient ftpsClient) throws IOException {
		ftpsClient.execPBSZ(0);
		ftpsClient.execPROT(this.prot);
	}

	@Override
	public FTPSClient getClient() throws SocketException, IOException {
		FTPSClient ftpsClient = super.getClient();
		if (StringUtils.hasText(this.authValue)) {
			ftpsClient.setAuthValue(authValue);
		}
		if (this.trustManager != null) {
			ftpsClient.setTrustManager(this.trustManager);
		}
		if (this.cipherSuites != null) {
			ftpsClient.setEnabledCipherSuites(this.cipherSuites);
		}
		if (this.protocols != null) {
			ftpsClient.setEnabledProtocols(this.protocols);
		}
		if (this.sessionCreation != null) {
			ftpsClient.setEnabledSessionCreation(this.sessionCreation);
		}
		if (this.useClientMode != null) {
			ftpsClient.setUseClientMode(this.useClientMode);
		}
		if (this.sessionCreation != null) {
			ftpsClient.setEnabledSessionCreation(this.sessionCreation);
		}
		if (this.keyManager != null) {
			ftpsClient.setKeyManager(keyManager);
		}
		if (this.needClientAuth != null) {
			ftpsClient.setNeedClientAuth(this.needClientAuth);
		}
		if (this.wantsClientAuth != null) {
			ftpsClient.setWantClientAuth(this.wantsClientAuth);
		}
		return ftpsClient;
	}

	@Override
	protected FTPSClient createSingleInstanceOfClient() {
		try {
			if (StringUtils.hasText(this.protocol)) {
				return new FTPSClient(this.protocol, this.implicit);
			}
			return new FTPSClient(this.implicit);
		}
		catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

}
