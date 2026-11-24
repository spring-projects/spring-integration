/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
