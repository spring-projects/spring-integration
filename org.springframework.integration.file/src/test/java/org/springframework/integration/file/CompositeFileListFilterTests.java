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

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.createNiceMock;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * @author Iwein Fuld
 */
public class CompositeFileListFilterTests {

	private FileListFilter fileFilterMock1 = createMock(FileListFilter.class);

	private FileListFilter fileFilterMock2 = createMock(FileListFilter.class);

	private File fileMock = createNiceMock(File.class);

	@Test
	public void forwardedToFilters() throws Exception {
		CompositeFileListFilter compositeFileFilter = new CompositeFileListFilter(fileFilterMock1, fileFilterMock2);
		List<File> returnedFiles = Arrays.asList(new File[] { fileMock });
		expect(fileFilterMock1.filterFiles(isA(File[].class))).andReturn(returnedFiles).times(1);
		expect(fileFilterMock2.filterFiles(isA(File[].class))).andReturn(returnedFiles).times(1);
		replay(fileFilterMock1, fileFilterMock2);
		assertEquals(returnedFiles, compositeFileFilter.filterFiles(new File[]{fileMock}));
		verify(fileFilterMock1, fileFilterMock2);
	}

	@Test
	public void forwardedToAddedFilters() throws Exception {
		CompositeFileListFilter compositeFileFilter = new CompositeFileListFilter().addFilter(fileFilterMock1, fileFilterMock2);
		List<File> returnedFiles = Arrays.asList(new File[] { fileMock });
		expect(fileFilterMock1.filterFiles(isA(File[].class))).andReturn(returnedFiles).times(1);
		expect(fileFilterMock2.filterFiles(isA(File[].class))).andReturn(returnedFiles).times(1);
		replay(fileFilterMock1, fileFilterMock2);
		assertEquals(returnedFiles, compositeFileFilter.filterFiles(new File[]{fileMock}));
		verify(fileFilterMock1, fileFilterMock2);
	}

	@Test
	public void negative() throws Exception {
		CompositeFileListFilter compositeFileFilter = new CompositeFileListFilter(fileFilterMock1, fileFilterMock2);
		expect(fileFilterMock2.filterFiles(isA(File[].class))).andReturn(new ArrayList<File>()).times(1);
		expect(fileFilterMock1.filterFiles(isA(File[].class))).andReturn(new ArrayList<File>()).times(1);
		replay(fileFilterMock1, fileFilterMock2);
		assertTrue(compositeFileFilter.filterFiles(new File[]{fileMock}).isEmpty());
		verify(fileFilterMock1, fileFilterMock2);
	}
}
