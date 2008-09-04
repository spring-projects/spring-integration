package org.springframework.integration.file;

import java.io.File;
import java.io.FileFilter;

public class TestFileFilter implements FileFilter{

	public boolean accept(File pathname) {
		return true;
	}

}
