/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.ftp.filters;

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

import static org.assertj.core.api.Assertions.assertThat;

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
