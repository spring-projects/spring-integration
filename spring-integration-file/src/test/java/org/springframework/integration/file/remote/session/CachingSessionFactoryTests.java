/*
 * Copyright 2013-present the original author or authors.
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

package org.springframework.integration.file.remote.session;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.jupiter.api.Test;

import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.test.support.TestApplicationContextAware;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 3.0
 *
 */
public class CachingSessionFactoryTests implements TestApplicationContextAware {

	@Test
	public void testCacheAndReset() {
		TestSessionFactory factory = new TestSessionFactory();
		CachingSessionFactory<String> cache = new CachingSessionFactory<>(factory);
		cache.setTestSession(true);
		Session<String> sess1 = cache.getSession();
		assertThat(TestUtils.<String>getPropertyValue(sess1, "targetSession.id"))
				.isEqualTo("session:1");
		Session<String> sess2 = cache.getSession();
		assertThat(TestUtils.<String>getPropertyValue(sess2, "targetSession.id"))
				.isEqualTo("session:2");
		sess1.close();
		// session back to pool; should be open and reused.
		assertThat(sess1.isOpen()).isTrue();
		sess1 = cache.getSession();
		assertThat(TestUtils.<String>getPropertyValue(sess1, "targetSession.id"))
				.isEqualTo("session:1");
		assertThat((TestUtils.<Boolean>getPropertyValue(sess1, "targetSession.testCalled"))).isTrue();
		sess1.close();
		assertThat(sess1.isOpen()).isTrue();
		// reset the cache; should close idle (sess1); sess2 should closed later
		cache.resetCache();
		assertThat(sess1.isOpen()).isFalse();
		sess1 = cache.getSession();
		assertThat(TestUtils.<String>getPropertyValue(sess1, "targetSession.id"))
				.isEqualTo("session:3");
		sess1.close();
		assertThat(sess1.isOpen()).isTrue();
		// session from previous epoch is closed on return
		sess2.close();
		assertThat(sess2.isOpen()).isFalse();
		cache.resetCache();
		assertThat(sess1.isOpen()).isFalse();
	}

	@Test
	public void testDirtySession() throws Exception {
		@SuppressWarnings("unchecked")
		SessionFactory<Object> factory = mock(SessionFactory.class);
		@SuppressWarnings("unchecked")
		Session<Object> session = mock(Session.class);
		when(factory.getSession()).thenReturn(session);
		when(session.readRaw("foo")).thenReturn(new ByteArrayInputStream("".getBytes()));
		when(session.finalizeRaw()).thenReturn(true);
		CachingSessionFactory<Object> ccf = new CachingSessionFactory<>(factory);
		RemoteFileTemplate<Object> template = new RemoteFileTemplate<>(ccf);
		template.setFileNameExpression(new LiteralExpression("foo"));
		template.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		template.afterPropertiesSet();

		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() ->
						template.get(new GenericMessage<>("foo"),
								stream -> {
									throw new RuntimeException("bar");
								}))
				.withCauseInstanceOf(RuntimeException.class)
				.withStackTraceContaining("bar");
		verify(session).close();
	}

	private static class TestSessionFactory implements SessionFactory<String> {

		private int n;

		@Override
		public Session<String> getSession() {
			return new TestSession("session:" + ++n);
		}

	}

	private static class TestSession implements Session<String> {

		@SuppressWarnings("unused")
		private final String id;

		private volatile boolean open = true;

		@SuppressWarnings("unused")
		private boolean testCalled;

		private TestSession(String id) {
			this.id = id;
		}

		@Override
		public boolean remove(String path) {
			return false;
		}

		@Override
		public String[] list(String path) {
			return null;
		}

		@Override
		public void read(String source, OutputStream outputStream) {
		}

		@Override
		public void write(InputStream inputStream, String destination) {
		}

		@Override
		public void append(InputStream inputStream, String destination) {
		}

		@Override
		public boolean mkdir(String directory) {
			return false;
		}

		@Override
		public boolean rmdir(String directory) {
			return false;
		}

		@Override
		public void rename(String pathFrom, String pathTo) {
		}

		@Override
		public void close() {
			this.open = false;
		}

		@Override
		public boolean isOpen() {
			return this.open;
		}

		@Override
		public boolean exists(String path) {
			return false;
		}

		@Override
		public String[] listNames(String path) {
			return null;
		}

		@Override
		public InputStream readRaw(String source) {
			return null;
		}

		@Override
		public boolean finalizeRaw() {
			return false;
		}

		@Override
		public Object getClientInstance() {
			return null;
		}

		@Override
		public String getHostPort() {
			return null;
		}

		@Override
		public boolean test() {
			this.testCalled = true;
			return true;
		}

	}

}
