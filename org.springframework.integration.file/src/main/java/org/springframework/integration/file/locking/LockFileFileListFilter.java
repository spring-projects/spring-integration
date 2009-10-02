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

package org.springframework.integration.file.locking;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.file.AbstractFileListFilter;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;

/**
 * LockFileFileListFilter keeps track of the files it should pick up by adding lock files in a directory. If this
 * directory is the same as the directory of the original files additional precautions need to be taken to avoid
 * treating the lock files as normal input.
 *
 * If different LockFileFileListFilters share their lock directory they are unlikely to pass the same file.
 *
 * @author Iwein Fuld
 * @since 2.0
 */
public class LockFileFileListFilter extends AbstractFileListFilter implements FileLocker{

    private static final Log logger = LogFactory.getLog(LockFileFileListFilter.class);

    private static final String LOCK_SUFFIX = ".lock";

    private static final String PRELOCK_SUFFIX = ".prelock";

    private File workdir;

    public LockFileFileListFilter(File workdir) {
        Assert.notNull(workdir, "Work directory must not be null.");
        Assert.isTrue(workdir.isDirectory(), "Work directory must be a directory.");
        Assert.isTrue(workdir.canWrite(), "Work directory must be write accessible.");
        this.workdir = workdir;
    }

    protected boolean accept(File file) {
        File lockFile = new File(workdir, file.getName() + LOCK_SUFFIX);
        File preLockFile = new File(workdir, file.getName() + PRELOCK_SUFFIX);
        if (lockFile.exists() || preLockFile.exists()) {
            return false;
        }
        String name = file.getName();
        return !name.endsWith(LOCK_SUFFIX) && !name.endsWith(PRELOCK_SUFFIX);
    }

    /**
     * Makes a best effort attempt at locking the file atomically.
     *
     * The locking mechanism will create a prelock file and move it to a lock
     * file location.
     */
    public boolean lock(File fileToLock) {
        File lockFile = new File(workdir, fileToLock.getName() + LOCK_SUFFIX);
        File preLockFile = new File(workdir, fileToLock.getName() + PRELOCK_SUFFIX);
        if (lockFile.exists() || preLockFile.exists()) {
            return false;
        }
        try {
            preLockFile.createNewFile();
        }
        catch (IOException e) {
            logger.warn("Failed to lock file", e);
            return false;
        }
        // there is still a small chance that the next line will be done concurrently and the file will be picked up twice
        if (!lockFile.exists() && preLockFile.renameTo(lockFile)) {
            lockFile.deleteOnExit();
            return true;
        }
        preLockFile.delete();
        logger.warn("Failed to lock file [" + fileToLock + "]");
        return false;
    }

    /**
     * Deletes the lockFile for this file.
     */
    public void unlock(File fileToUnlock) {
        File lockFile = new File(workdir, fileToUnlock.getName() + LOCK_SUFFIX);
        File preLockFile = new File(workdir, fileToUnlock.getName() + PRELOCK_SUFFIX);
        if (!preLockFile.exists()) {
            lockFile.delete();
        }
    }

}
