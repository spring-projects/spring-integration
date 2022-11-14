/*
 * Copyright 2014-2022 the original author or authors.
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

import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.apache.sshd.sftp.client.SftpClient;
import org.junit.jupiter.api.Test;

import org.springframework.integration.metadata.SimpleMetadataStore;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author David Liu
 * @author Artem Bilan
 *
 * @since 4.0.4
 *
 */
public class SftpPersistentAcceptOnceFileListFilterTests {

	@Test
	public void testRollback() throws Exception {
		SftpPersistentAcceptOnceFileListFilter filter = new SftpPersistentAcceptOnceFileListFilter(
				new SimpleMetadataStore(), "rollback:");
		SftpClient.Attributes attrs = new SftpClient.Attributes();
		attrs.setModifyTime(FileTime.from(Instant.now()));
		SftpClient.DirEntry sftpFile1 = new SftpClient.DirEntry("foo", "foo", attrs);
		SftpClient.DirEntry sftpFile2 = new SftpClient.DirEntry("bar", "bar", attrs);
		SftpClient.DirEntry sftpFile3 = new SftpClient.DirEntry("baz", "baz", attrs);
		SftpClient.DirEntry[] files = new SftpClient.DirEntry[] {sftpFile1, sftpFile2, sftpFile3};
		List<SftpClient.DirEntry> passed = filter.filterFiles(files);
		assertThat(Arrays.equals(files, passed.toArray())).isTrue();
		List<SftpClient.DirEntry> now = filter.filterFiles(files);
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
		SftpClient.Attributes attrs = new SftpClient.Attributes();
		attrs.setModifyTime(FileTime.from(Instant.now()));
		SftpClient.DirEntry sftpFile1 = new SftpClient.DirEntry("foo", "same", attrs);
		SftpClient.DirEntry sftpFile2 = new SftpClient.DirEntry("bar", "same", attrs);
		SftpClient.DirEntry[] files = new SftpClient.DirEntry[] {sftpFile1, sftpFile2};
		List<SftpClient.DirEntry> now = filter.filterFiles(files);
		assertThat(now.size()).isEqualTo(2);
		assertThat(now.get(0).getFilename()).isEqualTo("foo");
		assertThat(now.get(1).getFilename()).isEqualTo("bar");
		filter.close();
	}

}
