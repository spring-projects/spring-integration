/*
 * Copyright 2014-present the original author or authors.
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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpVersionSelector;
import org.apache.sshd.sftp.common.SftpException;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.file.DefaultFileNameGenerator;
import org.springframework.integration.file.remote.ClientCallbackWithoutResult;
import org.springframework.integration.file.remote.SessionCallbackWithoutResult;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.sftp.SftpTestSupport;
import org.springframework.integration.test.condition.LogLevels;
import org.springframework.integration.test.support.TestApplicationContextAware;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Darryl Smith
 * @author Glenn Renfro
 *
 * @since 4.1
 */
@SpringJUnitConfig
@DirtiesContext
public class SftpRemoteFileTemplateTests extends SftpTestSupport implements TestApplicationContextAware {

	@Autowired
	private CachingSessionFactory<SftpClient.DirEntry> sessionFactory;

	@LogLevels(level = "trace", categories = {"org.apache.sshd", "org.springframework.integration.sftp"})
	@Test
	public void testINT3412AppendStatRmdir() {
		SftpRemoteFileTemplate template = new SftpRemoteFileTemplate(sessionFactory);
		DefaultFileNameGenerator fileNameGenerator = new DefaultFileNameGenerator();
		fileNameGenerator.setExpression("'foobar.txt'");
		fileNameGenerator.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		template.setFileNameGenerator(fileNameGenerator);
		template.setRemoteDirectoryExpression(new LiteralExpression("/foo/"));
		template.setUseTemporaryFileName(false);
		template.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		template.afterPropertiesSet();

		template.execute(session -> {
			session.mkdir("/foo");
			return session.mkdir("/foo/bar");
		});
		template.append(new GenericMessage<>("foo"));
		template.append(new GenericMessage<>("bar"));
		assertThat(template.exists("foo/foobar.txt")).isTrue();
		template.executeWithClient((ClientCallbackWithoutResult<SftpClient>) client -> {
			try {
				SftpClient.Attributes file = client.stat("foo/foobar.txt");
				assertThat(file.getSize()).isEqualTo(6);
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		template.execute((SessionCallbackWithoutResult<SftpClient.DirEntry>) session -> {
			SftpClient.DirEntry[] files = session.list("/foo");
			assertThat(files.length).isEqualTo(4);
			assertThat(session.remove("/foo/foobar.txt")).isTrue();
			assertThat(session.rmdir("/foo/bar")).isTrue();
			files = session.list("/foo");
			assertThat(files.length).isEqualTo(2);
			List<String> fileNames = Arrays.stream(files).map(SftpClient.DirEntry::getFilename).toList();
			assertThat(fileNames).contains(".", "..");
			assertThat(session.rmdir("/foo")).isTrue();
		});
		assertThat(template.exists("/foo")).isFalse();
	}

	@Test
	public void testNoDeadLockOnSend() {
		CachingSessionFactory<SftpClient.DirEntry> sessionFactory = new CachingSessionFactory<>(sessionFactory(), 1);
		SftpRemoteFileTemplate template = new SftpRemoteFileTemplate(sessionFactory);
		template.setRemoteDirectoryExpression(new LiteralExpression(""));
		template.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		template.setUseTemporaryFileName(false);
		DefaultFileNameGenerator fileNameGenerator = new DefaultFileNameGenerator();
		fileNameGenerator.setExpression("'test.file'");
		fileNameGenerator.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		template.setFileNameGenerator(fileNameGenerator);
		template.afterPropertiesSet();

		template.send(new GenericMessage<>(""));

		assertThatExceptionOfType(MessageDeliveryException.class)
				.isThrownBy(() -> template.send(new GenericMessage<>(""), FileExistsMode.FAIL))
				.withCauseInstanceOf(MessagingException.class)
				.withStackTraceContaining("he destination file already exists at 'test.file'.");

		sessionFactory.destroy();
	}

	@Test
	public void lsUserHome() throws IOException {
		try (Session<SftpClient.DirEntry> session = this.sessionFactory.getSession()) {
			String[] entries = session.listNames("");
			assertThat(entries).contains(".", "sftpSource", "sftpTarget");
		}
	}

	@Test
	public void renameWithOldSftpVersion() throws Exception {
		DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory(false);
		factory.setHost("localhost");
		factory.setPort(port);
		factory.setUser("foo");
		factory.setPassword("foo");
		factory.setAllowUnknownKeys(true);

		SftpSession currentVersionSession = factory.getSession();
		assertThatNoException()
				.isThrownBy(() ->
						currentVersionSession.rename("sftpSource/ sftpSource1.txt", "sftpSource/sftpSource2.txt"));

		currentVersionSession.close();

		factory.setSftpVersionSelector(SftpVersionSelector.MINIMUM);

		SftpSession oldVersionSession = factory.getSession();
		assertThatNoException()
				.isThrownBy(() ->
						oldVersionSession.rename("sftpSource/sftpSource2.txt",
								"sftpSource/subSftpSource/subSftpSource1.txt"));

		oldVersionSession.close();

		factory.destroy();
	}

	@Test
	public void sessionIsNotDirtyOnNoSuchFileError() {
		Session<SftpClient.DirEntry> session = this.sessionFactory.getSession();
		session.close();

		SftpRemoteFileTemplate template = new SftpRemoteFileTemplate(this.sessionFactory);

		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> template.list("No_such_dir"))
				.withRootCauseInstanceOf(SftpException.class)
				.withStackTraceContaining("(SSH_FX_NO_SUCH_FILE): No such file or directory");

		Session<SftpClient.DirEntry> newSession = this.sessionFactory.getSession();
		assertThat(TestUtils.<Object>getPropertyValue(newSession, "targetSession"))
				.isSameAs(TestUtils.getPropertyValue(session, "targetSession"));

		newSession.close();
	}

	@Configuration
	public static class Config {

		@Bean
		public SessionFactory<SftpClient.DirEntry> ftpSessionFactory() {
			return SftpRemoteFileTemplateTests.sessionFactory();
		}

	}

}
