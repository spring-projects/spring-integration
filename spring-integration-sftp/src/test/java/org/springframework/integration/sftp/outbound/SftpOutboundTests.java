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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.file.DefaultFileNameGenerator;
import org.springframework.integration.file.remote.FileInfo;
import org.springframework.integration.file.remote.handler.FileTransferringMessageHandler;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.sftp.session.SftpSession;
import org.springframework.integration.sftp.session.SftpTestSessionFactory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.FileCopyUtils;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
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

		handler.handleMessage(new GenericMessage<String>("String data"));
		assertTrue(new File("remote-target-dir", "foo.txt").exists());
		byte[] inFile = FileCopyUtils.copyToByteArray(file);
		assertEquals("String data", new String(inFile));
		file.delete();
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

		handler.handleMessage(new GenericMessage<byte[]>("byte[] data".getBytes()));
		assertTrue(new File("remote-target-dir", "foo.txt").exists());
		byte[] inFile = FileCopyUtils.copyToByteArray(file);
		assertEquals("byte[] data", new String(inFile));
		file.delete();
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
			@Override
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

	@Test
	public void testSharedSession() throws Exception {
		JSch jsch = spy(new JSch());
		Constructor<com.jcraft.jsch.Session> ctor = com.jcraft.jsch.Session.class.getDeclaredConstructor(JSch.class, String.class, String.class, int.class);
		ctor.setAccessible(true);
		com.jcraft.jsch.Session jschSession1 = spy(ctor.newInstance(jsch, "foo", "host", 22));
		com.jcraft.jsch.Session jschSession2 = spy(ctor.newInstance(jsch, "foo", "host", 22));
		new DirectFieldAccessor(jschSession1).setPropertyValue("isConnected", true);
		new DirectFieldAccessor(jschSession2).setPropertyValue("isConnected", true);
		when(jsch.getSession("foo", "host", 22)).thenReturn(jschSession1, jschSession2);
		final ChannelSftp channel1 = spy(new ChannelSftp());
		doReturn("channel1").when(channel1).toString();
		final ChannelSftp channel2 = spy(new ChannelSftp());
		doReturn("channel2").when(channel2).toString();
		new DirectFieldAccessor(channel1).setPropertyValue("session", jschSession1);
		new DirectFieldAccessor(channel2).setPropertyValue("session", jschSession1);
		// Can't use when(session.open()) with a spy
		final AtomicInteger n = new AtomicInteger();
		doAnswer(new Answer<ChannelSftp>() {

			@Override
			public ChannelSftp answer(InvocationOnMock invocation) throws Throwable {
				return n.getAndIncrement() == 0 ? channel1 : channel2;
			}
		}).when(jschSession1).openChannel("sftp");
		DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory(jsch, true);
		factory.setHost("host");
		factory.setUser("foo");
		factory.setPassword("bar");
		noopConnect(channel1);
		noopConnect(channel2);
		Session<LsEntry> s1 = factory.getSession();
		Session<LsEntry> s2 = factory.getSession();
		assertSame(TestUtils.getPropertyValue(s1, "jschSession"), TestUtils.getPropertyValue(s2, "jschSession"));
		assertSame(channel1, TestUtils.getPropertyValue(s1, "channel"));
		assertSame(channel2, TestUtils.getPropertyValue(s2, "channel"));
	}

	@Test
	public void testNotSharedSession() throws Exception {
		JSch jsch = spy(new JSch());
		Constructor<com.jcraft.jsch.Session> ctor = com.jcraft.jsch.Session.class.getDeclaredConstructor(JSch.class, String.class, String.class, int.class);
		ctor.setAccessible(true);
		com.jcraft.jsch.Session jschSession1 = spy(ctor.newInstance(jsch, "foo", "host", 22));
		com.jcraft.jsch.Session jschSession2 = spy(ctor.newInstance(jsch, "foo", "host", 22));
		new DirectFieldAccessor(jschSession1).setPropertyValue("isConnected", true);
		new DirectFieldAccessor(jschSession2).setPropertyValue("isConnected", true);
		when(jsch.getSession("foo", "host", 22)).thenReturn(jschSession1, jschSession2);
		ChannelSftp channel1 = spy(new ChannelSftp());
		ChannelSftp channel2 = spy(new ChannelSftp());
		new DirectFieldAccessor(channel1).setPropertyValue("session", jschSession1);
		new DirectFieldAccessor(channel2).setPropertyValue("session", jschSession1);
		doReturn(channel1).when(jschSession1).openChannel("sftp");
		doReturn(channel2).when(jschSession2).openChannel("sftp");
		DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory(jsch, false);
		factory.setHost("host");
		factory.setUser("foo");
		factory.setPassword("bar");
		noopConnect(channel1);
		noopConnect(channel2);
		Session<LsEntry> s1 = factory.getSession();
		Session<LsEntry> s2 = factory.getSession();
		assertNotSame(TestUtils.getPropertyValue(s1, "jschSession"), TestUtils.getPropertyValue(s2, "jschSession"));
		assertSame(channel1, TestUtils.getPropertyValue(s1, "channel"));
		assertSame(channel2, TestUtils.getPropertyValue(s2, "channel"));
	}

	@Test
	public void testSharedSessionCachedReset() throws Exception {
		JSch jsch = spy(new JSch());
		Constructor<com.jcraft.jsch.Session> ctor = com.jcraft.jsch.Session.class.getDeclaredConstructor(JSch.class, String.class, String.class, int.class);
		ctor.setAccessible(true);
		com.jcraft.jsch.Session jschSession1 = spy(ctor.newInstance(jsch, "foo", "host", 22));
		com.jcraft.jsch.Session jschSession2 = spy(ctor.newInstance(jsch, "foo", "host", 22));
		new DirectFieldAccessor(jschSession1).setPropertyValue("isConnected", true);
		new DirectFieldAccessor(jschSession2).setPropertyValue("isConnected", true);
		when(jsch.getSession("foo", "host", 22)).thenReturn(jschSession1, jschSession2);
		final ChannelSftp channel1 = spy(new ChannelSftp());
		doReturn("channel1").when(channel1).toString();
		final ChannelSftp channel2 = spy(new ChannelSftp());
		doReturn("channel2").when(channel2).toString();
		final ChannelSftp channel3 = spy(new ChannelSftp());
		doReturn("channel3").when(channel3).toString();
		final ChannelSftp channel4 = spy(new ChannelSftp());
		doReturn("channel4").when(channel4).toString();
		new DirectFieldAccessor(channel1).setPropertyValue("session", jschSession1);
		new DirectFieldAccessor(channel2).setPropertyValue("session", jschSession1);
		// Can't use when(session.open()) with a spy
		final AtomicInteger n = new AtomicInteger();
		doAnswer(new Answer<ChannelSftp>() {

			@Override
			public ChannelSftp answer(InvocationOnMock invocation) throws Throwable {
				return n.getAndIncrement() == 0 ? channel1 : channel2;
			}
		}).when(jschSession1).openChannel("sftp");
		doAnswer(new Answer<ChannelSftp>() {

			@Override
			public ChannelSftp answer(InvocationOnMock invocation) throws Throwable {
				return n.getAndIncrement() < 3 ? channel3 : channel4;
			}
		}).when(jschSession2).openChannel("sftp");
		DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory(jsch, true);
		factory.setHost("host");
		factory.setUser("foo");
		factory.setPassword("bar");
		CachingSessionFactory<LsEntry> cachedFactory = new CachingSessionFactory<LsEntry>(factory);
		noopConnect(channel1);
		noopConnect(channel2);
		noopConnect(channel3);
		noopConnect(channel4);
		Session<LsEntry> s1 = cachedFactory.getSession();
		Session<LsEntry> s2 = cachedFactory.getSession();
		assertSame(jschSession1, TestUtils.getPropertyValue(s2, "targetSession.jschSession"));
		assertSame(channel1, TestUtils.getPropertyValue(s1, "targetSession.channel"));
		assertSame(channel2, TestUtils.getPropertyValue(s2, "targetSession.channel"));
		assertSame(TestUtils.getPropertyValue(s1, "targetSession.jschSession"), TestUtils.getPropertyValue(s2, "targetSession.jschSession"));
		s1.close();
		Session<LsEntry> s3 = cachedFactory.getSession();
		assertSame(TestUtils.getPropertyValue(s1, "targetSession"), TestUtils.getPropertyValue(s3, "targetSession"));
		assertSame(channel1, TestUtils.getPropertyValue(s3, "targetSession.channel"));
		s3.close();
		cachedFactory.resetCache();
		verify(jschSession1, never()).disconnect();
		s3 = cachedFactory.getSession();
		assertSame(jschSession2, TestUtils.getPropertyValue(s3, "targetSession.jschSession"));
		assertNotSame(TestUtils.getPropertyValue(s1, "targetSession"), TestUtils.getPropertyValue(s3, "targetSession"));
		assertSame(channel3, TestUtils.getPropertyValue(s3, "targetSession.channel"));
		s2.close();
		verify(jschSession1).disconnect();
		s2 = cachedFactory.getSession();
		assertSame(jschSession2, TestUtils.getPropertyValue(s2, "targetSession.jschSession"));
		assertNotSame(TestUtils.getPropertyValue(s3, "targetSession"), TestUtils.getPropertyValue(s2, "targetSession"));
		assertSame(channel4, TestUtils.getPropertyValue(s2, "targetSession.channel"));
		s2.close();
		s3.close();
		verify(jschSession2, never()).disconnect();
		cachedFactory.resetCache();
		verify(jschSession2).disconnect();
	}

	private void noopConnect(ChannelSftp channel1) throws JSchException {
		doAnswer(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return null;
			}
		}).when(channel1).connect();
	}

	public static class TestSftpSessionFactory extends DefaultSftpSessionFactory {

		@Override
		public SftpSession getSession() {
			try {
				ChannelSftp channel = mock(ChannelSftp.class);

				doAnswer(new Answer<Object>() {
					@Override
					public Object answer(InvocationOnMock invocation)
							throws Throwable {
						File file = new File((String)invocation.getArguments()[1]);
						assertTrue(file.getName().endsWith(".writing"));
						FileCopyUtils.copy((InputStream)invocation.getArguments()[0], new FileOutputStream(file));
						return null;
					}

				}).when(channel).put(Mockito.any(InputStream.class), Mockito.anyString());

				doAnswer(new Answer<Object>() {
					@Override
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
