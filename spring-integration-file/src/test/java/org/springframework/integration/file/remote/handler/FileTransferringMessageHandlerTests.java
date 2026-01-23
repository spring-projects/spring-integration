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

package org.springframework.integration.file.remote.handler;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.expression.ExpressionParser;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.support.TestApplicationContextAware;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.util.SimplePool;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Glenn Renfro
 */
public class FileTransferringMessageHandlerTests implements TestApplicationContextAware {

	@SuppressWarnings("unchecked")
	@Test
	public <F> void testRemoteDirWithEmptyString() throws Exception {
		SessionFactory<F> sf = mock(SessionFactory.class);
		Session<F> session = mock(Session.class);

		when(sf.getSession()).thenReturn(session);
		doAnswer(invocation -> {
			String path = invocation.getArgument(1);
			assertThat(path.startsWith("/")).isFalse();
			return null;
		}).when(session).rename(Mockito.anyString(), Mockito.anyString());
		ExpressionParser parser = new SpelExpressionParser();
		FileTransferringMessageHandler<F> handler = new FileTransferringMessageHandler<F>(sf);
		handler.setRemoteDirectoryExpression(parser.parseExpression("''"));
		handler.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		handler.afterPropertiesSet();
		handler.handleMessage(new GenericMessage<String>("hello"));
		verify(session, times(1)).write(Mockito.any(InputStream.class), Mockito.anyString());
	}

	@SuppressWarnings("unchecked")
	@Test
	public <F> void testTemporaryRemoteDir() throws Exception {
		SessionFactory<F> sf = mock(SessionFactory.class);
		Session<F> session = mock(Session.class);

		final AtomicReference<String> temporaryPath = new AtomicReference<String>();
		final AtomicReference<String> finalPath = new AtomicReference<String>();
		when(sf.getSession()).thenReturn(session);
		doAnswer(invocation -> {
			temporaryPath.set(invocation.getArgument(0));
			finalPath.set(invocation.getArgument(1));
			return null;
		}).when(session).rename(Mockito.anyString(), Mockito.anyString());
		FileTransferringMessageHandler<F> handler = new FileTransferringMessageHandler<F>(sf);
		handler.setRemoteDirectoryExpression(new LiteralExpression("foo"));
		handler.setTemporaryRemoteDirectoryExpression(new LiteralExpression("bar"));
		handler.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		handler.afterPropertiesSet();
		handler.handleMessage(new GenericMessage<String>("hello"));
		verify(session, times(1)).write(Mockito.any(InputStream.class), Mockito.anyString());
		assertThat(temporaryPath.get().substring(0, 3)).isEqualTo("bar");
		assertThat(finalPath.get().substring(0, 3)).isEqualTo("foo");
	}

	@SuppressWarnings("unchecked")
	@Test
	public <F> void testRemoteDirWithNull() throws Exception {
		SessionFactory<F> sf = mock(SessionFactory.class);
		Session<F> session = mock(Session.class);

		when(sf.getSession()).thenReturn(session);
		doAnswer(invocation -> {
			String path = invocation.getArgument(1);
			assertThat(path.startsWith("/")).isFalse();
			return null;
		}).when(session).rename(Mockito.anyString(), Mockito.anyString());
		ExpressionParser parser = new SpelExpressionParser();
		FileTransferringMessageHandler<F> handler = new FileTransferringMessageHandler<F>(sf);
		handler.setRemoteDirectoryExpression(parser.parseExpression("headers['path']"));
		handler.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		handler.afterPropertiesSet();
		Message<?> message = MessageBuilder.withPayload("hello").setHeader("path", null).build();
		handler.handleMessage(message);
		verify(session, times(1)).write(Mockito.any(InputStream.class), Mockito.anyString());
	}

	@SuppressWarnings("unchecked")
	@Test
	public <F> void testEmptyTemporaryFileSuffixCannotBeNull() throws Exception {
		SessionFactory<F> sf = mock(SessionFactory.class);
		Session<F> session = mock(Session.class);
		when(sf.getSession()).thenReturn(session);
		FileTransferringMessageHandler<F> handler = new FileTransferringMessageHandler<F>(sf);
		handler.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		handler.setRemoteDirectoryExpressionString("headers['path']");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> handler.setTemporaryFileSuffix(null));
	}

	@SuppressWarnings("unchecked")
	@Test
	public <F> void testUseTemporaryFileNameFalse() throws Exception {
		SessionFactory<F> sf = mock(SessionFactory.class);
		Session<F> session = mock(Session.class);

		when(sf.getSession()).thenReturn(session);

		ExpressionParser parser = new SpelExpressionParser();
		FileTransferringMessageHandler<F> handler = new FileTransferringMessageHandler<F>(sf);
		handler.setRemoteDirectoryExpression(parser.parseExpression("headers['path']"));
		handler.setUseTemporaryFileName(false);
		handler.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		handler.afterPropertiesSet();
		Message<?> message = MessageBuilder.withPayload("hello").setHeader("path", null).build();
		handler.handleMessage(message);
		verify(session, times(1)).write(Mockito.any(InputStream.class), Mockito.anyString());
		verify(session, times(0)).rename(Mockito.anyString(), Mockito.anyString());
	}

	@SuppressWarnings("unchecked")
	@Test
	public <F> void testServerException() throws Exception {
		SessionFactory<F> sf = mock(SessionFactory.class);
		CachingSessionFactory<F> csf = new CachingSessionFactory<F>(sf, 2);
		FileTransferringMessageHandler<F> handler = new FileTransferringMessageHandler<F>(csf);
		handler.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		Session<F> session1 = newSession();
		Session<F> session2 = newSession();
		Session<F> session3 = newSession();
		when(sf.getSession()).thenReturn(session1, session2, session3);
		handler.setRemoteDirectoryExpressionString("'foo'");
		handler.afterPropertiesSet();
		for (int i = 0; i < 3; i++) {
			try {
				handler.handleMessage(new GenericMessage<String>("hello"));
			}
			catch (Exception e) {
				assertThat(e.getCause().getCause().getMessage()).isEqualTo("test");
			}
		}
		verify(session1, times(1)).write(Mockito.any(InputStream.class), Mockito.anyString());
		verify(session2, times(1)).write(Mockito.any(InputStream.class), Mockito.anyString());
		verify(session3, times(1)).write(Mockito.any(InputStream.class), Mockito.anyString());
		SimplePool<?> pool = TestUtils.getPropertyValue(csf, "pool");
		assertThat(pool.getAllocatedCount()).isEqualTo(1);
		assertThat(pool.getIdleCount()).isEqualTo(1);
		assertThat(TestUtils.<Set<?>>getPropertyValue(pool, "allocated").iterator().next()).isSameAs(session3);
	}

	private <F> Session<F> newSession() throws IOException {
		@SuppressWarnings("unchecked")
		Session<F> session = mock(Session.class);
		doThrow(new IOException("test")).when(session).write(any(InputStream.class), anyString());
		when(session.isOpen()).thenReturn(false);
		return session;
	}

}
