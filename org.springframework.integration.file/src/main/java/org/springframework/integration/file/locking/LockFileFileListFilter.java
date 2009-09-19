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
import org.springframework.integration.file.AbstractFileListFilter;
import static org.springframework.integration.file.locking.LockFileFileLocker.*;

import java.io.File;

/**
 * LockFileFileListFilter keeps track of the files it should pick up by adding lock files in a directory. If this
 * directory is the same as the directory of the original files addition precautions need to be taken to avoid treating
 * the lock files as normal input.
 *
 * If different LockFileFileListFilters share their lock directory they are guaranteed not to pass the same file. 
 *
 * @author Iwein Fuld
 */
public class LockFileFileListFilter extends AbstractFileListFilter {
    private static final Log logger = LogFactory.getLog(LockFileFileListFilter.class);

    private File workdir;

    public LockFileFileListFilter(File workdir) {
        this.workdir = workdir;
    }

    protected boolean accept(File file) {
        File lockFile = new File(workdir, file.getName()+ LOCK_SUFFIX);
        File preLockFile = new File(workdir, file.getName()+ PRELOCK_SUFFIX);
        if (lockFile.exists()||preLockFile.exists()){
            return false;
        }
        String name = file.getName();
        return !name.endsWith(LOCK_SUFFIX)&&!name.endsWith(PRELOCK_SUFFIX);
    }
}
