/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.integration.file.remote.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Test;

import org.springframework.integration.test.util.TestUtils;

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
		Session<String> sess1 = cache.getSession();
		assertEquals("session:1", TestUtils.getPropertyValue(sess1, "targetSession.id"));
		Session<String> sess2 = cache.getSession();
		assertEquals("session:2", TestUtils.getPropertyValue(sess2, "targetSession.id"));
		sess1.close();
		// session back to pool; should be open and reused.
		assertTrue(sess1.isOpen());
		sess1 = cache.getSession();
		assertEquals("session:1", TestUtils.getPropertyValue(sess1, "targetSession.id"));
		sess1.close();
		assertTrue(sess1.isOpen());
		// reset the cache; should close idle (sess1); sess2 should closed later
		cache.resetCache();
		assertFalse(sess1.isOpen());
		sess1 = cache.getSession();
		assertEquals("session:3", TestUtils.getPropertyValue(sess1, "targetSession.id"));
		sess1.close();
		assertTrue(sess1.isOpen());
		// session from previous epoch is closed on return
		sess2.close();
		assertFalse(sess2.isOpen());
		cache.resetCache();
		assertFalse(sess1.isOpen());
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
		public boolean mkdir(String directory) throws IOException {
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

	}
}
