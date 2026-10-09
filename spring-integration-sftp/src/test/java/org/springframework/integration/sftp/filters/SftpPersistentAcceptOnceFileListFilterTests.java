/*
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.sftp.filters;

import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;

import org.apache.sshd.sftp.client.SftpClient;
import org.junit.jupiter.api.Test;

import org.springframework.integration.metadata.SimpleMetadataStore;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author David Liu
 * @author Artem Bilan
 *
 * @since 4.0.4
 *
 */
public class SftpPersistentAcceptOnceFileListFilterTests {

	@Test
	public void testRollback() throws Exception {
		SftpPersistentAcceptOnceFileListFilter filter = new SftpPersistentAcceptOnceFileListFilter(
				new SimpleMetadataStore(), "rollback:");
		SftpClient.Attributes attrs = new SftpClient.Attributes();
		attrs.setModifyTime(FileTime.from(Instant.now()));
		SftpClient.DirEntry sftpFile1 = new SftpClient.DirEntry("file1", null, attrs);
		SftpClient.DirEntry sftpFile2 = new SftpClient.DirEntry("file2", null, attrs);
		SftpClient.DirEntry sftpFile3 = new SftpClient.DirEntry("file3", null, attrs);
		SftpClient.DirEntry[] files = new SftpClient.DirEntry[] {sftpFile1, sftpFile2, sftpFile3};
		List<SftpClient.DirEntry> passed = filter.filterFiles(files);
		assertThat(passed.toArray()).isEqualTo(files);
		List<SftpClient.DirEntry> now = filter.filterFiles(files);
		assertThat(now).isEmpty();
		filter.rollback(passed.get(1), passed);
		now = filter.filterFiles(files);
		assertThat(now)
				.hasSize(2)
				.extracting(SftpClient.DirEntry::getFilename)
				.contains("file2", "file3");
		now = filter.filterFiles(files);
		assertThat(now).isEmpty();
		filter.close();
	}

	@Test
	public void testKeyUsingFileName() throws Exception {
		SftpPersistentAcceptOnceFileListFilter filter = new SftpPersistentAcceptOnceFileListFilter(
				new SimpleMetadataStore(), "rollback:");
		SftpClient.Attributes attrs = new SftpClient.Attributes();
		attrs.setModifyTime(FileTime.from(Instant.now()));
		SftpClient.DirEntry sftpFile1 = new SftpClient.DirEntry("same", "dir1/same", attrs);
		SftpClient.DirEntry sftpFile2 = new SftpClient.DirEntry("same", "dir2/same", attrs);
		SftpClient.DirEntry[] files = new SftpClient.DirEntry[] {sftpFile1, sftpFile2};
		List<SftpClient.DirEntry> now = filter.filterFiles(files);
		assertThat(now)
				.hasSize(2)
				.extracting(SftpClient.DirEntry::getFilename)
				.contains("same", "same");
		filter.close();
	}

}
