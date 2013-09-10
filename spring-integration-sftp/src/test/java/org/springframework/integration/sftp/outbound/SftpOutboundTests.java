/*
 * Copyright 2002-2013 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.integration.file.DefaultFileNameGenerator;
import org.springframework.integration.file.remote.FileInfo;
import org.springframework.integration.file.remote.handler.FileTransferringMessageHandler;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.sftp.session.SftpTestSessionFactory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.FileCopyUtils;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.SftpATTRS;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Gunnar Hillert
 */
public class SftpOutboundTests {

	private static com.jcraft.jsch.Session jschSession = mock(com.jcraft.jsch.Session.class);

	@Test
	public void testHandleFileMessage() throws Exception {
		File targetDir = new File("remote-target-dir");
		assertTrue("target directory does not exist: " + targetDir.getName(), targetDir.exists());

		SessionFactory<LsEntry> sessionFactory = new TestSftpSessionFactory();
		FileTransferringMessageHandler<LsEntry> handler = new FileTransferringMessageHandler<LsEntry>(sessionFactory);
		handler.setRemoteDirectoryExpression(new LiteralExpression(targetDir.getName()));
		DefaultFileNameGenerator fGenerator = new DefaultFileNameGenerator();
		fGenerator.setBeanFactory(mock(BeanFactory.class));
		fGenerator.setExpression("payload + '.test'");
		handler.setFileNameGenerator(fGenerator);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

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
		fGenerator.setBeanFactory(mock(BeanFactory.class));
		fGenerator.setExpression("'foo.txt'");
		handler.setFileNameGenerator(fGenerator);
		handler.setRemoteDirectoryExpression(new LiteralExpression("remote-target-dir"));
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

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
		fGenerator.setBeanFactory(mock(BeanFactory.class));
		fGenerator.setExpression("'foo.txt'");
		handler.setFileNameGenerator(fGenerator);
		handler.setRemoteDirectoryExpression(new LiteralExpression("remote-target-dir"));
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

		handler.handleMessage(new GenericMessage<byte[]>("hello".getBytes()));
		assertTrue(new File("remote-target-dir", "foo.txt").exists());
	}

	@Test //INT-2275
	public void testSftpOutboundChannelAdapterInsideChain() throws Exception {
		File targetDir = new File("remote-target-dir");
		assertTrue("target directory does not exist: " + targetDir.getName(), targetDir.exists());

		File srcFile = File.createTempFile("testHandleFileMessage", ".tmp");
		srcFile.deleteOnExit();

		File destFile = new File(targetDir, srcFile.getName());
		destFile.deleteOnExit();

		ApplicationContext context = new ClassPathXmlApplicationContext("SftpOutboundInsideChainTests-context.xml", getClass());

		MessageChannel channel = context.getBean("outboundChannelAdapterInsideChain", MessageChannel.class);

		channel.send(new GenericMessage<File>(srcFile));
		assertTrue("destination file was not created", destFile.exists());
	}

	@Test //INT-2275
	public void testFtpOutboundGatewayInsideChain() throws Exception {
		ApplicationContext context = new ClassPathXmlApplicationContext("SftpOutboundInsideChainTests-context.xml", getClass());

		MessageChannel channel = context.getBean("outboundGatewayInsideChain", MessageChannel.class);

		channel.send(MessageBuilder.withPayload("remote-test-dir").build());

		PollableChannel output = context.getBean("replyChannel", PollableChannel.class);

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
	}

	@Test //INT-2954
	public void testMkDir() throws Exception {
		@SuppressWarnings("unchecked")
		Session<LsEntry> session = mock(Session.class);
		when(session.exists(anyString())).thenReturn(Boolean.FALSE);
		@SuppressWarnings("unchecked")
		SessionFactory<LsEntry> sessionFactory = mock(SessionFactory.class);
		when(sessionFactory.getSession()).thenReturn(session);
		FileTransferringMessageHandler<LsEntry> handler = new FileTransferringMessageHandler<LsEntry>(sessionFactory);
		handler.setAutoCreateDirectory(true);
		handler.setRemoteDirectoryExpression(new LiteralExpression("/foo/bar/baz"));
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		final List<String> madeDirs = new ArrayList<String>();
		doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				madeDirs.add((String) invocation.getArguments()[0]);
				return null;
			}
		}).when(session).mkdir(anyString());
		handler.handleMessage(new GenericMessage<String>("qux"));
		assertEquals(3, madeDirs.size());
		assertEquals("/foo", madeDirs.get(0));
		assertEquals("/foo/bar", madeDirs.get(1));
		assertEquals("/foo/bar/baz", madeDirs.get(2));
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

				String[] files = new File("remote-test-dir").list();
				Vector<LsEntry> sftpEntries = new Vector<LsEntry>();
				for (String fileName : files) {
					LsEntry lsEntry = mock(LsEntry.class);
					SftpATTRS attributes = mock(SftpATTRS.class);
					when(lsEntry.getAttrs()).thenReturn(attributes);
					when(lsEntry.getFilename()).thenReturn(fileName);
					sftpEntries.add(lsEntry);
				}
				when(channel.ls("remote-test-dir/")).thenReturn(sftpEntries);

				when(jschSession.openChannel("sftp")).thenReturn(channel);
				return SftpTestSessionFactory.createSftpSession(jschSession);
			} catch (Exception e) {
				throw new RuntimeException("Failed to create mock sftp session", e);
			}
		}
	}

}
