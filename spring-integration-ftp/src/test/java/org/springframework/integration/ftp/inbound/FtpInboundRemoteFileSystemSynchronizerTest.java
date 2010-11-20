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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.ftp.filters.FtpPatternMatchingFileListFilter;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;
import org.springframework.integration.ftp.session.FtpSession;

/**
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class FtpInboundRemoteFileSystemSynchronizerTest {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testCopyFileToLocalDir() throws Exception {
		File file = new File(System.getProperty("java.io.tmpdir") + "/foo.txt");
		if (file.exists()){
			file.delete();
		}
		FtpInboundFileSynchronizer syncronizer = new FtpInboundFileSynchronizer();
		FileListFilter filter = new FtpPatternMatchingFileListFilter("foo.txt");
		syncronizer.setFilter(filter);
		
		DefaultFtpSessionFactory factory = mock(DefaultFtpSessionFactory.class);
		FtpSession ftpSession = mock(FtpSession.class);
		//when(ftpSession.sendNoOp()).thenReturn(true);
		when(factory.getSession()).thenReturn(ftpSession);

//		Queue clientPoll = new QueuedFtpClientPool(factory);
//		
//		FTPFile f1 = mock(FTPFile.class);
//		when(f1.isFile()).thenReturn(true);
//		when(f1.getName()).thenReturn("foo.txt");
//		
//		FTPFile[] files = new FTPFile[]{f1};
//		when(ftpClient.listFiles()).thenReturn(files);

		syncronizer.setSessionFactory(factory);
		syncronizer.setShouldDeleteSourceFile(true);
		syncronizer.afterPropertiesSet();
		
		File localDirectory = new File(System.getProperty("java.io.tmpdir"));
		syncronizer.synchronizeToLocalDirectory(localDirectory);
		
		//verify(ftpClient, times(1)).retrieveFile(Mockito.anyString(), Mockito.any(OutputStream.class));
		//verify(ftpClient, times(1)).deleteFile(Mockito.anyString());
	}
}
