/*
 * Copyright 2023 the original author or authors.
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
import java.time.Duration;
import java.time.Instant;

import org.apache.sshd.sftp.client.SftpClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Adama Sorho
 * @author Artem Bilan
 *
 * @since 6.2
 */
public class SftpLastModifiedFileListFilterTests {

	@Test
	public void testAge() {
		SftpLastModifiedFileListFilter filter = new SftpLastModifiedFileListFilter();
		SftpClient.Attributes attributes1 = new SftpClient.Attributes();
		attributes1.setModifyTime(FileTime.from(Instant.now()));
		SftpClient.Attributes attributes2 = new SftpClient.Attributes();
		attributes2.setModifyTime(FileTime.from(Instant.now()));

		SftpClient.DirEntry sftpFile1 = new SftpClient.DirEntry("foo", "foo", attributes1);
		SftpClient.DirEntry sftpFile2 = new SftpClient.DirEntry("bar", "bar", attributes2);
		SftpClient.DirEntry[] files = new SftpClient.DirEntry[] {sftpFile1, sftpFile2};

		assertThat(filter.filterFiles(files)).hasSize(0);
		assertThat(filter.accept(sftpFile2)).isFalse();

		FileTime fileTime = FileTime.from(Instant.now().minus(Duration.ofDays(1)));
		sftpFile2.getAttributes().setModifyTime(fileTime);
		assertThat(filter.filterFiles(files)).hasSize(1);
		assertThat(filter.accept(sftpFile2)).isTrue();
	}

}
