/*
 * Copyright 2015-2024 the original author or authors.
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

package org.springframework.integration.file;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.file.filters.ChainFileListFilter;
import org.springframework.integration.file.filters.FileSystemPersistentAcceptOnceFileListFilter;
import org.springframework.integration.file.filters.LastModifiedFileListFilter;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.2
 *
 */
public class WatchServiceDirectoryScannerTests {

	@TempDir
	public File rootDir;

	private File foo;

	private File bar;

	private File top1;

	private File foo1;

	private File bar1;

	private File skipped;

	private File skippedFile;

	@BeforeEach
	public void setUp() throws IOException {
		this.foo = new File(rootDir, "foo");
		this.foo.mkdir();
		this.bar = new File(rootDir, "bar");
		this.bar.mkdir();
		this.top1 = File.createTempFile("tmp", null, this.rootDir);
		this.foo1 = File.createTempFile("foo", ".txt", this.foo);
		this.bar1 = File.createTempFile("bar", ".txt", this.bar);
		this.skipped = new File(rootDir, "skipped");
		this.skipped.mkdir();
		this.skippedFile = File.createTempFile("skippedFile", null, this.skipped);
	}

	@Test
	public void testWatchServiceDirectoryScanner() throws Exception {
		FileReadingMessageSource fileReadingMessageSource = new FileReadingMessageSource();
		fileReadingMessageSource.setDirectory(this.rootDir);
		fileReadingMessageSource.setUseWatchService(true);
		fileReadingMessageSource.setWatchEvents(FileReadingMessageSource.WatchEventType.CREATE,
				FileReadingMessageSource.WatchEventType.MODIFY,
				FileReadingMessageSource.WatchEventType.DELETE);
		fileReadingMessageSource.setWatchDirPredicate(path -> !path.getFileName().toString().equals("skipped"));
		fileReadingMessageSource.setBeanFactory(mock(BeanFactory.class));

		final CountDownLatch removeFileLatch = new CountDownLatch(1);

		FileSystemPersistentAcceptOnceFileListFilter fileSystemPersistentAcceptOnceFileListFilter =
				new FileSystemPersistentAcceptOnceFileListFilter(new SimpleMetadataStore(), "test") {

					@Override
					public boolean remove(File fileToRemove) {
						removeFileLatch.countDown();
						return super.remove(fileToRemove);
					}

				};

		LastModifiedFileListFilter fileLastModifiedFileListFilter = new LastModifiedFileListFilter();

		ChainFileListFilter<File> fileChainFileListFilter = new ChainFileListFilter<>();
		fileChainFileListFilter.addFilters(fileLastModifiedFileListFilter, fileSystemPersistentAcceptOnceFileListFilter);

		fileReadingMessageSource.setFilter(fileChainFileListFilter);
		fileReadingMessageSource.afterPropertiesSet();
		fileReadingMessageSource.start();
		DirectoryScanner scanner = fileReadingMessageSource.getScanner();
		assertThat(scanner.getClass().getName()).contains("FileReadingMessageSource$WatchServiceDirectoryScanner");

		// Files are skipped by the LastModifiedFileListFilter
		List<File> files = scanner.listFiles(this.rootDir);
		assertThat(files).hasSize(0);
		// Consider all the files as one day old
		fileLastModifiedFileListFilter.setAge(-60 * 60 * 24);
		files = scanner.listFiles(this.rootDir);
		assertThat(files).hasSize(3);
		assertThat(files).contains(top1);
		assertThat(files).contains(foo1);
		assertThat(files).contains(bar1);
		assertThat(files).doesNotContain(this.skippedFile);
		fileReadingMessageSource.start();
		File top2 = File.createTempFile("tmp", null, this.rootDir);
		File foo2 = File.createTempFile("foo", ".txt", this.foo);
		File bar2 = File.createTempFile("bar", ".txt", this.bar);
		File baz = new File(this.foo, "baz");
		baz.mkdir();
		File baz1 = File.createTempFile("baz", ".txt", baz);
		files = scanner.listFiles(this.rootDir);
		int n = 0;
		Set<File> accum = new HashSet<>(files);
		while (n++ < 300 && accum.size() != 4) {
			Thread.sleep(100);
			files = scanner.listFiles(this.rootDir);
			accum.addAll(files);
		}
		assertThat(accum).hasSize(4);
		assertThat(accum).contains(top2);
		assertThat(accum).contains(foo2);
		assertThat(accum).contains(bar2);
		assertThat(accum).contains(baz1);

		/*See AbstractWatchKey#signalEvent source code:
			if(var5 >= 512) {
				var1 = StandardWatchEventKinds.OVERFLOW;
			}
		*/
		fileReadingMessageSource.start();
		List<File> filesForOverflow = new ArrayList<>(600);

		for (int i = 0; i < 600; i++) {
			filesForOverflow.add(File.createTempFile("tmp" + i, null, this.rootDir));
		}

		n = 0;
		while (n++ < 300 && accum.size() < 604) {
			Thread.sleep(100);
			files = scanner.listFiles(this.rootDir);
			accum.addAll(files);
		}

		assertThat(accum).hasSize(604);

		assertThat(accum).containsAll(filesForOverflow);

		File baz2 = File.createTempFile("baz2", ".txt", baz);

		n = 0;
		while (n++ < 300 && accum.size() < 605) {
			Thread.sleep(100);
			files = scanner.listFiles(this.rootDir);
			accum.addAll(files);
		}

		assertThat(accum).contains(baz2);

		File baz2Copy = new File(baz2.getAbsolutePath());

		baz2Copy.setLastModified(baz2.lastModified() + 100000);

		n = 0;
		files.clear();
		while (n++ < 300 && files.size() < 1) {
			Thread.sleep(100);
			files = scanner.listFiles(this.rootDir);
			accum.addAll(files);
		}

		assertThat(files).hasSize(1);
		assertThat(files).contains(baz2);

		baz2.delete();

		n = 0;
		while (n++ < 300 && removeFileLatch.getCount() > 0) {
			Thread.sleep(100);
			scanner.listFiles(this.rootDir);
		}

		assertThat(removeFileLatch.await(10, TimeUnit.SECONDS)).isTrue();

		File baz3 = File.createTempFile("baz3", ".txt", baz);

		n = 0;
		Message<File> fileMessage = null;
		while (n++ < 300 && (fileMessage = fileReadingMessageSource.receive()) == null) {
			Thread.sleep(100);
		}

		assertThat(fileMessage).isNotNull();
		assertThat(fileMessage.getPayload()).isEqualTo(baz3);
		assertThat(fileMessage.getHeaders().get(FileHeaders.RELATIVE_PATH, String.class))
				.startsWith(TestUtils.applySystemFileSeparator("foo/baz/"));

		fileReadingMessageSource.stop();
	}

}
