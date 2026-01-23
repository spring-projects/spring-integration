/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.file.inbound;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Iwein Fuld
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
 */
@SpringJUnitConfig
@DirtiesContext
public class FileReadingMessageSourceIntegrationTests {

	@Autowired
	FileReadingMessageSource pollableFileSource;

	@TempDir
	public static File inputDir;

	@BeforeEach
	public void generateTestFiles() throws IOException {
		File.createTempFile("test", null, inputDir).setLastModified(System.currentTimeMillis() - 1000);
		File.createTempFile("test", null, inputDir).setLastModified(System.currentTimeMillis() - 1000);
		File.createTempFile("test", null, inputDir).setLastModified(System.currentTimeMillis() - 1000);
	}

	@AfterEach
	public void cleanupInputDir() {
		File[] listFiles = inputDir.listFiles();
		for (File listFile : listFiles) {
			listFile.delete();
		}
	}

	@Test
	public void configured() {
		assertThat(TestUtils.<File>getPropertyValue(this.pollableFileSource,
				"directoryExpression.value")).isEqualTo(inputDir);
	}

	@Test
	public void getFiles() {
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
	public void parallelRetrieval() {
		Message<File> received1 = pollableFileSource.receive();
		Message<File> received2 = pollableFileSource.receive();
		Message<File> received3 = pollableFileSource.receive();
		assertThat(received2).as(received1 + " == " + received2).isNotSameAs(received1);
		assertThat(received3).as(received1 + " == " + received3).isNotSameAs(received1);
		assertThat(received3).as(received2 + " == " + received3).isNotSameAs(received2);
	}

	@Test
	public void inputDirExhausted() {
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
	public void concurrentProcessing() {
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
	private static CountDownLatch doConcurrently(int numberOfThreads, Runnable runnable, CountDownLatch start) {
		final CountDownLatch started = new CountDownLatch(numberOfThreads);
		final CountDownLatch done = new CountDownLatch(numberOfThreads);
		for (int i = 0; i < numberOfThreads; i++) {
			new Thread(() -> {
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
			}).start();
		}
		return done;
	}

}
