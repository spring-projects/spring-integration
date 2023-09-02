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

package org.springframework.integration.smb.filters;

import java.util.concurrent.TimeUnit;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.smb.SmbTestSupport;
import org.springframework.integration.smb.session.SmbRemoteFileTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Adama Sorho
 * @since 6.2
 */
@SpringJUnitConfig
@DirtiesContext
public class LastModifiedSmbFileListFilterTests extends SmbTestSupport {

	@Autowired
	private SmbRemoteFileTemplate smbRemoteFileTemplate;

	@Test
	public void testAge() throws SmbException {
		LastModifiedSmbFileListFilter filter = new LastModifiedSmbFileListFilter();
		filter.setAge(60, TimeUnit.SECONDS);
		SmbFile[] files = smbRemoteFileTemplate.list("smbSource");
		assertThat(files.length).isGreaterThan(0);
		assertThat(filter.filterFiles(files)).hasSize(0);
		SmbFile smbFile = files[1];
		assertThat(filter.accept(smbFile)).isFalse();

		// Make a file as of yesterday's
		smbFile.setLastModified(System.currentTimeMillis() - 1000 * 60 * 60 * 24);
		assertThat(filter.filterFiles(files)).hasSize(1);
		assertThat(filter.accept(smbFile)).isTrue();
	}

	@Configuration
	public static class Config {

		@Bean
		public SessionFactory<SmbFile> smbFileSessionFactory() {
			return LastModifiedSmbFileListFilterTests.sessionFactory();
		}

		@Bean
		public SmbRemoteFileTemplate smbRemoteFileTemplate() {
			return new SmbRemoteFileTemplate(smbFileSessionFactory());
		}
	}
}
