/*
 * Copyright 2016-2019 the original author or authors.
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

package org.springframework.integration.file.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.filters.AbstractPersistentAcceptOnceFileListFilter;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.splitter.FileSplitter;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.integration.transformer.StreamTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
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
		Streamer streamer = new Streamer(new StringRemoteFileTemplate(new StringSessionFactory()), null);
		streamer.setBeanFactory(mock(BeanFactory.class));
		streamer.setRemoteDirectory("/foo");
		streamer.afterPropertiesSet();
		streamer.start();
		Message<byte[]> received = (Message<byte[]>) this.transformer.transform(streamer.receive());
		assertEquals("foo\nbar", new String(received.getPayload()));
		assertEquals("/foo", received.getHeaders().get(FileHeaders.REMOTE_DIRECTORY));
		assertEquals("foo", received.getHeaders().get(FileHeaders.REMOTE_FILE));

		verify(new IntegrationMessageHeaderAccessor(received).getCloseableResource()).close();

		received = (Message<byte[]>) this.transformer.transform(streamer.receive());
		assertEquals("baz\nqux", new String(received.getPayload()));
		assertEquals("/foo", received.getHeaders().get(FileHeaders.REMOTE_DIRECTORY));
		assertEquals("bar", received.getHeaders().get(FileHeaders.REMOTE_FILE));

		verify(new IntegrationMessageHeaderAccessor(received).getCloseableResource()).close();
	}

	@Test
	public void testExceptionOnFetch() {
		exception.expect(MessagingException.class);
		StringSessionFactory sessionFactory = new StringSessionFactory();
		Streamer streamer = new Streamer(new StringRemoteFileTemplate(sessionFactory), null);
		streamer.setBeanFactory(mock(BeanFactory.class));
		streamer.setRemoteDirectory("/bad");
		streamer.afterPropertiesSet();
		streamer.start();
		streamer.receive();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testLineByLine() throws Exception {
		Streamer streamer = new Streamer(new StringRemoteFileTemplate(new StringSessionFactory()), null);
		streamer.setBeanFactory(mock(BeanFactory.class));
		streamer.setRemoteDirectory("/foo");
		streamer.afterPropertiesSet();
		streamer.start();
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
		verify(new IntegrationMessageHeaderAccessor(receivedStream).getCloseableResource(), times(2)).close();

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
		verify(new IntegrationMessageHeaderAccessor(receivedStream).getCloseableResource(), times(2)).close();
	}

	public static class Streamer extends AbstractRemoteFileStreamingMessageSource<String> {

		ConcurrentHashMap<String, String> metadataMap = new ConcurrentHashMap<>();

		protected Streamer(RemoteFileTemplate<String> template, Comparator<AbstractFileInfo<String>> comparator) {
			super(template, comparator);
			doSetFilter(new StringPersistentFileListFilter(new SimpleMetadataStore(this.metadataMap), "streamer"));
		}

		@Override
		public String getComponentType() {
			return "Streamer";
		}

		@Override
		protected List<AbstractFileInfo<String>> asFileInfoList(Collection<String> files) {
			List<AbstractFileInfo<String>> infos = new ArrayList<>();
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
			return name;
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

				willReturn(new String[] { "/bad/file1", "/bad/file2" }).given(session).list("/bad");
				willThrow(new IOException("No file")).given(session).readRaw("/bad/file1");
				willThrow(new IOException("No file")).given(session).readRaw("/bad/file2");

				given(session.finalizeRaw()).willReturn(true);
				return session;
			}
			catch (Exception e) {
				throw new RuntimeException("failed to mock session", e);
			}
		}

	}

	public static class StringPersistentFileListFilter extends AbstractPersistentAcceptOnceFileListFilter<String> {

		public StringPersistentFileListFilter(ConcurrentMetadataStore store, String prefix) {
			super(store, prefix);
		}

		@Override
		protected long modified(String file) {
			return 0;
		}

		@Override
		protected String fileName(String file) {
			return file;
		}

	}

}
