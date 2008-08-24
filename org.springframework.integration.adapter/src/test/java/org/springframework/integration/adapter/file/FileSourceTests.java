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

package org.springframework.integration.adapter.file;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.integration.message.Message;

/**
 * @author Iwein Fuld
 */
public class FileSourceTests {

	private FileSource fileSource;

	private static final String INPUT_DIR = System.getProperty("java.io.tmpdir") + "/"
			+ FileSourceTests.class.getCanonicalName();

	@BeforeClass
	public static void createTmpDir() {
		new File(INPUT_DIR).mkdirs();
	}

	@Before
	public void refreshFileSource() {
		fileSource = new FileSource(new FileSystemResource(INPUT_DIR));
	}

	@After
	public void cleanOutTmp() {
		for (File file : new File(INPUT_DIR).listFiles()) {
			file.delete();
		}
	}

	@AfterClass
	public static void removeTmp() {
		if (!new File(INPUT_DIR).delete()) {
			throw new RuntimeException("failed to clean up directory [" + INPUT_DIR + "]");
		}
	}

	@Test
	public void testNoFile() {
		assertNull("There should be no message on the source", fileSource.receive());
	}

	@Test
	public void closedEmptyFile() throws Exception {
		new File(INPUT_DIR + "/test").createNewFile();
		assertNotNull("No file received after writing to input directory", fileSource.receive());
	}

	@Test
	public void closedWriter() throws Exception {
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(INPUT_DIR + "/test")));
		writer.write("some stuff");
		writer.close();
		assertNotNull("No file received after writing to input directory", fileSource.receive());
	}

	@Test
	/*
	 * This test shows the how not to do it (i.e. without a proper external
	 * trigger)
	 */
	public void testOpenWriter() throws Exception {
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(INPUT_DIR + "/test")));
		writer.write("some stuff");
		// don't close the writer yet
		Message<File> received = fileSource.receive();
		assertNotNull("incomplete file was not received", received);
		fileSource.onSend(received);
		writer.write("some more stuff");
		writer.close();
		assertNotNull("something shoulda happened don't you think?", received);
	}
}
