package org.springframework.integration.file;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class TestFileFilter implements FileListFilter {

	public List<File> filterFiles(File[] files) {
		return Arrays.asList(files);
	}

}
