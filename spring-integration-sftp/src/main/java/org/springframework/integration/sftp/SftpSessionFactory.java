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
package org.springframework.integration.sftp;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;


/**
 * Factories {@link SftpSession} instances. There are lots of ways to construct a
 * {@link SftpSession} instance, and not all of them are obvious. This factory
 * does its best to make it work.
 *
 * @author Josh Long
 * @author Mario Gray
 */
public class SftpSessionFactory implements FactoryBean<SftpSession>, InitializingBean {
	private volatile String knownHosts;
	private volatile String password;
	private volatile String privateKey;
	private volatile String privateKeyPassphrase;
	private volatile String remoteHost;
	private volatile String user;
	private volatile int port = 22; // the default

	public void afterPropertiesSet() throws Exception {
		Assert.hasText(this.remoteHost, "remoteHost can't be empty!");
		Assert.hasText(this.user, "user can't be empty!");
		Assert.state(StringUtils.hasText(this.password) || StringUtils.hasText(this.privateKey) || StringUtils.hasText(this.privateKeyPassphrase),
				"you must configure either a password or a private key and/or a private key passphrase!");
		Assert.state(this.port >= 0, "port must be a valid number! ");
	}

	public SftpSession getObject() throws Exception {
		return new SftpSession(this.user, this.remoteHost, this.password, this.port, this.knownHosts, null, this.privateKey, this.privateKeyPassphrase);
	}

	public Class<? extends SftpSession> getObjectType() {
		return SftpSession.class;
	}

	public boolean isSingleton() {
		return false;
	}

	public void setKnownHosts(String knownHosts) {
		this.knownHosts = knownHosts;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setPrivateKey(String privateKey) {
		this.privateKey = privateKey;
	}

	public void setPrivateKeyPassphrase(String privateKeyPassphrase) {
		this.privateKeyPassphrase = privateKeyPassphrase;
	}

	public void setRemoteHost(String remoteHost) {
		this.remoteHost = remoteHost;
	}

	public void setUser(String user) {
		this.user = user;
	}
}
