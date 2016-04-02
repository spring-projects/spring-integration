/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.integration.file.filters;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @since 4.2
 *
 */
public class LastModifiedFileListFilterTests {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void testAge() throws Exception {
		LastModifiedFileListFilter filter = new LastModifiedFileListFilter();
		filter.setAge(60, TimeUnit.SECONDS);
		File foo = this.folder.newFile();
		FileOutputStream fileOutputStream = new FileOutputStream(foo);
		fileOutputStream.write("x".getBytes());
		fileOutputStream.close();
		assertEquals(0, filter.filterFiles(new File[] { foo }).size());
		// Make a file as of yesterday's
		foo.setLastModified(System.currentTimeMillis() - 1000 * 60 * 60 * 24);
		assertEquals(1, filter.filterFiles(new File[] { foo }).size());
	}

}
