/*
 * Copyright 2014-2020 the original author or authors.
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

package org.springframework.integration.sftp.filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

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
		assertThat(Arrays.equals(files, passed.toArray())).isTrue();
		List<LsEntry> now = filter.filterFiles(files);
		assertThat(now.size()).isEqualTo(0);
		filter.rollback(passed.get(1), passed);
		now = filter.filterFiles(files);
		assertThat(now.size()).isEqualTo(2);
		assertThat(now.get(0).getFilename()).isEqualTo("bar");
		assertThat(now.get(1).getFilename()).isEqualTo("baz");
		now = filter.filterFiles(files);
		assertThat(now.size()).isEqualTo(0);
		filter.close();
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
		assertThat(now.size()).isEqualTo(2);
		assertThat(now.get(0).getFilename()).isEqualTo("foo");
		assertThat(now.get(1).getFilename()).isEqualTo("bar");
		filter.close();
	}

}
