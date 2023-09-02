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

package org.springframework.integration.ftp.filters;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Adama Sorho
 * @since 6.2
 */
@SpringJUnitConfig
@DirtiesContext
public class LastModifiedFTPFileListFilterTests extends FtpTestSupport {

	@Autowired
	private FtpRemoteFileTemplate ftpRemoteFileTemplate;

	@Test
	public void testAge() {
		LastModifiedFTPFileListFilter filter = new LastModifiedFTPFileListFilter();
		filter.setAge(60, TimeUnit.SECONDS);
		FTPFile[] files = ftpRemoteFileTemplate.list("ftpSource");
		assertThat(files.length).isGreaterThan(0);
		assertThat(filter.filterFiles(files)).hasSize(0);
		FTPFile ftpFile = files[1];
		assertThat(filter.accept(ftpFile)).isFalse();

		// Make a file as of yesterday's
		final Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DATE, -1);
		ftpFile.setTimestamp(calendar);
		assertThat(filter.filterFiles(files)).hasSize(1);
		assertThat(filter.accept(ftpFile)).isTrue();
	}

	@Configuration
	public static class Config {

		@Bean
		public SessionFactory<FTPFile> ftpFileSessionFactory() {
			return LastModifiedFTPFileListFilterTests.sessionFactory();
		}

		@Bean
		public FtpRemoteFileTemplate ftpRemoteFileTemplate() {
			return new FtpRemoteFileTemplate(ftpFileSessionFactory());
		}
	}
}
