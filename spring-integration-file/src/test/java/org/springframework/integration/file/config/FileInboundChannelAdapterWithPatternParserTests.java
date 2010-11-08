/*
 * Copyright 2002-2010 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.filters.SimplePatternFileListFilter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @author Iwein Fuld
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class FileInboundChannelAdapterWithPatternParserTests {

    @Autowired(required = true)
    private ApplicationContext context;

    @Autowired(required = true)
    @Qualifier("adapterWithPattern.adapter")
    private AbstractEndpoint endpoint;

    private DirectFieldAccessor accessor;


    @Autowired(required = true)
    public void setSource(FileReadingMessageSource source) {
        this.accessor = new DirectFieldAccessor(source);
    }


    @Test
    public void channelName() {
        AbstractMessageChannel channel = context.getBean("adapterWithPattern", AbstractMessageChannel.class);
        assertEquals("adapterWithPattern", channel.getComponentName());
    }

    @Test
    public void autoStartupDisabled() {
        assertFalse(this.endpoint.isRunning());
        assertEquals(Boolean.FALSE, new DirectFieldAccessor(endpoint).getPropertyValue("autoStartup"));
    }

    @Test
    public void inputDirectory() {
        File expected = new File(System.getProperty("java.io.tmpdir"));
        File actual = (File) accessor.getPropertyValue("directory");
        assertEquals(expected, actual);
    }

    @Test
    public void compositeFilterType() {
        DirectFieldAccessor scannerAccessor = new DirectFieldAccessor(accessor.getPropertyValue("scanner"));
        assertTrue(scannerAccessor.getPropertyValue("filter") instanceof CompositeFileListFilter);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void compositeFilterSetSize() {
        DirectFieldAccessor scannerAccessor = new DirectFieldAccessor(accessor.getPropertyValue("scanner"));
        Set<FileListFilter<File>> filters = (Set<FileListFilter<File>>) new DirectFieldAccessor(
                scannerAccessor.getPropertyValue("filter")).getPropertyValue("fileFilters");
        assertEquals(2, filters.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void acceptOnceFilter() {
        DirectFieldAccessor scannerAccessor = new DirectFieldAccessor(accessor.getPropertyValue("scanner"));
        Set<FileListFilter<File>> filters = (Set<FileListFilter<File>>) new DirectFieldAccessor(
                scannerAccessor.getPropertyValue("filter")).getPropertyValue("fileFilters");
        boolean hasAcceptOnceFilter = false;
        for (FileListFilter<File> filter : filters) {
            if (filter instanceof AcceptOnceFileListFilter) {
                hasAcceptOnceFilter = true;
            }
        }
        assertTrue("expected AcceptOnceFileListFilter", hasAcceptOnceFilter);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void patternFilter() {
        DirectFieldAccessor scannerAccessor = new DirectFieldAccessor(accessor.getPropertyValue("scanner"));
        Set<FileListFilter<?>> filters = (Set<FileListFilter<?>>) new DirectFieldAccessor(
                scannerAccessor.getPropertyValue("filter")).getPropertyValue("fileFilters");
        String pattern = null;
        for (FileListFilter<?> filter : filters) {
            if (filter instanceof SimplePatternFileListFilter) {
                pattern = (String) new DirectFieldAccessor(filter).getPropertyValue("path");
            }
        }
        assertNotNull("expected SimplePatternFileListFilterTest", pattern);
        assertEquals("*.txt", pattern.toString());
    }

}
