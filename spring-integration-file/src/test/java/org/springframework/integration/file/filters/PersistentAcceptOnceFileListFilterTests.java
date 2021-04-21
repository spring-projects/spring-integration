/*
 * Copyright 2013-2021 the original author or authors.
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

package org.springframework.integration.file.filters;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Closeable;
import java.io.File;
import java.io.Flushable;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.metadata.SimpleMetadataStore;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 3.0
 *
 */
public class PersistentAcceptOnceFileListFilterTests extends AcceptOnceFileListFilterTests {

	@Test
	public void testFileSystem() throws Exception {
		final AtomicBoolean suspend = new AtomicBoolean();
		final CountDownLatch latch1 = new CountDownLatch(1);
		final CountDownLatch latch2 = new CountDownLatch(1);
		ConcurrentMetadataStore store = new SimpleMetadataStore() {

			@Override
			public boolean replace(String key, String oldValue, String newValue) {
				if (suspend.get()) {
					latch2.countDown();
					try {
						latch1.await(10, TimeUnit.SECONDS);
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
				return super.replace(key, oldValue, newValue);
			}

		};
		final FileSystemPersistentAcceptOnceFileListFilter filter =
				new FileSystemPersistentAcceptOnceFileListFilter(store, "foo:");
		final File file = File.createTempFile("foo", ".txt");
		assertThat(filter.filterFiles(new File[]{ file }).size()).isEqualTo(1);
		String ts = store.get("foo:" + file.getAbsolutePath());
		assertThat(ts).isEqualTo(String.valueOf(file.lastModified()));
		assertThat(filter.filterFiles(new File[]{ file }).size()).isEqualTo(0);
		file.setLastModified(file.lastModified() + 5000L);
		assertThat(filter.filterFiles(new File[]{ file }).size()).isEqualTo(1);
		ts = store.get("foo:" + file.getAbsolutePath());
		assertThat(ts).isEqualTo(String.valueOf(file.lastModified()));
		assertThat(filter.filterFiles(new File[]{ file }).size()).isEqualTo(0);

		suspend.set(true);
		file.setLastModified(file.lastModified() + 5000L);

		Future<Integer> result = Executors.newSingleThreadExecutor()
				.submit(() -> filter.filterFiles(new File[]{ file }).size());
		assertThat(latch2.await(10, TimeUnit.SECONDS)).isTrue();
		store.put("foo:" + file.getAbsolutePath(), "43");
		latch1.countDown();
		Integer theResult = result.get(10, TimeUnit.SECONDS);
		assertThat(theResult).isEqualTo(Integer.valueOf(0)); // lost the race, key changed

		file.delete();
		filter.close();
	}

	@Override
	@Test
	public void testRollback() {
		AbstractPersistentAcceptOnceFileListFilter<String> filter =
				new AbstractPersistentAcceptOnceFileListFilter<>(new SimpleMetadataStore(), "rollback:") {

					@Override
					protected long modified(String file) {
						return 0;
					}

					@Override
					protected String fileName(String file) {
						return file;
					}

				};
		doTestRollback(filter);
	}

	@Test
	public void testRollbackFileSystem() throws Exception {
		FileSystemPersistentAcceptOnceFileListFilter filter = new FileSystemPersistentAcceptOnceFileListFilter(
				new SimpleMetadataStore(), "rollback:");
		File[] files = new File[]{ new File("foo"), new File("bar"), new File("baz") };
		List<File> passed = filter.filterFiles(files);
		assertThat(passed.size()).isEqualTo(0);
		for (File file : files) {
			file.createNewFile();
		}
		passed = filter.filterFiles(files);
		assertThat(Arrays.equals(files, passed.toArray())).isTrue();
		List<File> now = filter.filterFiles(files);
		assertThat(now.size()).isEqualTo(0);
		filter.rollback(passed.get(1), passed);
		now = filter.filterFiles(files);
		assertThat(now.size()).isEqualTo(2);
		assertThat(now.get(0).getName()).isEqualTo("bar");
		assertThat(now.get(1).getName()).isEqualTo("baz");
		now = filter.filterFiles(files);
		assertThat(now.size()).isEqualTo(0);
		filter.close();
		for (File file : files) {
			file.delete();
		}
	}

	@Test
	/*
	 * INT-3721: Test all operations that can cause the metadata to be flushed.
	 */
	public void testFlush() throws Exception {
		final AtomicInteger flushes = new AtomicInteger();
		final AtomicBoolean replaced = new AtomicBoolean();
		class MS extends SimpleMetadataStore implements Flushable, Closeable {

			@Override
			public void flush() {
				flushes.incrementAndGet();
			}

			@Override
			public void close() throws IOException {
				flush();
			}

			@Override
			public boolean replace(String key, String oldValue, String newValue) {
				replaced.set(true);
				return super.replace(key, oldValue, newValue);
			}

		}
		MS store = new MS();
		String prefix = "flush:";
		FileSystemPersistentAcceptOnceFileListFilter filter = new FileSystemPersistentAcceptOnceFileListFilter(
				store, prefix);
		final File file = File.createTempFile("foo", ".txt");
		File[] files = new File[]{ file };
		List<File> passed = filter.filterFiles(files);
		assertThat(Arrays.equals(files, passed.toArray())).isTrue();
		filter.rollback(passed.get(0), passed);
		assertThat(flushes.get()).isEqualTo(0);
		filter.setFlushOnUpdate(true);
		passed = filter.filterFiles(files);
		assertThat(Arrays.equals(files, passed.toArray())).isTrue();
		assertThat(flushes.get()).isEqualTo(1);
		filter.rollback(passed.get(0), passed);
		assertThat(flushes.get()).isEqualTo(2);
		passed = filter.filterFiles(files);
		assertThat(Arrays.equals(files, passed.toArray())).isTrue();
		assertThat(flushes.get()).isEqualTo(3);
		passed = filter.filterFiles(files);
		assertThat(passed.size()).isEqualTo(0);
		assertThat(flushes.get()).isEqualTo(3);
		assertThat(replaced.get()).isFalse();
		store.put(prefix + file.getAbsolutePath(), "1");
		passed = filter.filterFiles(files);
		assertThat(Arrays.equals(files, passed.toArray())).isTrue();
		assertThat(flushes.get()).isEqualTo(4);
		assertThat(replaced.get()).isTrue();
		file.delete();
		filter.close();
		assertThat(flushes.get()).isEqualTo(5);
	}

}
