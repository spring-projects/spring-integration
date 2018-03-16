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

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.splitter.FileSplitter;
import org.springframework.integration.transformer.StreamTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * @author Gary Russell
 * @since 4.3
 *
 */
public class StreamingInboundTests {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	private final StreamTransformer transformer = new StreamTransformer();

	@SuppressWarnings("unchecked")
	@Test
	public void testAllData() throws Exception {
		StringSessionFactory sessionFactory = new StringSessionFactory();
		Streamer streamer = new Streamer(new StringRemoteFileTemplate(sessionFactory), null);
		streamer.setBeanFactory(mock(BeanFactory.class));
		streamer.setRemoteDirectory("/foo");
		streamer.afterPropertiesSet();
		Message<byte[]> received = (Message<byte[]>) this.transformer.transform(streamer.receive());
		assertEquals("foo\nbar", new String(received.getPayload()));
		assertEquals("/foo", received.getHeaders().get(FileHeaders.REMOTE_DIRECTORY));
		assertEquals("foo", received.getHeaders().get(FileHeaders.REMOTE_FILE));
		String fileInfo = (String) received.getHeaders().get(FileHeaders.REMOTE_FILE_INFO);
		assertThat(fileInfo, containsString("remoteDirectory\":\"/foo"));
		assertThat(fileInfo, containsString("permissions\":\"-rw-rw-rw"));
		assertThat(fileInfo, containsString("size\":42"));
		assertThat(fileInfo, containsString("directory\":false"));
		assertThat(fileInfo, containsString("filename\":\"foo"));
		assertThat(fileInfo, containsString("modified\":42000"));
		assertThat(fileInfo, containsString("link\":false"));

		// close after list, transform
		verify(StaticMessageHeaderAccessor.getCloseableResource(received), times(2)).close();

		received = (Message<byte[]>) this.transformer.transform(streamer.receive());
		assertEquals("baz\nqux", new String(received.getPayload()));
		assertEquals("/foo", received.getHeaders().get(FileHeaders.REMOTE_DIRECTORY));
		assertEquals("bar", received.getHeaders().get(FileHeaders.REMOTE_FILE));
		fileInfo = (String) received.getHeaders().get(FileHeaders.REMOTE_FILE_INFO);
		assertThat(fileInfo, containsString("remoteDirectory\":\"/foo"));
		assertThat(fileInfo, containsString("permissions\":\"-rw-rw-rw"));
		assertThat(fileInfo, containsString("size\":42"));
		assertThat(fileInfo, containsString("directory\":false"));
		assertThat(fileInfo, containsString("filename\":\"bar"));
		assertThat(fileInfo, containsString("modified\":42000"));
		assertThat(fileInfo, containsString("link\":false"));

		// close after transform
		verify(StaticMessageHeaderAccessor.getCloseableResource(received), times(3)).close();

		verify(sessionFactory.getSession()).list("/foo");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testAllDataMaxFetch() throws Exception {
		StringSessionFactory sessionFactory = new StringSessionFactory();
		Streamer streamer = new Streamer(new StringRemoteFileTemplate(sessionFactory), null);
		streamer.setBeanFactory(mock(BeanFactory.class));
		streamer.setRemoteDirectory("/foo");
		streamer.setMaxFetchSize(1);
		streamer.setFilter(new AcceptOnceFileListFilter<>());
		streamer.afterPropertiesSet();
		Message<byte[]> received = (Message<byte[]>) this.transformer.transform(streamer.receive());
		assertEquals("foo\nbar", new String(received.getPayload()));
		assertEquals("/foo", received.getHeaders().get(FileHeaders.REMOTE_DIRECTORY));
		assertEquals("foo", received.getHeaders().get(FileHeaders.REMOTE_FILE));

		// close after list, transform
		verify(StaticMessageHeaderAccessor.getCloseableResource(received), times(2)).close();

		received = (Message<byte[]>) this.transformer.transform(streamer.receive());
		assertEquals("baz\nqux", new String(received.getPayload()));
		assertEquals("/foo", received.getHeaders().get(FileHeaders.REMOTE_DIRECTORY));
		assertEquals("bar", received.getHeaders().get(FileHeaders.REMOTE_FILE));

		// close after list, transform
		verify(new IntegrationMessageHeaderAccessor(received).getCloseableResource(), times(4)).close();

		verify(sessionFactory.getSession(), times(2)).list("/foo");
	}

	@Test
	public void testExceptionOnFetch() {
		exception.expect(MessagingException.class);
		StringSessionFactory sessionFactory = new StringSessionFactory();
		Streamer streamer = new Streamer(new StringRemoteFileTemplate(sessionFactory), null);
		streamer.setBeanFactory(mock(BeanFactory.class));
		streamer.setRemoteDirectory("/bad");
		streamer.afterPropertiesSet();
		streamer.receive();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testLineByLine() throws Exception {
		Streamer streamer = new Streamer(new StringRemoteFileTemplate(new StringSessionFactory()), null);
		streamer.setBeanFactory(mock(BeanFactory.class));
		streamer.setRemoteDirectory("/foo");
		streamer.afterPropertiesSet();
		QueueChannel out = new QueueChannel();
		FileSplitter splitter = new FileSplitter();
		splitter.setBeanFactory(mock(BeanFactory.class));
		splitter.setOutputChannel(out);
		splitter.afterPropertiesSet();
		Message<InputStream> receivedStream = streamer.receive();
		splitter.handleMessage(receivedStream);
		Message<byte[]> received = (Message<byte[]>) out.receive(0);
		assertEquals("foo", received.getPayload());
		assertEquals("/foo", received.getHeaders().get(FileHeaders.REMOTE_DIRECTORY));
		assertEquals("foo", received.getHeaders().get(FileHeaders.REMOTE_FILE));
		received = (Message<byte[]>) out.receive(0);
		assertEquals("bar", received.getPayload());
		assertEquals("/foo", received.getHeaders().get(FileHeaders.REMOTE_DIRECTORY));
		assertEquals("foo", received.getHeaders().get(FileHeaders.REMOTE_FILE));
		assertNull(out.receive(0));

		// close by list, splitter
		verify(new IntegrationMessageHeaderAccessor(receivedStream).getCloseableResource(), times(3)).close();

		receivedStream = streamer.receive();
		splitter.handleMessage(receivedStream);
		received = (Message<byte[]>) out.receive(0);
		assertEquals("baz", received.getPayload());
		assertEquals("/foo", received.getHeaders().get(FileHeaders.REMOTE_DIRECTORY));
		assertEquals("bar", received.getHeaders().get(FileHeaders.REMOTE_FILE));
		received = (Message<byte[]>) out.receive(0);
		assertEquals("qux", received.getPayload());
		assertEquals("/foo", received.getHeaders().get(FileHeaders.REMOTE_DIRECTORY));
		assertEquals("bar", received.getHeaders().get(FileHeaders.REMOTE_FILE));
		assertNull(out.receive(0));

		// close by splitter
		verify(new IntegrationMessageHeaderAccessor(receivedStream).getCloseableResource(), times(5)).close();
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
		protected List<AbstractFileInfo<String>> asFileInfoList(Collection<String> files) {
			List<AbstractFileInfo<String>> infos = new ArrayList<AbstractFileInfo<String>>();
			for (String file : files) {
				infos.add(new StringFileInfo(file));
			}
			return infos;
		}

	}

	public static class StringFileInfo extends AbstractFileInfo<String> {

		private final String name;

		private StringFileInfo(String name) {
			this.name = name;
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
			return 42;
		}

		@Override
		public long getModified() {
			return 42_000;
		}

		@Override
		public String getFilename() {
			return this.name.substring(this.name.lastIndexOf("/") + 1);
		}

		@Override
		public String getPermissions() {
			return "-rw-rw-rw";
		}

		@Override
		public String getFileInfo() {
			return asString();
		}

		private String asString() {
			return "StringFileInfo [name=" + this.name + "]";
		}

	}

	public static class StringRemoteFileTemplate extends RemoteFileTemplate<String> {

		public StringRemoteFileTemplate(SessionFactory<String> sessionFactory) {
			super(sessionFactory);
		}

	}

	public static class StringSessionFactory implements SessionFactory<String> {

		private Session<String> session;

		@SuppressWarnings("unchecked")
		@Override
		public Session<String> getSession() {
			if (this.session != null) {
				return this.session;
			}
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

				willReturn(new String[] { "/bad/file" }).given(session).list("/bad");
				willThrow(new IOException("No file")).given(session).readRaw("/bad/file");

				given(session.finalizeRaw()).willReturn(true);

				this.session = session;

				return session;
			}
			catch (Exception e) {
				throw new RuntimeException("failed to mock session", e);
			}
		}

	}

}
