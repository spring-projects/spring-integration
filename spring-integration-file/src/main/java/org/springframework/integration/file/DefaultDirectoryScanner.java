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

import org.springframework.integration.MessagingException;

import java.io.File;
import java.util.List;

/**
 * Default directory scanner and base class for other directory scanners. It takes care of the default interrelations
 * between filtering, scanning and locking.
 *
 * @author Iwein Fuld
 * @since 2.0
 */
public class DefaultDirectoryScanner implements DirectoryScanner {
    private FileListFilter filter = new AcceptOnceFileListFilter();
    private FileLocker locker;

    public final List<File> listFiles(File directory) throws IllegalArgumentException {
        File[] files = listEligibleFiles(directory);
        if (files == null) {
            throw new MessagingException("The path [" + directory
                    + "] does not denote a properly accessible directory.");
        }
        return this.filter.filterFiles(files);
    }

    /**
     * Subclasses may refine the listing strategy by overriding this method. The files returned here are passed onto the
     * filter.
     *
     * @param directory root directory to use for listing
     * @return the files this scanner should consider
     */
    protected File[] listEligibleFiles(File directory) {
        return directory.listFiles();
    }

    /**
     * {@inheritDoc}
     */
    public final void setFilter(FileListFilter filter) {
        this.filter = filter;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This class takes the minimal implementation and merely delegates to the locker if set.
     */
    public final boolean tryClaim(File file) {
        return locker != null ? locker.lock(file) : true;
    }

    /**
     * {@inheritDoc}
     */
    public final void setLocker(FileLocker locker) {
        this.locker = locker;
    }
}
