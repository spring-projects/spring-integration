/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.integration.file.remote.synchronizer;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.messaging.MessagingException;

/**
 * @author Gary Russell
 * @since 4.0.4
 *
 */
public class AbstractRemoteFileSynchronizerTests {

	@Test
	public void testRollback() {
		final AtomicBoolean failWhenCopyingBar = new AtomicBoolean(true);
		final AtomicInteger count = new AtomicInteger();
		SessionFactory<String> sf = new StringSessionFactory();
		AbstractInboundFileSynchronizer<String> sync = new AbstractInboundFileSynchronizer<String>(sf) {

			@Override
			protected boolean isFile(String file) {
				return true;
			}

			@Override
			protected String getFilename(String file) {
				return file;
			}

			@Override
			protected long getModified(String file) {
				return 0;
			}

			@Override
			protected void copyFileToLocalDirectory(String remoteDirectoryPath, String remoteFile, File localDirectory,
					Session<String> session) throws IOException {
				if ("bar".equals(remoteFile) && failWhenCopyingBar.getAndSet(false)) {
					throw new IOException("fail");
				}
				count.incrementAndGet();
			}

		};
		sync.setFilter(new AcceptOnceFileListFilter<String>());

		try {
			sync.synchronizeToLocalDirectory(mock(File.class));
			assertEquals(1, count.get());
			fail("Expected exception");
		}
		catch (MessagingException e) {
			assertThat(e.getCause(), instanceOf(MessagingException.class));
			assertThat(e.getCause().getCause(), instanceOf(IOException.class));
			assertEquals("fail", e.getCause().getCause().getMessage());
		}
		sync.synchronizeToLocalDirectory(mock(File.class));
		assertEquals(3, count.get());
	}

	private class StringSessionFactory implements SessionFactory<String> {

		@Override
		public Session<String> getSession() {
			return new StringSession();
		}

	}

	private class StringSession implements Session<String> {

		@Override
		public boolean remove(String path) throws IOException {
			return true;
		}

		@Override
		public String[] list(String path) throws IOException {
			return new String[] {"foo", "bar", "baz"};
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
			return true;
		}

		@Override
		public boolean rmdir(String directory) throws IOException {
			return true;
		}

		@Override
		public void rename(String pathFrom, String pathTo) throws IOException {
		}

		@Override
		public void close() {
		}

		@Override
		public boolean isOpen() {
			return true;
		}

		@Override
		public boolean exists(String path) throws IOException {
			return true;
		}

		@Override
		public String[] listNames(String path) throws IOException {
			return new String[0];
		}

		@Override
		public InputStream readRaw(String source) throws IOException {
			return null;
		}

		@Override
		public boolean finalizeRaw() throws IOException {
			return true;
		}

		@Override
		public Object getClientInstance() {
			return null;
		}

	}

}
