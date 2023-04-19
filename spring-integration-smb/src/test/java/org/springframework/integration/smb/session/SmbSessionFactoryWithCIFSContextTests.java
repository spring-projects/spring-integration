/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.integration.smb.session;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import jcifs.CIFSContext;
import jcifs.context.SingletonContext;
import jcifs.smb.SmbFile;
import jcifs.util.Strings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.file.remote.handler.FileTransferringMessageHandler;
import org.springframework.integration.smb.AbstractBaseTests;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Gregory Bragg
 * @author Artem Bilan
 */
public class SmbSessionFactoryWithCIFSContextTests extends AbstractBaseTests {

	private SmbSession smbSession;

	private SmbSessionFactory smbSessionFactory;

	@BeforeEach
	public void prepare() {
		smbSession = mock(SmbSession.class);

		smbSessionFactory = new TestSmbSessionFactory(SingletonContext.getInstance());
		assertThat(smbSessionFactory).as("TestSmbSessionFactory object is null.").isNotNull();

		smbSessionFactory.setHost("localhost");
		smbSessionFactory.setPort(445);
		smbSessionFactory.setDomain("");
		smbSessionFactory.setUsername("sambaguest");
		smbSessionFactory.setPassword("sambaguest");
		smbSessionFactory.setShareAndDir("smb-share/");
	}

	@AfterEach
	public void cleanup() {
		FileSystemUtils.deleteRecursively(new File("remote-target-dir"));
	}

	@Test
	public void testHandleFileContentMessage() {
		File file = createNewFile("remote-target-dir/handlerContent.test");
		FileTransferringMessageHandler<?> handler = new FileTransferringMessageHandler<>(smbSessionFactory);
		handler.setRemoteDirectoryExpression(new LiteralExpression("remote-target-dir"));
		handler.setFileNameGenerator(message -> "handlerContent.test");
		handler.setAutoCreateDirectory(true);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		handler.handleMessage(new GenericMessage<>("hello"));
		assertFileExists(file);
	}

	class TestSmbSessionFactory extends SmbSessionFactory {

		private final CIFSContext context;

		protected TestSmbSessionFactory(CIFSContext _context) {
			this.context = _context;
		}

		@Override
		protected SmbSession createSession() {
			try {
				// test for a constructor with a CIFSContext
				SmbShare smbShare = new SmbShare(this, this.context);
				assertThat(smbShare).as("SmbShare object is null.").isNotNull();
				assertThat(smbShare.toString())
						.isEqualTo(Strings.maskSecretValue("smb://sambaguest:sambaguest@localhost:445/smb-share/"));

				// the rest has been copied from SmbSendingMessageHandlerTests
				when(smbSession.remove(Mockito.anyString())).thenReturn(true);
				when(smbSession.list(Mockito.anyString())).thenReturn(new SmbFile[0]);

				doAnswer(_invocation -> {
					String path = _invocation.getArgument(0);
					OutputStream os = (OutputStream) _invocation.getArguments()[1];
					writeToFile((this.getClass().getSimpleName() + " : TEST : " + path).getBytes(), os);
					return null;
				}).when(smbSession).read(Mockito.anyString(), Mockito.any(OutputStream.class));

				doAnswer(_invocation -> {
					InputStream inputStream = _invocation.getArgument(0);
					String path = _invocation.getArgument(1);
					writeToFile(inputStream, path);
					return null;
				}).when(smbSession)
						.write(Mockito.any(InputStream.class), Mockito.anyString());

				doAnswer(_invocation -> {
					String path = _invocation.getArgument(0);
					return new File(path).mkdirs();
				}).when(smbSession).mkdir(Mockito.anyString());

				doAnswer(_invocation -> {
					String pathFrom = _invocation.getArgument(0);
					String pathTo = _invocation.getArgument(1);
					new File(pathFrom).renameTo(new File(pathTo));
					return null;
				}).when(smbSession)
						.rename(Mockito.anyString(), Mockito.anyString());

				doNothing().when(smbSession).close();
				when(smbSession.isOpen()).thenReturn(true);
				return smbSession;
			}
			catch (Exception _ex) {
				throw new RuntimeException("Failed to create mock session.", _ex);
			}
		}

	}

}
