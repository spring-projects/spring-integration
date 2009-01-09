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

import static org.easymock.classextension.EasyMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.io.File;
import java.util.Comparator;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.Resource;
import org.springframework.integration.core.Message;

/**
 * @author Iwein Fuld
 */
@SuppressWarnings("unchecked")
public class FileReadingMessageSourceTests {

	private FileReadingMessageSource source;

	private File inputDirectoryMock = createMock(File.class);

	private Resource inputDirectoryResourceMock = createNiceMock(Resource.class);

	private File fileMock = createMock(File.class);

	private Comparator<File> comparator = createMock(Comparator.class);

	private Object[] allMocks = new Object[] { inputDirectoryMock, fileMock, inputDirectoryResourceMock, comparator };

	public void prepResource() throws Exception {
		expect(inputDirectoryResourceMock.exists()).andReturn(true).anyTimes();
		expect(inputDirectoryResourceMock.getFile()).andReturn(inputDirectoryMock).anyTimes();
		expect(inputDirectoryMock.canRead()).andReturn(true);
		replay(inputDirectoryResourceMock, inputDirectoryMock);
	}

	@Before
	public void initialize() throws Exception {
		prepResource();
		this.source = new FileReadingMessageSource(comparator);
		source.setInputDirectory(inputDirectoryResourceMock);
		reset(allMocks);
	}

	@Test
	public void straightProcess() throws Exception {
		expect(inputDirectoryMock.listFiles()).andReturn(new File[] { fileMock });
		replay(allMocks);
		source.onSend(source.receive());
		verify(allMocks);
	}

	@Test
	public void requeueOnFailure() throws Exception {
		expect(inputDirectoryMock.listFiles()).andReturn(new File[] { fileMock }).times(2);
		replay(allMocks);
		Message received = source.receive();
		assertNotNull(received);
		source.onFailure(received, new RuntimeException("failed"));
		assertEquals(received.getPayload(), source.receive().getPayload());
		verify(allMocks);
	}

	@Test
	public void noDuplication() throws Exception {
		expect(inputDirectoryMock.listFiles()).andReturn(new File[] { fileMock }).times(2);
		replay(allMocks);
		Message<File> received = source.receive();
		assertNotNull(received);
		assertEquals(fileMock, received.getPayload());
		assertNull(source.receive());
		verify(allMocks);
	}

	@Test
	public void orderedReception() throws Exception {
		File file1 = createMock(File.class);
		File file2 = createMock(File.class);
		File file3 = createMock(File.class);
		
		//record the comparator to reverse order the files
		expect(comparator.compare(file1, file2)).andReturn(1).anyTimes();
		expect(comparator.compare(file1, file3)).andReturn(1).anyTimes();
		expect(comparator.compare(file2, file3)).andReturn(1).anyTimes();
		expect(comparator.compare(file2, file1)).andReturn(-1).anyTimes();
		expect(comparator.compare(file3, file1)).andReturn(-1).anyTimes();
		expect(comparator.compare(file3, file2)).andReturn(-1).anyTimes();
		
		expect(inputDirectoryMock.listFiles()).andReturn(new File[] { file2, file3, file1 }).anyTimes();
		replay(allMocks);
		assertSame(file3, source.receive().getPayload());
		assertSame(file2, source.receive().getPayload());
		assertSame(file1, source.receive().getPayload());
		assertNull(source.receive());
		verify(allMocks);
	}
}
