/*
 * Copyright 2002-2010 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.sftp.impl;

import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.integration.file.synchronization.AbstractInboundRemoteFileSystemSychronizer.EntryAcknowledgmentStrategy;
import org.springframework.integration.sftp.SftpSession;
import org.springframework.util.ReflectionUtils;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;

/**
 * @author Oleg Zhurakousky
 *
 */
public class SftpInboundRemoteFileSystemSynchronizerTests {
	@Before
	public void prepare() throws Exception{
		File foo = new File("target/foo.txt");
		if (foo.exists()){
			foo.delete();
		}
		File bar = new File("target/bar.txt");
		if (bar.exists()){
			bar.delete();
		}
		if (!foo.createNewFile()){
			throw new IOException("Can not create test file 'foo.txt' in the 'temp' directory");
		}
		foo.deleteOnExit();
		bar.deleteOnExit();
	}
	
	/**
	 * Asserts that if local file with the name provided exists, then this method 
	 * will return 'true', although no copy will be performed. Just a pass through.
	 * @throws Exception
	 */
	@Test
	public void testCopyAndRenameWhenLocalFileExists() throws Exception {
		SftpInboundRemoteFileSystemSynchronizer synchronizer = new SftpInboundRemoteFileSystemSynchronizer();
		Method method = 
			ReflectionUtils.findMethod(synchronizer.getClass(), "copyFromRemoteToLocalDirectory", SftpSession.class, LsEntry.class, Resource.class);
		method.setAccessible(true);
		SftpSession session = mock(SftpSession.class);
		LsEntry entry = mock(LsEntry.class);
		when(entry.getFilename()).thenReturn("foo.txt");
		Resource localDir = new FileSystemResource(new File("target"));
		boolean success = (Boolean) method.invoke(synchronizer, session, entry, localDir);
		assertTrue(success);
	}
	/**
	 * Asserts that if local file with the name provided doesn't exists, then this method 
	 * will perfrom copy file and cleanup of the open resource INT-1433, meaning
	 * it should not result in exception.
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@org.junit.Ignore
	@Test
	public void testCopyAndRenameWhenLocalFileDoesntExist() throws Exception {
		SftpInboundRemoteFileSystemSynchronizer synchronizer = new SftpInboundRemoteFileSystemSynchronizer();
		synchronizer.setEntryAcknowledgmentStrategy(mock(EntryAcknowledgmentStrategy.class));
		Method method = 
			ReflectionUtils.findMethod(synchronizer.getClass(), "copyFromRemoteToLocalDirectory", SftpSession.class, LsEntry.class, Resource.class);
		method.setAccessible(true);
		SftpSession session = mock(SftpSession.class);
		ChannelSftp channelSftp = mock(ChannelSftp.class);
		when(session.getChannel()).thenReturn(channelSftp);
		File originalFile = new File("pom.xml");
		when(channelSftp.get("null/bar.txt")).thenReturn(new FileInputStream(originalFile));
		LsEntry entry = mock(LsEntry.class);
		when(entry.getFilename()).thenReturn("bar.txt");
		Resource localDir = new FileSystemResource(new File("target"));
		boolean success = (Boolean) method.invoke(synchronizer, session, entry, localDir);
		assertTrue(success);
		File file = new File("target/bar.txt");
		assertTrue(file.exists());
		assertTrue(file.length() == originalFile.length());
	}
}
