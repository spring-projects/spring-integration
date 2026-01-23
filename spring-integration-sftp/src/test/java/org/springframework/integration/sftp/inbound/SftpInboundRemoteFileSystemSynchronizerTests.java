/*
 * Copyright 2002-present the original author or authors.
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

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.sshd.sftp.client.SftpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
import org.springframework.integration.test.support.TestApplicationContextAware;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 * @author Joaquin Santana
 * @author Darryl Smith
 * @author Glenn Renfro
 *
 * @since 2.0
 */
public class SftpInboundRemoteFileSystemSynchronizerTests implements TestApplicationContextAware {

	@BeforeEach
	@AfterEach
	public void cleanup() {
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
		SftpPersistentAcceptOnceFileListFilter persistFilter = new SftpPersistentAcceptOnceFileListFilter(store, "foo");
		List<FileListFilter<SftpClient.DirEntry>> filters = new ArrayList<>();
		filters.add(persistFilter);
		filters.add(patternFilter);
		CompositeFileListFilter<SftpClient.DirEntry> filter = new CompositeFileListFilter<>(filters);
		synchronizer.setFilter(filter);
		synchronizer.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		synchronizer.afterPropertiesSet();

		SftpInboundFileSynchronizingMessageSource ms = new SftpInboundFileSynchronizingMessageSource(synchronizer);
		ms.setAutoCreateLocalDirectory(true);
		ms.setLocalDirectory(localDirectory);
		ms.setBeanFactory(TEST_INTEGRATION_CONTEXT);
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
				TestUtils.getPropertyValue(synchronizer, "remoteFileMetadataStore.metadata");

		String next = remoteFileMetadataStore.values().iterator().next();
		assertThat(URI.create(next).getHost()).isEqualTo("mock.sftp.host");

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

		TestUtils.<Set<?>>getPropertyValue(localAcceptOnceFilter, "seenSet").clear();

		new File("test/a.test").delete();
		new File("test/b.test").delete();
		// the remote filter should prevent a re-fetch
		nothing = ms.receive();
		assertThat(nothing).isNull();

		ms.stop();
		verify(synchronizer).close();
		verify(store).close();

		ftpSessionFactory.destroy();
	}

	public static class TestSftpSessionFactory extends DefaultSftpSessionFactory {

		private List<SftpClient.DirEntry> sftpEntries;

		private void init() {
			String[] files = new File("remote-test-dir").list();
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.DATE, 1);
			this.sftpEntries =
					Arrays.stream(files)
							.map((file) -> {
								SftpClient.Attributes attributes = spy(new SftpClient.Attributes());
								attributes.setModifyTime(FileTime.fromMillis(calendar.getTimeInMillis()));
								given(attributes.isRegularFile()).willReturn(true);
								return new SftpClient.DirEntry(file, file, attributes);
							})
							.toList();
		}

		@Override
		public SftpSession getSession() {
			if (this.sftpEntries == null) {
				init();
			}

			try {
				SftpClient sftpClient = mock(SftpClient.class);
				when(sftpClient.canonicalPath("remote-test-dir")).thenReturn("/remote-test-dir");

				String[] files = new File("remote-test-dir").list();
				for (String fileName : files) {
					String remoteFilePath = "remote-test-dir/" + fileName;
					when(sftpClient.canonicalPath(remoteFilePath))
							.thenReturn("/" + remoteFilePath);
					when(sftpClient.read("/" + remoteFilePath))
							.thenReturn(new FileInputStream(remoteFilePath));
				}
				when(sftpClient.readDir("/remote-test-dir")).thenReturn(this.sftpEntries);

				return SftpTestSessionFactory.createSftpSession(sftpClient);
			}
			catch (Exception e) {
				throw new RuntimeException("Failed to create mock sftp session", e);
			}
		}

	}

}
