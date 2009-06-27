/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.file;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.core.Message;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Iwein Fuld
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class FileToChannelIntegrationTests {

	private static File inputDir;

	@Autowired
	@Qualifier("fileMessages")
	PollableChannel fileMessages;

	@BeforeClass
	public static void setupInputDir() {
		inputDir = new File(System.getProperty("java.io.tmpdir") + "/"
				+ FileToChannelIntegrationTests.class.getSimpleName());
		inputDir.mkdir();
	}

	@After
	public void cleanoutInputDir() throws Exception {
		File[] listFiles = inputDir.listFiles();
		for (int i = 0; i < listFiles.length; i++) {
			listFiles[i].delete();
		}
	}

	@AfterClass
	public static void removeInputDir() throws Exception {
		inputDir.delete();
	}

	@Test(timeout = 2000)
	public void fileMessageToChannel() throws Exception {
		File.createTempFile("test", null, inputDir).setLastModified(System.currentTimeMillis() - 1000);
		Message<File> received = receiveFileMessage();
		while (received == null) {
			Thread.sleep(50);
			received = receiveFileMessage();
		}
		assertNotNull(received.getPayload());
	}

	@SuppressWarnings("unchecked")
	private Message<File> receiveFileMessage() {
		return (Message<File>) fileMessages.receive();
	}

	@Test(timeout = 2000)
	public void directoryExhaustion() throws Exception {
		File.createTempFile("test", null, inputDir).setLastModified(System.currentTimeMillis() - 1000);
		Message<File> received = receiveFileMessage();
		while (received == null) {
			Thread.sleep(5);
			received = receiveFileMessage();
		}
		assertNotNull(received.getPayload());
		assertNull(fileMessages.receive(200));
	}

}
