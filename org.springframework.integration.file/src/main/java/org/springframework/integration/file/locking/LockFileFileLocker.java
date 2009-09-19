/*
 * Copyright 2002-2008 the original author or authors.
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
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;

/**
 * @author Iwein Fuld
 */
public class LockFileFileLocker implements FileLocker {
    private static final Log logger = LogFactory.getLog(LockFileFileLocker.class);

    private File workdir;
    static final String LOCK_SUFFIX = ".lock";
    static final String PRELOCK_SUFFIX = ".prelock";

    public LockFileFileLocker(File workdir) {
        Assert.notNull(workdir, "Work directy must not be null.");
        Assert.isTrue(workdir.isDirectory(),"Work directy must be a directory.");
        Assert.isTrue(workdir.canWrite(), "Work directy must be write accessible.");
        this.workdir = workdir;
    }

    /**
     * Makes a best effort attempt at locking the file atomically. The chances of success are wholly dependant on the
     * underlying operating system.
     *
     * The locking mechanism will create a prelock file, remove write permissions from that file and move it to a lock
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
        } catch (IOException e) {
            logger.warn("Failed to lock file", e);
            return false;
        }
        preLockFile.setWritable(false);
        if (preLockFile.renameTo(lockFile)) {
            return true;
        }
        preLockFile.delete();
        logger.warn("Failed to lock file [" + fileToLock + "]");
        return false;
    }

    /**
     * Sets the lockFile for this file to be writable and then deletes it. If a lock happens concurrently, the delete
     * should not succeed, so the lock will remain in place. This might be different on various operating systems.
     */
    public void unlock(File fileToUnlock) {
        File lockFile = new File(workdir, fileToUnlock.getName() + LOCK_SUFFIX);
        lockFile.setWritable(true);
        lockFile.delete();
    }
}
