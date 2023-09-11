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

package org.springframework.integration.ftp.filters;

import java.util.Calendar;

import org.apache.commons.net.ftp.FTPFile;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Adama Sorho
 * @since 6.2
 */
public class FtpLastModifiedFileListFilterTests {

	@Test
	public void testAge() {
		FtpLastModifiedFileListFilter filter = new FtpLastModifiedFileListFilter();
		FTPFile ftpFile1 = new FTPFile();
		ftpFile1.setName("foo");
		ftpFile1.setTimestamp(Calendar.getInstance());
		FTPFile ftpFile2 = new FTPFile();
		ftpFile2.setName("bar");
		ftpFile2.setTimestamp(Calendar.getInstance());
		FTPFile[] files = new FTPFile[] {ftpFile1, ftpFile2};
		assertThat(filter.filterFiles(files)).hasSize(0);
		assertThat(filter.accept(ftpFile2)).isFalse();

		// Make a file as of yesterday's
		final Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DATE, -1);
		ftpFile2.setTimestamp(calendar);

		assertThat(filter.filterFiles(files)).hasSize(1);
		assertThat(filter.accept(ftpFile2)).isTrue();
	}

}
