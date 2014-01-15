/*
 * Copyright 2002-2014 the original author or authors.
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
 * A FileLocker is a strategy that can ensure that files are only processed a
 * single time. Implementations are free to implement any relation between
 * locking and unlocking. This means that there are no safety guarantees in the
 * contract, defining these guarantees is up to the implementation.
 *
 * If a filter that respects locks is required extend
 * {@link org.springframework.integration.file.locking.AbstractFileLockerFilter} instead.
 *
 * @author Iwein Fuld
 * @since 2.0
 */
public interface FileLocker {

	/**
	 * Tries to lock the given file and returns <code>true</code> if it was
	 * successful, <code>false</code> otherwise.
     *
     * @param fileToLock   the file that should be locked according to this locker
     * @return true if successful.
     */
	boolean lock(File fileToLock);

    /**
     * Checks whether the file passed in can be locked by this locker. This method never changes the locked state.
     *
     * @param file The file.
     * @return true if the file was locked by another locker than this locker
     */
    boolean isLockable(File file);

	/**
	 * Unlocks the given file.
     *
     * @param fileToUnlock  the file that should be unlocked according to this locker
     */
	void unlock(File fileToUnlock);

}
