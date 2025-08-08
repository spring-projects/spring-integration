/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.ftp.filters;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.net.ftp.FTPFile;
import org.junit.jupiter.api.Test;

import org.springframework.integration.metadata.SimpleMetadataStore;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @since 4.0.4
 *
 */
public class FtpPersistentAcceptOnceFileListFilterTests {

	@Test
	public void testRollback() throws Exception {
		FtpPersistentAcceptOnceFileListFilter filter = new FtpPersistentAcceptOnceFileListFilter(
				new SimpleMetadataStore(), "rollback:");
		FTPFile ftpFile1 = new FTPFile();
		ftpFile1.setName("foo");
		ftpFile1.setTimestamp(Calendar.getInstance());
		FTPFile ftpFile2 = new FTPFile();
		ftpFile2.setName("bar");
		ftpFile2.setTimestamp(Calendar.getInstance());
		FTPFile ftpFile3 = new FTPFile();
		ftpFile3.setName("baz");
		ftpFile3.setTimestamp(Calendar.getInstance());
		FTPFile[] files = new FTPFile[] {ftpFile1, ftpFile2, ftpFile3};
		List<FTPFile> passed = filter.filterFiles(files);
		assertThat(Arrays.equals(files, passed.toArray())).isTrue();
		List<FTPFile> now = filter.filterFiles(files);
		assertThat(now.size()).isEqualTo(0);
		filter.rollback(passed.get(1), passed);
		now = filter.filterFiles(files);
		assertThat(now.size()).isEqualTo(2);
		assertThat(now.get(0).getName()).isEqualTo("bar");
		assertThat(now.get(1).getName()).isEqualTo("baz");
		now = filter.filterFiles(files);
		assertThat(now.size()).isEqualTo(0);
		filter.close();
	}

}
