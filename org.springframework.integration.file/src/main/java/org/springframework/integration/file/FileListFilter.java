package org.springframework.integration.file;

import java.io.File;
import java.util.List;

public interface FileListFilter {

	List<File> filterFiles(File[] files);

}
