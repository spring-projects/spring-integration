/*
 * Copyright 2014-2020 the original author or authors.
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
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.time.Duration;
import java.util.Collections;

import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.integration.test.util.TestUtils;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.UserInfo;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 3.0.2
 */
public class SftpSessionFactoryTests {

	/*
	 * Verify the socket is closed if the channel.connect() fails.
	 * INT-3305
	 */
	@Test
	public void testConnectFailSocketOpen() throws Exception {
		SshServer server = SshServer.setUpDefaultServer();
		try {
			server.setPasswordAuthenticator((arg0, arg1, arg2) -> true);
			server.setPort(0);
			server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(new File("hostkey.ser").toPath()));
			server.start();

			DefaultSftpSessionFactory f = new DefaultSftpSessionFactory();
			f.setHost("localhost");
			f.setPort(server.getPort());
			f.setUser("user");
			f.setPassword("pass");
			int n = 0;
			while (true) {
				try {
					f.getSession();
					fail("Expected Exception");
				}
				catch (Exception e) {
					if (e instanceof IllegalStateException && "failed to create SFTP Session".equals(e.getMessage())) {
						if (e.getCause() instanceof IllegalStateException) {
							if (e.getCause().getCause() instanceof JSchException) {
								if (e.getCause().getCause().getCause() instanceof ConnectException) {
									assertThat(n++ < 100).as("Server failed to start in 10 seconds").isTrue();
									Thread.sleep(100);
									continue;
								}
							}
						}
					}
					assertThat(e).isInstanceOf(IllegalStateException.class);
					assertThat(e.getCause()).isInstanceOf(IllegalStateException.class);
					assertThat(e.getCause().getMessage()).isEqualTo("failed to connect");
					break;
				}
			}

			n = 0;
			while (n++ < 100 && server.getActiveSessions().size() > 0) {
				Thread.sleep(100);
			}

			assertThat(server.getActiveSessions().size()).isEqualTo(0);
		}
		finally {
			server.stop(true);
		}
	}

	@Test
	public void testPasswordPassPhraseViaUserInfo() {
		DefaultSftpSessionFactory f = new DefaultSftpSessionFactory();
		f.setUser("user");
		f.setAllowUnknownKeys(true);
		UserInfo ui = mock(UserInfo.class);
		when(ui.getPassword()).thenReturn("pass");
		when(ui.getPassphrase()).thenReturn("pp").thenReturn(null);
		f.setUserInfo(ui);
		UserInfo userInfo = TestUtils.getPropertyValue(f, "userInfoWrapper", UserInfo.class);
		assertThat(userInfo.getPassword()).isEqualTo("pass");
		f.setPassword("foo");
		try {
			userInfo.getPassword();
			fail("expected Exception");
		}
		catch (IllegalStateException e) {
			assertThat(e.getMessage()).startsWith("When a 'UserInfo' is provided, 'password' is not allowed");
		}
		assertThat(userInfo.getPassphrase()).isEqualTo("pp");
		f.setPrivateKeyPassphrase("bar");
		try {
			userInfo.getPassphrase();
			fail("expected Exception");
		}
		catch (IllegalStateException e) {
			assertThat(e
					.getMessage()).startsWith("When a 'UserInfo' is provided, 'privateKeyPassphrase' is not allowed");
		}
		f.setUserInfo(null);
		assertThat(userInfo.getPassword()).isEqualTo("foo");
		assertThat(userInfo.getPassphrase()).isEqualTo("bar");
	}

	@Test
	public void testDefaultUserInfoFalse() throws Exception {
		SshServer server = SshServer.setUpDefaultServer();
		try {
			DefaultSftpSessionFactory f = createServerAndClient(server);
			expectReject(f);
		}
		finally {
			server.stop(true);
		}
	}

	@Test
	public void testDefaultUserInfoTrue() throws Exception {
		SshServer server = SshServer.setUpDefaultServer();
		try {
			DefaultSftpSessionFactory f = createServerAndClient(server);
			f.setChannelConnectTimeout(Duration.ofSeconds(6));
			f.setAllowUnknownKeys(true);
			SftpSession session = f.getSession();
			assertThat(TestUtils.getPropertyValue(session, "channelConnectTimeout", Integer.class)).isEqualTo(6_000);
			session.close();
		}
		finally {
			server.stop(true);
		}
	}

	@Test
	public void testCustomUserInfoFalse() throws Exception {
		SshServer server = SshServer.setUpDefaultServer();
		try {
			DefaultSftpSessionFactory f = createServerAndClient(server);
			UserInfo userInfo = mock(UserInfo.class);
			when(userInfo.promptYesNo(anyString())).thenReturn(false);
			f.setUserInfo(userInfo);
			expectReject(f);
		}
		finally {
			server.stop(true);
		}
	}

	private void expectReject(DefaultSftpSessionFactory f) {
		try {
			f.getSession().close();
			fail("Expected Exception");
		}
		catch (Exception e) {
			assertThat(e).isInstanceOf(IllegalStateException.class);
			assertThat(e.getCause()).isInstanceOf(IllegalStateException.class);
			assertThat(e.getCause().getCause()).isInstanceOf(JSchException.class);
			assertThat(e.getCause().getCause().getMessage()).contains("reject HostKey");
		}
	}

	@Test
	public void testCustomUserInfoTrue() throws Exception {
		SshServer server = SshServer.setUpDefaultServer();
		try {
			DefaultSftpSessionFactory f = createServerAndClient(server);
			UserInfo userInfo = mock(UserInfo.class);
			when(userInfo.promptYesNo(anyString())).thenReturn(true);
			f.setUserInfo(userInfo);
			f.getSession().close();
		}
		finally {
			server.stop(true);
		}
	}

	private DefaultSftpSessionFactory createServerAndClient(SshServer server) throws IOException {
		server.setPublickeyAuthenticator((username, key, session) -> true);
		server.setPort(0);
		server.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));
		server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(new File("hostkey.ser").toPath()));
		server.start();

		DefaultSftpSessionFactory f = new DefaultSftpSessionFactory();
		f.setHost("localhost");
		f.setPort(server.getPort());
		f.setUser("user");
		Resource privateKey = new ClassPathResource("id_rsa");
		f.setPrivateKey(privateKey);
		return f;
	}

}
