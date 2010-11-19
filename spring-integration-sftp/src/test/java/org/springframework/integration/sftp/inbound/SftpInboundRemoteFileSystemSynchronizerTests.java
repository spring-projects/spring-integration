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
package org.springframework.integration.sftp.inbound;

import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.util.Vector;

import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.sftp.session.SftpSession;
import org.springframework.integration.sftp.session.SftpSessionFactory;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.SftpATTRS;

/**
 * @author Oleg Zhurakousky
 *
 */
public class SftpInboundRemoteFileSystemSynchronizerTests {
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testCopyFileToLocalDir() throws Exception {
		File file = new File(System.getProperty("java.io.tmpdir") + "/foo.txt");
		if (file.exists()){
			file.delete();
		}
		SftpSessionFactory sessionFactory = mock(SftpSessionFactory.class);
		SftpInboundSynchronizer syncronizer = new SftpInboundSynchronizer(sessionFactory);
		syncronizer.setRemotePath("foo/bar");
		
		FileListFilter filter = mock(FileListFilter.class);
		
		syncronizer.setFilter(filter);
		
		
		SftpSession sftpSession = mock(SftpSession.class);
		
		when(sessionFactory.getSession()).thenReturn(sftpSession);
		ChannelSftp channel = mock(ChannelSftp.class);
		when(channel.get((String) Mockito.any())).thenReturn(new FileInputStream(new File("template.mf")));
		when(sftpSession.getChannel()).thenReturn(channel);
		Vector<LsEntry> entries = new Vector<ChannelSftp.LsEntry>();
		LsEntry entry = mock(LsEntry.class);
		SftpATTRS attr = mock(SftpATTRS.class);
		when(attr.isDir()).thenReturn(false);
		when(attr.isLink()).thenReturn(false);
		when(entry.getFilename()).thenReturn("foo.txt");
		when(entry.getAttrs()).thenReturn(attr);
		entries.add(entry);
		when(channel.ls("foo/bar")).thenReturn(entries);
		when(filter.filterFiles((Object[]) Mockito.any())).thenReturn(entries);

		syncronizer.setShouldDeleteSourceFile(true);
		syncronizer.afterPropertiesSet();
		
		Resource localDirectory = new FileSystemResource(System.getProperty("java.io.tmpdir"));
		syncronizer.syncRemoteToLocalFileSystem(localDirectory);
		
		verify(sessionFactory, times(1)).getSession();
		verify(sftpSession, atLeast(1)).getChannel();
		// will add more validation, but for now this test is mainly to get the test coverage up
	}
}
