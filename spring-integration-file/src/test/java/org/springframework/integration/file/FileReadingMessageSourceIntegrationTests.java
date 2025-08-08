/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.file;

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
import org.springframework.messaging.Message;
import org.springframework.test.annotation.Repeat;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Iwein Fuld
 * @author Gary Russell
 * @author Artem Bilan
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class FileReadingMessageSourceIntegrationTests {

	@Autowired
	FileReadingMessageSource pollableFileSource;

	private static File inputDir;

	@AfterClass
	public static void cleanUp() throws Throwable {
		if (inputDir.exists()) {
			inputDir.delete();
		}
	}

	@BeforeClass
	public static void setupInputDir() {
		inputDir = new File(System.getProperty("java.io.tmpdir") + "/"
				+ FileReadingMessageSourceIntegrationTests.class.getSimpleName());
		inputDir.mkdir();
		clean();
	}

	@AfterClass
	public static void tearDown() {
		clean();
		inputDir.delete();
	}

	private static void clean() {
		File[] files = inputDir.listFiles();
		for (File file : files) {
			file.delete();
		}
	}

	@Before
	public void generateTestFiles() throws Exception {
		File.createTempFile("test", null, inputDir).setLastModified(System.currentTimeMillis() - 1000);
		File.createTempFile("test", null, inputDir).setLastModified(System.currentTimeMillis() - 1000);
		File.createTempFile("test", null, inputDir).setLastModified(System.currentTimeMillis() - 1000);
	}

	@After
	public void cleanupInputDir() throws Exception {
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
		assertThat(accessor.getPropertyValue("directory")).isEqualTo(inputDir);
	}

	@Test
	public void getFiles() throws Exception {
		Message<File> received1 = pollableFileSource.receive();
		assertThat(received1).as("This should return the first message").isNotNull();
		Message<File> received2 = pollableFileSource.receive();
		assertThat(received2).isNotNull();
		Message<File> received3 = pollableFileSource.receive();
		assertThat(received3).isNotNull();
		assertThat(received2.getPayload()).as(received1 + " == " + received2).isNotSameAs(received1.getPayload());
		assertThat(received3.getPayload()).as(received1 + " == " + received3).isNotSameAs(received1.getPayload());
		assertThat(received3.getPayload()).as(received2 + " == " + received3).isNotSameAs(received2.getPayload());
	}

	@Test
	public void parallelRetrieval() throws Exception {
		Message<File> received1 = pollableFileSource.receive();
		Message<File> received2 = pollableFileSource.receive();
		Message<File> received3 = pollableFileSource.receive();
		assertThat(received2).as(received1 + " == " + received2).isNotSameAs(received1);
		assertThat(received3).as(received1 + " == " + received3).isNotSameAs(received1);
		assertThat(received3).as(received2 + " == " + received3).isNotSameAs(received2);
	}

	@Test
	public void inputDirExhausted() throws Exception {
		assertThat(pollableFileSource.receive()).isNotNull();
		assertThat(pollableFileSource.receive()).isNotNull();
		Message<File> receive = pollableFileSource.receive();
		assertThat(receive).isNotNull();
		File payload = receive.getPayload();
		assertThat(receive.getHeaders().get(FileHeaders.ORIGINAL_FILE)).isEqualTo(payload);
		assertThat(receive.getHeaders().get(FileHeaders.FILENAME)).isEqualTo(payload.getName());
		assertThat(receive.getHeaders().get(FileHeaders.RELATIVE_PATH)).isEqualTo(payload.getName());
		assertThat(pollableFileSource.receive()).isNull();
	}

	@Test
	@Repeat(5)
	public void concurrentProcessing() throws Exception {
		CountDownLatch go = new CountDownLatch(1);
		Runnable successfulConsumer = () -> {
			Message<File> received = pollableFileSource.receive();
			while (received == null) {
				Thread.yield();
				received = pollableFileSource.receive();
			}
		};
		Runnable failingConsumer = () -> {
			Message<File> received = pollableFileSource.receive();
			if (received != null) {
				pollableFileSource.onFailure(received);
			}
		};
		CountDownLatch successfulDone = doConcurrently(3, successfulConsumer, go);
		CountDownLatch failingDone = doConcurrently(10, failingConsumer, go);
		go.countDown();
		try {
			successfulDone.await();
			failingDone.await();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		// make sure three different files were taken
		Message<File> received = pollableFileSource.receive();
		assertThat(received).isNull();
	}

	/**
	 * Convenience method to run part of a test concurrently in multiple threads
	 *
	 * @param numberOfThreads how many threads to spawn
	 * @param runnable        the runnable that should be run by all the threads
	 * @param start           the {@link java.util.concurrent.CountDownLatch} instance
	 *                        telling it when to assume everything works
	 * @return a latch that will be counted down once all threads have run their
	 *		 runnable.
	 */
	private CountDownLatch doConcurrently(int numberOfThreads, final Runnable runnable, final CountDownLatch start) {
		final CountDownLatch started = new CountDownLatch(numberOfThreads);
		final CountDownLatch done = new CountDownLatch(numberOfThreads);
		for (int i = 0; i < numberOfThreads; i++) {
			new Thread(new Runnable() {

				@Override
				public void run() {
					started.countDown();
					try {
						started.await();
						start.await();
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					runnable.run();
					done.countDown();
				}

			}).start();
		}
		return done;
	}

}
