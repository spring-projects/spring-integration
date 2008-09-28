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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.message.Message;
import org.springframework.test.annotation.Repeat;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Iwein Fuld
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class PollableFileSourceIntegrationTests {

	@Autowired
	PollableFileSource pollableFileSource;

	private static File inputDir;


	@BeforeClass
	public static void setupInputDir() {
		inputDir = new File(System.getProperty("java.io.tmpdir") + "/"
				+ PollableFileSourceIntegrationTests.class.getSimpleName());
		inputDir.mkdir();
	}

	@Before
	public void generateTestFiles() throws Exception {
		File.createTempFile("test", null, inputDir).setLastModified(System.currentTimeMillis() - 1000);
		File.createTempFile("test", null, inputDir).setLastModified(System.currentTimeMillis() - 1000);
		File.createTempFile("test", null, inputDir).setLastModified(System.currentTimeMillis() - 1000);
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
	}


	@Test
	public void configured() throws Exception {
		DirectFieldAccessor accessor = new DirectFieldAccessor(pollableFileSource);
		assertEquals(inputDir, accessor.getPropertyValue("inputDirectory"));
	}

	@Test
	public void getFiles() throws Exception {
		Message<File> received1 = pollableFileSource.receive();
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
	}

	@Test
	public void parallelRetrieval() throws Exception {
		Message<File> received1 = pollableFileSource.receive();
		Message<File> received2 = pollableFileSource.receive();
		Message<File> received3 = pollableFileSource.receive();
		assertNotSame(received1 + " == " + received2, received1, received2);
		assertNotSame(received1 + " == " + received3, received1, received3);
		assertNotSame(received2 + " == " + received3, received2, received3);
	}

	@Test(timeout = 3000)
	@Repeat(15)
	public void concurrentProcessing() throws Exception {
		CountDownLatch go = new CountDownLatch(1);
		Runnable succesfulConsumer = new Runnable() {
			public void run() {
				Message<File> received = pollableFileSource.receive();
				while (received == null) {
					Thread.yield();
					received = pollableFileSource.receive();
				}
				pollableFileSource.onSend(received);
			}
		};
		Runnable failingConsumer = new Runnable() {
			public void run() {
				Message<File> received = pollableFileSource.receive();
				if (received != null) {
					pollableFileSource.onFailure(received, new RuntimeException("nothing"));
				}
			}
		};
		CountDownLatch succesfulDone = doConcurrently(3, succesfulConsumer, go);
		CountDownLatch failingDone = doConcurrently(10, failingConsumer, go);
		go.countDown();
		try {
			succesfulDone.await();
			failingDone.await();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		// make sure three different files were taken
		Message<File> received = pollableFileSource.receive();
		if (received != null) {
			pollableFileSource.onSend(received);
		}
		assertNull(received);
	}

	/**
	 * Convenience method to run part of a test concurrently in multiple threads
	 * 
	 * @param numberOfThreads
	 * @param todo the runnable that should be run by all the threads
	 * @return a latch that will be counted down once all threads have run their
	 * runnable.
	 */
	private CountDownLatch doConcurrently(int numberOfThreads, final Runnable todo, final CountDownLatch start) {
		final CountDownLatch started = new CountDownLatch(numberOfThreads);
		final CountDownLatch done = new CountDownLatch(numberOfThreads);
		for (int i = 0; i < numberOfThreads; i++) {
			new Thread(new Runnable() {

				public void run() {
					started.countDown();
					try {
						started.await();
						start.await();
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					todo.run();
					done.countDown();
				}
			}).start();
		}
		return done;
	}
}
