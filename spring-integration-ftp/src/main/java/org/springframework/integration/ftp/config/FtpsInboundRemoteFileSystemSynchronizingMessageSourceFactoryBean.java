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

package org.springframework.integration.ftp.config;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;

import org.apache.commons.net.ftp.FTPClient;

import org.springframework.integration.ftp.client.AbstractFtpClientFactory;
import org.springframework.integration.ftp.client.DefaultFtpsClientFactory;
import org.springframework.util.StringUtils;

/**
 * Factory to make building the namespace easier.
 *
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @since 2.0
 */
class FtpsInboundRemoteFileSystemSynchronizingMessageSourceFactoryBean extends FtpInboundRemoteFileSystemSynchronizingMessageSourceFactoryBean {

	/**
	 * Sets whether the connection is implicit. Local testing reveals this to be a good choice.
	 */
	protected volatile Boolean implicit = Boolean.FALSE;

	/**
	 * "TLS" or "SSL"
	 */
	protected volatile String protocol;

	/**
	 * "P"
	 */
	protected volatile String prot;

	private KeyManager keyManager;

	private TrustManager trustManager;

	protected volatile String authValue;

	private Boolean sessionCreation;

	private Boolean useClientMode;

	private Boolean needClientAuth;

	private Boolean wantsClientAuth;

	private String[] cipherSuites;


	public FtpsInboundRemoteFileSystemSynchronizingMessageSourceFactoryBean() {
		this.defaultFtpInboundFolderName = "ftpsInbound";
		this.clientMode = FTPClient.PASSIVE_LOCAL_DATA_CONNECTION_MODE;
	}


	public void setKeyManager(KeyManager keyManager) {
		this.keyManager = keyManager;
	}

	public void setTrustManager(TrustManager trustManager) {
		this.trustManager = trustManager;
	}

	public void setImplicit(Boolean implicit) {
		this.implicit = implicit;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public void setProt(String prot) {
		this.prot = prot;
	}

	public void setAuthValue(String authValue) {
		this.authValue = authValue;
	}

	public void setSessionCreation(Boolean sessionCreation) {
		this.sessionCreation = sessionCreation;
	}

	public void setUseClientMode(Boolean useClientMode) {
		this.useClientMode = useClientMode;
	}

	public void setNeedClientAuth(Boolean needClientAuth) {
		this.needClientAuth = needClientAuth;
	}

	public void setWantsClientAuth(Boolean wantsClientAuth) {
		this.wantsClientAuth = wantsClientAuth;
	}

	public void setCipherSuites(String[] cipherSuites) {
		this.cipherSuites = cipherSuites;
	}

	protected AbstractFtpClientFactory<?> initializeFactory(AbstractFtpClientFactory<?> factory) throws Exception {
		super.initializeFactory(factory);
		DefaultFtpsClientFactory ftpsFactory = (DefaultFtpsClientFactory) factory;
		ftpsFactory.setCipherSuites(this.cipherSuites);
		ftpsFactory.setAuthValue(this.authValue);
		ftpsFactory.setTrustManager(this.trustManager);
		ftpsFactory.setKeyManager(this.keyManager);
		ftpsFactory.setNeedClientAuth(this.needClientAuth);
		ftpsFactory.setWantsClientAuth(this.wantsClientAuth);
		ftpsFactory.setSessionCreation(this.sessionCreation);
		ftpsFactory.setUseClientMode(this.useClientMode);
		if (StringUtils.hasText(this.prot)) {
			ftpsFactory.setProt(this.prot);
		}
		if (StringUtils.hasText(this.protocol)) {
			ftpsFactory.setProtocol(this.protocol);
		}
		if (this.implicit != null) {
			ftpsFactory.setImplicit(this.implicit);
		}
		return factory;
	}
	
	protected AbstractFtpClientFactory<?> createClientFactory(){
		return new DefaultFtpsClientFactory();
	}

}
