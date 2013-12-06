/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.integration.file.recursive;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.springframework.integration.test.matcher.PayloadMatcher.hasPayload;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Iwein Fuld
 * @author Gunnar Hillert
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode=ClassMode.AFTER_EACH_TEST_METHOD)
public class FileInboundChannelAdapterWithRecursiveDirectoryTests {

	@Autowired
	private TemporaryFolder directory;

	@Autowired
	private PollableChannel files;

	@Test(timeout = 2000)
	public void shouldScanDirectoriesRecursively() throws IOException {

		//when
		File folder = directory.newFolder("foo");
		File file = new File(folder, "bar");
		assertTrue(file.createNewFile());

		//verify
		assertThat(files.receive(), hasPayload(file));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test(timeout = 3000)
	public void shouldReturnFilesMultipleLevels() throws IOException {

		//when
		File folder = directory.newFolder("foo");
		File siblingFile = directory.newFile("bar");
		File childFile = new File(folder, "baz");
		assertTrue(childFile.createNewFile());

		List<Message> received = Arrays.asList((Message) files.receive(), files.receive());
		//verify
	//TODO	assertThat(received, hasItems(hasPayload(siblingFile), hasPayload(childFile)));
	}
}
