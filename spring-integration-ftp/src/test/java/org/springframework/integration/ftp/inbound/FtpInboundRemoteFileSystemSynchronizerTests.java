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

package org.springframework.integration.ftp.inbound;

import java.io.File;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.RecursiveDirectoryScanner;
import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.filters.RegexPatternFileListFilter;
import org.springframework.integration.ftp.filters.FtpPersistentAcceptOnceFileListFilter;
import org.springframework.integration.ftp.filters.FtpRegexPatternFileListFilter;
import org.springframework.integration.ftp.session.AbstractFtpSessionFactory;
import org.springframework.integration.metadata.MetadataStore;
import org.springframework.integration.metadata.PropertiesPersistingMetadataStore;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.integration.test.support.TestApplicationContextAware;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;
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
 * @author Glenn Renfro
 *
 * @since 2.0
 */
public class FtpInboundRemoteFileSystemSynchronizerTests implements TestApplicationContextAware {

	private static final FTPClient ftpClient = mock(FTPClient.class);

	@BeforeEach
	@AfterEach
	public void cleanup() {
		recursiveDelete(new File("test"));
	}

	@Test
	public void testCopyFileToLocalDir() throws Exception {
		File localDirectory = new File("test");
		assertThat(localDirectory.exists()).isFalse();
		MetadataStore remoteFileMetadataStore = new SimpleMetadataStore();
		TestFtpSessionFactory ftpSessionFactory = new TestFtpSessionFactory();
		ftpSessionFactory.setUsername("kermit");
		ftpSessionFactory.setPassword("frog");
		ftpSessionFactory.setHost("some-host.com");
		FtpInboundFileSynchronizer synchronizer = spy(new FtpInboundFileSynchronizer(ftpSessionFactory));
		synchronizer.setDeleteRemoteFiles(true);
		synchronizer.setPreserveTimestamp(true);
		synchronizer.setRemoteDirectory("remote-test-dir");
		synchronizer.setRemoteFileMetadataStore(remoteFileMetadataStore);
		synchronizer.setMetadataStorePrefix("ftpPollingTest:");
		FtpRegexPatternFileListFilter patternFilter = new FtpRegexPatternFileListFilter(".*\\.test$");
		PropertiesPersistingMetadataStore store = spy(new PropertiesPersistingMetadataStore());
		store.setBaseDirectory("test");
		store.afterPropertiesSet();
		FtpPersistentAcceptOnceFileListFilter persistFilter =
				new FtpPersistentAcceptOnceFileListFilter(store, "someKey/");
		List<FileListFilter<FTPFile>> filters = new ArrayList<>();
		filters.add(persistFilter);
		filters.add(patternFilter);
		CompositeFileListFilter<FTPFile> filter = new CompositeFileListFilter<>(filters);
		synchronizer.setFilter(filter);

		ExpressionParser expressionParser = new SpelExpressionParser(new SpelParserConfiguration(true, true));
		Expression expression = expressionParser.parseExpression("'subdir/' + #this.toUpperCase() + '.a'");
		synchronizer.setLocalFilenameGeneratorExpression(expression);
		synchronizer.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		synchronizer.afterPropertiesSet();

		FtpInboundFileSynchronizingMessageSource ms = new FtpInboundFileSynchronizingMessageSource(synchronizer);

		ms.setAutoCreateLocalDirectory(true);
		ms.setLocalDirectory(localDirectory);
		ms.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		CompositeFileListFilter<File> localFileListFilter = new CompositeFileListFilter<>();
		localFileListFilter.addFilter(new RegexPatternFileListFilter(".*\\.TEST\\.a$"));
		AcceptOnceFileListFilter<File> localAcceptOnceFilter = new AcceptOnceFileListFilter<>();
		localFileListFilter.addFilter(localAcceptOnceFilter);
		RecursiveDirectoryScanner scanner = new RecursiveDirectoryScanner();
		ms.setScanner(scanner);
		ms.setLocalFilter(localFileListFilter);
		ms.afterPropertiesSet();
		ms.start();

		Message<File> atestFile = ms.receive();
		assertThat(atestFile).isNotNull();
		assertThat(atestFile.getPayload().getName()).isEqualTo("A.TEST.a");
		// The test remote files are created with the current timestamp + 1 day.
		assertThat(atestFile.getPayload().lastModified()).isGreaterThan(System.currentTimeMillis());

		assertThat(atestFile.getHeaders().get(FileHeaders.FILENAME)).isEqualTo("A.TEST.a");

		Message<File> btestFile = ms.receive();
		assertThat(btestFile).isNotNull();
		assertThat(btestFile.getPayload().getName()).isEqualTo("B.TEST.a");
		// The test remote files are created with the current timestamp + 1 day.
		assertThat(atestFile.getPayload().lastModified()).isGreaterThan(System.currentTimeMillis());

		Message<File> nothing = ms.receive();
		assertThat(nothing).isNull();

		// two times because on the third receive (above) the internal queue will be empty, so it will attempt
		verify(synchronizer, times(2)).synchronizeToLocalDirectory(localDirectory, Integer.MIN_VALUE);

		assertThat(new File("test/subdir/A.TEST.a").exists()).isTrue();
		assertThat(new File("test/subdir/B.TEST.a").exists()).isTrue();

		TestUtils.<HashSet<?>>getPropertyValue(localAcceptOnceFilter, "seenSet").clear();

		File aFile = new File("test/subdir/A.TEST.a");
		aFile.delete();
		synchronizer.removeRemoteFileMetadata(aFile);
		File bFile = new File("test/subdir/B.TEST.a");
		bFile.delete();
		synchronizer.removeRemoteFileMetadata(bFile);
		// the remote filter should prevent a re-fetch
		nothing = ms.receive();
		assertThat(nothing).isNull();

		ms.stop();
		verify(synchronizer).close();
		verify(store).close();

		Map<?, ?> metadata = TestUtils.getPropertyValue(remoteFileMetadataStore, "metadata");
		assertThat(metadata).isEmpty();

		Properties metadataProperties = TestUtils.getPropertyValue(store, "metadata");
		metadataProperties.stringPropertyNames()
				.forEach(name -> assertThat(name).startsWith("someKey/remote-test-dir/"));
	}

