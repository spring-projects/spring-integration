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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.file.DefaultFileNameGenerator;
import org.springframework.integration.file.remote.ClientCallbackWithoutResult;
import org.springframework.integration.file.remote.SessionCallbackWithoutResult;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.sftp.SftpTestSupport;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.1
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class SftpRemoteFileTemplateTests extends SftpTestSupport {

	@Autowired
	private CachingSessionFactory<LsEntry> sessionFactory;

	@Test
	public void testINT3412AppendStatRmdir() {
		SftpRemoteFileTemplate template = new SftpRemoteFileTemplate(sessionFactory);
		DefaultFileNameGenerator fileNameGenerator = new DefaultFileNameGenerator();
		fileNameGenerator.setExpression("'foobar.txt'");
		fileNameGenerator.setBeanFactory(mock(BeanFactory.class));
		template.setFileNameGenerator(fileNameGenerator);
		template.setRemoteDirectoryExpression(new LiteralExpression("foo/"));
		template.setUseTemporaryFileName(false);
		template.setBeanFactory(mock(BeanFactory.class));
		template.afterPropertiesSet();

		template.execute(session -> {
			session.mkdir("foo/");
			return session.mkdir("foo/bar/");
		});
		template.append(new GenericMessage<>("foo"));
		template.append(new GenericMessage<>("bar"));
		assertThat(template.exists("foo/foobar.txt")).isTrue();
		template.executeWithClient((ClientCallbackWithoutResult<ChannelSftp>) client -> {
			try {
				SftpATTRS file = client.lstat("foo/foobar.txt");
				assertThat(file.getSize()).isEqualTo(6);
			}
			catch (SftpException e) {
				throw new RuntimeException(e);
			}
		});
		template.execute((SessionCallbackWithoutResult<LsEntry>) session -> {
			LsEntry[] files = session.list("foo/");
			assertThat(files.length).isEqualTo(4);
			assertThat(session.remove("foo/foobar.txt")).isTrue();
			assertThat(session.rmdir("foo/bar/")).isTrue();
			files = session.list("foo/");
			assertThat(files.length).isEqualTo(2);
			List<LsEntry> list = Arrays.asList(files);
			assertThat(list.stream().map(l -> l.getFilename()).collect(Collectors.toList())).contains(".", "..");
			assertThat(session.rmdir("foo/")).isTrue();
		});
		assertThat(template.exists("foo")).isFalse();
	}

	@Test
	public void testNoDeadLockOnSend() {
		CachingSessionFactory<LsEntry> cachingSessionFactory = new CachingSessionFactory<>(sessionFactory(), 1);
		SftpRemoteFileTemplate template = new SftpRemoteFileTemplate(cachingSessionFactory);
		template.setRemoteDirectoryExpression(new LiteralExpression(""));
		template.setBeanFactory(mock(BeanFactory.class));
		template.setUseTemporaryFileName(false);
		DefaultFileNameGenerator fileNameGenerator = new DefaultFileNameGenerator();
		fileNameGenerator.setExpression("'test.file'");
		fileNameGenerator.setBeanFactory(mock(BeanFactory.class));
		template.setFileNameGenerator(fileNameGenerator);
		template.afterPropertiesSet();

		template.send(new GenericMessage<>(""));

		assertThatExceptionOfType(MessageDeliveryException.class)
				.isThrownBy(() -> template.send(new GenericMessage<>(""), FileExistsMode.FAIL))
				.withCauseInstanceOf(MessagingException.class)
				.withStackTraceContaining("he destination file already exists at 'test.file'.");

		cachingSessionFactory.destroy();
	}

	@Configuration
	public static class Config {

		@Bean
		public SessionFactory<LsEntry> ftpSessionFactory() {
			return SftpRemoteFileTemplateTests.sessionFactory();
		}

	}

}
