/*
 * Copyright 2015-2020 the original author or authors.
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;

/**
 * @author Lukas Gemela
 * @author Artem Bilan
 *
 * @since 5.2.2
 *
 */
public class RemoteFileStreamingMessageSourceTests {

	@Test
	@SuppressWarnings("unchecked")
	public void filterOutFilesNotAcceptedByFilter() throws IOException {
		RemoteFileTemplate<String> remoteFileTemplate = mock(RemoteFileTemplate.class);
		when(remoteFileTemplate.list("remoteDirectory")).thenReturn(new String[] { "file1", "file2" });
		Session<String> session = mock(Session.class);
		when(session.readRaw(anyString())).thenReturn(mock(InputStream.class));
		when(remoteFileTemplate.getSession()).thenReturn(session);

		FileListFilter<String> fileListFilter = mock(FileListFilter.class);
		when(fileListFilter.supportsSingleFileFiltering()).thenReturn(true);
		when(fileListFilter.accept("file1")).thenReturn(false);
		when(fileListFilter.accept("file2")).thenReturn(false);

		Comparator<String> comparator = mock(Comparator.class);
		TestRemoteFileStreamingMessageSource testRemoteFileStreamingMessageSource =
				new TestRemoteFileStreamingMessageSource(remoteFileTemplate, comparator);

		testRemoteFileStreamingMessageSource.setFilter(fileListFilter);
		testRemoteFileStreamingMessageSource.setRemoteDirectory("remoteDirectory");
		testRemoteFileStreamingMessageSource.setBeanFactory(mock(BeanFactory.class));
		testRemoteFileStreamingMessageSource.start();

		assertThat(testRemoteFileStreamingMessageSource.doReceive(-1)).isNull();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void sessionReturnedToCacheProperlyOnDoReceive() throws IOException {
		Session<String> session = mock(Session.class);
		when(session.readRaw(anyString())).thenThrow(IOException.class);
		when(session.list("remoteDirectory")).thenReturn(new String[] { "file1" });

		SessionFactory<String> sessionFactory = mock(SessionFactory.class);
		when(sessionFactory.getSession()).thenReturn(session);

		CachingSessionFactory<String> cachingSessionFactory = new CachingSessionFactory<>(sessionFactory, 1);
		RemoteFileTemplate<String> remoteFileTemplate = new RemoteFileTemplate<>(cachingSessionFactory);

		TestRemoteFileStreamingMessageSource testRemoteFileStreamingMessageSource =
				new TestRemoteFileStreamingMessageSource(remoteFileTemplate, null);

		testRemoteFileStreamingMessageSource.setRemoteDirectory("remoteDirectory");
		testRemoteFileStreamingMessageSource.setBeanFactory(mock(BeanFactory.class));
		testRemoteFileStreamingMessageSource.start();

		assertThatExceptionOfType(UncheckedIOException.class)
				.isThrownBy(() -> testRemoteFileStreamingMessageSource.doReceive(anyInt()));

		assertThat(cachingSessionFactory.getSession()).isNotNull();
	}

	static class TestRemoteFileStreamingMessageSource extends AbstractRemoteFileStreamingMessageSource<String> {

		TestRemoteFileStreamingMessageSource(RemoteFileTemplate<String> template, Comparator<String> comparator) {
			super(template, comparator);
		}

		@Override
		protected List<AbstractFileInfo<String>> asFileInfoList(Collection<String> files) {
			return files
					.stream()
					.map(TestFileInfo::new)
					.collect(Collectors.toList());
		}

		@Override
		protected boolean isDirectory(String file) {
			return false;
		}

		@Override
		public String getComponentType() {
			return null;
		}

	}

	static class TestFileInfo extends AbstractFileInfo<String> {

		TestFileInfo(String fileName) {
			this.fileName = fileName;
		}

		private final String fileName;

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
			return fileName;
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

}
