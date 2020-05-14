/*
 * Copyright 2017-2020 the original author or authors.
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

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.sftp.SftpTestSupport;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.jcraft.jsch.ChannelSftp.LsEntry;

/**
 * @author Gary Russell
 * @since 5.0
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class SftpFileListFilterTests extends SftpTestSupport {

	@Autowired
	private SftpRemoteFileTemplate template;

	@Test
	public void testMarkerFile() throws Exception {
		SftpSystemMarkerFilePresentFileListFilter filter = new SftpSystemMarkerFilePresentFileListFilter(
				new SftpSimplePatternFileListFilter("*.txt"));
		LsEntry[] files = template.list("sftpSource");
		assertThat(files.length).isGreaterThan(0);
		List<LsEntry> filtered = filter.filterFiles(files);
		assertThat(filtered.size()).isEqualTo(0);
		File remoteDir = getSourceRemoteDirectory();
		File marker = new File(remoteDir, "sftpSource2.txt.complete");
		marker.createNewFile();
		files = template.list("sftpSource");
		filtered = filter.filterFiles(files);
		assertThat(filtered.size()).isEqualTo(1);
		assertThat(filtered.get(0).getFilename()).isEqualTo("sftpSource2.txt");
		marker.delete();
	}

	@Configuration
	public static class Config {

		@Bean
		public SessionFactory<LsEntry> sftpSessionFactory() {
			return SftpFileListFilterTests.sessionFactory();
		}

		@Bean
		public SftpRemoteFileTemplate remoteFileTempalte() {
			return new SftpRemoteFileTemplate(sftpSessionFactory());
		}

	}

}
