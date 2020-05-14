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

package org.springframework.integration.ftp.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.file.DefaultFileNameGenerator;
import org.springframework.integration.file.remote.ClientCallbackWithoutResult;
import org.springframework.integration.file.remote.SessionCallbackWithoutResult;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.ftp.FtpTestSupport;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.util.SimplePool;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.1
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class FtpRemoteFileTemplateTests extends FtpTestSupport {

	@Autowired
	private SessionFactory<FTPFile> sessionFactory;

	@Test
	public void testINT3412AppendStatRmdir() {
		FtpRemoteFileTemplate template = new FtpRemoteFileTemplate(sessionFactory);
		DefaultFileNameGenerator fileNameGenerator = new DefaultFileNameGenerator();
		fileNameGenerator.setBeanFactory(mock(BeanFactory.class));
		fileNameGenerator.setExpression("'foobar.txt'");
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
		template.executeWithClient((ClientCallbackWithoutResult<FTPClient>) client -> {
			try {
				FTPFile[] files = client.listFiles("foo/foobar.txt");
				assertThat(files[0].getSize()).isEqualTo(6);
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		template.execute((SessionCallbackWithoutResult<FTPFile>) session -> {
			assertThat(session.remove("foo/foobar.txt")).isTrue();
			assertThat(session.rmdir("foo/bar/")).isTrue();
			FTPFile[] files = session.list("foo/");
			assertThat(files.length).isEqualTo(0);
			assertThat(session.rmdir("foo/")).isTrue();
		});
		assertThat(template.exists("foo")).isFalse();
	}

	@Test
	public void testFileCloseOnBadConnect() throws Exception {
		@SuppressWarnings("unchecked")
		SessionFactory<FTPFile> sessionFactory = mock(SessionFactory.class);
		when(sessionFactory.getSession()).thenThrow(new RuntimeException("bar"));
		FtpRemoteFileTemplate template = new FtpRemoteFileTemplate(sessionFactory);
		template.setRemoteDirectoryExpression(new LiteralExpression("foo"));
		template.afterPropertiesSet();
		File file = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
		FileOutputStream fileOutputStream = new FileOutputStream(file);
		fileOutputStream.write("foo".getBytes());
		fileOutputStream.close();
		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> template.send(new GenericMessage<>(file)))
				.withStackTraceContaining("bar");
		File newFile = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
		assertThat(file.renameTo(newFile)).isTrue();
		file.delete();
		newFile.delete();
	}

	@Test
	public void testConnectionClosedAfterExists() throws Exception {
		FtpRemoteFileTemplate template = new FtpRemoteFileTemplate(this.sessionFactory);
		template.setRemoteDirectoryExpression(new LiteralExpression("/"));
		template.setExistsMode(FtpRemoteFileTemplate.ExistsMode.NLST_AND_DIRS);
		template.setBeanFactory(mock(BeanFactory.class));
		template.afterPropertiesSet();
		File file = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
		FileOutputStream fileOutputStream = new FileOutputStream(file);
		fileOutputStream.write("foo".getBytes());
		fileOutputStream.close();
		template.send(new GenericMessage<>(file), FileExistsMode.IGNORE);
		File newFile = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
		assertThat(file.renameTo(newFile)).isTrue();
		file.delete();
		newFile.delete();

		SimplePool<?> pool = TestUtils.getPropertyValue(this.sessionFactory, "pool", SimplePool.class);
		assertThat(pool.getActiveCount()).isEqualTo(0);
	}

	@Configuration
	public static class Config {

		@Bean
		public SessionFactory<FTPFile> ftpSessionFactory() {
			return FtpRemoteFileTemplateTests.sessionFactory();
		}

	}

}
