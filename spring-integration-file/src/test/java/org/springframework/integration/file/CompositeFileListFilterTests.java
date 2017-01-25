/*
 * Copyright 2002-2016 the original author or authors.
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

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.integration.file.filters.ChainFileListFilter;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;

/**
 * @author Iwein Fuld
 * @author Gary Russell
 * @author Aaron Grant
 * @author Artem Bilan
 */
public class CompositeFileListFilterTests {

	@SuppressWarnings("unchecked")
	private final FileListFilter<File> fileFilterMock1 = mock(FileListFilter.class);

	@SuppressWarnings("unchecked")
	private final FileListFilter<File> fileFilterMock2 = mock(FileListFilter.class);

	private final File fileMock = mock(File.class);

	@Test
	public void forwardedToFilters() throws Exception {
		CompositeFileListFilter<File> compositeFileFilter = new CompositeFileListFilter<File>();
		compositeFileFilter.addFilter(fileFilterMock1);
		compositeFileFilter.addFilter(fileFilterMock2);
		List<File> returnedFiles = Collections.singletonList(fileMock);
		when(fileFilterMock1.filterFiles(isA(File[].class))).thenReturn(returnedFiles);
		when(fileFilterMock2.filterFiles(isA(File[].class))).thenReturn(returnedFiles);
		assertEquals(returnedFiles, compositeFileFilter.filterFiles(new File[] { fileMock }));
		verify(fileFilterMock1).filterFiles(isA(File[].class));
		verify(fileFilterMock2).filterFiles(isA(File[].class));
		compositeFileFilter.close();
	}

	@Test
	public void forwardedToAddedFilters() throws Exception {
		CompositeFileListFilter<File> compositeFileFilter = new CompositeFileListFilter<File>();
		compositeFileFilter.addFilter(fileFilterMock1);
		compositeFileFilter.addFilter(fileFilterMock2);
		List<File> returnedFiles = Collections.singletonList(fileMock);
		when(fileFilterMock1.filterFiles(isA(File[].class))).thenReturn(returnedFiles);
		when(fileFilterMock2.filterFiles(isA(File[].class))).thenReturn(returnedFiles);
		assertEquals(returnedFiles, compositeFileFilter.filterFiles(new File[] { fileMock }));
		verify(fileFilterMock1).filterFiles(isA(File[].class));
		verify(fileFilterMock2).filterFiles(isA(File[].class));
		compositeFileFilter.close();
	}

	@Test
	public void negative() throws Exception {
		CompositeFileListFilter<File> compositeFileFilter = new CompositeFileListFilter<File>();
		compositeFileFilter.addFilter(fileFilterMock1);
		compositeFileFilter.addFilter(fileFilterMock2);

		when(fileFilterMock2.filterFiles(isA(File[].class))).thenReturn(new ArrayList<File>());
		when(fileFilterMock1.filterFiles(isA(File[].class))).thenReturn(new ArrayList<File>());
		assertTrue(compositeFileFilter.filterFiles(new File[] { fileMock }).isEmpty());
		compositeFileFilter.close();
	}

	@Test
	public void excludeFromLaterFilters() throws Exception {
		CompositeFileListFilter<File> compositeFileFilter = new ChainFileListFilter<>();
		compositeFileFilter.addFilter(this.fileFilterMock1);
		compositeFileFilter.addFilter(this.fileFilterMock2);
		List<File> noFiles = new ArrayList<>();
		when(this.fileFilterMock1.filterFiles(isA(File[].class))).thenReturn(noFiles);
		assertEquals(noFiles, compositeFileFilter.filterFiles(new File[] { this.fileMock }));

		ArgumentCaptor<File[]> captor = ArgumentCaptor.forClass(File[].class);
		verify(fileFilterMock1).filterFiles(captor.capture());
		assertThat(captor.getValue().length, equalTo(1));
		verify(fileFilterMock2, never()).filterFiles(isA(File[].class));

		compositeFileFilter.close();
	}

}
