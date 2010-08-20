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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.file.entries.CompositeEntryListFilter;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Iwein Fuld
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class FileLockingNamespaceTests {

    @Autowired
    @Qualifier("nioLockingAdapter.adapter")
    SourcePollingChannelAdapter nioAdapter;

    FileReadingMessageSource nioLockingSource;

    @Autowired
    @Qualifier("customLockingAdapter.adapter")
    SourcePollingChannelAdapter customAdapter;

    FileReadingMessageSource customLockingSource;

    @Before public void extractSources() {
        nioLockingSource = (FileReadingMessageSource) new DirectFieldAccessor( nioAdapter).getPropertyValue("source");
        customLockingSource = (FileReadingMessageSource) new DirectFieldAccessor( customAdapter).getPropertyValue("source");
    }

    @Test
    public void shouldLoadConfig() {
        //verify Spring can load the configuration
    }

    @Test
    public void shouldSetCustomLockerProperly() {
        assertThat(extractFromScanner("locker", customLockingSource), is(StubLocker.class));
        assertThat(extractFromScanner("filter", customLockingSource), is(CompositeEntryListFilter.class));
    }

    private Object extractFromScanner(String propertyName, FileReadingMessageSource source) {
        return new DirectFieldAccessor( new DirectFieldAccessor(source).getPropertyValue("scanner") ).getPropertyValue(propertyName);
    }

    @Test
    public void shouldSetNioLockerProperly() {
        assertThat(extractFromScanner("locker", nioLockingSource), is(NioFileLocker.class));
        assertThat(extractFromScanner("filter", nioLockingSource), is(CompositeEntryListFilter.class));
    }

    public static class StubLocker extends AbstractFileLockerFilter {
        public boolean lock(File fileToLock) {
            return true;
        }

        public boolean isLockable(File file) {
            return true;
        }

        public void unlock(File fileToUnlock) {
            //
        }
    }
}
