/*
 * Copyright 2015 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Gary Russell
 * @since 4.2
 *
 */
public class WatchServiceDirectoryScannerTests {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private File foo;

	private File bar;

	private File top1;

	private File foo1;

	private File bar1;

	@Before
	public void setUp() throws IOException {
		this.foo = this.folder.newFolder("foo");
		this.bar = this.folder.newFolder("bar");
		this.top1 = this.folder.newFile();
		this.foo1 = File.createTempFile("foo", ".txt", this.foo);
		this.bar1 = File.createTempFile("bar", ".txt", this.bar);
	}

	@Test
	public void testInitial() throws Exception {
		WatchServiceDirectoryScanner scanner = new WatchServiceDirectoryScanner(folder.getRoot().getAbsolutePath());
		scanner.start();
		List<File> files = scanner.listFiles(folder.getRoot());
		assertEquals(3, files.size());
		assertTrue(files.contains(top1));
		assertTrue(files.contains(foo1));
		assertTrue(files.contains(bar1));
		File top2 = this.folder.newFile();
		File foo2 = File.createTempFile("foo", ".txt", this.foo);
		File bar2 = File.createTempFile("bar", ".txt", this.bar);
		File baz = new File(this.foo, "baz");
		baz.mkdir();
		File baz1 = File.createTempFile("baz", ".txt", baz);
		files = scanner.listFiles(folder.getRoot());
		int n = 0;
		while (n++ < 200 && files.size() == 0) {
			Thread.sleep(100);
			files = scanner.listFiles(folder.getRoot());
		}
		assertEquals(4, files.size());
		assertTrue(files.contains(top2));
		assertTrue(files.contains(foo2));
		assertTrue(files.contains(bar2));
		assertTrue(files.contains(baz1));
		scanner.stop();
	}

}
