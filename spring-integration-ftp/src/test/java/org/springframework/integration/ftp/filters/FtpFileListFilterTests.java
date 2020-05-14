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

package org.springframework.integration.ftp.filters;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.List;

import org.apache.commons.net.ftp.FTPFile;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.ftp.FtpTestSupport;
import org.springframework.integration.ftp.session.FtpRemoteFileTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Gary Russell
 * @since 5.0
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class FtpFileListFilterTests extends FtpTestSupport {

	@Autowired
	private FtpRemoteFileTemplate template;

	@Test
	public void testMarkerFile() throws Exception {
		FtpSystemMarkerFilePresentFileListFilter filter = new FtpSystemMarkerFilePresentFileListFilter(
				new FtpSimplePatternFileListFilter("*.txt"));
		FTPFile[] files = template.list("ftpSource");
		assertThat(files.length).isGreaterThan(0);
		List<FTPFile> filtered = filter.filterFiles(files);
		assertThat(filtered.size()).isEqualTo(0);
		File remoteDir = getSourceRemoteDirectory();
		File marker = new File(remoteDir, "ftpSource2.txt.complete");
		marker.createNewFile();
		files = template.list("ftpSource");
		filtered = filter.filterFiles(files);
		assertThat(filtered.size()).isEqualTo(1);
		assertThat(filtered.get(0).getName()).isEqualTo("ftpSource2.txt");
		marker.delete();
	}

	@Configuration
	public static class Config {

		@Bean
		public SessionFactory<FTPFile> ftpSessionFactory() {
			return FtpFileListFilterTests.sessionFactory();
		}

		@Bean
		public FtpRemoteFileTemplate remoteFileTempalte() {
			return new FtpRemoteFileTemplate(ftpSessionFactory());
		}

	}


}
