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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.junit.BeforeClass;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Iwein Fuld
 */
public class LockFileFileListFilterTests {

    private static File workdir = new File(new File(System.getProperty("java.io.tmpdir")), LockFileFileListFilterTests.class.getSimpleName());

    @BeforeClass
    public static void setupWorkDir() {
        workdir.mkdir();
    }

    private FileLocker locker = new LockFileFileLocker(workdir);

    @Before
    public void cleanDirectory() {
        File[] files = workdir.listFiles();
        for (File file : files) {
            file.delete();
        }
    }

    @Test
    public void fileListedOnlyWhenNotLocked() throws IOException {
        LockFileFileListFilter filter = new LockFileFileListFilter(workdir);
        File testFile = new File(workdir, "test0");
        testFile.createNewFile();
        assertThat(filter.filterFiles(workdir.listFiles()).get(0), is(testFile));
        locker.lock(testFile);
        assertThat(filter.filterFiles(workdir.listFiles()), is((List)new ArrayList<File>()));
    }

    @Test
    public void fileListedByOneFilterOnly() throws IOException {
        LockFileFileListFilter filter1 = new LockFileFileListFilter(workdir);
        LockFileFileListFilter filter2 = new LockFileFileListFilter(workdir);
        File testFile = new File(workdir, "test1");
        testFile.createNewFile();
        assertThat(filter1.filterFiles(workdir.listFiles()).get(0), is(testFile));
        locker.lock(testFile);
        assertThat(filter2.filterFiles(workdir.listFiles()), is((List)new ArrayList<File>()));
    }

}
