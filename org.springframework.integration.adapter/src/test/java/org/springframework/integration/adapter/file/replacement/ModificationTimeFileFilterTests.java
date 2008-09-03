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
package org.springframework.integration.adapter.file.replacement;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Iwein Fuld
 */
public class ModificationTimeFileFilterTests {

	private AtomicLong timestamp = new AtomicLong(System.currentTimeMillis() - 5000);

	private ModificationTimeFileFilter modificationTimeFileFilter;

	@Before
	public void initialize() {
		modificationTimeFileFilter = new ModificationTimeFileFilter(timestamp);
	}

	@Test
	public void accept() throws Exception {
		File tempFile = File.createTempFile("test", null);
		assertTrue("File modification date too early", tempFile.lastModified() > (timestamp.get()));
		assertTrue("not accepted", modificationTimeFileFilter.accept(tempFile));
	}
}
