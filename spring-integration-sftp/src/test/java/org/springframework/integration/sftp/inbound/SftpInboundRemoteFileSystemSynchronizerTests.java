/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.sftp.inbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.filters.RegexPatternFileListFilter;
import org.springframework.integration.metadata.PropertiesPersistingMetadataStore;
import org.springframework.integration.sftp.filters.SftpPersistentAcceptOnceFileListFilter;
import org.springframework.integration.sftp.filters.SftpRegexPatternFileListFilter;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.sftp.session.SftpSession;
import org.springframework.integration.sftp.session.SftpTestSessionFactory;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.SftpATTRS;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 * @author Joaquin Santana
 *
 * @since 2.0
 */
public class SftpInboundRemoteFileSystemSynchronizerTests {

	private static final com.jcraft.jsch.Session jschSession = mock(com.jcraft.jsch.Session.class);

	@BeforeEach
	@AfterEach
	public void cleanup() {
		willReturn("::1")
				.given(jschSession)
				.getHost();
		File file = new File("test");
		if (file.exists()) {
			String[] files = file.list();
			for (String fileName : files) {
				new File(file, fileName).delete();
			}
			file.delete();
		}
	}

	@Test
	public void testCopyFileToLocalDir() throws Exception {
		File localDirectory = new File("test");
		assertThat(localDirectory.exists()).isFalse();

		TestSftpSessionFactory ftpSessionFactory = new TestSftpSessionFactory();
		ftpSessionFactory.setUser("kermit");
		ftpSessionFactory.setPassword("frog");
		ftpSessionFactory.setHost("foo.com");

		SftpInboundFileSynchronizer synchronizer = spy(new SftpInboundFileSynchronizer(ftpSessionFactory));
		synchronizer.setDeleteRemoteFiles(true);
		synchronizer.setPreserveTimestamp(true);
		synchronizer.setRemoteDirectory("remote-test-dir");
		SftpRegexPatternFileListFilter patternFilter = new SftpRegexPatternFileListFilter(".*\\.test$");
		PropertiesPersistingMetadataStore store = spy(new PropertiesPersistingMetadataStore());
		store.setBaseDirectory("test");
		store.afterPropertiesSet();
		SftpPersistentAcceptOnceFileListFilter persistFilter =
				new SftpPersistentAcceptOnceFileListFilter(store, "foo");
		List<FileListFilter<LsEntry>> filters = new ArrayList<>();
		filters.add(persistFilter);
		filters.add(patternFilter);
		CompositeFileListFilter<LsEntry> filter = new CompositeFileListFilter<>(filters);
		synchronizer.setFilter(filter);
		synchronizer.setBeanFactory(mock(BeanFactory.class));
		synchronizer.afterPropertiesSet();

		SftpInboundFileSynchronizingMessageSource ms = new SftpInboundFileSynchronizingMessageSource(synchronizer);
		ms.setAutoCreateLocalDirectory(true);
		ms.setLocalDirectory(localDirectory);
		ms.setBeanFactory(mock(BeanFactory.class));
		CompositeFileListFilter<File> localFileListFilter = new CompositeFileListFilter<>();
		localFileListFilter.addFilter(new RegexPatternFileListFilter(".*\\.test$"));
		AcceptOnceFileListFilter<File> localAcceptOnceFilter = new AcceptOnceFileListFilter<>();
		localFileListFilter.addFilter(localAcceptOnceFilter);
		ms.setLocalFilter(localFileListFilter);
		ms.afterPropertiesSet();
		ms.start();

		Message<File> atestFile = ms.receive();
		assertThat(atestFile).isNotNull();
		assertThat(atestFile.getPayload().getName()).isEqualTo("a.test");
		// The test remote files are created with the current timestamp + 1 day.
		assertThat(atestFile.getPayload().lastModified()).isGreaterThan(System.currentTimeMillis());

		assertThat(atestFile.getHeaders())
				.containsKeys(FileHeaders.REMOTE_HOST_PORT, FileHeaders.REMOTE_DIRECTORY, FileHeaders.REMOTE_FILE);

		@SuppressWarnings("unchecked")
		Map<String, String> remoteFileMetadataStore =
				TestUtils.getPropertyValue(synchronizer, "remoteFileMetadataStore.metadata", Map.class);

		String next = remoteFileMetadataStore.values().iterator().next();
		assertThat(URI.create(next).getHost()).isEqualTo("[::1]");

		Message<File> btestFile = ms.receive();
		assertThat(btestFile).isNotNull();
		assertThat(btestFile.getPayload().getName()).isEqualTo("b.test");
		// The test remote files are created with the current timestamp + 1 day.
		assertThat(atestFile.getPayload().lastModified()).isGreaterThan(System.currentTimeMillis());

		Message<File> nothing = ms.receive();
		assertThat(nothing).isNull();

		// two times because on the third receive (above) the internal queue will be empty, so it will attempt
		verify(synchronizer, times(2)).synchronizeToLocalDirectory(localDirectory, Integer.MIN_VALUE);

		assertThat(new File("test/a.test").exists()).isTrue();
		assertThat(new File("test/b.test").exists()).isTrue();

		TestUtils.getPropertyValue(localAcceptOnceFilter, "seenSet", Collection.class).clear();

		new File("test/a.test").delete();
		new File("test/b.test").delete();
		// the remote filter should prevent a re-fetch
		nothing = ms.receive();
		assertThat(nothing).isNull();

		ms.stop();
		verify(synchronizer).close();
		verify(store).close();
	}

	public static class TestSftpSessionFactory extends DefaultSftpSessionFactory {

		private final Vector<LsEntry> sftpEntries = new Vector<>();

		private void init() {
			String[] files = new File("remote-test-dir").list();
			for (String fileName : files) {
				LsEntry lsEntry = mock(LsEntry.class);
				SftpATTRS attributes = mock(SftpATTRS.class);
				when(lsEntry.getAttrs()).thenReturn(attributes);

				Calendar calendar = Calendar.getInstance();
				calendar.add(Calendar.DATE, 1);
				when(lsEntry.getAttrs().getMTime())
						.thenReturn(Long.valueOf(calendar.getTimeInMillis() / 1000).intValue());
				when(lsEntry.getFilename()).thenReturn(fileName);
				when(lsEntry.getLongname()).thenReturn(fileName);
				sftpEntries.add(lsEntry);
			}
		}

		@Override
		public SftpSession getSession() {
			if (this.sftpEntries.size() == 0) {
				this.init();
			}

			try {
				ChannelSftp channel = mock(ChannelSftp.class);

				String[] files = new File("remote-test-dir").list();
				for (String fileName : files) {
					when(channel.get("remote-test-dir/" + fileName))
							.thenReturn(new FileInputStream("remote-test-dir/" + fileName));
				}
				when(channel.ls("remote-test-dir")).thenReturn(sftpEntries);

				when(jschSession.openChannel("sftp")).thenReturn(channel);
				return SftpTestSessionFactory.createSftpSession(jschSession);
			}
			catch (Exception e) {
				throw new RuntimeException("Failed to create mock sftp session", e);
			}
		}

	}

}
