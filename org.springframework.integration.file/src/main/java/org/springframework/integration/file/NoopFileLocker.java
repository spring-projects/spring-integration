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
package org.springframework.integration.file;

import org.springframework.integration.file.locking.FileLocker;

import java.io.File;

/**
 * Implementation of FileLocker that doesn't provide any protection against duplicate listing. This is the default used
 * by the FileReadingMessageSource.
 *
 * @author Iwein Fuld
 */
final class NoopFileLocker implements FileLocker {

    public boolean lock(File fileToLock) {
        return true;
    }

    public void unlock(File fileToUnlock) {
        //noop
    }
}

