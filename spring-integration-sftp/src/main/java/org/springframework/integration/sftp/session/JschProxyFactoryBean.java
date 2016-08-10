/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.sftp.session;

import org.springframework.beans.factory.config.AbstractFactoryBean;

import com.jcraft.jsch.Proxy;
import com.jcraft.jsch.ProxyHTTP;
import com.jcraft.jsch.ProxySOCKS4;
import com.jcraft.jsch.ProxySOCKS5;

/**
 * Spring-friendly factory bean to create Jsch {@link Proxy} objects.
 *
 * @author Gary Russell
 * @since 4.3
 *
 */
public class JschProxyFactoryBean extends AbstractFactoryBean<Proxy> {

	public enum Type {
		HTTP, SOCKS4, SOCKS5
	}

	private final Type type;

	private final String host;

	private final int port;

	private final String user;

	private final String password;

	public JschProxyFactoryBean(Type type, String host, int port, String user, String password) {
		this.type = type;
		this.host = host;
		this.port = port;
		this.user = user;
		this.password = password;
	}

	@Override
	public Class<?> getObjectType() {
		switch (this.type) {
		case SOCKS5:
			return ProxySOCKS5.class;
		case SOCKS4:
			return ProxySOCKS4.class;
		case HTTP:
			return ProxyHTTP.class;
		default:
			throw new IllegalArgumentException("Invalid type:" + this.type);
		}
	}

	@Override
	protected Proxy createInstance() throws Exception {
		switch (this.type) {
		case SOCKS5:
			ProxySOCKS5 socks5proxy = new ProxySOCKS5(this.host, this.port);
			socks5proxy.setUserPasswd(this.user, this.password);
			return socks5proxy;
		case SOCKS4:
			ProxySOCKS4 socks4proxy = new ProxySOCKS4(this.host, this.port);
			socks4proxy.setUserPasswd(this.user, this.password);
			return socks4proxy;
		case HTTP:
			ProxyHTTP httpProxy = new ProxyHTTP(this.host, this.port);
			httpProxy.setUserPasswd(this.user, this.password);
			return httpProxy;
		default:
			throw new IllegalArgumentException("Invalid type:" + this.type);
		}
	}

}
