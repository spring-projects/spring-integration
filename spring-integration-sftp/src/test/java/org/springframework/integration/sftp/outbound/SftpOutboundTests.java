/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.sftp.outbound;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.sshd.common.SshConstants;
import org.apache.sshd.common.SshException;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.common.SftpConstants;
import org.apache.sshd.sftp.common.SftpException;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.file.DefaultFileNameGenerator;
import org.springframework.integration.file.remote.FileInfo;
import org.springframework.integration.file.remote.handler.FileTransferringMessageHandler;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.sftp.session.SftpSession;
import org.springframework.integration.sftp.session.SftpTestSessionFactory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.support.TestApplicationContextAware;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.AdditionalMatchers.and;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Darryl Smith
 * @author Glenn Renfro
 */
public class SftpOutboundTests implements TestApplicationContextAware {

	@Test
	public void testHandleFileMessage() throws Exception {
		File targetDir = new File("remote-target-dir");
		assertThat(targetDir.exists()).as("target directory does not exist: " + targetDir.getName()).isTrue();

		TestSftpSessionFactory sessionFactory = new TestSftpSessionFactory();
		FileTransferringMessageHandler<SftpClient.DirEntry> handler =
				new FileTransferringMessageHandler<>(sessionFactory);
		handler.setRemoteDirectoryExpression(new LiteralExpression(targetDir.getName()));
		DefaultFileNameGenerator fGenerator = new DefaultFileNameGenerator();
		fGenerator.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		fGenerator.setExpression("payload.name + '.test'");
		handler.setFileNameGenerator(fGenerator);
		handler.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		handler.afterPropertiesSet();

		File srcFile = File.createTempFile("testHandleFileMessage", ".tmp", new File("."));
		srcFile.deleteOnExit();

		File destFile = new File(targetDir, srcFile.getName() + ".test");
		destFile.deleteOnExit();

		handler.handleMessage(new GenericMessage<>(srcFile));
		assertThat(destFile.exists()).as("destination file was not created").isTrue();

		sessionFactory.destroy();
	}

	@Test
	public void testHandleStringMessage() throws Exception {
		File file = new File("remote-target-dir", "foo.txt");
		if (file.exists()) {
			file.delete();
		}
		TestSftpSessionFactory sessionFactory = new TestSftpSessionFactory();
		FileTransferringMessageHandler<SftpClient.DirEntry> handler =
				new FileTransferringMessageHandler<>(sessionFactory);
		DefaultFileNameGenerator fGenerator = new DefaultFileNameGenerator();
		fGenerator.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		fGenerator.setExpression("'foo.txt'");
		handler.setFileNameGenerator(fGenerator);
		handler.setRemoteDirectoryExpression(new LiteralExpression("remote-target-dir"));
		handler.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		handler.afterPropertiesSet();

		handler.handleMessage(new GenericMessage<>("String data"));
		assertThat(new File("remote-target-dir", "foo.txt").exists()).isTrue();
		byte[] inFile = FileCopyUtils.copyToByteArray(file);
		assertThat(new String(inFile)).isEqualTo("String data");
		file.delete();

		sessionFactory.destroy();
	}

	@Test
	public void testHandleBytesMessage() throws Exception {
		File file = new File("remote-target-dir", "foo.txt");
		if (file.exists()) {
			file.delete();
		}
		TestSftpSessionFactory sessionFactory = new TestSftpSessionFactory();
		FileTransferringMessageHandler<SftpClient.DirEntry> handler =
				new FileTransferringMessageHandler<>(sessionFactory);
		DefaultFileNameGenerator fGenerator = new DefaultFileNameGenerator();
		fGenerator.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		fGenerator.setExpression("'foo.txt'");
		handler.setFileNameGenerator(fGenerator);
		handler.setRemoteDirectoryExpression(new LiteralExpression("remote-target-dir"));
		handler.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		handler.afterPropertiesSet();

		handler.handleMessage(new GenericMessage<>("byte[] data".getBytes()));
		assertThat(new File("remote-target-dir", "foo.txt").exists()).isTrue();
		byte[] inFile = FileCopyUtils.copyToByteArray(file);
		assertThat(new String(inFile)).isEqualTo("byte[] data");
		file.delete();

		sessionFactory.destroy();
	}

	@Test //INT-2275
	public void testSftpOutboundChannelAdapterInsideChain() throws Exception {
		File targetDir = new File("remote-target-dir");
		assertThat(targetDir.exists()).as("target directory does not exist: " + targetDir.getName()).isTrue();

		File srcFile = File.createTempFile("testHandleFileMessage", ".tmp");
		srcFile.deleteOnExit();

		File destFile = new File(targetDir, srcFile.getName());
		destFile.deleteOnExit();

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"SftpOutboundInsideChainTests-context.xml", getClass());

		MessageChannel channel = context.getBean("outboundChannelAdapterInsideChain", MessageChannel.class);

