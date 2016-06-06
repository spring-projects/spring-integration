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

package org.springframework.integration.ftp.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.file.DefaultFileNameGenerator;
import org.springframework.integration.file.remote.ClientCallbackWithoutResult;
import org.springframework.integration.file.remote.SessionCallback;
import org.springframework.integration.file.remote.SessionCallbackWithoutResult;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.ftp.FtpTestSupport;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 4.1
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class FtpRemoteFileTemplateTests extends FtpTestSupport {

	@Autowired
	private SessionFactory<FTPFile> sessionFactory;

	@Test
	public void testINT3412AppendStatRmdir() throws IOException {
		FtpRemoteFileTemplate template = new FtpRemoteFileTemplate(sessionFactory);
		DefaultFileNameGenerator fileNameGenerator = new DefaultFileNameGenerator();
		fileNameGenerator.setExpression("'foobar.txt'");
		template.setFileNameGenerator(fileNameGenerator);
		template.setRemoteDirectoryExpression(new LiteralExpression("foo/"));
		template.setUseTemporaryFileName(false);
		template.execute(new SessionCallback<FTPFile, Boolean>() {

			@Override
			public Boolean doInSession(Session<FTPFile> session) throws IOException {
				session.mkdir("foo/");
				return session.mkdir("foo/bar/");
			}

		});
		template.append(new GenericMessage<String>("foo"));
		template.append(new GenericMessage<String>("bar"));
		assertTrue(template.exists("foo/foobar.txt"));
		template.executeWithClient(new ClientCallbackWithoutResult<FTPClient>() {

			@Override
			public void doWithClientWithoutResult(FTPClient client) {
				try {
					FTPFile[] files = client.listFiles("foo/foobar.txt");
					assertEquals(6, files[0].getSize());
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		});
		template.execute(new SessionCallbackWithoutResult<FTPFile>() {

			@Override
			public void doInSessionWithoutResult(Session<FTPFile> session) throws IOException {
				assertTrue(session.remove("foo/foobar.txt"));
				assertTrue(session.rmdir("foo/bar/"));
				FTPFile[] files = session.list("foo/");
				assertEquals(0, files.length);
				assertTrue(session.rmdir("foo/"));
			}
		});
		assertFalse(template.getSession().exists("foo"));
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
		try {
			template.send(new GenericMessage<File>(file));
			fail("exception expected");
		}
		catch (MessagingException e) {
			assertEquals("bar", e.getCause().getMessage());
		}
		File newFile = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
		assertTrue(file.renameTo(newFile));
		file.delete();
		newFile.delete();
	}

	@Configuration
	public static class Config {

		@Bean
		public SessionFactory<FTPFile> ftpSessionFactory() {
			return FtpRemoteFileTemplateTests.sessionFactory();
		}

	}

}
