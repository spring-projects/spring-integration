/*
 * Copyright 2002-2011 the original author or authors.
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

import java.io.InputStream;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.Message;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.support.MessageBuilder;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Oleg Zhurakousky
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
	@Test
	public <F> void testEmptyTemporaryFileSuffix() throws Exception {
		SessionFactory<F> sf = mock(SessionFactory.class);
		Session<F> session = mock(Session.class);
		when(sf.getSession()).thenReturn(session);
		ExpressionParser parser = new SpelExpressionParser();
		FileTransferringMessageHandler<F> handler = new FileTransferringMessageHandler<F>(sf);
		handler.setRemoteDirectoryExpression(parser.parseExpression("headers['path']"));
		handler.setTemporaryFileSuffix(null);
		handler.onInit();
		assertTrue(handler.getTemporaryFileSuffix().isEmpty());
		
		handler.setTemporaryFileSuffix("     ");
		handler.onInit();
		assertTrue(handler.getTemporaryFileSuffix().isEmpty());
		
		Message<?> message = MessageBuilder.withPayload("hello").setHeader("path", null).build();
		handler.handleMessage(message);
		verify(session, times(1)).write(Mockito.any(InputStream.class), Mockito.anyString());
		verify(session, times(0)).rename(Mockito.anyString(), Mockito.anyString());
	}

}
