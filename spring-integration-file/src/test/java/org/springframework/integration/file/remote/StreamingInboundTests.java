/*
 * Copyright 2016-2021 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.filters.AbstractPersistentAcceptOnceFileListFilter;
import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.splitter.FileSplitter;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.transformer.StreamTransformer;
import org.springframework.messaging.Message;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.3
 *
 */
public class StreamingInboundTests {

	private final StreamTransformer transformer = new StreamTransformer();

	@Test
	public void testAllDataNoFilter() throws IOException {
		testAllData(null, true);
	}

	@Test
	public void testAllDataSingleCapableFilter() throws IOException {
		testAllData(null, false);
	}

	@Test
	public void testAllDataBulkOnlyFilter() throws IOException {
		testAllData(fs -> Stream.of(fs).collect(Collectors.toList()), false);
	}

	@SuppressWarnings("unchecked")
	private void testAllData(FileListFilter<String> filter, boolean nullFilter) throws IOException {
		StringSessionFactory sessionFactory = new StringSessionFactory();
		Streamer streamer = new Streamer(new StringRemoteFileTemplate(sessionFactory), null);
		streamer.setBeanFactory(mock(BeanFactory.class));
		streamer.setRemoteDirectory("/foo");
		if (filter != null) {
			streamer.setFilter(filter);
		}
		if (nullFilter) {
			streamer.setFilter(null);
		}
		streamer.afterPropertiesSet();
		streamer.start();
		Message<byte[]> received = (Message<byte[]>) this.transformer.transform(streamer.receive());
		assertThat(received.getPayload()).isEqualTo("foo\nbar".getBytes());
		assertThat(received.getHeaders().get(FileHeaders.REMOTE_DIRECTORY)).isEqualTo("/foo");
		assertThat(received.getHeaders().get(FileHeaders.REMOTE_FILE)).isEqualTo("foo");
		String fileInfo = (String) received.getHeaders().get(FileHeaders.REMOTE_FILE_INFO);
		assertThat(fileInfo).contains("remoteDirectory\":\"/foo");
		assertThat(fileInfo).contains("permissions\":\"-rw-rw-rw");
		assertThat(fileInfo).contains("size\":42");
		assertThat(fileInfo).contains("directory\":false");
		assertThat(fileInfo).contains("filename\":\"foo");
		assertThat(fileInfo).contains("modified\":42000");
		assertThat(fileInfo).contains("link\":false");

		// close after list, transform
		verify(StaticMessageHeaderAccessor.getCloseableResource(received), times(2)).close();

		received = (Message<byte[]>) this.transformer.transform(streamer.receive());
		assertThat(received.getPayload()).isEqualTo("baz\nqux".getBytes());
		assertThat(received.getHeaders().get(FileHeaders.REMOTE_DIRECTORY)).isEqualTo("/foo");
		assertThat(received.getHeaders().get(FileHeaders.REMOTE_FILE)).isEqualTo("bar");
		fileInfo = (String) received.getHeaders().get(FileHeaders.REMOTE_FILE_INFO);
		assertThat(fileInfo).contains("remoteDirectory\":\"/foo");
		assertThat(fileInfo).contains("permissions\":\"-rw-rw-rw");
		assertThat(fileInfo).contains("size\":42");
		assertThat(fileInfo).contains("directory\":false");
		assertThat(fileInfo).contains("filename\":\"bar");
		assertThat(fileInfo).contains("modified\":42000");
		assertThat(fileInfo).contains("link\":false");

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
		streamer.setFilter(new AcceptOnceFileListFilter<>());
		streamer.afterPropertiesSet();
		streamer.start();
		Message<byte[]> received = (Message<byte[]>) this.transformer.transform(streamer.receive());
		assertThat(received.getPayload()).isEqualTo("foo\nbar".getBytes());
		assertThat(received.getHeaders().get(FileHeaders.REMOTE_DIRECTORY)).isEqualTo("/foo");
		assertThat(received.getHeaders().get(FileHeaders.REMOTE_FILE)).isEqualTo("foo");

		// close after list, transform
		verify(StaticMessageHeaderAccessor.getCloseableResource(received), times(2)).close();

		received = (Message<byte[]>) this.transformer.transform(streamer.receive());
		assertThat(received.getPayload()).isEqualTo("baz\nqux".getBytes());
		assertThat(received.getHeaders().get(FileHeaders.REMOTE_DIRECTORY)).isEqualTo("/foo");
		assertThat(received.getHeaders().get(FileHeaders.REMOTE_FILE)).isEqualTo("bar");

		// close after transform
		verify(new IntegrationMessageHeaderAccessor(received).getCloseableResource(), times(3)).close();

		verify(sessionFactory.getSession()).list("/foo");
	}

	@Test
	public void testExceptionOnFetch() {
		StringSessionFactory sessionFactory = new StringSessionFactory();
		Streamer streamer = new Streamer(new StringRemoteFileTemplate(sessionFactory), null);
		streamer.setBeanFactory(mock(BeanFactory.class));
		streamer.setRemoteDirectory("/bad");
		streamer.afterPropertiesSet();
		streamer.start();
		assertThatExceptionOfType(UncheckedIOException.class)
				.isThrownBy(streamer::receive);
	}

