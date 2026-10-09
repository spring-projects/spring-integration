/*
 * Copyright 2025-present the original author or authors.
 */

package org.springframework.integration.file.filters;

import java.io.File;
import java.io.FileOutputStream;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 *
 * @since 6.5
 *
 */
public class RecentFileListFilterTests {

	@TempDir
	public File folder;

	@Test
	public void testAge() throws Exception {
		RecentFileListFilter filter = new RecentFileListFilter(Duration.ofHours(20));
		File testFile = new File(folder, "test.tmp");
		FileOutputStream fileOutputStream = new FileOutputStream(testFile);
		fileOutputStream.write("x".getBytes());
		fileOutputStream.close();
		assertThat(filter.filterFiles(new File[] {testFile})).hasSize(1);
		assertThat(filter.accept(testFile)).isTrue();

		testFile.setLastModified(Instant.now().minus(Duration.ofDays(1)).toEpochMilli());

		assertThat(filter.filterFiles(new File[] {testFile})).hasSize(0);
		assertThat(filter.accept(testFile)).isFalse();
	}

}
