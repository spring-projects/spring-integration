/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.integration.smb.outbound;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.smb.AbstractBaseTests;
import org.springframework.integration.smb.session.SmbSession;
import org.springframework.integration.smb.session.SmbSessionFactory;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.FileSystemUtils;

import jcifs.smb.SmbFile;

/**
 * @author Markus Spann
 * @author Artem Bilan
 * @author Prafull Kumar Soni
 * @author Gregory Bragg
 */
public class SmbSendingMessageHandlerTests extends AbstractBaseTests {

	private SmbSession smbSession;

	private SmbSessionFactory smbSessionFactory;

	@Before
	public void prepare() {
		smbSession = mock(SmbSession.class);
		smbSessionFactory = new TestSmbSessionFactory();
		smbSessionFactory.setHost("localhost");
		smbSessionFactory.setPort(0);
		smbSessionFactory.setDomain("");
		smbSessionFactory.setUsername("sambaguest");
		smbSessionFactory.setPassword("sambaguest");
		smbSessionFactory.setShareAndDir("smb-share/");
	}

	@After
	public void cleanup() {
		FileSystemUtils.deleteRecursively(new File("remote-target-dir"));
	}

	@Test
	public void testHandleFileContentMessage() {
		File file = createNewFile("remote-target-dir/handlerContent.test");
		SmbMessageHandler handler = new SmbMessageHandler(smbSessionFactory);
		handler.setRemoteDirectoryExpression(new LiteralExpression("remote-target-dir"));
		handler.setFileNameGenerator(message -> "handlerContent.test");
		handler.setAutoCreateDirectory(true);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		handler.handleMessage(new GenericMessage<>("hello"));
		assertFileExists(file);
	}

	@Test
	public void testHandleFileAsByte() {
		File file = createNewFile("remote-target-dir/handlerContent.test");
		SmbMessageHandler handler = new SmbMessageHandler(smbSessionFactory);
		handler.setRemoteDirectoryExpression(new LiteralExpression("remote-target-dir"));
		handler.setFileNameGenerator(message -> "handlerContent.test");
		handler.setAutoCreateDirectory(true);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		handler.handleMessage(new GenericMessage<>("hello".getBytes()));
		assertFileExists(file);
	}

//	@Test
//	public void testHandleFileMessage() throws Exception {
//		File file = createNewFile("remote-target-dir/template.mf.test");
//		SmbMessageHandler handler = new SmbMessageHandler(smbSessionFactory);
//		handler.setRemoteDirectoryExpression(new LiteralExpression("remote-target-dir"));
//		handler.setFileNameGenerator(new FileNameGenerator() {
//			public String generateFileName(Message<?> message) {
//				return ((File) message.getPayload()).getName() + ".test";
//			}
//		});
//		handler.afterPropertiesSet();
//		handler.handleMessage(new GenericMessage<File>(new File("template.mf")));
//		assertFileExists(file);
//	}

	class TestSmbSessionFactory extends SmbSessionFactory {

		@Override
		protected SmbSession createSession() {
			try {
				when(smbSession.remove(Mockito.anyString())).thenReturn(true);
				when(smbSession.list(Mockito.anyString())).thenReturn(new SmbFile[0]);

				doAnswer(new Answer<Object>() {

					@Override
					public Object answer(InvocationOnMock _invocation) throws Throwable {
						String path = (String) _invocation.getArguments()[0];
						OutputStream os = (OutputStream) _invocation.getArguments()[1];
						writeToFile((this.getClass().getSimpleName() + " : TEST : " + path).getBytes(), os);
						return null;
					}
				}).when(smbSession).read(Mockito.anyString(), Mockito.any(OutputStream.class));

				doAnswer(_invocation -> {
					InputStream inputStream = (InputStream) _invocation.getArguments()[0];
					String path = (String) _invocation.getArguments()[1];
					writeToFile(inputStream, path);
					return null;
				}).when(smbSession)
						.write(Mockito.any(InputStream.class), Mockito.anyString());

				// when(smbSession.write(Mockito.any(byte[].class), Mockito.anyString())).thenReturn(null);
				// when(smbSession.write(Mockito.any(File.class), Mockito.anyString())).thenReturn(null);

				doAnswer((Answer<Object>) _invocation -> {
					String path = (String) _invocation.getArguments()[0];
					return new File(path).mkdirs();
				}).when(smbSession).mkdir(Mockito.anyString());

				doAnswer(_invocation -> {
					String pathFrom = (String) _invocation.getArguments()[0];
					String pathTo = (String) _invocation.getArguments()[1];
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
