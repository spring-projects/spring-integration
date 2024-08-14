/*
 * Copyright 2014-2024 the original author or authors.
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

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ConnectException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.auth.password.PasswordIdentityProvider;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.PropertyResolverUtils;
import org.apache.sshd.common.SshException;
import org.apache.sshd.core.CoreModuleProperties;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.impl.AbstractSftpClient;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.junit.jupiter.api.Test;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Auke Zaaiman
 * @author Darryl Smith
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
		try (SshServer server = SshServer.setUpDefaultServer()) {
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
					f.getSession().connect();
					fail("Expected Exception");
				}
				catch (Exception e) {
					if (e instanceof IllegalStateException && "failed to create SFTP Session".equals(e.getMessage())) {
						if (e.getCause() instanceof IllegalStateException) {
							if (e.getCause().getCause() instanceof ConnectException) {
								assertThat(n++ < 100).as("Server failed to start in 10 seconds").isTrue();
								Thread.sleep(100);
								continue;
							}
						}
					}
					assertThat(e).isInstanceOf(IllegalStateException.class);
					assertThat(e.getCause()).isInstanceOf(SshException.class);
					assertThat(e.getCause().getMessage()).isEqualTo("Server key did not validate");
					break;
				}
			}

			n = 0;
			while (n++ < 100 && server.getActiveSessions().size() > 0) {
				Thread.sleep(100);
			}

			assertThat(server.getActiveSessions().size()).isEqualTo(0);

			f.destroy();
		}
	}

	@Test
	public void concurrentGetSessionDoesntCauseFailure() throws Exception {
		try (SshServer server = SshServer.setUpDefaultServer()) {
			server.setPasswordAuthenticator((arg0, arg1, arg2) -> true);
			server.setPort(0);
			server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(new File("hostkey.ser").toPath()));
			server.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));
			server.start();

			DefaultSftpSessionFactory sftpSessionFactory = new DefaultSftpSessionFactory();
			sftpSessionFactory.setHost("localhost");
			sftpSessionFactory.setPort(server.getPort());
			sftpSessionFactory.setUser("user");
			sftpSessionFactory.setPassword("pass");
			sftpSessionFactory.setAllowUnknownKeys(true);

			List<SftpSession> concurrentSessions = new ArrayList<>();

			AsyncTaskExecutor asyncTaskExecutor = new SimpleAsyncTaskExecutor();
			for (int i = 0; i < 3; i++) {
				asyncTaskExecutor.execute(() -> concurrentSessions.add(sftpSessionFactory.getSession()));
			}

			await().atMost(Duration.ofSeconds(30)).until(() -> concurrentSessions.size() == 3);

			assertThat(concurrentSessions.get(0))
					.isNotEqualTo(concurrentSessions.get(1))
					.isNotEqualTo(concurrentSessions.get(2));

			assertThat(concurrentSessions.get(1)).isNotEqualTo(concurrentSessions.get(2));

			sftpSessionFactory.destroy();
		}
	}

	@Test
	void externallyProvidedSshClientShouldNotHaveItsConfigurationOverwritten() throws Exception {
		try (SshServer server = SshServer.setUpDefaultServer()) {
			server.setPasswordAuthenticator((arg0, arg1, arg2) -> true);
			server.setPort(0);
			server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(new File("hostkey.ser").toPath()));
			server.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));
			server.start();

			SshClient externalClient = SshClient.setUpDefaultClient();
			externalClient.setServerKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE);
			externalClient.setPasswordIdentityProvider(PasswordIdentityProvider.wrapPasswords("pass"));

			DefaultSftpSessionFactory sftpSessionFactory = new DefaultSftpSessionFactory(externalClient, false);
			sftpSessionFactory.setHost("localhost");
			sftpSessionFactory.setPort(server.getPort());
			sftpSessionFactory.setUser("user");

			assertThatNoException().isThrownBy(sftpSessionFactory::getSession);

			sftpSessionFactory.destroy();
		}
	}

	@Test
	void concurrentSessionListDoesntCauseFailure() throws Exception {
		try (SshServer server = SshServer.setUpDefaultServer()) {
			server.setPasswordAuthenticator((arg0, arg1, arg2) -> true);
			server.setPort(0);
			server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(new File("hostkey.ser").toPath()));
			server.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));
			server.start();

			DefaultSftpSessionFactory sftpSessionFactory = new DefaultSftpSessionFactory();
			sftpSessionFactory.setHost("localhost");
			sftpSessionFactory.setPort(server.getPort());
			sftpSessionFactory.setUser("user");
			sftpSessionFactory.setPassword("pass");
			sftpSessionFactory.setAllowUnknownKeys(true);

			SftpSession session = sftpSessionFactory.getSession();

			List<SftpClient.DirEntry[]> dirEntries =
					IntStream.range(0, 10)
							.boxed()
							.parallel()
							.map(i -> {
								try {
									return session.list(".");
								}
								catch (IOException e) {
									throw new UncheckedIOException(e);
								}
							})
							.toList();

			assertThat(dirEntries).hasSize(10);

			sftpSessionFactory.destroy();
		}
	}

	@Test
	void customPropertiesAreApplied() throws Exception {
		try (SshServer server = SshServer.setUpDefaultServer()) {
			server.setPasswordAuthenticator((arg0, arg1, arg2) -> true);
			server.setPort(0);
			server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(new File("hostkey.ser").toPath()));
			server.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));
			server.start();

			DefaultSftpSessionFactory sftpSessionFactory = new DefaultSftpSessionFactory();
			sftpSessionFactory.setHost("localhost");
			sftpSessionFactory.setPort(server.getPort());
			sftpSessionFactory.setUser("user");
			sftpSessionFactory.setPassword("pass");
			sftpSessionFactory.setAllowUnknownKeys(true);
			sftpSessionFactory.setTimeout(15_000);
			sftpSessionFactory.setSshClientConfigurer((sshClient) -> {
				sshClient.setNioWorkers(27);
				PropertyResolverUtils.updateProperty(sshClient, CoreModuleProperties.MAX_PACKET_SIZE.getName(), 48 * 1024);
			});

			ClientChannel clientChannel = sftpSessionFactory.getSession().getClientInstance().getClientChannel();

			assertThat(AbstractSftpClient.SFTP_CLIENT_CMD_TIMEOUT.getRequired(clientChannel)).hasSeconds(15);
			assertThat(CoreModuleProperties.MAX_PACKET_SIZE.getRequired(clientChannel)).isEqualTo(48 * 1024);

			sftpSessionFactory.destroy();
		}
	}

	@Test
	void clientSessionIsClosedOnSessionClose() throws Exception {
		try (SshServer server = SshServer.setUpDefaultServer()) {
			server.setPasswordAuthenticator((arg0, arg1, arg2) -> true);
			server.setPort(0);
			server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(new File("hostkey.ser").toPath()));
			server.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));
			server.start();

			DefaultSftpSessionFactory sftpSessionFactory = new DefaultSftpSessionFactory();
			sftpSessionFactory.setHost("localhost");
			sftpSessionFactory.setPort(server.getPort());
			sftpSessionFactory.setUser("user");
			sftpSessionFactory.setPassword("pass");
			sftpSessionFactory.setAllowUnknownKeys(true);

			SftpSession session = sftpSessionFactory.getSession();
			ClientSession clientSession = session.getClientInstance().getClientSession();

			assertThat(session.isOpen()).isTrue();
			assertThat(clientSession.isOpen()).isTrue();

			session.close();

			assertThat(session.isOpen()).isFalse();
			assertThat(clientSession.isClosed()).isTrue();

			sftpSessionFactory.destroy();
		}
	}

}
