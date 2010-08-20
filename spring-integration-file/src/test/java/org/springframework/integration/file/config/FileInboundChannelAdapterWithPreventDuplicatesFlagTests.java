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
import org.springframework.integration.file.TestFileListFilter;
import org.springframework.integration.file.entries.AcceptOnceEntryFileListFilter;
import org.springframework.integration.file.entries.CompositeEntryListFilter;
import org.springframework.integration.file.entries.EntryListFilter;
import org.springframework.integration.file.entries.PatternMatchingEntryListFilter;
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
        EntryListFilter filter = this.extractFilter("filterAndNull");
        assertFalse(filter instanceof CompositeEntryListFilter);
        assertSame(testFilter, filter);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void filterAndTrue() {
        EntryListFilter filter = this.extractFilter("filterAndTrue");
        assertTrue(filter instanceof CompositeEntryListFilter);
        Collection filters = (Collection) new DirectFieldAccessor(filter).getPropertyValue("fileFilters");
        assertTrue(filters.iterator().next() instanceof AcceptOnceEntryFileListFilter);
        assertTrue(filters.contains(testFilter));
    }

    @Test
    public void filterAndFalse() throws Exception {
        EntryListFilter filter = this.extractFilter("filterAndFalse");
        assertFalse(filter instanceof CompositeEntryListFilter);
        assertSame(testFilter, filter);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void patternAndNull() throws Exception {
        EntryListFilter filter = this.extractFilter("patternAndNull");
        assertTrue(filter instanceof CompositeEntryListFilter);
        Collection filters = (Collection) new DirectFieldAccessor(filter).getPropertyValue("fileFilters");
        Iterator<EntryListFilter<File>> iterator = filters.iterator();
        assertTrue(iterator.next() instanceof AcceptOnceEntryFileListFilter);
        assertTrue(iterator.next() instanceof PatternMatchingEntryListFilter);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void patternAndTrue() throws Exception {
        EntryListFilter filter = this.extractFilter("patternAndTrue");
        assertTrue(filter instanceof CompositeEntryListFilter);
        Collection filters = (Collection) new DirectFieldAccessor(filter).getPropertyValue("fileFilters");
        Iterator<EntryListFilter> iterator = filters.iterator();
        assertTrue(iterator.next() instanceof AcceptOnceEntryFileListFilter);
        assertTrue(iterator.next() instanceof PatternMatchingEntryListFilter);
    }

    @Test
    public void patternAndFalse() throws Exception {
        EntryListFilter<File> filter = this.extractFilter("patternAndFalse");
        assertFalse(filter instanceof CompositeEntryListFilter);
        assertTrue(filter instanceof PatternMatchingEntryListFilter);
    }

    @Test
    public void defaultAndNull() throws Exception {
        EntryListFilter<File> filter = this.extractFilter("defaultAndNull");
        assertNotNull(filter);
        assertFalse(filter instanceof CompositeEntryListFilter);
        assertTrue(filter instanceof AcceptOnceEntryFileListFilter);
        File testFile = new File("test");
        File[] files = new File[]{testFile, testFile, testFile};
        List<File> result = filter.filterEntries(files);
        assertEquals(1, result.size());
    }

    @Test
    public void defaultAndTrue() throws Exception {
        EntryListFilter filter = this.extractFilter("defaultAndTrue");
        assertFalse(filter instanceof CompositeEntryListFilter);
        assertTrue(filter instanceof AcceptOnceEntryFileListFilter);
        File testFile = new File("test");
        File[] files = new File[]{testFile, testFile, testFile};
        List<File> result = filter.filterEntries(files);
        assertEquals(1, result.size());
    }

    @Test
    public void defaultAndFalse() throws Exception {
        EntryListFilter filter = this.extractFilter("defaultAndFalse");
        assertNotNull(filter);
        assertFalse(filter instanceof CompositeEntryListFilter);
        assertFalse(filter instanceof AcceptOnceEntryFileListFilter);
        File testFile = new File("test");
        File[] files = new File[]{testFile, testFile, testFile};
        List<File> result = filter.filterEntries(files);
        assertEquals(3, result.size());
    }


    private EntryListFilter<File> extractFilter(String beanName) {
        return (EntryListFilter<File>) new DirectFieldAccessor(
                new DirectFieldAccessor(
                        new DirectFieldAccessor(context.getBean(beanName))
                                .getPropertyValue("source"))
                        .getPropertyValue("scanner"))
                .getPropertyValue("filter");
    }
}
