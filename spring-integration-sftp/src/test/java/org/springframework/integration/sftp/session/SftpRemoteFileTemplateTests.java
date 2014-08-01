/*
 * Copyright 2014 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.file.DefaultFileNameGenerator;
import org.springframework.integration.file.remote.ClientCallbackWithoutResult;
import org.springframework.integration.file.remote.SessionCallback;
import org.springframework.integration.file.remote.SessionCallbackWithoutResult;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.sftp.TestSftpServer;
import org.springframework.integration.sftp.TestSftpServerConfig;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

/**
 * @author Gary Russell
 * @since 4.1
 *
 */
@ContextConfiguration(classes=TestSftpServerConfig.class)
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class SftpRemoteFileTemplateTests {

	@Autowired
	private TestSftpServer sftpServer;

	@Autowired
	private DefaultSftpSessionFactory sessionFactory;

	@Before
	@After
	public void setup() {
		this.sftpServer.recursiveDelete(sftpServer.getTargetLocalDirectory());
		this.sftpServer.recursiveDelete(sftpServer.getTargetSftpDirectory());
	}

	@Test
	public void testINT3412AppendStatRmdir() {
		SftpRemoteFileTemplate template = new SftpRemoteFileTemplate(sessionFactory);
		DefaultFileNameGenerator fileNameGenerator = new DefaultFileNameGenerator();
		fileNameGenerator.setExpression("'foobar.txt'");
		template.setFileNameGenerator(fileNameGenerator);
		template.setRemoteDirectoryExpression(new LiteralExpression("foo/"));
		template.setUseTemporaryFileName(false);
		template.execute(new SessionCallback<LsEntry, Boolean>() {

			@Override
			public Boolean doInSession(Session<LsEntry> session) throws IOException {
				session.mkdir("foo/");
				return session.mkdir("foo/bar/");
			}

		});
		template.append(new GenericMessage<String>("foo"));
		template.append(new GenericMessage<String>("bar"));
		assertTrue(template.exists("foo/foobar.txt"));
		template.executeWithClient(new ClientCallbackWithoutResult<ChannelSftp>() {

			@Override
			public void doWithClientWithoutResult(ChannelSftp client) {
				try {
					SftpATTRS file = client.lstat("foo/foobar.txt");
					assertEquals(6, file.getSize());
				}
				catch (SftpException e) {
					throw new RuntimeException(e);
				}
			}
		});
		template.execute(new SessionCallbackWithoutResult<LsEntry>() {

			@Override
			public void doInSessionWithoutResult(Session<LsEntry> session) throws IOException {
				assertTrue(session.remove("foo/foobar.txt"));
				assertTrue(session.rmdir("foo/bar/"));
				LsEntry[] files = session.list("foo/");
				assertEquals(0, files.length);
				assertTrue(session.rmdir("foo/"));
			}
		});
		assertFalse(template.exists("foo"));
	}

}