		channel.send(new GenericMessage<>(srcFile));
		assertThat(destFile.exists()).as("destination file was not created").isTrue();
		context.close();
	}

	@Test
	public void testFtpOutboundGatewayInsideChain() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"SftpOutboundInsideChainTests-context.xml", getClass());

		MessageChannel channel = context.getBean("outboundGatewayInsideChain", MessageChannel.class);

		channel.send(MessageBuilder.withPayload("/remote-test-dir").build());

		PollableChannel output = context.getBean("replyChannel", PollableChannel.class);

		Message<?> result = output.receive();
		Object payload = result.getPayload();
		assertThat(payload instanceof List<?>).isTrue();
		@SuppressWarnings("unchecked")
		List<? extends FileInfo<?>> remoteFiles = (List<? extends FileInfo<?>>) payload;
		assertThat(remoteFiles.size()).isEqualTo(3);
		List<String> files = Arrays.asList(new File("remote-test-dir").list());
		for (FileInfo<?> remoteFile : remoteFiles) {
			assertThat(files.contains(remoteFile.getFilename())).isTrue();
		}
		context.close();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testMkDir() throws Exception {
		Session<SftpClient.DirEntry> session = mock(Session.class);
		when(session.exists(anyString())).thenReturn(Boolean.FALSE);
		SessionFactory<SftpClient.DirEntry> sessionFactory = mock(SessionFactory.class);
		when(sessionFactory.getSession()).thenReturn(session);
		FileTransferringMessageHandler<SftpClient.DirEntry> handler =
				new FileTransferringMessageHandler<>(sessionFactory);
		handler.setAutoCreateDirectory(true);
		handler.setRemoteDirectoryExpression(new LiteralExpression("/foo/bar/baz"));
		handler.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		handler.afterPropertiesSet();
		final List<String> madeDirs = new ArrayList<>();
		doAnswer(invocation -> {
			madeDirs.add(invocation.getArgument(0));
			return true;
		}).when(session).mkdir(anyString());
		handler.handleMessage(new GenericMessage<>("qux"));
		assertThat(madeDirs.size()).isEqualTo(3);
		assertThat(madeDirs.get(0)).isEqualTo("/foo");
		assertThat(madeDirs.get(1)).isEqualTo("/foo/bar");
		assertThat(madeDirs.get(2)).isEqualTo("/foo/bar/baz");
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testSharedSession(boolean sharedSession) throws Exception {
		try (SshServer server = SshServer.setUpDefaultServer()) {
			server.setPasswordAuthenticator((arg0, arg1, arg2) -> true);
			server.setPort(0);
			server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(new File("hostkey.ser").toPath()));
			server.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));
			final String pathname = System.getProperty("java.io.tmpdir") + File.separator + "sftptest" + File.separator;
			new File(pathname).mkdirs();
			server.setFileSystemFactory(new VirtualFileSystemFactory(Paths.get(pathname)));
			server.start();

			DefaultSftpSessionFactory f = new DefaultSftpSessionFactory(sharedSession);
			f.setHost("localhost");
			f.setPort(server.getPort());
			f.setUser("user");
			f.setPassword("pass");
			f.setAllowUnknownKeys(true);

			Session<SftpClient.DirEntry> s1 = f.getSession();
			Session<SftpClient.DirEntry> s2 = f.getSession();
			if (sharedSession) {
				assertThat(TestUtils.<Object>getPropertyValue(s2, "sftpClient"))
						.isSameAs(TestUtils.getPropertyValue(s1, "sftpClient"));
			}
			else {
				assertThat(TestUtils.<Object>getPropertyValue(s2, "sftpClient"))
						.isNotSameAs(TestUtils.getPropertyValue(s1, "sftpClient"));
			}

			f.destroy();
		}
	}

	@Test
	public void testExists() throws IOException {
		SftpClient sftpClient = mock(SftpClient.class);

		willReturn("/exist")
				.given(sftpClient)
				.canonicalPath("exist");

		willReturn("/notExist")
				.given(sftpClient)
				.canonicalPath("notExist");

		willReturn(new SftpClient.Attributes())
				.given(sftpClient)
				.stat("/exist");

		willThrow(new SftpException(SftpConstants.SSH_FX_NO_SUCH_FILE, "notExist"))
				.given(sftpClient)
				.stat("/notExist");

		willThrow(new SshException(SshConstants.SSH_OPEN_CONNECT_FAILED, "Connection lost."))
				.given(sftpClient)
				.stat(and(not(eq("/exist")), not(eq("/notExist"))));

		SftpSession sftpSession = new SftpSession(sftpClient);

		assertThat(sftpSession.exists("exist")).isTrue();

		assertThat(sftpSession.exists("notExist")).isFalse();

		assertThatExceptionOfType(UncheckedIOException.class).
				isThrownBy(() -> sftpSession.exists("foo"));
	}

	public static class TestSftpSessionFactory extends DefaultSftpSessionFactory {

		@Override
		public SftpSession getSession() {
			try {
				SftpClient sftpClient = mock(SftpClient.class);

				when(sftpClient.getVersion()).thenReturn(SftpConstants.SFTP_V6);
				doAnswer(invocation -> {
					File file = new File((String) invocation.getArgument(0));
					assertThat(file.getName()).endsWith(".writing");
					return new FileOutputStream(file);
				}).when(sftpClient).write(Mockito.anyString());

				doAnswer(invocation -> {
					File file = new File((String) invocation.getArgument(0));
					assertThat(file.getName()).endsWith(".writing");
					File renameToFile = new File((String) invocation.getArgument(1));
					file.renameTo(renameToFile);
					return null;
				}).when(sftpClient).rename(Mockito.anyString(), Mockito.anyString(), eq(SftpClient.CopyMode.Overwrite));

				String[] files = new File("remote-test-dir").list();
				List<SftpClient.DirEntry> dirEntries =
						Arrays.stream(files)
								.map((file) -> new SftpClient.DirEntry(file, file, new SftpClient.Attributes()))
								.toList();
				when(sftpClient.readDir("/remote-test-dir")).thenReturn(dirEntries);

				return SftpTestSessionFactory.createSftpSession(sftpClient);
			}
			catch (Exception e) {
				throw new RuntimeException("Failed to create mock sftp session", e);
			}
		}

	}

}
