/*
 * Copyright 2014-present the original author or authors.
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

package org.springframework.integration.file.remote.synchronizer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.expression.EvaluationContext;
import org.springframework.integration.expression.SupplierExpression;
import org.springframework.integration.file.HeadDirectoryScanner;
import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
import org.springframework.integration.file.filters.ChainFileListFilter;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.test.support.TestApplicationContextAware;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessagingException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Venil Noronha
 *
 * @since 4.0.4
 *
 */
public class AbstractRemoteFileSynchronizerTests implements TestApplicationContextAware {

	@Test
	public void testRollback() throws Exception {
		final AtomicBoolean failWhenCopyingTestFile2 = new AtomicBoolean(true);
		final AtomicInteger count = new AtomicInteger();
		SessionFactory<String> sf = new StringSessionFactory();
		AbstractInboundFileSynchronizer<String> sync = new AbstractInboundFileSynchronizer<>(sf) {

			@Override
			protected boolean isFile(String file) {
				return true;
			}

			@Override
			protected String getFilename(String file) {
				return file;
			}

			@Override
			protected long getModified(String file) {
				return 0;
			}

			@Override
			protected String protocol() {
				return "file";
			}

			@Override
			protected boolean copyFileToLocalDirectory(String remoteDirectoryPath,
					EvaluationContext localFileEvaluationContext, String remoteFile,
					File localDirectory, Session<String> session) throws IOException {

				if ("testFile2".equals(remoteFile) && failWhenCopyingTestFile2.getAndSet(false)) {
					throw new IOException("fail");
				}
				count.incrementAndGet();
				return true;
			}

		};
		sync.setFilter(new AcceptOnceFileListFilter<>());
		sync.setRemoteDirectory("testRemoteDirectory");

		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> sync.synchronizeToLocalDirectory(mock(File.class)))
				.withRootCauseInstanceOf(IOException.class)
				.withStackTraceContaining("fail");

