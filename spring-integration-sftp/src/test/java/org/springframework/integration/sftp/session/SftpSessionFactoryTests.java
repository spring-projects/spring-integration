/*
 * Copyright 2014-2016 the original author or authors.
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.ConnectException;
import java.security.PublicKey;
import java.util.Collections;

import org.apache.sshd.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.sftp.SftpSubsystem;
import org.junit.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.integration.test.util.TestUtils;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.UserInfo;

/**
 * @author Gary Russell
 * @author Artem Bilan
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
			server.setPasswordAuthenticator(new PasswordAuthenticator() {

				@Override
				public boolean authenticate(String arg0, String arg1, ServerSession arg2) {
					return true;
				}
			});
			server.setPort(0);
			server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider("hostkey.ser"));
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
									assertTrue("Server failed to start in 10 seconds", n++ < 100);
									Thread.sleep(100);
									continue;
								}
							}
						}
					}
					assertThat(e, instanceOf(IllegalStateException.class));
					assertThat(e.getCause(), instanceOf(IllegalStateException.class));
					assertThat(e.getCause().getMessage(), equalTo("failed to connect"));
					break;
				}
			}

			n = 0;
			while (n++ < 100 && server.getActiveSessions().size() > 0) {
				Thread.sleep(100);
			}

			assertEquals(0, server.getActiveSessions().size());
		}
		finally {
			server.stop(true);
		}
	}

	@Test
	public void testPasswordPassPhraseViaUserInfo() throws Exception {
		DefaultSftpSessionFactory f = new DefaultSftpSessionFactory();
		f.setUser("user");
		f.setAllowUnknownKeys(true);
		UserInfo ui = mock(UserInfo.class);
		when(ui.getPassword()).thenReturn("pass");
		when(ui.getPassphrase()).thenReturn("pp").thenReturn(null);
		f.setUserInfo(ui);
		UserInfo userInfo = TestUtils.getPropertyValue(f, "userInfoWrapper", UserInfo.class);
		assertEquals("pass", userInfo.getPassword());
		f.setPassword("foo");
		try {
			userInfo.getPassword();
			fail("expected Exception");
		}
		catch (IllegalStateException e) {
			assertThat(e.getMessage(), startsWith("When a 'UserInfo' is provided, 'password' is not allowed"));
		}
		assertEquals("pp", userInfo.getPassphrase());
		f.setPrivateKeyPassphrase("bar");
		try {
			userInfo.getPassphrase();
			fail("expected Exception");
		}
		catch (IllegalStateException e) {
			assertThat(e.getMessage(), startsWith("When a 'UserInfo' is provided, 'privateKeyPassphrase' is not allowed"));
		}
		f.setUserInfo(null);
		assertEquals("foo", userInfo.getPassword());
		assertEquals("bar", userInfo.getPassphrase());
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
			f.setAllowUnknownKeys(true);
			f.getSession().close();
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
			assertThat(e, instanceOf(IllegalStateException.class));
			assertThat(e.getCause(), instanceOf(IllegalStateException.class));
			assertThat(e.getCause().getCause(), instanceOf(JSchException.class));
			assertThat(e.getCause().getCause().getMessage(), containsString("reject HostKey"));
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

	@SuppressWarnings("unchecked")
	private DefaultSftpSessionFactory createServerAndClient(SshServer server) throws IOException {
		server.setPublickeyAuthenticator(new PublickeyAuthenticator() {

			@Override
			public boolean authenticate(String username, PublicKey key, ServerSession session) {
				return true;
			}
		});
		server.setPort(0);
		server.setSubsystemFactories(Collections.<NamedFactory<Command>>singletonList(new SftpSubsystem.Factory()));
		server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider("hostkey.ser"));
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
