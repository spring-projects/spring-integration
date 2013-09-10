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

package org.springframework.integration.file;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.messaging.Message;

/**
 * @author Iwein Fuld
 * @author Mark Fisher
 */
@RunWith(MockitoJUnitRunner.class)
public class FileReadingMessageSourceTests {

    private FileReadingMessageSource source;

    @Mock
    private File inputDirectoryMock;

    @Mock
    private File fileMock;

    @Mock
    private FileLocker locker;

    @Mock
    private Comparator<File> comparator;

    public void prepResource() throws Exception {
        when(inputDirectoryMock.exists()).thenReturn(true);
        when(inputDirectoryMock.canRead()).thenReturn(true);
        when(locker.lock(isA(File.class))).thenReturn(true);
    }

    @Before
    public void initialize() throws Exception {
        prepResource();
        this.source = new FileReadingMessageSource(comparator);
        source.setDirectory(inputDirectoryMock);
        source.setLocker(locker);
    }

    @Test
    public void straightProcess() throws Exception {
        when(inputDirectoryMock.listFiles()).thenReturn(new File[]{fileMock});
        assertThat(source.receive().getPayload(), is(fileMock));
    }

    @Test
    public void requeueOnFailure() throws Exception {
        when(inputDirectoryMock.listFiles()).thenReturn(new File[]{fileMock});
        Message<File> received = source.receive();
        assertNotNull(received);
        source.onFailure(received);
        assertEquals(received.getPayload(), source.receive().getPayload());
        verify(inputDirectoryMock, times(1)).listFiles();
    }

    @Test
    public void scanEachPoll() throws Exception {
        File anotherFileMock = mock(File.class);
        when(inputDirectoryMock.listFiles()).thenReturn(new File[]{fileMock, anotherFileMock});
        source.setScanEachPoll(true);
        assertNotNull(source.receive());
        assertNotNull(source.receive());
        assertNull(source.receive());
        verify(inputDirectoryMock, times(3)).listFiles();
    }

    @Test
    public void noDuplication() throws Exception {
        when(inputDirectoryMock.listFiles()).thenReturn(new File[]{fileMock});
        Message<File> received = source.receive();
        assertNotNull(received);
        assertEquals(fileMock, received.getPayload());
        assertNull(source.receive());
        verify(inputDirectoryMock, times(2)).listFiles();
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullFilter() throws Exception {
        source.setFilter(null);
    }

    @Test
    public void lockIsAquired() throws IOException {
        when(inputDirectoryMock.listFiles()).thenReturn(new File[]{fileMock});
        Message<File> received = source.receive();
        assertNotNull(received);
        assertEquals(fileMock, received.getPayload());
        verify(locker).lock(fileMock);
    }

    @Test
    public void lockedFilesAreIgnored() throws IOException {
        when(inputDirectoryMock.listFiles()).thenReturn(new File[]{fileMock});
        when(locker.lock(fileMock)).thenReturn(false);
        Message<File> received = source.receive();
        assertNull(received);
        verify(locker).lock(fileMock);
    }

    @Test
    public void orderedReception() throws Exception {
        File file1 = mock(File.class);
        File file2 = mock(File.class);
        File file3 = mock(File.class);

        // record the comparator to reverse order the files
        when(comparator.compare(file1, file2)).thenReturn(1);
        when(comparator.compare(file1, file3)).thenReturn(1);
        when(comparator.compare(file2, file3)).thenReturn(1);
        when(comparator.compare(file2, file1)).thenReturn(-1);
        when(comparator.compare(file3, file1)).thenReturn(-1);
        when(comparator.compare(file3, file2)).thenReturn(-1);

        when(inputDirectoryMock.listFiles()).thenReturn(new File[]{file2, file3, file1});
        assertSame(file3, source.receive().getPayload());
        assertSame(file2, source.receive().getPayload());
        assertSame(file1, source.receive().getPayload());
        assertNull(source.receive());
        verify(inputDirectoryMock, times(2)).listFiles();
    }

}
