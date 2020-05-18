/*
 * Copyright 2016-2020 the original author or authors.
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

package org.springframework.integration.sftp.session;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.integration.test.util.TestUtils;

import com.jcraft.jsch.Proxy;
import com.jcraft.jsch.ProxyHTTP;
import com.jcraft.jsch.ProxySOCKS4;
import com.jcraft.jsch.ProxySOCKS5;

/**
 * @author Gary Russell
 * @since 4.3
 *
 */
public class ProxyTests {

	@Test
	@Disabled // TODO Use SftpTestSupport
	/*
	 * Needs host and account
	 */
	public void testSimpleConnect() {
		DefaultSftpSessionFactory sf = new DefaultSftpSessionFactory();
		sf.setHost("10.0.0.3");
		sf.setPort(22);
		sf.setUser("ftptest");
		sf.setPassword("ftptest");
		sf.setAllowUnknownKeys(true);
		sf.getSession().close();
	}

	@Test
	@Disabled
	/*
	 * Needs host and account and...
	 * $ ssh -D 1080 -f -N gpr@10.0.0.3
	 */
	public void testProxyConnect() throws Exception {
		DefaultSftpSessionFactory sf = new DefaultSftpSessionFactory();
		JschProxyFactoryBean proxyFactoryBean = new JschProxyFactoryBean(JschProxyFactoryBean.Type.SOCKS5, "localhost",
				1080, "ftptest", "ftptest");
		proxyFactoryBean.afterPropertiesSet();
		sf.setHost("10.0.0.3");
		sf.setPort(22);
		sf.setUser("ftptest");
		sf.setPassword("ftptest");
		sf.setProxy(proxyFactoryBean.getObject());
		sf.setAllowUnknownKeys(true);
		sf.getSession().close();
	}

	@Test
	public void testFactoryBean() throws Exception {
		JschProxyFactoryBean proxyFactoryBean = new JschProxyFactoryBean(JschProxyFactoryBean.Type.SOCKS5, "localhost",
				1080, "ftptest", "pass");
		proxyFactoryBean.afterPropertiesSet();
		Proxy proxy = proxyFactoryBean.getObject();
		assertProxy(proxy, ProxySOCKS5.class);

		proxyFactoryBean = new JschProxyFactoryBean(JschProxyFactoryBean.Type.SOCKS4, "localhost",
				1080, "ftptest", "pass");
		proxyFactoryBean.afterPropertiesSet();
		proxy = proxyFactoryBean.getObject();
		assertProxy(proxy, ProxySOCKS4.class);

		proxyFactoryBean = new JschProxyFactoryBean(JschProxyFactoryBean.Type.HTTP, "localhost",
				1080, "ftptest", "pass");
		proxyFactoryBean.afterPropertiesSet();
		proxy = proxyFactoryBean.getObject();
		assertProxy(proxy, ProxyHTTP.class);
	}

	private void assertProxy(Proxy proxy, Class<? extends Proxy> clazz) {
		assertThat(proxy).isInstanceOf(clazz);
		assertThat(TestUtils.getPropertyValue(proxy, "user")).isEqualTo("ftptest");
		assertThat(TestUtils.getPropertyValue(proxy, "passwd")).isEqualTo("pass");
	}

}