	@Test
	public void testSyncRemoteFileOnlyOnceByDefault() {
		File localDirectory = new File("test");
		localDirectory.mkdir();

		TestFtpSessionFactory ftpSessionFactory = new TestFtpSessionFactory();
		ftpSessionFactory.setUsername("kermit");
		ftpSessionFactory.setPassword("frog");
		FtpInboundFileSynchronizer synchronizer = spy(new FtpInboundFileSynchronizer(ftpSessionFactory));
		synchronizer.setRemoteDirectory("remote-test-dir");

		synchronizer.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		synchronizer.afterPropertiesSet();

		synchronizer.synchronizeToLocalDirectory(localDirectory);

		File[] files = localDirectory.listFiles();
		assertThat(files.length).isEqualTo(3);

		for (File f : files) {
			f.delete();
		}

		synchronizer.synchronizeToLocalDirectory(localDirectory);

		assertThat(localDirectory.list().length).isEqualTo(0);
	}

	private static void recursiveDelete(File file) {
		if (file != null && file.exists()) {
			File[] files = file.listFiles();
			if (files != null) {
				for (File f : files) {
					if (f.isDirectory()) {
						recursiveDelete(f);
					}
					else {
						f.delete();
					}
				}
			}
			file.delete();
		}
	}

	public static class TestFtpSessionFactory extends AbstractFtpSessionFactory<FTPClient> {

		private final Collection<FTPFile> ftpFiles = new ArrayList<>();

		private void init() {
			String[] files = new File("remote-test-dir").list();
			for (String fileName : files) {
				FTPFile file = new FTPFile();
				file.setName(fileName);
				file.setType(FTPFile.FILE_TYPE);
				Calendar calendar = Calendar.getInstance();
				calendar.add(Calendar.DATE, 1);
				file.setTimestamp(calendar);
				ftpFiles.add(file);
			}

		}

		@Override
		protected FTPClient createClientInstance() {
			if (this.ftpFiles.size() == 0) {
				this.init();
			}

			try {
				when(ftpClient.getReplyCode()).thenReturn(250);
				when(ftpClient.login("kermit", "frog")).thenReturn(true);
				when(ftpClient.changeWorkingDirectory(Mockito.anyString())).thenReturn(true);

				String[] files = new File("remote-test-dir").list();
				for (String fileName : files) {
					when(ftpClient.retrieveFile(Mockito.eq("remote-test-dir/" + fileName),
							Mockito.any(OutputStream.class))).thenReturn(true);
				}
				when(ftpClient.listFiles("remote-test-dir"))
						.thenReturn(ftpFiles.toArray(new FTPFile[0]));
				when(ftpClient.deleteFile(Mockito.anyString())).thenReturn(true);
				when(ftpClient.getRemoteAddress()).thenReturn(InetAddress.getByName("localhost"));
				when(ftpClient.getRemotePort()).thenReturn(-1);
				return ftpClient;
			}
			catch (Exception e) {
				throw new RuntimeException("Failed to create mock client", e);
			}
		}

	}

}
