/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.ftp.config;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import org.springframework.integration.ftp.FtpSource;
import org.springframework.integration.ftp.QueuedFTPClientPool;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageCreator;

/**
 * @author Iwein Fuld
 */
/*
 * These tests assume you have a local ftp server running. The whole class
 * should be disabled and only run when you have started your ftp server and are
 * in need of experimenting.
 * 
 * To pass the test you should have an ftp server running at localhost that
 * accepts a login for ftp-user/kaas and has a remote directory ftp-test with at
 * least one file in it. Nothing is stopping you from changing the code to your
 * needs of course, this is just a starting point for local testing.
 */
// ftp server dependency. comment away Ignore if you want to run this
@Ignore
public class FtpSourceIntegrationTests {

	private static File localWorkDir;

	private FtpSource ftpSource;

	private MessageCreator<List<File>, List<File>> messageCreator = new MessageCreator<List<File>, List<File>>() {
		public Message<List<File>> createMessage(List<File> object) {
			return new GenericMessage<List<File>>(object);
		}
	};


	@BeforeClass
	public static void initializeEnvironment() {
		localWorkDir = new File(System.getProperty("java.io.tmpdir") + "/" + FtpSourceIntegrationTests.class.getName());
		localWorkDir.mkdir();
	}

	@Before
	public void initializeFtpSource() throws Exception {
		QueuedFTPClientPool queuedFTPClientPool = new QueuedFTPClientPool();
		ftpSource = new FtpSource(messageCreator, queuedFTPClientPool);
		queuedFTPClientPool.setHost("localhost");
		queuedFTPClientPool.setUsername("ftp-user");
		queuedFTPClientPool.setPassword("kaas");
		ftpSource.setLocalWorkingDirectory(localWorkDir);
		queuedFTPClientPool.setRemoteWorkingDirectory("ftp-test");
	}

	@Test
	public void receive() {
		Message<List<File>> received = ftpSource.receive();
		assertTrue(received.getPayload().iterator().next().exists());
	}

}
