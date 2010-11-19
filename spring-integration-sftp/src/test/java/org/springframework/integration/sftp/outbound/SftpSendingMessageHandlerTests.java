/*
 * Copyright 2002-2010 the original author or authors.
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

import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;

import org.junit.Test;

import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.sftp.session.SftpSession;
import org.springframework.integration.sftp.session.SftpSessionFactory;

/**
 * @author Oleg Zhurakousky
 */
// there are few validations in this tests, but it is mainly to increase code coverage during CI
public class SftpSendingMessageHandlerTests {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testHandleFileNameMessage() throws Exception {
		SftpSessionFactory sessionFactory = mock(SftpSessionFactory.class);
		SftpSession session = mock(SftpSession.class);
		when(sessionFactory.getSession()).thenReturn(session);
		SftpSendingMessageHandler handler = new SftpSendingMessageHandler(sessionFactory);
		handler.setRemoteDirectoryExpression(new SpelExpressionParser().parseExpression("'foo.txt'"));

		handler.handleMessage(new GenericMessage("hello"));
		verify(sessionFactory, times(1)).getSession();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testHandleFileAsByte() throws Exception {
		SftpSessionFactory sessionFactory = mock(SftpSessionFactory.class);
		SftpSession session = mock(SftpSession.class);
		when(sessionFactory.getSession()).thenReturn(session);
		SftpSendingMessageHandler handler = new SftpSendingMessageHandler(sessionFactory);
		handler.setRemoteDirectoryExpression(new SpelExpressionParser().parseExpression("'foo.txt'"));

		handler.handleMessage(new GenericMessage("hello".getBytes()));
		verify(sessionFactory, times(1)).getSession();
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testHandleFileMessage() throws Exception {
		SftpSessionFactory sessionFactory = mock(SftpSessionFactory.class);
		SftpSession session = mock(SftpSession.class);
		when(sessionFactory.getSession()).thenReturn(session);
		SftpSendingMessageHandler handler = new SftpSendingMessageHandler(sessionFactory);
		handler.setRemoteDirectoryExpression(new SpelExpressionParser().parseExpression("'foo.txt'"));

		handler.handleMessage(new GenericMessage("hello".getBytes()));
		
		File file = File.createTempFile("foo", ".txt");
		handler.handleMessage(new GenericMessage(file));
		verify(sessionFactory, atLeast(1)).getSession();
	}

}
