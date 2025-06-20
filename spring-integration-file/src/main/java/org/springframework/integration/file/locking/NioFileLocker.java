/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.file.locking;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.messaging.MessagingException;

/**
 * File locking strategy that uses java.nio. The locks taken by FileChannel are shared with all the threads in a single
 * JVM, so this locking strategy <b>does not</b> prevent files being picked up multiple times within the same JVM.
 * {@link org.springframework.integration.file.FileReadingMessageSource}s sharing a
 * Locker will not pick up the same files.
 * <p>
 * This implementation will acquire or create a {@link FileLock} for the given file. Caching locks might be expensive,
 * so this locking strategy is not recommended for scenarios where many files are accessed in parallel.
 *
 * @author Iwein Fuld
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
public class NioFileLocker extends AbstractFileLockerFilter {

	private final ConcurrentMap<File, FileLock> lockCache = new ConcurrentHashMap<>();

	@Override
	public boolean lock(File fileToLock) {
		FileLock lock = this.lockCache.get(fileToLock);
		if (lock == null) {
			FileLock newLock = null;
			try {
				newLock = FileChannelCache.tryLockFor(fileToLock);
			}
			catch (IOException e) {
				throw new MessagingException("Failed to lock file: " + fileToLock, e);
			}
			if (newLock != null) {
				FileLock original = this.lockCache.putIfAbsent(fileToLock, newLock);
				lock = original != null ? original : newLock;
			}
		}
		return lock != null;
	}

	@Override
	public boolean isLockable(File file) {
		return this.lockCache.containsKey(file) || !FileChannelCache.isLocked(file);
	}

	@Override
	public void unlock(File fileToUnlock) {
		FileLock fileLock = this.lockCache.remove(fileToUnlock);
		try {
			if (fileLock != null) {
				fileLock.release();
			}
			FileChannelCache.closeChannelFor(fileToUnlock);
		}
		catch (IOException e) {
			throw new MessagingException("Failed to unlock file: " + fileToUnlock, e);
		}
	}

}
