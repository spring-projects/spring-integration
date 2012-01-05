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

package org.springframework.integration.ftp.outbound;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.Message;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.file.remote.handler.FileTransferringMessageHandler;
import org.springframework.integration.ftp.session.AbstractFtpSessionFactory;
import org.springframework.integration.message.GenericMessage;
import org.springframework.util.FileCopyUtils;

/**
 * @author Oleg Zhurakousky
 */
public class FtpSendingMessageHandlerTests {
	
	private static FTPClient ftpClient;
	private TestFtpSessionFactory sessionFactory;
	
	@Before
	public void prepare(){
		ftpClient = mock(FTPClient.class);
		sessionFactory = new TestFtpSessionFactory();
		sessionFactory.setUsername("kermit");
		sessionFactory.setPassword("frog");
		sessionFactory.setHost("foo.com");
		//sessionFactory.setRemoteWorkingDirectory("remote-test-dir");
	}

	@Test
	public void testHandleFileContentMessage() throws Exception {
		File file = new File("remote-target-dir/handlerContent.test");
		if (file.exists()){
			file.delete();
		}
		assertFalse(file.exists());
		FileTransferringMessageHandler<FTPFile> handler = new FileTransferringMessageHandler<FTPFile>(sessionFactory);
		handler.setRemoteDirectoryExpression(new LiteralExpression("remote-target-dir"));
		handler.setFileNameGenerator(new FileNameGenerator() {	
			public String generateFileName(Message<?> message) {
				return "handlerContent.test";
			}
		});
		handler.afterPropertiesSet();
		handler.handleMessage(new GenericMessage<String>("hello"));
		assertTrue(file.exists());
	}

	@Test
	public void testHandleFileAsByte() throws Exception {
		File file = new File("remote-target-dir/handlerContent.test");
		if (file.exists()){
			file.delete();
		}
		assertFalse(file.exists());
		FileTransferringMessageHandler<FTPFile> handler = new FileTransferringMessageHandler<FTPFile>(sessionFactory);
		handler.setRemoteDirectoryExpression(new LiteralExpression("remote-target-dir"));
		handler.setFileNameGenerator(new FileNameGenerator() {	
			public String generateFileName(Message<?> message) {
				return "handlerContent.test";
			}
		});
		handler.afterPropertiesSet();
		handler.handleMessage(new GenericMessage<byte[]>("hello".getBytes()));
		assertTrue(file.exists());
	}
	
	@Test
	public void testHandleFileMessage() throws Exception {
		File targetDir = new File("remote-target-dir");
		assertTrue("target directory does not exist: " + targetDir.getName(), targetDir.exists());

		FileTransferringMessageHandler<FTPFile> handler = new FileTransferringMessageHandler<FTPFile>(sessionFactory);
		handler.setRemoteDirectoryExpression(new LiteralExpression(targetDir.getName()));
		handler.setFileNameGenerator(new FileNameGenerator() {
			public String generateFileName(Message<?> message) {
				return ((File)message.getPayload()).getName() + ".test";
			}
		});
		handler.afterPropertiesSet();

		File srcFile = File.createTempFile("testHandleFileMessage", ".tmp");
		srcFile.deleteOnExit();

		File destFile = new File(targetDir, srcFile.getName() + ".test");
		destFile.deleteOnExit();

		handler.handleMessage(new GenericMessage<File>(srcFile));
		assertTrue("destination file was not created", destFile.exists());
	}


	public static class TestFtpSessionFactory extends AbstractFtpSessionFactory<FTPClient> {

		@Override
		protected FTPClient createClientInstance() {
			try {
				when(ftpClient.getReplyCode()).thenReturn(250);
				when(ftpClient.login("kermit", "frog")).thenReturn(true);
				when(ftpClient.changeWorkingDirectory(Mockito.anyString())).thenReturn(true);
				when(ftpClient.printWorkingDirectory()).thenReturn("remote-target-dir");
				when(ftpClient.storeFile(Mockito.anyString(), Mockito.any(InputStream.class))).thenAnswer(new Answer<Boolean>() {
					public Boolean answer(InvocationOnMock invocation) throws Throwable {
						String fileName = (String) invocation.getArguments()[0];
						InputStream fis = (InputStream) invocation.getArguments()[1];
						FileCopyUtils.copy(fis, new FileOutputStream(fileName));
						return true;
					}
				});
				when(ftpClient.rename(Mockito.anyString(), Mockito.anyString())).thenAnswer(new Answer<Boolean>() {
					public Boolean answer(InvocationOnMock invocation)
							throws Throwable {
						File file = new File((String) invocation.getArguments()[0]);
						File renameToFile = new File((String) invocation.getArguments()[1]);
						file.renameTo(renameToFile);
						return true;
					}
				});
				return ftpClient;
			} catch (Exception e) {
				throw new RuntimeException("Failed to create mock client", e);
			}
		}
	}

}
