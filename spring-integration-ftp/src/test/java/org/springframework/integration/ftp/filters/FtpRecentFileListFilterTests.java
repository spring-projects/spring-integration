/*
 * Copyright 2025-present the original author or authors.
 */

package org.springframework.integration.ftp.filters;

import java.time.Duration;
import java.util.Calendar;

import org.apache.commons.net.ftp.FTPFile;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 *
 * @since 6.5
 */
public class FtpRecentFileListFilterTests {

	@Test
	public void testAge() {
		FtpRecentFileListFilter filter = new FtpRecentFileListFilter(Duration.ofHours(20));
		FTPFile ftpFile1 = new FTPFile();
		ftpFile1.setName("foo");
		ftpFile1.setTimestamp(Calendar.getInstance());
		FTPFile ftpFile2 = new FTPFile();
		ftpFile2.setName("bar");
		ftpFile2.setTimestamp(Calendar.getInstance());
		FTPFile[] files = new FTPFile[] {ftpFile1, ftpFile2};
		assertThat(filter.filterFiles(files)).hasSize(2);
		assertThat(filter.accept(ftpFile1)).isTrue();
		assertThat(filter.accept(ftpFile2)).isTrue();

		// Make a file as of yesterday's
		final Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DATE, -1);
		ftpFile2.setTimestamp(calendar);

		assertThat(filter.filterFiles(files)).hasSize(1);
		assertThat(filter.accept(ftpFile1)).isTrue();
	}

}
