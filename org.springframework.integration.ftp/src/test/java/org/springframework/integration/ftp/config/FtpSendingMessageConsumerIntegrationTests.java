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

import java.io.File;
import java.io.FilenameFilter;

import org.apache.oro.io.Perl5FilenameFilter;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.springframework.integration.ftp.FtpSendingMessageConsumer;
import org.springframework.integration.ftp.QueuedFTPClientPool;
import org.springframework.integration.message.GenericMessage;

/**
 * @author Iwein Fuld
 */
@Ignore
public class FtpSendingMessageConsumerIntegrationTests {

	private FtpSendingMessageConsumer consumer;

	@Before
	public void initFtpTarget() {
		QueuedFTPClientPool clientPool = new QueuedFTPClientPool();
		clientPool.setHost("localhost");
		clientPool.setUsername("ftp-user");
		clientPool.setPassword("kaas");
		clientPool.setRemoteWorkingDirectory("ftp-test");
		consumer = new FtpSendingMessageConsumer(clientPool);
	}

	@Test
	public void send() throws Exception {
		File file = File.createTempFile("test", "");
		consumer.onMessage(new GenericMessage<File>(file));
	}

	@AfterClass
	public static void deleteTestFiles() {
		File tmpDir = new File(System.getProperty("java.io.tmpdir"));
		File[] files = tmpDir.listFiles((FilenameFilter) new Perl5FilenameFilter("test\\d"));
		for (File file : files) {
			file.delete();
		}
	}

}
