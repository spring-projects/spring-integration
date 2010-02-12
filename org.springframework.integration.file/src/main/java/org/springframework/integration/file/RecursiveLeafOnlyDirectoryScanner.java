package org.springframework.integration.file;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * DirectoryScanner that lists all files inside a directory and subdirectories, without limit. This scanner should not
 * be used with directories that contain a vast number of files or on deep trees, as all the file names will be read
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
            } else {
                files.add(rootFile);
            }
        }
        return files.toArray(new File[files.size()]);
    }
}
