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

import java.io.File;
import java.io.FileFilter;
import static org.easymock.classextension.EasyMock.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Iwein Fuld
 */
public class CompositeFileFilterTest {

	private FileFilter fileFilterMock1 = createMock(FileFilter.class);
	private FileFilter fileFilterMock2 = createMock(FileFilter.class);

	private File fileMock = createNiceMock(File.class);



	@Test
	public void forwardedToFilters() throws Exception {
		CompositeFileFilter compositeFileFilter = new CompositeFileFilter(fileFilterMock1, fileFilterMock2);
		expect(fileFilterMock1.accept(fileMock)).andReturn(true).times(1);
		expect(fileFilterMock2.accept(fileMock)).andReturn(true).times(1);
		replay(fileFilterMock1, fileFilterMock2);
		assertTrue(compositeFileFilter.accept(fileMock));
		verify(fileFilterMock1, fileFilterMock2);
	}

	@Test
	public void forwardedToAddedFilters() throws Exception {
		CompositeFileFilter compositeFileFilter = new CompositeFileFilter().addFilter(fileFilterMock1, fileFilterMock2);
		expect(fileFilterMock1.accept(fileMock)).andReturn(true).times(1);
		expect(fileFilterMock2.accept(fileMock)).andReturn(true).times(1);
		replay(fileFilterMock1, fileFilterMock2);
		assertTrue(compositeFileFilter.accept(fileMock));
		verify(fileFilterMock1, fileFilterMock2);
	}

	@Test
	public void negative() throws Exception {
		CompositeFileFilter compositeFileFilter = new CompositeFileFilter(fileFilterMock1, fileFilterMock2);
		expect(fileFilterMock1.accept(fileMock)).andReturn(false).times(1);
		expect(fileFilterMock2.accept(fileMock)).andReturn(true).anyTimes();
		replay(fileFilterMock1, fileFilterMock2);
		assertFalse(compositeFileFilter.accept(fileMock));
		verify(fileFilterMock1, fileFilterMock2);
	}
}
