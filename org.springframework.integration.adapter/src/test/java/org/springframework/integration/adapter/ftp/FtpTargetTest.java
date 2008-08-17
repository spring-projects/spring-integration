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
package org.springframework.integration.adapter.ftp;

import static org.easymock.classextension.EasyMock.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;

import org.apache.commons.net.ftp.FTPClient;
import org.junit.Before;
import org.junit.Test;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageMapper;

/**
 * 
 * @author Iwein Fuld
 * 
 */
@SuppressWarnings("unchecked")
public class FtpTargetTest {

	private FtpTarget ftpTarget;

	// Mocks and initialization
	private MessageMapper<?, File> messageMapper = createMock(MessageMapper.class);

	private Message message = createMock(Message.class);

	private FTPClient ftpClient = createMock(FTPClient.class);

	/*
	 * We don't want tests to worry about interaction with the pool (with the
	 * exception of one dedicated test), so let's make the pool as transparent
	 * as possible.
	 */
	private FTPClientPool ftpClientPool = createNiceMock(FTPClientPool.class);

	@Before
	public void liberalPool() throws Exception {
		expect(ftpClientPool.getClient()).andReturn(ftpClient).anyTimes();
	}

	/*
	 * Handle to all mocks in this test so you can't forget to include one in a
	 * replay, verify or reset call.
	 */
	private Object[] allMocks = new Object[] { messageMapper, message, ftpClient, ftpClientPool };

	@Before
	public void intitializeSubject() {
		this.ftpTarget = new FtpTarget(messageMapper, ftpClientPool);
	}

	// Tests
	@Test
	public void send() throws Exception {
		expect(messageMapper.mapMessage(message)).andReturn(File.createTempFile("test", ".tmp"));
		expect(ftpClient.storeFile(isA(String.class), isA(FileInputStream.class))).andReturn(true);
		replay(allMocks);
		boolean sent = ftpTarget.send(message);
		assertTrue(sent);
		verify(allMocks);
	}

	@Test
	public void sendFailed_negative() throws Exception {
		expect(messageMapper.mapMessage(message)).andReturn(File.createTempFile("test", ".tmp"));
		expect(ftpClient.storeFile(isA(String.class), isA(FileInputStream.class))).andReturn(false);
		replay(allMocks);
		boolean sent = ftpTarget.send(message);
		assertFalse(sent);
		verify(allMocks);
	}

	@Test
	public void sendFailed_() throws Exception {

	}
}
