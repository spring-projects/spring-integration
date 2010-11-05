/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.file;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * DirectoryScanner that lists all files inside a directory and subdirectories,
 * without limit. This scanner should not be used with directories that contain
 * a vast number of files or on deep trees, as all the file names will be read
 * into memory and the scanning will be done recursively.
 * 
 * @author Iwein Fuld
 */
public class RecursiveLeafOnlyDirectoryScanner extends DefaultDirectoryScanner {

	protected File[] listEligibleFiles(File directory) throws IllegalArgumentException {
		File[] rootFiles = directory.listFiles();
		List<File> files = new ArrayList<File>(rootFiles.length);
		for (File rootFile : rootFiles) {
			if (rootFile.isDirectory()) {
				files.addAll(Arrays.asList(listEligibleFiles(rootFile)));
			}
			else {
				files.add(rootFile);
			}
		}
		return files.toArray(new File[files.size()]);
	}

}
