/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.file;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.springframework.integration.file.filters.FileListFilter;

/**
 * @author Iwein Fuld
 * @author Mark Fisher
 */
public class TestFileListFilter implements FileListFilter<File> {

	public List<File> filterFiles(File[] entries) {
		return Arrays.asList(entries);
	}

}
