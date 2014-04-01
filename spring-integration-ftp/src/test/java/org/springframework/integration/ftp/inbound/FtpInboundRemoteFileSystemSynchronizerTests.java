/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.integration.ftp.inbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Queue;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.ftp.filters.FtpPersistentAcceptOnceFileListFilter;
import org.springframework.integration.ftp.filters.FtpRegexPatternFileListFilter;
import org.springframework.integration.ftp.session.AbstractFtpSessionFactory;
import org.springframework.integration.metadata.PropertiesPersistingMetadataStore;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.0
 */
public class FtpInboundRemoteFileSystemSynchronizerTests {

	private static FTPClient ftpClient = mock(FTPClient.class);

	@Before
	@After
	public void cleanup(){
		File file = new File("test");
		if (file.exists()){
			String[] files = file.list();
			for (String fileName : files) {
				new File(file, fileName).delete();
			}
			file.delete();
		}
	}

	@Test
	public void testCopyFileToLocalDir() throws Exception {
		File localDirectoy = new File("test");
		assertFalse(localDirectoy.exists());

		TestFtpSessionFactory ftpSessionFactory = new TestFtpSessionFactory();
		ftpSessionFactory.setUsername("kermit");
		ftpSessionFactory.setPassword("frog");
		ftpSessionFactory.setHost("foo.com");
		FtpInboundFileSynchronizer synchronizer = spy(new FtpInboundFileSynchronizer(ftpSessionFactory));
		synchronizer.setDeleteRemoteFiles(true);
		synchronizer.setPreserveTimestamp(true);
		synchronizer.setRemoteDirectory("remote-test-dir");
		FtpRegexPatternFileListFilter patternFilter = new FtpRegexPatternFileListFilter(".*\\.test$");
		PropertiesPersistingMetadataStore store = new PropertiesPersistingMetadataStore();
		store.setBaseDirectory("test");
		FtpPersistentAcceptOnceFileListFilter persistFilter =
				new FtpPersistentAcceptOnceFileListFilter(store, "foo");
		List<FileListFilter<FTPFile>> filters = new ArrayList<FileListFilter<FTPFile>>();
		filters.add(persistFilter);
		filters.add(patternFilter);
		CompositeFileListFilter<FTPFile> filter = new CompositeFileListFilter<FTPFile>(filters);
		synchronizer.setFilter(filter);
		synchronizer.setIntegrationEvaluationContext(ExpressionUtils.createStandardEvaluationContext());

		ExpressionParser expressionParser = new SpelExpressionParser(new SpelParserConfiguration(true, true));
		Expression expression = expressionParser.parseExpression("#this.toUpperCase() + '.a'");
		synchronizer.setLocalFilenameGeneratorExpression(expression);

		FtpInboundFileSynchronizingMessageSource ms =
				new FtpInboundFileSynchronizingMessageSource(synchronizer);

		ms.setAutoCreateLocalDirectory(true);

		ms.setLocalDirectory(localDirectoy);
		ms.setBeanFactory(mock(BeanFactory.class));
		ms.afterPropertiesSet();
		Message<File> atestFile =  ms.receive();
		assertNotNull(atestFile);
		assertEquals("A.TEST.a", atestFile.getPayload().getName());
		// The test remote files are created with the current timestamp + 1 day.
		assertThat(atestFile.getPayload().lastModified(), Matchers.greaterThan(System.currentTimeMillis()));

		Message<File> btestFile =  ms.receive();
		assertNotNull(btestFile);
		assertEquals("B.TEST.a", btestFile.getPayload().getName());
		// The test remote files are created with the current timestamp + 1 day.
		assertThat(atestFile.getPayload().lastModified(), Matchers.greaterThan(System.currentTimeMillis()));

		Message<File> nothing =  ms.receive();
		assertNull(nothing);

		// two times because on the third receive (above) the internal queue will be empty, so it will attempt
		verify(synchronizer, times(2)).synchronizeToLocalDirectory(localDirectoy);

		assertTrue(new File("test/A.TEST.a").exists());
		assertTrue(new File("test/B.TEST.a").exists());

		TestUtils.getPropertyValue(ms, "localFileListFilter.seen", Queue.class).clear();

		new File("test/A.TEST.a").delete();
		new File("test/B.TEST.a").delete();
		// the remote filter should prevent a re-fetch
		nothing =  ms.receive();
		assertNull(nothing);

	}


	public static class TestFtpSessionFactory extends AbstractFtpSessionFactory<FTPClient> {

		private final Collection<Object> ftpFiles = new ArrayList<Object>();

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
					when(ftpClient.retrieveFile(Mockito.eq("remote-test-dir/" + fileName) , Mockito.any(OutputStream.class))).thenReturn(true);
				}
				when(ftpClient.listFiles("remote-test-dir")).thenReturn(ftpFiles.toArray(new FTPFile[]{}));
				when(ftpClient.deleteFile(Mockito.anyString())).thenReturn(true);
				return ftpClient;
			} catch (Exception e) {
				throw new RuntimeException("Failed to create mock client", e);
			}
		}
	}
}
