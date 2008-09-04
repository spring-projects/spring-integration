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
import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.createNiceMock;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.verify;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileFilter;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Iwein Fuld
 */
public class CompositeFileFilterTest {

	private FileFilter fileFilterMock = createMock(FileFilter.class);

	private CompositeFileFilter compositeFileFilter;

	private File fileMock = createNiceMock(File.class);


	@Before
	public void initialize() {
		compositeFileFilter = CompositeFileFilter.with(fileFilterMock, fileFilterMock);
	}


	@Test
	public void forwardedToFilters() throws Exception {
		expect(fileFilterMock.accept(fileMock)).andReturn(true).times(2);
		replay(fileFilterMock);
		assertTrue(compositeFileFilter.accept(fileMock));
		verify(fileFilterMock);
	}

	@Test
	public void forwardedToAddedFilters() throws Exception {
		expect(fileFilterMock.accept(fileMock)).andReturn(true).times(4);
		replay(fileFilterMock);
		compositeFileFilter.addFilter(fileFilterMock, fileFilterMock);
		assertTrue(compositeFileFilter.accept(fileMock));
		verify(fileFilterMock);
	}

	@Test
	public void notForwardedWhenNegative() throws Exception {
		expect(fileFilterMock.accept(fileMock)).andReturn(true).times(2);
		expect(fileFilterMock.accept(fileMock)).andReturn(false).times(1);
		replay(fileFilterMock);
		compositeFileFilter.addFilter(fileFilterMock, fileFilterMock, fileFilterMock, fileFilterMock);
		assertFalse(compositeFileFilter.accept(fileMock));
		verify(fileFilterMock);
	}

}
