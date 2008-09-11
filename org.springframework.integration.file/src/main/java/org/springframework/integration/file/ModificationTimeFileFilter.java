package org.springframework.integration.file;

import java.io.File;
import java.io.FileFilter;

class ModificationTimeFileFilter implements FileFilter {
	
	private final long modificationTime;
	public ModificationTimeFileFilter(long modificationTime) {
		this.modificationTime = modificationTime;
	}
	public boolean accept(File file) {
		return modificationTime <= file.lastModified();
	}
}
