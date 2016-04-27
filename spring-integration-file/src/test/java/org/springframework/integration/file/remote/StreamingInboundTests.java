/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.file.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.messaging.Message;

/**
 * @author Gary Russell
 * @since 4.3
 *
 */
public class StreamingInboundTests {

	@Test
	public void testAllData() {
		Streamer streamer = new Streamer(new StringRemoteFileTemplate(new StringSessionFactory()), null);
		streamer.setBeanFactory(mock(BeanFactory.class));
		streamer.setRemoteDirectory("/foo");
		streamer.afterPropertiesSet();
		Message<byte[]> received = streamer.receive();
		assertEquals("foo\nbar", new String(received.getPayload()));
		assertEquals("/foo", received.getHeaders().get(FileHeaders.REMOTE_DIRECTORY));
		assertEquals("foo", received.getHeaders().get(FileHeaders.REMOTE_FILE));
		received = streamer.receive();
		assertEquals("baz\nqux", new String(received.getPayload()));
		assertEquals("/foo", received.getHeaders().get(FileHeaders.REMOTE_DIRECTORY));
		assertEquals("bar", received.getHeaders().get(FileHeaders.REMOTE_FILE));
	}

	@Test
	public void testLineByLine() {
		LineStreamer streamer = new LineStreamer(new StringRemoteFileTemplate(new StringSessionFactory()), null);
		streamer.setBeanFactory(mock(BeanFactory.class));
		streamer.setRemoteDirectory("/foo");
		streamer.afterPropertiesSet();
		Message<byte[]> received = streamer.receive();
		assertEquals("foo\n", new String(received.getPayload()));
		assertEquals("/foo", received.getHeaders().get(FileHeaders.REMOTE_DIRECTORY));
		assertEquals("foo", received.getHeaders().get(FileHeaders.REMOTE_FILE));
		received = streamer.receive();
		assertEquals("bar", new String(received.getPayload()));
		assertEquals("/foo", received.getHeaders().get(FileHeaders.REMOTE_DIRECTORY));
		assertEquals("foo", received.getHeaders().get(FileHeaders.REMOTE_FILE));
		received = streamer.receive();
		assertEquals("baz\n", new String(received.getPayload()));
		assertEquals("/foo", received.getHeaders().get(FileHeaders.REMOTE_DIRECTORY));
		assertEquals("bar", received.getHeaders().get(FileHeaders.REMOTE_FILE));
		received = streamer.receive();
		assertEquals("qux", new String(received.getPayload()));
		assertEquals("/foo", received.getHeaders().get(FileHeaders.REMOTE_DIRECTORY));
		assertEquals("bar", received.getHeaders().get(FileHeaders.REMOTE_FILE));
	}

	@Test
	public void testLineByLineNoDelims() {
		LineStreamer streamer = new LineStreamer(new StringRemoteFileTemplate(new StringSessionFactory()), null);
		streamer.setBeanFactory(mock(BeanFactory.class));
		streamer.setRemoteDirectory("/foo");
		streamer.setIncludeDelimiters(false);
		streamer.afterPropertiesSet();
		Message<byte[]> received = streamer.receive();
		assertEquals("foo", new String(received.getPayload()));
		assertEquals("/foo", received.getHeaders().get(FileHeaders.REMOTE_DIRECTORY));
		assertEquals("foo", received.getHeaders().get(FileHeaders.REMOTE_FILE));
		received = streamer.receive();
		assertEquals("bar", new String(received.getPayload()));
		assertEquals("/foo", received.getHeaders().get(FileHeaders.REMOTE_DIRECTORY));
		assertEquals("foo", received.getHeaders().get(FileHeaders.REMOTE_FILE));
		received = streamer.receive();
		assertEquals("baz", new String(received.getPayload()));
		assertEquals("/foo", received.getHeaders().get(FileHeaders.REMOTE_DIRECTORY));
		assertEquals("bar", received.getHeaders().get(FileHeaders.REMOTE_FILE));
		received = streamer.receive();
		assertEquals("qux", new String(received.getPayload()));
		assertEquals("/foo", received.getHeaders().get(FileHeaders.REMOTE_DIRECTORY));
		assertEquals("bar", received.getHeaders().get(FileHeaders.REMOTE_FILE));
	}

	@Test
	public void testLineByLineCRLF() {
		LineStreamer streamer = new LineStreamer(new StringRemoteFileTemplate(new StringSessionFactory()), null);
		streamer.setBeanFactory(mock(BeanFactory.class));
		streamer.setRemoteDirectory("/bar");
		streamer.setLineDelimiter(new byte[] { '\r', '\n' });
		streamer.afterPropertiesSet();
		Message<byte[]> received = streamer.receive();
		assertEquals("foo\r\n", new String(received.getPayload()));
		assertEquals("/bar", received.getHeaders().get(FileHeaders.REMOTE_DIRECTORY));
		assertEquals("foo", received.getHeaders().get(FileHeaders.REMOTE_FILE));
		received = streamer.receive();
		assertEquals("bar", new String(received.getPayload()));
		assertEquals("/bar", received.getHeaders().get(FileHeaders.REMOTE_DIRECTORY));
		assertEquals("foo", received.getHeaders().get(FileHeaders.REMOTE_FILE));
		received = streamer.receive();
		assertEquals("baz\r\n", new String(received.getPayload()));
		assertEquals("/bar", received.getHeaders().get(FileHeaders.REMOTE_DIRECTORY));
		assertEquals("bar", received.getHeaders().get(FileHeaders.REMOTE_FILE));
		received = streamer.receive();
		assertEquals("qux", new String(received.getPayload()));
		assertEquals("/bar", received.getHeaders().get(FileHeaders.REMOTE_DIRECTORY));
		assertEquals("bar", received.getHeaders().get(FileHeaders.REMOTE_FILE));
	}

