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

import org.springframework.integration.ftp.client.AbstractFtpClientFactory;
import org.springframework.integration.ftp.client.DefaultFtpsClientFactory;
import org.springframework.util.StringUtils;

/**
 * Sends files to a remote FTPS file system. Based heavily on {@link org.springframework.integration.ftp.outbound.FtpSendingMessageHandler}
 *
 * @author Josh Long
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @since 2.0
 */
class FtpsSendingMessageHandlerFactoryBean extends FtpSendingMessageHandlerFactoryBean {

	/**
	 * Sets whether the connection is implicit. Default is FALSE.
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

	private volatile KeyManager keyManager;

	private volatile TrustManager trustManager;

	protected volatile String authValue;

	private volatile Boolean sessionCreation;

	private volatile Boolean useClientMode;

	private volatile Boolean needClientAuth;

	private volatile Boolean wantsClientAuth;

	private volatile String[] cipherSuites;

	public void setImplicit(Boolean implicit) {
		this.implicit = implicit;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public void setProt(String prot) {
		this.prot = prot;
	}

	public void setKeyManager(KeyManager keyManager) {
		this.keyManager = keyManager;
	}

	public void setTrustManager(TrustManager trustManager) {
		this.trustManager = trustManager;
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

	protected AbstractFtpClientFactory<?> initializeClientFactory(AbstractFtpClientFactory<?> factory) {
		super.initializeClientFactory(factory);
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
		return ftpsFactory;
	}

	protected AbstractFtpClientFactory<?> createClientFactory(){
		return new DefaultFtpsClientFactory();
	}
}
