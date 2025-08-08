/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
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
 * @author Gary Russell
 * @author Artem Bilan
 * @author Adama Sorho
 *
 * @since 4.2
 *
 */
public class LastModifiedFileListFilterTests {

	@TempDir
	public File folder;

	@Test
	public void testAge() throws Exception {
		LastModifiedFileListFilter filter = new LastModifiedFileListFilter();
		filter.setAge(60);
		File foo = new File(folder, "test.tmp");
		FileOutputStream fileOutputStream = new FileOutputStream(foo);
		fileOutputStream.write("x".getBytes());
		fileOutputStream.close();
		assertThat(filter.filterFiles(new File[] {foo})).hasSize(0);
		assertThat(filter.accept(foo)).isFalse();
		foo.setLastModified(Instant.now().minus(Duration.ofDays(1)).toEpochMilli());
		assertThat(filter.filterFiles(new File[] {foo})).hasSize(1);
		assertThat(filter.accept(foo)).isTrue();
	}

}