	@Test
	public void testLineByLineCRLFNoDelims() {
		LineStreamer streamer = new LineStreamer(new StringRemoteFileTemplate(new StringSessionFactory()), null);
		streamer.setBeanFactory(mock(BeanFactory.class));
		streamer.setRemoteDirectory("/bar");
		streamer.setLineDelimiter(new byte[] { '\r', '\n' });
		streamer.setIncludeDelimiters(false);
		streamer.afterPropertiesSet();
		Message<byte[]> received = streamer.receive();
		assertEquals("foo", new String(received.getPayload()));
		assertEquals("/bar", received.getHeaders().get(FileHeaders.REMOTE_DIRECTORY));
		assertEquals("foo", received.getHeaders().get(FileHeaders.REMOTE_FILE));
		received = streamer.receive();
		assertEquals("bar", new String(received.getPayload()));
		assertEquals("/bar", received.getHeaders().get(FileHeaders.REMOTE_DIRECTORY));
		assertEquals("foo", received.getHeaders().get(FileHeaders.REMOTE_FILE));
		received = streamer.receive();
		assertEquals("baz", new String(received.getPayload()));
		assertEquals("/bar", received.getHeaders().get(FileHeaders.REMOTE_DIRECTORY));
		assertEquals("bar", received.getHeaders().get(FileHeaders.REMOTE_FILE));
		received = streamer.receive();
		assertEquals("qux", new String(received.getPayload()));
		assertEquals("/bar", received.getHeaders().get(FileHeaders.REMOTE_DIRECTORY));
		assertEquals("bar", received.getHeaders().get(FileHeaders.REMOTE_FILE));
	}

	public static class Streamer extends AbstractRemoteFileStreamingMessageSource<String> {

		protected Streamer(RemoteFileTemplate<String> template, Comparator<AbstractFileInfo<String>> comparator) {
			super(template, comparator);
		}

		@Override
		public String getComponentType() {
			return "Streamer";
		}

		@Override
		protected List<AbstractFileInfo<String>> asFileInfoList(Collection<String> files, String remoteDir) {
			List<AbstractFileInfo<String>> infos = new ArrayList<AbstractFileInfo<String>>();
			for (String file : files) {
				infos.add(new StringFileInfo(file, remoteDir));
			}
			return infos;
		}

	}

	public static class LineStreamer extends AbstractLineByLineRemoteFileMessageSource<String> {

		protected LineStreamer(RemoteFileTemplate<String> template, Comparator<AbstractFileInfo<String>> comparator) {
			super(template, comparator);
		}

		@Override
		public String getComponentType() {
			return "LineStreamer";
		}

		@Override
		protected List<AbstractFileInfo<String>> asFileInfoList(Collection<String> files, String remoteDir) {
			List<AbstractFileInfo<String>> infos = new ArrayList<AbstractFileInfo<String>>();
			for (String file : files) {
				infos.add(new StringFileInfo(file, remoteDir));
			}
			return infos;
		}

	}

	public static class StringFileInfo extends AbstractFileInfo<String> {

		private final String name;

		private StringFileInfo(String name, String remoteDir) {
			this.name = name;
			setRemoteDirectory(remoteDir);
		}

		@Override
		public boolean isDirectory() {
			return false;
		}

		@Override
		public boolean isLink() {
			return false;
		}

		@Override
		public long getSize() {
			return 0;
		}

		@Override
		public long getModified() {
			return 0;
		}

		@Override
		public String getFilename() {
			return this.name.substring(this.name.lastIndexOf("/") + 1);
		}

		@Override
		public String getPermissions() {
			return null;
		}

		@Override
		public String getFileInfo() {
			return null;
		}

	}

	public static class StringRemoteFileTemplate extends RemoteFileTemplate<String> {

		public StringRemoteFileTemplate(SessionFactory<String> sessionFactory) {
			super(sessionFactory);
		}

	}

	public static class StringSessionFactory implements SessionFactory<String> {

		@SuppressWarnings("unchecked")
		@Override
		public Session<String> getSession() {
			try {
				Session<String> session = mock(Session.class);
				willReturn(new String[] { "/foo/foo", "/foo/bar" }).given(session).list("/foo");
				ByteArrayInputStream foo = new ByteArrayInputStream("foo\nbar".getBytes());
				ByteArrayInputStream bar = new ByteArrayInputStream("baz\nqux".getBytes());
				willReturn(foo).given(session).readRaw("/foo/foo");
				willReturn(bar).given(session).readRaw("/foo/bar");

				willReturn(new String[] { "/bar/foo", "/bar/bar" }).given(session).list("/bar");
				ByteArrayInputStream foo2 = new ByteArrayInputStream("foo\r\nbar".getBytes());
				ByteArrayInputStream bar2 = new ByteArrayInputStream("baz\r\nqux".getBytes());
				willReturn(foo2).given(session).readRaw("/bar/foo");
				willReturn(bar2).given(session).readRaw("/bar/bar");

				given(session.finalizeRaw()).willReturn(true);
				return session;
			}
			catch (Exception e) {
				fail("failed to mock session");
			}
			return null;
		}

	}

}
