/*
 * Copyright 2002-2010 the original author or authors.
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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.integration.Message;
import org.springframework.integration.ftp.filters.FtpPatternMatchingFileListFilter;
import org.springframework.integration.ftp.session.AbstractFtpSessionFactory;

/**
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class FtpInboundRemoteFileSystemSynchronizerTest {
	
	private static FTPClient ftpClient = mock(FTPClient.class);
	
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
		ftpSessionFactory.setRemoteWorkingDirectory("remote-test-dir");

		FtpInboundFileSynchronizingMessageSource ms = 
			new FtpInboundFileSynchronizingMessageSource();
		
		FtpInboundFileSynchronizer synchronizer = spy(new FtpInboundFileSynchronizer(ftpSessionFactory));
		synchronizer.setDeleteRemoteFiles(true);
		synchronizer.setRemoteDirectory("remote-test-dir");
		synchronizer.setFilter(new FtpPatternMatchingFileListFilter(".*\\.test$"));
		
		ms.setSynchronizer(synchronizer);
		ms.setAutoCreateDirectories(true);

		ms.setLocalDirectory(localDirectoy);
		ms.afterPropertiesSet();
		Message<File> atestFile =  ms.receive();
		assertNotNull(atestFile);
		assertEquals("a.test", atestFile.getPayload().getName());
		Message<File> btestFile =  ms.receive();
		assertNotNull(btestFile);
		assertEquals("b.test", btestFile.getPayload().getName());
		Message<File> nothing =  ms.receive();
		assertNull(nothing);
		
		// two times becouse on teh third receive (above) the internal queue will be empty, so it will attempt
		verify(synchronizer, times(2)).synchronizeToLocalDirectory(localDirectoy);

		assertTrue(new File("test/a.test").exists());
		assertTrue(new File("test/b.test").exists());
	}
	
	public static class TestFtpSessionFactory extends AbstractFtpSessionFactory<FTPClient> {
		
		@Override
		protected FTPClient createSingleInstanceOfClient() {
			try {
				when(ftpClient.getReplyCode()).thenReturn(250);
				when(ftpClient.login("kermit", "frog")).thenReturn(true);
				when(ftpClient.changeWorkingDirectory(Mockito.anyString())).thenReturn(true);
				
				String[] files = new File("remote-test-dir").list();
				Collection<Object> ftpFiles = new ArrayList<Object>();
				for (String fileName : files) {
					FTPFile file = new FTPFile();
					file.setName(fileName);
					file.setType(FTPFile.FILE_TYPE);
					ftpFiles.add(file);
					when(ftpClient.retrieveFileStream(fileName)).thenReturn(new FileInputStream("remote-test-dir/" + fileName));
				}
				when(ftpClient.listFiles("remote-test-dir")).thenReturn(ftpFiles.toArray(new FTPFile[]{}));
						
				return ftpClient;
			} catch (Exception e) {
				throw new RuntimeException("Failed to create mock client", e);
			}
		}
	}
}
