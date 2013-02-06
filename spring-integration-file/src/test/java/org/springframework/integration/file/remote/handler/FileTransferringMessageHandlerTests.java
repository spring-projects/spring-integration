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

package org.springframework.integration.file.remote.handler;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.Message;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.support.MessageBuilder;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Gunnar Hillert
 */
public class FileTransferringMessageHandlerTests {

	@SuppressWarnings("unchecked")
	@Test
	public <F> void testRemoteDirWithEmptyString() throws Exception{
		SessionFactory<F> sf = mock(SessionFactory.class);
		Session<F> session = mock(Session.class);

		when(sf.getSession()).thenReturn(session);
		doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				String path =  (String) invocation.getArguments()[1];
				assertFalse(path.startsWith("/"));
				return null;
			}
		}).when(session).rename(Mockito.anyString(), Mockito.anyString());
		ExpressionParser parser = new SpelExpressionParser();
		FileTransferringMessageHandler<F> handler = new FileTransferringMessageHandler<F>(sf);
		handler.setRemoteDirectoryExpression(parser.parseExpression("''"));
		handler.afterPropertiesSet();
		handler.handleMessage(new GenericMessage<String>("hello"));
		verify(session, times(1)).write(Mockito.any(InputStream.class), Mockito.anyString());
	}

	@SuppressWarnings("unchecked")
	@Test
	public <F> void testTemporaryRemoteDir() throws Exception{
		SessionFactory<F> sf = mock(SessionFactory.class);
		Session<F> session = mock(Session.class);

		final AtomicReference<String> temporaryPath = new AtomicReference<String>();
		final AtomicReference<String> finalPath = new AtomicReference<String>();
		when(sf.getSession()).thenReturn(session);
		doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				temporaryPath.set((String) invocation.getArguments()[0]);
				finalPath.set((String) invocation.getArguments()[1]);
				return null;
			}
		}).when(session).rename(Mockito.anyString(), Mockito.anyString());
		FileTransferringMessageHandler<F> handler = new FileTransferringMessageHandler<F>(sf);
		handler.setRemoteDirectoryExpression(new LiteralExpression("foo"));
		handler.setTemporaryRemoteDirectoryExpression(new LiteralExpression("bar"));
		handler.afterPropertiesSet();
		handler.handleMessage(new GenericMessage<String>("hello"));
		verify(session, times(1)).write(Mockito.any(InputStream.class), Mockito.anyString());
		assertEquals("bar", temporaryPath.get().substring(0, 3));
		assertEquals("foo", finalPath.get().substring(0, 3));
	}

	@SuppressWarnings("unchecked")
	@Test
	public <F> void testRemoteDirWithNull() throws Exception{
		SessionFactory<F> sf = mock(SessionFactory.class);
		Session<F> session = mock(Session.class);

		when(sf.getSession()).thenReturn(session);
		doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				String path =  (String) invocation.getArguments()[1];
				assertFalse(path.startsWith("/"));
				return null;
			}
		}).when(session).rename(Mockito.anyString(), Mockito.anyString());
		ExpressionParser parser = new SpelExpressionParser();
		FileTransferringMessageHandler<F> handler = new FileTransferringMessageHandler<F>(sf);
		handler.setRemoteDirectoryExpression(parser.parseExpression("headers['path']"));
		handler.afterPropertiesSet();
		Message<?> message = MessageBuilder.withPayload("hello").setHeader("path", null).build();
		handler.handleMessage(message);
		verify(session, times(1)).write(Mockito.any(InputStream.class), Mockito.anyString());
	}

	@SuppressWarnings("unchecked")
	@Test(expected=IllegalArgumentException.class)
	public <F> void testEmptyTemporaryFileSuffixCannotBeNull() throws Exception {
		SessionFactory<F> sf = mock(SessionFactory.class);
		Session<F> session = mock(Session.class);
		when(sf.getSession()).thenReturn(session);
		ExpressionParser parser = new SpelExpressionParser();
		FileTransferringMessageHandler<F> handler = new FileTransferringMessageHandler<F>(sf);
		handler.setRemoteDirectoryExpression(parser.parseExpression("headers['path']"));
		handler.setTemporaryFileSuffix(null);
		handler.onInit();
	}

	@SuppressWarnings("unchecked")
	@Test
	public <F> void testUseTemporaryFileNameFalse() throws Exception{
		SessionFactory<F> sf = mock(SessionFactory.class);
		Session<F> session = mock(Session.class);

		when(sf.getSession()).thenReturn(session);

		ExpressionParser parser = new SpelExpressionParser();
		FileTransferringMessageHandler<F> handler = new FileTransferringMessageHandler<F>(sf);
		handler.setRemoteDirectoryExpression(parser.parseExpression("headers['path']"));
		handler.setUseTemporaryFileName(false);
		handler.afterPropertiesSet();
		Message<?> message = MessageBuilder.withPayload("hello").setHeader("path", null).build();
		handler.handleMessage(message);
		verify(session, times(1)).write(Mockito.any(InputStream.class), Mockito.anyString());
		verify(session, times(0)).rename(Mockito.anyString(), Mockito.anyString());
	}

}
