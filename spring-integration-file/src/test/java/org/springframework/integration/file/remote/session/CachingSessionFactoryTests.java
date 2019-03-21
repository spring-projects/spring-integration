/*
 * Copyright 2013-2019 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.file.remote.InputStreamCallback;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public class CachingSessionFactoryTests {

	@Test
	public void testCacheAndReset() {
		TestSessionFactory factory = new TestSessionFactory();
		CachingSessionFactory<String> cache = new CachingSessionFactory<String>(factory);
		cache.setTestSession(true);
		Session<String> sess1 = cache.getSession();
		assertThat(TestUtils.getPropertyValue(sess1, "targetSession.id")).isEqualTo("session:1");
		Session<String> sess2 = cache.getSession();
		assertThat(TestUtils.getPropertyValue(sess2, "targetSession.id")).isEqualTo("session:2");
		sess1.close();
		// session back to pool; should be open and reused.
		assertThat(sess1.isOpen()).isTrue();
		sess1 = cache.getSession();
		assertThat(TestUtils.getPropertyValue(sess1, "targetSession.id")).isEqualTo("session:1");
		assertThat((TestUtils.getPropertyValue(sess1, "targetSession.testCalled", Boolean.class))).isTrue();
		sess1.close();
		assertThat(sess1.isOpen()).isTrue();
		// reset the cache; should close idle (sess1); sess2 should closed later
		cache.resetCache();
		assertThat(sess1.isOpen()).isFalse();
		sess1 = cache.getSession();
		assertThat(TestUtils.getPropertyValue(sess1, "targetSession.id")).isEqualTo("session:3");
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
		CachingSessionFactory<Object> ccf = new CachingSessionFactory<Object>(factory);
		RemoteFileTemplate<Object> template = new RemoteFileTemplate<Object>(ccf);
		template.setFileNameExpression(new LiteralExpression("foo"));
		template.setBeanFactory(mock(BeanFactory.class));
		template.afterPropertiesSet();
		try {
			template.get(new GenericMessage<String>("foo"), (InputStreamCallback) stream -> {
				throw new RuntimeException("bar");
			});
			fail("Expected exception");
		}
		catch (Exception e) {
			assertThat(e.getCause()).isInstanceOf(RuntimeException.class);
			assertThat(e.getCause().getMessage()).isEqualTo("bar");
		}
		verify(session).close();
	}

	private class TestSessionFactory implements SessionFactory<String> {

		private int n;

		@Override
		public Session<String> getSession() {
			return new TestSession("session:" + ++n);
		}

	}

	private class TestSession implements Session<String> {

		@SuppressWarnings("unused")
		private final String id;

		private volatile boolean open = true;

		@SuppressWarnings("unused")
		private boolean testCalled;

		private TestSession(String id) {
			this.id = id;
		}

		@Override
		public boolean remove(String path) throws IOException {
			return false;
		}

		@Override
		public String[] list(String path) throws IOException {
			return null;
		}

		@Override
		public void read(String source, OutputStream outputStream) throws IOException {
		}

		@Override
		public void write(InputStream inputStream, String destination) throws IOException {
		}

		@Override
		public void append(InputStream inputStream, String destination) throws IOException {
		}

		@Override
		public boolean mkdir(String directory) throws IOException {
			return false;
		}

		@Override
		public boolean rmdir(String directory) throws IOException {
			return false;
		}

		@Override
		public void rename(String pathFrom, String pathTo) throws IOException {
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
		public boolean exists(String path) throws IOException {
			return false;
		}

		@Override
		public String[] listNames(String path) throws IOException {
			return null;
		}

		@Override
		public InputStream readRaw(String source) throws IOException {
			return null;
		}

		@Override
		public boolean finalizeRaw() throws IOException {
			return false;
		}

		@Override
		public Object getClientInstance() {
			return null;
		}

		@Override
		public boolean test() {
			this.testCalled = true;
			return true;
		}

	}

}
