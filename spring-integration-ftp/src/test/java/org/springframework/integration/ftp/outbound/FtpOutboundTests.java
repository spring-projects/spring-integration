/*
 * Copyright 2002-2014 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.file.remote.FileInfo;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.handler.FileTransferringMessageHandler;
import org.springframework.integration.ftp.session.AbstractFtpSessionFactory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.FileCopyUtils;

/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gunnar Hillert
 * @author Gary Russell
 */
public class FtpOutboundTests {

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
			@Override
			public String generateFileName(Message<?> message) {
				return "handlerContent.test";
			}
		});
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		handler.handleMessage(new GenericMessage<String>("String data"));
		assertTrue(file.exists());
		byte[] inFile = FileCopyUtils.copyToByteArray(file);
		assertEquals("String data", new String(inFile));
		file.delete();
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
			@Override
			public String generateFileName(Message<?> message) {
				return "handlerContent.test";
			}
		});
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		handler.handleMessage(new GenericMessage<byte[]>("byte[] data".getBytes()));
		assertTrue(file.exists());
		byte[] inFile = FileCopyUtils.copyToByteArray(file);
		assertEquals("byte[] data", new String(inFile));
		file.delete();
	}

	@Test
	public void testHandleFileMessage() throws Exception {
		File targetDir = new File("remote-target-dir");
		assertTrue("target directory does not exist: " + targetDir.getName(), targetDir.exists());

		FileTransferringMessageHandler<FTPFile> handler = new FileTransferringMessageHandler<FTPFile>(sessionFactory);
		handler.setRemoteDirectoryExpression(new LiteralExpression(targetDir.getName()));
		handler.setFileNameGenerator(new FileNameGenerator() {
			@Override
			public String generateFileName(Message<?> message) {
				return ((File)message.getPayload()).getName() + ".test";
			}
		});
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

		File srcFile = File.createTempFile("testHandleFileMessage", ".tmp");
		srcFile.deleteOnExit();

		File destFile = new File(targetDir, srcFile.getName() + ".test");
		destFile.deleteOnExit();

		handler.handleMessage(new GenericMessage<File>(srcFile));
		assertTrue("destination file was not created", destFile.exists());
	}

	@Test
	public void testHandleMissingFileMessage() throws Exception {
		File targetDir = new File("remote-target-dir");
		assertTrue("target directory does not exist: " + targetDir.getName(), targetDir.exists());

		FileTransferringMessageHandler<FTPFile> handler = new FileTransferringMessageHandler<FTPFile>(sessionFactory);
		handler.setRemoteDirectoryExpression(new LiteralExpression(targetDir.getName()));
		handler.setFileNameGenerator(new FileNameGenerator() {
			@Override
			public String generateFileName(Message<?> message) {
				return ((File)message.getPayload()).getName() + ".test";
			}
		});
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

		File srcFile = new File(UUID.randomUUID() + ".txt");

		Log logger = spy(TestUtils.getPropertyValue(handler, "remoteFileTemplate.logger", Log.class));
		when(logger.isWarnEnabled()).thenReturn(true);
		final AtomicReference<String> logged = new AtomicReference<String>();
		doAnswer(new Answer<Object>(){

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				logged.set((String) invocation.getArguments()[0]);
				invocation.callRealMethod();
				return null;
			}
		}).when(logger).warn(Mockito.anyString());
		RemoteFileTemplate<?> template = TestUtils.getPropertyValue(handler, "remoteFileTemplate", RemoteFileTemplate.class);
		new DirectFieldAccessor(template).setPropertyValue("logger", logger);
		handler.handleMessage(new GenericMessage<File>(srcFile));
		assertNotNull(logged.get());
		assertEquals("File " + srcFile.toString() + " does not exist", logged.get());
	}

	@Test //INT-2275
	public void testFtpOutboundChannelAdapterInsideChain() throws Exception {
		File targetDir = new File("remote-target-dir");
		assertTrue("target directory does not exist: " + targetDir.getName(), targetDir.exists());

		File srcFile = File.createTempFile("testHandleFileMessage", ".tmp");
		srcFile.deleteOnExit();

		File destFile = new File(targetDir, srcFile.getName());
		destFile.deleteOnExit();

		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"FtpOutboundInsideChainTests-context.xml", getClass());

		MessageChannel channel = context.getBean("outboundChainChannel", MessageChannel.class);

		channel.send(new GenericMessage<File>(srcFile));
		assertTrue("destination file was not created", destFile.exists());
		context.close();
	}

	@Test //INT-2275
	public void testFtpOutboundGatewayInsideChain() throws Exception {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"FtpOutboundInsideChainTests-context.xml", getClass());

		MessageChannel channel = context.getBean("ftpOutboundGatewayInsideChain", MessageChannel.class);

		channel.send(MessageBuilder.withPayload("remote-test-dir").build());

		PollableChannel output = context.getBean("output", PollableChannel.class);

		Message<?> result = output.receive();
		Object payload = result.getPayload();
		assertTrue(payload instanceof List<?>);
		@SuppressWarnings("unchecked")
		List<? extends FileInfo<?>> remoteFiles = (List<? extends FileInfo<?>>) payload;
		assertEquals(3, remoteFiles.size());
		List<String> files = Arrays.asList(new File("remote-test-dir").list());
		for (FileInfo<?> remoteFile : remoteFiles) {
			assertTrue(files.contains(remoteFile.getFilename()));
		}
		context.close();
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
					@Override
					public Boolean answer(InvocationOnMock invocation) throws Throwable {
						String fileName = (String) invocation.getArguments()[0];
						InputStream fis = (InputStream) invocation.getArguments()[1];
						FileCopyUtils.copy(fis, new FileOutputStream(fileName));
						return true;
					}
				});
				when(ftpClient.rename(Mockito.anyString(), Mockito.anyString())).thenAnswer(new Answer<Boolean>() {
					@Override
					public Boolean answer(InvocationOnMock invocation)
							throws Throwable {
						File file = new File((String) invocation.getArguments()[0]);
						File renameToFile = new File((String) invocation.getArguments()[1]);
						file.renameTo(renameToFile);
						return true;
					}
				});
				String[] files = new File("remote-test-dir").list();
				Collection<Object> ftpFiles = new ArrayList<Object>();
				for (String fileName : files) {
					FTPFile file = new FTPFile();
					file.setName(fileName);
					file.setType(FTPFile.FILE_TYPE);
					file.setTimestamp(Calendar.getInstance());
					ftpFiles.add(file);
					when(ftpClient.retrieveFile(Mockito.eq("remote-test-dir/" + fileName) , Mockito.any(OutputStream.class))).thenReturn(true);
				}
				when(ftpClient.listFiles("remote-test-dir/")).thenReturn(ftpFiles.toArray(new FTPFile[]{}));
				return ftpClient;
			} catch (Exception e) {
				throw new RuntimeException("Failed to create mock client", e);
			}
		}
	}

}
