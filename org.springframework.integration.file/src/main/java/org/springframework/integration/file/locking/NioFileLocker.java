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

import org.springframework.integration.file.FileListFilter;
import org.springframework.integration.file.AbstractFileListFilter;
import org.springframework.integration.core.MessagingException;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * File locking strategy that uses java.nio. The locks taken by FileChannel are shared with all the threads in a single
 * JVM, so this locking strategy <b>does not</b> prevent files being picked up multiple times within the same JVM.
 *
 * @author Iwein Fuld
 * @author Mark Fisher
 */
public class NioFileLocker extends AbstractFileListFilter implements FileLocker {
    private final ConcurrentMap<File, FileLock> lockCache = new ConcurrentHashMap<File, FileLock>();

    public boolean lock(File fileToLock) {
        FileLock lock = lockCache.get(fileToLock);
        if (lock == null) {
            FileLock newLock = null;
            try {
                newLock = FileChannelCache.tryLockFor(fileToLock);
            } catch (IOException e) {
                throw new MessagingException("Failed to lock file: " + fileToLock, e);
            }
            if (newLock != null) {
                FileLock original = lockCache.putIfAbsent(fileToLock, newLock);
                lock = original != null ? original : newLock;
            }
        }
        return lock != null;
    }

    public void unlock(File fileToUnlock) {
        FileLock fileLock = lockCache.get(fileToUnlock);
        try {
            if (fileLock != null) {
                fileLock.release();
            }
            FileChannelCache.closeChannelFor(fileToUnlock);
        } catch (IOException e) {
            throw new MessagingException("Failed to unlock file: " + fileToUnlock, e);
        }
    }

    protected boolean accept(File file) {
        return this.lock(file);
    }
}
