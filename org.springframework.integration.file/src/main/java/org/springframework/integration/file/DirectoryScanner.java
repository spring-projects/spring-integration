/*
 * Copyright 2002-2009 the original author or authors.
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

/**
 * Strategy for scanning directories. Implementations may select all children and grandchildren of the scanned directory
 * in any order. This interface is intended to enable the selection and ordering of files in a directory like
 * RecursiveDirectoryScanner. If the only requirement is to ignore certain files a FileListFilter implementation should
 * suffice.
 *
 *  @author Iwein Fuld
 */
public interface DirectoryScanner {

    /**
     * Scans the directory according to the strategy particular to this implementation and returns the selected files
     * as a File array.
     *
     * @param directory the directory to scan for files
     * @return a list of files representing the content of the directory
     */
    File[] listFiles(File directory) throws IllegalArgumentException;
}
