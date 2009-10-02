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

import java.io.File;

/**
 * A FileLocker is a strategy that can ensure that files are only processed a
 * single time. Implementations are free to implement any relation between
 * locking and unlocking. This means that there are no safety guarantees in the
 * contract, defining these guarantees is up to the implementation.
 * 
 * @author Iwein Fuld
 * @since 2.0
 */
public interface FileLocker {

	/**
	 * Tries to lock the given file and returns <code>true</code> if it was
	 * successful, <code>false</code> otherwise.
	 */
	boolean lock(File fileToLock);

	/**
	 * Unlocks the given file.
	 */
	void unlock(File fileToUnlock);

}
