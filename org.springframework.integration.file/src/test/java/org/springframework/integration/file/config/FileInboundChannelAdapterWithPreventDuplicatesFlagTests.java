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

package org.springframework.integration.file.config;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.file.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Mark Fisher
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class FileInboundChannelAdapterWithPreventDuplicatesFlagTests {

    @Autowired
    private ApplicationContext context;

    @Autowired
    @Qualifier("testFilter")
    private TestFileListFilter testFilter;


    @Test
    public void filterAndNull() {
        FileListFilter filter = this.extractFilter("filterAndNull");
        assertFalse(filter instanceof CompositeFileListFilter);
        assertSame(testFilter, filter);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void filterAndTrue() {
        FileListFilter filter = this.extractFilter("filterAndTrue");
        assertTrue(filter instanceof CompositeFileListFilter);
        Collection filters = (Collection) new DirectFieldAccessor(filter).getPropertyValue("fileFilters");
        assertTrue(filters.iterator().next() instanceof AcceptOnceFileListFilter);
        assertTrue(filters.contains(testFilter));
    }

    @Test
    public void filterAndFalse() throws Exception {
        FileListFilter filter = this.extractFilter("filterAndFalse");
        assertFalse(filter instanceof CompositeFileListFilter);
        assertSame(testFilter, filter);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void patternAndNull() throws Exception {
        FileListFilter filter = this.extractFilter("patternAndNull");
        assertTrue(filter instanceof CompositeFileListFilter);
        Collection filters = (Collection) new DirectFieldAccessor(filter).getPropertyValue("fileFilters");
        Iterator<FileListFilter> iterator = filters.iterator();
        assertTrue(iterator.next() instanceof AcceptOnceFileListFilter);
        assertTrue(iterator.next() instanceof PatternMatchingFileListFilter);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void patternAndTrue() throws Exception {
        FileListFilter filter = this.extractFilter("patternAndTrue");
        assertTrue(filter instanceof CompositeFileListFilter);
        Collection filters = (Collection) new DirectFieldAccessor(filter).getPropertyValue("fileFilters");
        Iterator<FileListFilter> iterator = filters.iterator();
        assertTrue(iterator.next() instanceof AcceptOnceFileListFilter);
        assertTrue(iterator.next() instanceof PatternMatchingFileListFilter);
    }

    @Test
    public void patternAndFalse() throws Exception {
        FileListFilter filter = this.extractFilter("patternAndFalse");
        assertFalse(filter instanceof CompositeFileListFilter);
        assertTrue(filter instanceof PatternMatchingFileListFilter);
    }

    @Test
    public void defaultAndNull() throws Exception {
        FileListFilter filter = this.extractFilter("defaultAndNull");
        assertNotNull(filter);
        assertFalse(filter instanceof CompositeFileListFilter);
        assertTrue(filter instanceof AcceptOnceFileListFilter);
        File testFile = new File("test");
        File[] files = new File[]{testFile, testFile, testFile};
        List<File> result = filter.filterFiles(files);
        assertEquals(1, result.size());
    }

    @Test
    public void defaultAndTrue() throws Exception {
        FileListFilter filter = this.extractFilter("defaultAndTrue");
        assertFalse(filter instanceof CompositeFileListFilter);
        assertTrue(filter instanceof AcceptOnceFileListFilter);
        File testFile = new File("test");
        File[] files = new File[]{testFile, testFile, testFile};
        List<File> result = filter.filterFiles(files);
        assertEquals(1, result.size());
    }

    @Test
    public void defaultAndFalse() throws Exception {
        FileListFilter filter = this.extractFilter("defaultAndFalse");
        assertNotNull(filter);
        assertFalse(filter instanceof CompositeFileListFilter);
        assertFalse(filter instanceof AcceptOnceFileListFilter);
        File testFile = new File("test");
        File[] files = new File[]{testFile, testFile, testFile};
        List<File> result = filter.filterFiles(files);
        assertEquals(3, result.size());
    }


    private FileListFilter extractFilter(String beanName) {
        return (FileListFilter) new DirectFieldAccessor(
                new DirectFieldAccessor(
                        new DirectFieldAccessor(context.getBean(beanName))
                                .getPropertyValue("source"))
                        .getPropertyValue("scanner"))
                .getPropertyValue("filter");
    }
}