		sync.synchronizeToLocalDirectory(mock(File.class));
		assertThat(count.get()).isEqualTo(3);
		sync.close();
	}

	@Test
	public void testMaxFetchSizeSynchronizer() throws Exception {
		final AtomicInteger count = new AtomicInteger();
		AbstractInboundFileSynchronizer<String> sync = createLimitingSynchronizer(count);

		File localDirectory = FileUtils.getTempDirectory();
		sync.synchronizeToLocalDirectory(localDirectory, 1);
		assertThat(count.get()).isEqualTo(1);

		@SuppressWarnings("unchecked")
		Map<String, List<String>> fetchCache = TestUtils.getPropertyValue(sync, "fetchCache", Map.class);
		List<String> cachedFiles = fetchCache.get("testRemoteDirectory");
		assertThat(cachedFiles).containsExactly("testFile2", "testFile3");

		sync.synchronizeToLocalDirectory(localDirectory, 1);
		assertThat(count.get()).isEqualTo(2);

		cachedFiles = fetchCache.get("testRemoteDirectory");
		assertThat(cachedFiles).containsExactly("testFile3");

		sync.synchronizeToLocalDirectory(localDirectory, 1);
		assertThat(count.get()).isEqualTo(3);

		cachedFiles = fetchCache.get("testRemoteDirectory");
		assertThat(cachedFiles).isNull();

		StringSession stringSession =
				TestUtils.getPropertyValue(sync, "remoteFileTemplate.sessionFactory.session", StringSession.class);
		assertThat(stringSession.listCallCount).isEqualTo(1);

		sync.close();
	}

	@Test
	public void testMaxFetchSizeSource() {
		final AtomicInteger count = new AtomicInteger();
		AbstractInboundFileSynchronizer<String> sync = createLimitingSynchronizer(count);
		AbstractInboundFileSynchronizingMessageSource<String> source = createSource(sync);
		source.afterPropertiesSet();
		source.start();

		source.receive();
		assertThat(count.get()).isEqualTo(1);
		sync.synchronizeToLocalDirectory(mock(File.class), 1);
		source.receive();
		sync.synchronizeToLocalDirectory(mock(File.class), 1);
		source.receive();
		source.stop();
	}

	@Test
	public void testDefaultFilter() {
		final AtomicInteger count = new AtomicInteger();
		AbstractInboundFileSynchronizingMessageSource<String> source = createSource(count);
		source.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		source.afterPropertiesSet();
		source.start();
		source.receive();
		assertThat(count.get()).isEqualTo(1);
		source.receive();
		assertThat(count.get()).isEqualTo(2);
		source.receive();
		assertThat(count.get()).isEqualTo(3);
		source.receive();
		assertThat(count.get()).isEqualTo(3);
	}

	@Test
	public void testNoFilter() {
		final AtomicInteger count = new AtomicInteger();
		AbstractInboundFileSynchronizer<String> sync = createLimitingSynchronizer(count);
		sync.setFilter(null);
		AbstractInboundFileSynchronizingMessageSource<String> source = createSource(sync);
		source.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		source.afterPropertiesSet();
		source.start();
		source.receive();
		assertThat(count.get()).isEqualTo(1);
		source.receive();
		assertThat(count.get()).isEqualTo(2);
		source.receive();
		assertThat(count.get()).isEqualTo(3);
		source.receive();
		assertThat(count.get()).isEqualTo(4);
	}

	@Test
	public void testBulkOnlyFilter() {
		final AtomicInteger count = new AtomicInteger();
		AbstractInboundFileSynchronizer<String> sync = createLimitingSynchronizer(count);
		ChainFileListFilter<String> cflf = new ChainFileListFilter<>();
		cflf.addFilter(new AcceptOnceFileListFilter<>());
		cflf.addFilter(fs -> Stream.of(fs).collect(Collectors.toList()));
		sync.setFilter(cflf);
		AbstractInboundFileSynchronizingMessageSource<String> source = createSource(sync);
		source.afterPropertiesSet();
		source.start();
		source.receive();
		assertThat(count.get()).isEqualTo(1);
		source.receive();
		assertThat(count.get()).isEqualTo(2);
		source.receive();
		assertThat(count.get()).isEqualTo(3);
		source.receive();
		assertThat(count.get()).isEqualTo(3);
	}

	@Test
	public void testExclusiveScanner() {
		final AtomicInteger count = new AtomicInteger();
		AbstractInboundFileSynchronizingMessageSource<String> source = createSource(count);
		source.setScanner(new HeadDirectoryScanner(1));
		source.afterPropertiesSet();
		source.start();
		source.receive();
		assertThat(count.get()).isEqualTo(1);
	}

	@Test
	public void testExclusiveWatchService() {
		final AtomicInteger count = new AtomicInteger();
		AbstractInboundFileSynchronizingMessageSource<String> source = createSource(count);
		source.setUseWatchService(true);
		source.afterPropertiesSet();
		source.start();
		source.receive();
		assertThat(count.get()).isEqualTo(1);
	}

	@Test
	public void testScannerAndWatchServiceConflict() {
		final AtomicInteger count = new AtomicInteger();
		AbstractInboundFileSynchronizingMessageSource<String> source = createSource(count);
		source.setUseWatchService(true);
		source.setScanner(new HeadDirectoryScanner(1));
		assertThatIllegalStateException()
				.isThrownBy(source::afterPropertiesSet);
	}

	@Test
	public void testRemoteDirectoryRefreshedOnEachSynchronization(@TempDir File localDir) {
		AbstractInboundFileSynchronizer<String> sync =
				new AbstractInboundFileSynchronizer<>(new StringSessionFactory()) {

					@Override
					protected boolean isFile(String file) {
						return true;
					}

					@Override
					protected String getFilename(String file) {
						return file;
					}

					@Override
					protected long getModified(String file) {
						return 0;
					}

					@Override
					protected String protocol() {
						return "mock";
					}

				};

		Queue<String> remoteDirs = new LinkedList<>();
		remoteDirs.add("dir1");
		remoteDirs.add("dir2");
		sync.setRemoteDirectoryExpression(new SupplierExpression<>(remoteDirs::poll));
		sync.setLocalFilenameGeneratorExpressionString("#remoteDirectory+'/'+#root");
		sync.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		sync.afterPropertiesSet();

		sync.synchronizeToLocalDirectory(localDir);
		sync.synchronizeToLocalDirectory(localDir);

		/*Files.find(localDir.toPath(),
				Integer.MAX_VALUE,
				(filePath, fileAttr) -> fileAttr.isRegularFile())
				.forEach(System.out::println);*/

		assertThat(localDir.list()).contains("dir1", "dir2");
	}

	private AbstractInboundFileSynchronizingMessageSource<String> createSource(AtomicInteger count) {
		return createSource(createLimitingSynchronizer(count));
	}

	private AbstractInboundFileSynchronizingMessageSource<String> createSource(
			AbstractInboundFileSynchronizer<String> sync) {

		AbstractInboundFileSynchronizingMessageSource<String> source =
				new AbstractInboundFileSynchronizingMessageSource<>(sync) {

					@Override
					public String getComponentType() {
						return "MessageSource";
					}

				};
		source.setMaxFetchSize(1);
		source.setLocalDirectory(new File(FileUtils.getTempDirectoryPath() + File.separator + UUID.randomUUID()));
		source.setAutoCreateLocalDirectory(true);
		source.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		source.setBeanName("testSource");
		return source;
	}

	private AbstractInboundFileSynchronizer<String> createLimitingSynchronizer(final AtomicInteger count) {
		SessionFactory<String> sf = new StringSessionFactory();
		AbstractInboundFileSynchronizer<String> sync = new AbstractInboundFileSynchronizer<>(sf) {

			@Override
			protected boolean isFile(String file) {
				return true;
			}

			@Override
			protected String getFilename(String file) {
				return file;
			}

			@Override
			protected long getModified(String file) {
				return 0;
			}

			@Override
			protected String protocol() {
				return "file";
			}

			@Override
			protected boolean copyFileToLocalDirectory(String remoteDirectoryPath,
					EvaluationContext localFileEvaluationContext, String remoteFile,
					File localDirectory, Session<String> session) {

				count.incrementAndGet();
				return true;
			}

		};
		sync.setFilter(new AcceptOnceFileListFilter<>());
		sync.setRemoteDirectory("testRemoteDirectory");
		sync.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		return sync;
	}

	private static class StringSessionFactory implements SessionFactory<String> {

		private final Session<String> session = new StringSession();

		@Override
		public Session<String> getSession() {
			return this.session;
		}

	}

	private static class StringSession implements Session<String> {

		int listCallCount = 0;

		StringSession() {
		}

		@Override
		public boolean remove(String path) {
			return true;
		}

		@Override
		public String[] list(String path) {
			this.listCallCount++;
			return new String[] {"testFile1", "testFile2", "testFile3"};
		}

		@Override
		public void read(String source, OutputStream outputStream) {
		}

		@Override
		public void write(InputStream inputStream, String destination) {
		}

		@Override
		public void append(InputStream inputStream, String destination) {
		}

		@Override
		public boolean mkdir(String directory) {
			return true;
		}

		@Override
		public boolean rmdir(String directory) {
			return true;
		}

		@Override
		public void rename(String pathFrom, String pathTo) {
		}

		@Override
		public void close() {
		}

		@Override
		public boolean isOpen() {
			return true;
		}

		@Override
		public boolean exists(String path) {
			return true;
		}

		@Override
		public String[] listNames(String path) {
			return new String[0];
		}

		@Override
		public InputStream readRaw(String source) {
			return null;
		}

		@Override
		public boolean finalizeRaw() {
			return true;
		}

		@Override
		public Object getClientInstance() {
			return null;
		}

		@Override
		public String getHostPort() {
			return "mock:6666";
		}

	}

}
