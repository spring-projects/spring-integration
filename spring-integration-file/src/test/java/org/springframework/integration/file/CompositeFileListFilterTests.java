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

import org.junit.Test;
import org.springframework.integration.file.entries.CompositeEntryListFilter;
import org.springframework.integration.file.entries.EntryListFilter;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * @author Iwein Fuld
 */
public class CompositeFileListFilterTests {

    private EntryListFilter<File> fileFilterMock1 = mock(EntryListFilter.class);

    private EntryListFilter<File> fileFilterMock2 = mock(EntryListFilter.class);

    private File fileMock = mock(File.class);

    @Test
    public void forwardedToFilters() throws Exception {
        CompositeEntryListFilter<File> compositeFileFilter = new CompositeEntryListFilter<File>(fileFilterMock1, fileFilterMock2);
        List<File> returnedFiles = Arrays.asList(new File[]{fileMock});
        when(fileFilterMock1.filterEntries(isA(File[].class))).thenReturn(returnedFiles);
        when(fileFilterMock2.filterEntries(isA(File[].class))).thenReturn(returnedFiles);
        assertEquals(returnedFiles, compositeFileFilter.filterEntries(new File[]{fileMock}));
        verify(fileFilterMock1).filterEntries(isA(File[].class));
        verify(fileFilterMock2).filterEntries(isA(File[].class));
    }

    @Test
    public void forwardedToAddedFilters() throws Exception {
        CompositeEntryListFilter<File> compositeFileFilter = new CompositeEntryListFilter<File>();
        compositeFileFilter.addFilter(fileFilterMock1, fileFilterMock2);
        List<File> returnedFiles = Arrays.asList(fileMock);
        when(fileFilterMock1.filterEntries(isA(File[].class))).thenReturn(returnedFiles);
        when(fileFilterMock2.filterEntries(isA(File[].class))).thenReturn(returnedFiles);
        assertEquals(returnedFiles, compositeFileFilter.filterEntries(new File[]{fileMock}));
        verify(fileFilterMock1).filterEntries(isA(File[].class));
        verify(fileFilterMock2).filterEntries(isA(File[].class));
    }

    @Test
    public void negative() throws Exception {
        CompositeEntryListFilter<File> compositeFileFilter = new CompositeEntryListFilter<File>(fileFilterMock1, fileFilterMock2);
        when(fileFilterMock2.filterEntries(isA(File[].class))).thenReturn(new ArrayList<File>());
        when(fileFilterMock1.filterEntries(isA(File[].class))).thenReturn(new ArrayList<File>());
        assertTrue(compositeFileFilter.filterEntries(new File[]{fileMock}).isEmpty());
    }
}
