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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.PollableChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Iwein Fuld
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class FileToChannelIntegrationTests {

	@Autowired File inputDirectory;

	@Autowired
	PollableChannel fileMessages;

	@Test(timeout = 2000)
	public void fileMessageToChannel() throws Exception {
		File.createTempFile("test", null, inputDirectory).setLastModified(System.currentTimeMillis() - 1000);
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
		File.createTempFile("test", null, inputDirectory).setLastModified(System.currentTimeMillis() - 1000);
		Message<File> received = receiveFileMessage();
		while (received == null) {
			Thread.sleep(5);
			received = receiveFileMessage();
		}
		assertNotNull(received.getPayload());
		assertNull(fileMessages.receive(200));
	}

}
