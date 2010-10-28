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

package org.springframework.integration.ftp;

import org.springframework.util.StringUtils;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;

/**
 * Factors out the client factory creation.
 *
 * @author Josh Long
 */
public class ClientFactorySupport {

	public static DefaultFtpsClientFactory ftpsClientFactory(String host, int port, String remoteWorkingDirectory, String user, String password, int fileType,
															 int clientMode, String prot, String protocol, String authValue,
															 Boolean implicit, TrustManager trustManager, KeyManager keyManager,
															 Boolean sessionCreation, Boolean useClientMode,
															 Boolean wantsClientAuth, Boolean needClientAuth, String[] cipherSuites) {
		DefaultFtpsClientFactory defaultFtpClientFactory = new DefaultFtpsClientFactory();
		defaultFtpClientFactory.setHost(host);
		defaultFtpClientFactory.setPassword(password);
		defaultFtpClientFactory.setPort(port);
		defaultFtpClientFactory.setRemoteWorkingDirectory(remoteWorkingDirectory);
		defaultFtpClientFactory.setUsername(user);
		defaultFtpClientFactory.setFileType(fileType);
		defaultFtpClientFactory.setClientMode(clientMode);
		if (cipherSuites != null) {
			defaultFtpClientFactory.setCipherSuites(cipherSuites);
		}
		if (StringUtils.hasText(prot)) {
			defaultFtpClientFactory.setProt(prot);
		}
		if (StringUtils.hasText(protocol)) {
			defaultFtpClientFactory.setProtocol(protocol);
		}
		if (StringUtils.hasText(authValue)) {
			defaultFtpClientFactory.setAuthValue(authValue);
		}
		if (null != implicit) {
			defaultFtpClientFactory.setImplicit(implicit);
		}
		if (trustManager != null) {
			defaultFtpClientFactory.setTrustManager(trustManager);
		}
		if (keyManager != null) {
			defaultFtpClientFactory.setKeyManager(keyManager);
		}
		if (needClientAuth != null) {
			defaultFtpClientFactory.setNeedClientAuth(needClientAuth);
		}
		if (wantsClientAuth != null) {
			defaultFtpClientFactory.setWantsClientAuth(wantsClientAuth);
		}
		if (sessionCreation != null) {
			defaultFtpClientFactory.setSessionCreation(sessionCreation);
		}
		if (useClientMode != null) {
			defaultFtpClientFactory.setUseClientMode(useClientMode);
		}
		return defaultFtpClientFactory;
	}

	public static DefaultFtpClientFactory ftpClientFactory(String host,
														   int port,
														   String remoteWorkingDirectory,
														   String user,
														   String password,
														   int clientMode,
														   int fileType) {
		DefaultFtpClientFactory defaultFtpClientFactory = new DefaultFtpClientFactory();
		defaultFtpClientFactory.setHost(host);
		defaultFtpClientFactory.setPassword(password);
		defaultFtpClientFactory.setPort(port);
		defaultFtpClientFactory.setRemoteWorkingDirectory(remoteWorkingDirectory);
		defaultFtpClientFactory.setUsername(user);
		defaultFtpClientFactory.setClientMode(clientMode);
		defaultFtpClientFactory.setFileType(fileType);
		return defaultFtpClientFactory;
	}

}
