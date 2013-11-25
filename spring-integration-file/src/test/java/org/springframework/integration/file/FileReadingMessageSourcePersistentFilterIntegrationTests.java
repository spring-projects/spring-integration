/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

import java.io.File;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.messaging.Message;

/**
 * @author Iwein Fuld
 * @author Gary Russell
 */
public class FileReadingMessageSourcePersistentFilterIntegrationTests {

	AbstractApplicationContext context;

	FileReadingMessageSource pollableFileSource;

	private static File inputDir;

	@AfterClass
	public static void cleanUp() throws Throwable {
	  if(inputDir.exists()) {
		  inputDir.delete();
	  }
	}

	@BeforeClass
	public static void setupInputDir() {
		inputDir = new File(System.getProperty("java.io.tmpdir") + "/"
				+ FileReadingMessageSourcePersistentFilterIntegrationTests.class.getSimpleName());
		inputDir.mkdir();
	}

	@Before
	public void generateTestFiles() throws Exception {
		File.createTempFile("test", null, inputDir).setLastModified(System.currentTimeMillis() - 1000);
		File.createTempFile("test", null, inputDir).setLastModified(System.currentTimeMillis() - 1000);
		File.createTempFile("test", null, inputDir).setLastModified(System.currentTimeMillis() - 1000);
		this.loadContextAndGetMessageSource();
	}

	private void loadContextAndGetMessageSource() {
		this.context = new ClassPathXmlApplicationContext(this.getClass().getSimpleName() + "-context.xml",
				this.getClass());
		this.pollableFileSource = context.getBean(FileReadingMessageSource.class);
	}

	@After
	public void cleanoutInputDir() throws Exception {
		File[] listFiles = inputDir.listFiles();
		for (int i = 0; i < listFiles.length; i++) {
			listFiles[i].delete();
		}
	}

	@AfterClass
	public static void removeInputDir() throws Exception {
		inputDir.delete();
		File persistDir = new File(System.getProperty("java.io.tmpdir") + "/"
				+ FileReadingMessageSourcePersistentFilterIntegrationTests.class.getSimpleName()
				+ ".meta");
		File persist = new File(persistDir, "metadata-store.properties");
		persist.delete();
		persistDir.delete();
	}


	@Test
	public void configured() throws Exception {
		DirectFieldAccessor accessor = new DirectFieldAccessor(pollableFileSource);
		assertEquals(inputDir, accessor.getPropertyValue("directory"));
	}

	@Test
	public void getFiles() throws Exception {
		Message<File> received1 = pollableFileSource.receive();
		System.out.println("receive files round 1");
		assertNotNull("This should return the first message", received1);
		pollableFileSource.onSend(received1);
		Message<File> received2 = pollableFileSource.receive();
		assertNotNull(received2);
		pollableFileSource.onSend(received2);
		Message<File> received3 = pollableFileSource.receive();
		assertNotNull(received3);
		pollableFileSource.onSend(received3);
		assertNotSame(received1 + " == " + received2, received1.getPayload(), received2.getPayload());
		assertNotSame(received1 + " == " + received3, received1.getPayload(), received3.getPayload());
		assertNotSame(received2 + " == " + received3, received2.getPayload(), received3.getPayload());
		this.context.destroy();

		this.loadContextAndGetMessageSource();
		Message<File> received4 = pollableFileSource.receive();
		assertNull(received4);
		this.context.destroy();
	}

}