	@Test
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
		Message<?> received = out.receive(0);
		assertThat(received.getPayload()).isEqualTo("foo");
		assertThat(received.getHeaders().get(FileHeaders.REMOTE_DIRECTORY)).isEqualTo("/foo");
		assertThat(received.getHeaders().get(FileHeaders.REMOTE_FILE)).isEqualTo("foo");
		received = out.receive(0);
		assertThat(received.getPayload()).isEqualTo("bar");
		assertThat(received.getHeaders().get(FileHeaders.REMOTE_DIRECTORY)).isEqualTo("/foo");
		assertThat(received.getHeaders().get(FileHeaders.REMOTE_FILE)).isEqualTo("foo");
		assertThat(out.receive(0)).isNull();

		// close by list, splitter
		verify(new IntegrationMessageHeaderAccessor(receivedStream).getCloseableResource(), times(3)).close();

		receivedStream = streamer.receive();
		splitter.handleMessage(receivedStream);
		received = out.receive(0);
		assertThat(received.getPayload()).isEqualTo("baz");
		assertThat(received.getHeaders().get(FileHeaders.REMOTE_DIRECTORY)).isEqualTo("/foo");
		assertThat(received.getHeaders().get(FileHeaders.REMOTE_FILE)).isEqualTo("bar");
		received = out.receive(0);
		assertThat(received.getPayload()).isEqualTo("qux");
		assertThat(received.getHeaders().get(FileHeaders.REMOTE_DIRECTORY)).isEqualTo("/foo");
		assertThat(received.getHeaders().get(FileHeaders.REMOTE_FILE)).isEqualTo("bar");
		assertThat(out.receive(0)).isNull();

		// close by splitter
		verify(new IntegrationMessageHeaderAccessor(receivedStream).getCloseableResource(), times(5)).close();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testStopAdapterRemovesUnprocessed() {
		Streamer streamer = new Streamer(new StringRemoteFileTemplate(new StringSessionFactory()), null);
		streamer.setBeanFactory(mock(BeanFactory.class));
		streamer.setRemoteDirectory("/foo");
		streamer.afterPropertiesSet();
		streamer.start();
		assertThat(streamer.receive()).isNotNull();
		assertThat(TestUtils.getPropertyValue(streamer, "toBeReceived", BlockingQueue.class)).hasSize(1);
		assertThat(streamer.metadataMap).hasSize(1);
		streamer.stop();
		assertThat(TestUtils.getPropertyValue(streamer, "toBeReceived", BlockingQueue.class)).hasSize(0);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testFilterReversedOnBadFetch() {
		Streamer streamer = new Streamer(new StringRemoteFileTemplate(new StringSessionFactory()), null);
		streamer.setBeanFactory(mock(BeanFactory.class));
		streamer.setRemoteDirectory("/bad");
		streamer.afterPropertiesSet();
		streamer.start();
		assertThatExceptionOfType(UncheckedIOException.class)
				.isThrownBy(streamer::receive);
		assertThat(TestUtils.getPropertyValue(streamer, "toBeReceived", BlockingQueue.class)).hasSize(1);
		assertThat(streamer.metadataMap).hasSize(0);
	}

	public static class Streamer extends AbstractRemoteFileStreamingMessageSource<String> {

		ConcurrentHashMap<String, String> metadataMap = new ConcurrentHashMap<>();

		protected Streamer(RemoteFileTemplate<String> template) {
			super(template, null);
			doSetFilter(null);
		}

		protected Streamer(RemoteFileTemplate<String> template, Comparator<String> comparator) {
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

		@Override
		protected boolean isDirectory(String file) {
			return false;
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
			return name;
		}

	}

	public static class StringRemoteFileTemplate extends RemoteFileTemplate<String> {

		public StringRemoteFileTemplate(SessionFactory<String> sessionFactory) {
			super(sessionFactory);
		}

	}

	public static class StringSessionFactory implements SessionFactory<String> {

		private Session<String> singletonSession;

		@SuppressWarnings("unchecked")
		@Override
		public Session<String> getSession() {
			if (this.singletonSession != null) {
				return this.singletonSession;
			}
			try {
				Session<String> session = mock(Session.class);
				willReturn(new String[]{ "/foo/foo", "/foo/bar" }).given(session).list("/foo");
				ByteArrayInputStream foo = new ByteArrayInputStream("foo\nbar".getBytes());
				ByteArrayInputStream bar = new ByteArrayInputStream("baz\nqux".getBytes());
				willReturn(foo).given(session).readRaw("/foo/foo");
				willReturn(bar).given(session).readRaw("/foo/bar");

				willReturn(new String[]{ "/bar/foo", "/bar/bar" }).given(session).list("/bar");
				ByteArrayInputStream foo2 = new ByteArrayInputStream("foo\r\nbar".getBytes());
				ByteArrayInputStream bar2 = new ByteArrayInputStream("baz\r\nqux".getBytes());
				willReturn(foo2).given(session).readRaw("/bar/foo");
				willReturn(bar2).given(session).readRaw("/bar/bar");

				willReturn(new String[]{ "/bad/file1", "/bad/file2" }).given(session).list("/bad");
				willThrow(new IOException("No file")).given(session).readRaw("/bad/file1");
				willThrow(new IOException("No file")).given(session).readRaw("/bad/file2");

				given(session.finalizeRaw()).willReturn(true);

				this.singletonSession = session;

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
