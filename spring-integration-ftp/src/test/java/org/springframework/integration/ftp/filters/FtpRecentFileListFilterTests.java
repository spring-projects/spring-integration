/*
 * Copyright 2025 the original author or authors.
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
