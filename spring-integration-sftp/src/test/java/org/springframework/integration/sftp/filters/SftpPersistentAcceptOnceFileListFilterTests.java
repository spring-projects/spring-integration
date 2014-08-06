/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.integration.sftp.filters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import org.springframework.integration.metadata.SimpleMetadataStore;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.SftpATTRS;

/**
 * @author Gary Russell
 * @author David Liu
 * @since 4.0.4
 *
 */
public class SftpPersistentAcceptOnceFileListFilterTests {

	@Test
	public void testRollback() throws Exception {
		SftpPersistentAcceptOnceFileListFilter filter = new SftpPersistentAcceptOnceFileListFilter(
				new SimpleMetadataStore(), "rollback:");
		ChannelSftp channel = new ChannelSftp();
		SftpATTRS attrs = mock(SftpATTRS.class);
		@SuppressWarnings("unchecked")
		Constructor<LsEntry> ctor = (Constructor<LsEntry>) LsEntry.class.getDeclaredConstructors()[0];
		ctor.setAccessible(true);
		LsEntry sftpFile1 = ctor.newInstance(channel, "foo", "foo", attrs);
		LsEntry sftpFile2 = ctor.newInstance(channel, "bar", "bar", attrs);
		LsEntry ftpFile3 = ctor.newInstance(channel, "baz", "baz", attrs);
		LsEntry[] files = new LsEntry[] {sftpFile1, sftpFile2, ftpFile3};
		List<LsEntry> passed = filter.filterFiles(files);
		assertTrue(Arrays.equals(files, passed.toArray()));
		List<LsEntry> now = filter.filterFiles(files);
		assertEquals(0, now.size());
		filter.rollback(passed.get(1), passed);
		now = filter.filterFiles(files);
		assertEquals(2, now.size());
		assertEquals("bar", now.get(0).getFilename());
		assertEquals("baz", now.get(1).getFilename());
		now = filter.filterFiles(files);
		assertEquals(0, now.size());
	}

	@Test
	public void testKeyUsingFileName() throws Exception {
		SftpPersistentAcceptOnceFileListFilter filter = new SftpPersistentAcceptOnceFileListFilter(
				new SimpleMetadataStore(), "rollback:");
		ChannelSftp channel = new ChannelSftp();
		SftpATTRS attrs = mock(SftpATTRS.class);
		@SuppressWarnings("unchecked")
		Constructor<LsEntry> ctor = (Constructor<LsEntry>) LsEntry.class.getDeclaredConstructors()[0];
		ctor.setAccessible(true);
		LsEntry sftpFile1 = ctor.newInstance(channel, "foo", "same", attrs);
		LsEntry sftpFile2 = ctor.newInstance(channel, "bar", "same", attrs);
		LsEntry[] files = new LsEntry[] {sftpFile1, sftpFile2};
		List<LsEntry> now = filter.filterFiles(files);
		assertEquals(2, now.size());
		assertEquals("foo", now.get(0).getFilename());
		assertEquals("bar", now.get(1).getFilename());
	}

}
