/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.integration.sftp.outbound;

import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.file.DefaultFileNameGenerator;
import org.springframework.integration.file.remote.handler.FileTransferringMessageHandler;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.sftp.session.SftpTestSessionFactory;
import org.springframework.util.FileCopyUtils;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;

/**
 * @author Oleg Zhurakousky
 */
public class SftpSendingMessageHandlerTests {
	
	private static com.jcraft.jsch.Session jschSession = mock(com.jcraft.jsch.Session.class);

	@Test
	public void testHandleFileMessage() throws Exception {
		File targetDir = new File("remote-target-dir");
		assertTrue("target directory does not exist: " + targetDir.getName(), targetDir.exists());

		SessionFactory<LsEntry> sessionFactory = new TestSftpSessionFactory();
		FileTransferringMessageHandler<LsEntry> handler = new FileTransferringMessageHandler<LsEntry>(sessionFactory);
		handler.setRemoteDirectoryExpression(new LiteralExpression(targetDir.getName()));
		DefaultFileNameGenerator fGenerator = new DefaultFileNameGenerator();
		fGenerator.setExpression("payload + '.test'");
		handler.setFileNameGenerator(fGenerator);

		File srcFile = File.createTempFile("testHandleFileMessage", ".tmp", new File("."));
		srcFile.deleteOnExit();

		File destFile = new File(targetDir, srcFile.getName() + ".test");
		destFile.deleteOnExit();

		handler.handleMessage(new GenericMessage<File>(srcFile));
		assertTrue("destination file was not created", destFile.exists());
	}

	@Test
	public void testHandleStringMessage() throws Exception {
		File file = new File("remote-target-dir", "foo.txt");
		if (file.exists()){
			file.delete();
		}
		SessionFactory<LsEntry> sessionFactory = new TestSftpSessionFactory();
		FileTransferringMessageHandler<LsEntry> handler = new FileTransferringMessageHandler<LsEntry>(sessionFactory);
		DefaultFileNameGenerator fGenerator = new DefaultFileNameGenerator();
		fGenerator.setExpression("'foo.txt'");
		handler.setFileNameGenerator(fGenerator);
		handler.setRemoteDirectoryExpression(new LiteralExpression("remote-target-dir"));

		handler.handleMessage(new GenericMessage<String>("hello"));
		assertTrue(new File("remote-target-dir", "foo.txt").exists());
	}
	
	@Test
	public void testHandleBytesMessage() throws Exception {
		File file = new File("remote-target-dir", "foo.txt");
		if (file.exists()){
			file.delete();
		}
		SessionFactory<LsEntry> sessionFactory = new TestSftpSessionFactory();
		FileTransferringMessageHandler<LsEntry> handler = new FileTransferringMessageHandler<LsEntry>(sessionFactory);
		DefaultFileNameGenerator fGenerator = new DefaultFileNameGenerator();
		fGenerator.setExpression("'foo.txt'");
		handler.setFileNameGenerator(fGenerator);
		handler.setRemoteDirectoryExpression(new LiteralExpression("remote-target-dir"));

		handler.handleMessage(new GenericMessage<byte[]>("hello".getBytes()));
		assertTrue(new File("remote-target-dir", "foo.txt").exists());
	}


	public static class TestSftpSessionFactory extends DefaultSftpSessionFactory {
		
		@Override
		public Session<LsEntry> getSession() {
			try {
				ChannelSftp channel = mock(ChannelSftp.class);

				doAnswer(new Answer<Object>() {
					public Object answer(InvocationOnMock invocation)
							throws Throwable {	
						File file = new File((String)invocation.getArguments()[1]);
						assertTrue(file.getName().endsWith(".writing"));
						FileCopyUtils.copy((InputStream)invocation.getArguments()[0], new FileOutputStream(file));
						return null;
					}
					
				}).when(channel).put(Mockito.any(InputStream.class), Mockito.anyString());
				
				doAnswer(new Answer<Object>() {
					public Object answer(InvocationOnMock invocation)
							throws Throwable {
						File file = new File((String) invocation.getArguments()[0]);
						assertTrue(file.getName().endsWith(".writing"));
						File renameToFile = new File((String) invocation.getArguments()[1]);
						file.renameTo(renameToFile);
						return null;
					}
					
				}).when(channel).rename(Mockito.anyString(), Mockito.anyString());
				when(jschSession.openChannel("sftp")).thenReturn(channel);
				return SftpTestSessionFactory.createSftpSession(jschSession);
			} catch (Exception e) {
				throw new RuntimeException("Failed to create mock sftp session", e);
			}
		}
	}

}
