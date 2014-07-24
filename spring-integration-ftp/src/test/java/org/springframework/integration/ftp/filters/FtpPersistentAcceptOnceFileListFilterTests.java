/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.integration.ftp.filters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.net.ftp.FTPFile;
import org.junit.Test;

import org.springframework.integration.metadata.SimpleMetadataStore;

/**
 * @author Gary Russell
 * @since 4.0.4
 *
 */
public class FtpPersistentAcceptOnceFileListFilterTests {

	@Test
	public void testRollback() {
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
		assertTrue(Arrays.equals(files, passed.toArray()));
		List<FTPFile> now = filter.filterFiles(files);
		assertEquals(0, now.size());
		filter.rollback(passed.get(1), passed);
		now = filter.filterFiles(files);
		assertEquals(2, now.size());
		assertEquals("bar", now.get(0).getName());
		assertEquals("baz", now.get(1).getName());
		now = filter.filterFiles(files);
		assertEquals(0, now.size());
	}

}
