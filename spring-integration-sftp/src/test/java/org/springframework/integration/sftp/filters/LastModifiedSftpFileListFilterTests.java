/*
 * Copyright 2015-2023 the original author or authors.
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

import org.apache.sshd.sftp.client.SftpClient;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.sftp.SftpTestSupport;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Adama Sorho
 * @since 6.2
 */
@SpringJUnitConfig
@DirtiesContext
public class LastModifiedSftpFileListFilterTests extends SftpTestSupport {

	@Autowired
	private SftpRemoteFileTemplate sftpRemoteFileTemplate;

	@Test
	public void testAge() {
		LastModifiedSftpFileListFilter filter = new LastModifiedSftpFileListFilter();
		filter.setAge(60);
		SftpClient.DirEntry[] files = sftpRemoteFileTemplate.list("sftpSource");
		assertThat(files.length).isGreaterThan(0);
		assertThat(filter.filterFiles(files)).hasSize(0);
		SftpClient.DirEntry sftFile = files[1];
		assertThat(filter.accept(sftFile)).isFalse();

		// Make a file as of yesterday's
		final FileTime fileTime = FileTime.fromMillis(System.currentTimeMillis() - 1000 * 60 * 60 * 24);
		sftFile.getAttributes().setModifyTime(fileTime);
		assertThat(filter.filterFiles(files)).hasSize(1);
		assertThat(filter.accept(sftFile)).isTrue();
	}

	@Configuration
	public static class Config {

		@Bean
		public SessionFactory<SftpClient.DirEntry> sftpFileSessionFactory() {
			return LastModifiedSftpFileListFilterTests.sessionFactory();
		}

		@Bean
		public SftpRemoteFileTemplate sftpRemoteFileTemplate() {
			return new SftpRemoteFileTemplate(sftpFileSessionFactory());
		}
	}
}
