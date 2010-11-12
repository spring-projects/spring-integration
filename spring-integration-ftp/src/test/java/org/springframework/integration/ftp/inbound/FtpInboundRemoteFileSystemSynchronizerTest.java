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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.OutputStream;
import java.util.Arrays;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.core.io.FileSystemResource;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.ftp.client.FtpClientPool;

/**
 * @author Oleg Zhurakousky
 *
 */
public class FtpInboundRemoteFileSystemSynchronizerTest {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testCopyFileToLocalDir() throws Exception {
		File file = new File(System.getProperty("java.io.tmpdir") + "/foo.txt");
		if (file.exists()){
			file.delete();
		}
		FtpInboundRemoteFileSystemSynchronizer syncronizer = new FtpInboundRemoteFileSystemSynchronizer();
		syncronizer.setLocalDirectory(new FileSystemResource(System.getProperty("java.io.tmpdir")));
		FileListFilter filter = mock(FileListFilter.class);
		//
		syncronizer.setFilter(filter);
		
		FtpClientPool clientPoll = mock(FtpClientPool.class);
		FTPClient ftpClient = mock(FTPClient.class);
		FTPFile f1 = mock(FTPFile.class);
		when(f1.isFile()).thenReturn(true);
		when(f1.getName()).thenReturn("foo.txt");

		FTPFile[] files = new FTPFile[]{f1};
		when(ftpClient.listFiles()).thenReturn(files);
		when(clientPoll.getClient()).thenReturn(ftpClient);
		when(filter.filterFiles((Object[]) Mockito.any())).thenReturn(Arrays.asList(files));
		
		syncronizer.setClientPool(clientPoll);
		syncronizer.setShouldDeleteSourceFile(true);
		syncronizer.afterPropertiesSet();
		
		syncronizer.syncRemoteToLocalFileSystem();
		
		verify(ftpClient, times(1)).retrieveFile(Mockito.anyString(), Mockito.any(OutputStream.class));
		verify(ftpClient, times(1)).deleteFile(Mockito.anyString());
		verify(clientPoll, times(1)).releaseClient(ftpClient);
	}
}
