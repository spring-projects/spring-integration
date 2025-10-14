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
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import org.springframework.context.Lifecycle;
import org.springframework.expression.Expression;
import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.file.DefaultDirectoryScanner;
import org.springframework.integration.file.DirectoryScanner;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.FileLocker;
import org.springframework.integration.file.HeadDirectoryScanner;
import org.springframework.integration.file.filters.DiscardAwareFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.filters.ResettableFileListFilter;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.support.management.ManageableLifecycle;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.integration.core.MessageSource} that creates messages
 * from a file system directory.
 * To prevent messages for certain files, you may supply a {@link FileListFilter}.
 * By default, when configuring with XML or the DSL,
 * an {@link org.springframework.integration.file.filters.AcceptOnceFileListFilter} is used.
 * It ensures files are picked up only once from the directory.
 * <p>
 * A common problem with reading files is that a file may be detected before it
 * is ready. The default
 * {@link org.springframework.integration.file.filters.AcceptOnceFileListFilter}
 * does not prevent this. In most cases, this can be prevented if the
 * file-writing process renames each file as soon as it is ready for reading. A
 * pattern-matching filter that accepts only files that are ready (e.g. based on
 * a known suffix), composed with the default
 * {@link org.springframework.integration.file.filters.AcceptOnceFileListFilter}
 * would allow for this.
 * <p>
 * If an external {@link DirectoryScanner} is used, then the {@link FileLocker}
 * and {@link FileListFilter} objects should be set on the external
 * {@link DirectoryScanner}, not the instance of FileReadingMessageSource. An
 * {@link IllegalStateException} will result otherwise.
 * <p>
 * A {@link Comparator} can be used to ensure internal ordering of the Files in
 * a {@link PriorityBlockingQueue}. This does not provide the same guarantees as
 * a {@link org.springframework.integration.aggregator.ResequencingMessageGroupProcessor},
 * but in cases where writing files
 * and failure downstream are rare it might be sufficient.
 * <p>
 * FileReadingMessageSource is fully thread-safe under concurrent
 * <code>receive()</code> invocations and message delivery callbacks.
 *
 * @author Iwein Fuld
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Steven Pearce
 * @author Patryk Ziobron
 *
 * @since 7.0
 */
public class FileReadingMessageSource extends AbstractMessageSource<File> implements ManageableLifecycle {

	private static final int DEFAULT_INTERNAL_QUEUE_CAPACITY = 5;

	private final AtomicBoolean running = new AtomicBoolean();

	private final Queue<DirFile> toBeReceived;

	@SuppressWarnings("NullAway.Init")
	private Expression directoryExpression;

	private DirectoryScanner scanner = new DefaultDirectoryScanner();

	private boolean scannerExplicitlySet;

	private boolean autoCreateDirectory = true;

	private boolean scanEachPoll = false;

	private @Nullable FileListFilter<File> filter;

	private @Nullable FileLocker locker;

	private boolean useWatchService;

	private WatchEventType[] watchEvents = {WatchEventType.CREATE};

	private int watchMaxDepth = Integer.MAX_VALUE;

	private Predicate<Path> watchDirPredicate = path -> true;

	/**
	 * Create a FileReadingMessageSource with a naturally ordered queue of unbounded capacity.
	 */
	public FileReadingMessageSource() {
		this(null);
	}

	/**
	 * Create a FileReadingMessageSource with a bounded queue of the given
	 * capacity. This can be used to reduce the memory footprint of this
	 * component when reading from a large directory.
	 * @param internalQueueCapacity
	 *            the size of the queue used to cache files to be received
	 *            internally. This queue can be made larger to optimize the
	 *            directory scanning. With scanEachPoll set to false and the
	 *            queue to a large size, it will be filled once and then
	 *            completely emptied before a new directory listing is done.
	 *            This is particularly useful to reduce scans of large numbers
	 *            of files in a directory.
	 */
	public FileReadingMessageSource(int internalQueueCapacity) {
		this(null);
		Assert.isTrue(internalQueueCapacity > 0, "Cannot create a queue with non positive capacity");
		this.scanner = new HeadDirectoryScanner(internalQueueCapacity);
	}

	/**
	 * Create a FileReadingMessageSource with a {@link PriorityBlockingQueue}
	 * ordered with the passed in {@link Comparator}.
	 * <p> The size of the queue used should be large enough to hold all the files
	 * in the input directory in order to sort all of them, so restricting the
	 * size of the queue is mutually exclusive with ordering. No guarantees
	 * about file delivery order can be made under concurrent access.
	 * @param receptionOrderComparator the comparator to be used to order the files in the internal queue
	 */
	public FileReadingMessageSource(@Nullable Comparator<File> receptionOrderComparator) {
		Comparator<DirFile> comparatorToUse = null;
		if (receptionOrderComparator != null) {
			comparatorToUse = (dirFile1, dirFile2) -> receptionOrderComparator.compare(dirFile1.file, dirFile2.file);
		}
		this.toBeReceived = new PriorityBlockingQueue<>(DEFAULT_INTERNAL_QUEUE_CAPACITY, comparatorToUse);
	}

	/**
	 * Specify the input directory.
	 * @param directory to monitor
	 */
	public void setDirectory(File directory) {
		Assert.notNull(directory, "directory must not be null");
		setDirectoryExpression(new ValueExpression<>(directory));
	}

	/**
	 * Specify a SpEL expression for an input directory.
	 * This expression is evaluated on each scan, but not each poll.
	 * @param directoryExpression the SpEL expression to resolve a directory to monitor on each scan.
	 * @since 7.0
	 */
	public void setDirectoryExpression(Expression directoryExpression) {
		Assert.notNull(directoryExpression, "'directoryExpression' must not be null");
		this.directoryExpression = directoryExpression;
	}

	/**
	 * Optionally specify a custom scanner, for example the
	 * {@link WatchServiceDirectoryScanner}.
	 * @param scanner scanner implementation
	 */
	public void setScanner(DirectoryScanner scanner) {
		Assert.notNull(scanner, "'scanner' must not be null.");
		this.scanner = scanner;
		this.scannerExplicitlySet = true;
	}

	/**
	 * The {@link #scanner} property accessor to allow to modify its options
	 * ({@code filter}, {@code locker} etc.) at runtime using the
	 * {@link FileReadingMessageSource} bean.
	 * @return the {@link DirectoryScanner} of this {@link FileReadingMessageSource}.
	 */
	public DirectoryScanner getScanner() {
		return this.scanner;
	}

	/**
	 * Specify whether to create the source directory automatically if it does
	 * not yet exist upon initialization. By default, this value is
	 * <em>true</em>. If set to <em>false</em> and the
	 * source directory does not exist, an Exception will be thrown upon
	 * initialization.
	 * @param autoCreateDirectory
	 *            should the directory to be monitored be created when this
	 *            component starts up?
	 */
	public void setAutoCreateDirectory(boolean autoCreateDirectory) {
		this.autoCreateDirectory = autoCreateDirectory;
	}

	/**
	 * Set a {@link FileListFilter}.
	 * By default a {@link org.springframework.integration.file.filters.AcceptOnceFileListFilter}
	 * with no bounds is used. In most cases a customized {@link FileListFilter} will
	 * be needed to deal with modification and duplication concerns.
	 * If multiple filters are required a
	 * {@link org.springframework.integration.file.filters.CompositeFileListFilter}
	 * can be used to group them together.
	 * <p> <b>The supplied filter must be thread safe.</b>.
	 * @param filter a filter
	 */
	public void setFilter(FileListFilter<File> filter) {
		Assert.notNull(filter, "'filter' must not be null");
		this.filter = filter;
	}

	/**
	 * Set a {@link FileLocker} to be used to guard files against duplicate processing.
	 * <p> <b>The supplied FileLocker must be thread safe</b>
	 * @param locker a locker
	 */
	public void setLocker(FileLocker locker) {
		Assert.notNull(locker, "'fileLocker' must not be null.");
		this.locker = locker;
	}

	/**
	 * Set this flag if you want to make sure the internal queue is
	 * refreshed with the latest content of the input directory on each poll.
	 * <p>
	 * By default, this implementation will empty its queue before looking at the
	 * directory again. In cases where order is relevant it is important to
	 * consider the effects of setting this flag. The internal
	 * {@link java.util.concurrent.BlockingQueue} that this class is keeping
	 * will more likely be out of sync with the file system if this flag is set
	 * to false, but it will change more often (causing expensive reordering) if it is set to true.
	 * @param scanEachPoll whether the component should re-scan (as opposed to not
	 * rescanning until the entire backlog has been delivered)
	 */
	public void setScanEachPoll(boolean scanEachPoll) {
		this.scanEachPoll = scanEachPoll;
	}

	/**
	 * Switch this {@link FileReadingMessageSource} to use its internal
	 * {@link FileReadingMessageSource.WatchServiceDirectoryScanner}.
	 * @param useWatchService the {@code boolean} flag to switch to
	 * {@link FileReadingMessageSource.WatchServiceDirectoryScanner} on {@code true}.
	 * @see #setWatchEvents
	 */
	public void setUseWatchService(boolean useWatchService) {
		this.useWatchService = useWatchService;
	}

	public boolean isUseWatchService() {
		return this.useWatchService;
	}

	/**
	 * The {@link WatchService} event types.
	 * If {@link #setUseWatchService} isn't {@code true}, this option is ignored.
	 * @param watchEvents the set of {@link WatchEventType}.
	 * @see #setUseWatchService
	 */
	public void setWatchEvents(WatchEventType... watchEvents) {
		Assert.notEmpty(watchEvents, "'watchEvents' must not be empty.");
		Assert.noNullElements(watchEvents, "'watchEvents' must not contain null elements.");
		Assert.state(!this.running.get(), "Cannot change watch events while running.");

		this.watchEvents = Arrays.copyOf(watchEvents, watchEvents.length);
	}

	/**
	 * Set a max depth for the {@link Files#walkFileTree(Path, Set, int, FileVisitor)} API when
	 * {@link #useWatchService} is enabled.
	 * Defaults to {@link Integer#MAX_VALUE} - walk the whole tree.
	 * @param watchMaxDepth the depth for {@link Files#walkFileTree(Path, Set, int, FileVisitor)}.
	 */
	public void setWatchMaxDepth(int watchMaxDepth) {
		this.watchMaxDepth = watchMaxDepth;
	}

	/**
	 * Set a {@link Predicate} to check a directory in the {@link Files#walkFileTree(Path, Set, int, FileVisitor)} call
	 * if it is eligible for {@link WatchService}.
	 * @param watchDirPredicate the {@link Predicate} to check dirs for walking.
	 */
	public void setWatchDirPredicate(Predicate<Path> watchDirPredicate) {
		Assert.notNull(watchDirPredicate, "'watchDirPredicate' must not be null.");
		this.watchDirPredicate = watchDirPredicate;
	}

	@Override
	public String getComponentType() {
		return "file:inbound-channel-adapter";
	}

	@Override
	public void start() {
		if (!this.running.getAndSet(true)) {
			if (this.directoryExpression instanceof ValueExpression) {
				File directoryToCreate = this.directoryExpression.getValue(File.class);
				if (directoryToCreate == null ||
						(!directoryToCreate.exists() && this.autoCreateDirectory && !directoryToCreate.mkdirs())) {

					throw new IllegalStateException("Cannot create directory or its parents: " + directoryToCreate);
				}
				Assert.isTrue(directoryToCreate.exists(),
						() -> "Source directory [" + directoryToCreate + "] does not exist.");
				Assert.isTrue(directoryToCreate.isDirectory(),
						() -> "Source path [" + directoryToCreate + "] does not point to a directory.");
				Assert.isTrue(directoryToCreate.canRead(),
						() -> "Source directory [" + directoryToCreate + "] is not readable.");
				if (this.scanner instanceof WatchServiceDirectoryScanner watchServiceDirectoryScanner) {
					watchServiceDirectoryScanner.directory = directoryToCreate;
				}
			}
			if (this.scanner instanceof Lifecycle lifecycle) {
				lifecycle.start();
			}
		}
	}

	@Override
	public void stop() {
		if (this.running.getAndSet(false) && this.scanner instanceof Lifecycle lifecycle) {
			lifecycle.stop();
		}
	}

	@Override
	public boolean isRunning() {
		return this.running.get();
	}

	@Override
	protected void onInit() {
		Assert.notNull(this.directoryExpression, "'directoryExpression' must not be null");

		Assert.state(!(this.scannerExplicitlySet && this.useWatchService),
				() -> "The 'scanner' and 'useWatchService' options are mutually exclusive: " + this.scanner);

		if (this.useWatchService) {
			this.scanner = new WatchServiceDirectoryScanner();
		}

		// Check that the filter and locker options are _NOT_ set if an external scanner has been set.
		// The external scanner is responsible for the filter and locker options in that case.
		Assert.state(!(this.scannerExplicitlySet && (this.filter != null || this.locker != null)),
				() -> "When using an external scanner the 'filter' and 'locker' options should not be used. " +
						"Instead, set these options on the external DirectoryScanner: " + this.scanner);
		if (this.filter != null) {
			this.scanner.setFilter(this.filter);
		}
		if (this.locker != null) {
			this.scanner.setLocker(this.locker);
		}

	}

	@Override
	protected @Nullable AbstractIntegrationMessageBuilder<File> doReceive() {
		// rescan only if needed or explicitly configured
		if (this.scanEachPoll || this.toBeReceived.isEmpty()) {
			scanInputDirectory();
		}

		DirFile dirFile = this.toBeReceived.poll();

		while ((dirFile != null) && !this.scanner.tryClaim(dirFile.file)) {
			dirFile = this.toBeReceived.poll();
		}

		if (dirFile != null) {
			return getMessageBuilderFactory()
					.withPayload(dirFile.file)
					.setHeader(FileHeaders.RELATIVE_PATH,
							dirFile.root.toPath().relativize(dirFile.file.toPath()).toString())
					.setHeader(FileHeaders.FILENAME, dirFile.file.getName())
					.setHeader(FileHeaders.ORIGINAL_FILE, dirFile.file);
		}

		return null;
	}

	private void scanInputDirectory() {
		File directory = this.directoryExpression.getValue(getEvaluationContext(), File.class);
		Assert.notNull(directory, "'directoryExpression' must not evaluate to null");
		if (this.scanner instanceof WatchServiceDirectoryScanner watchServiceDirectoryScanner) {
			if (!watchServiceDirectoryScanner.directory.equals(directory)) {
				watchServiceDirectoryScanner.stop();
				watchServiceDirectoryScanner.directory = directory;
				watchServiceDirectoryScanner.start();
			}
		}
		List<File> filteredFiles = this.scanner.listFiles(directory);

		for (File file : filteredFiles) {
			this.toBeReceived.add(new DirFile(file, directory));
		}

		if (!filteredFiles.isEmpty()) {
			logger.debug(() -> "Added to queue: " + filteredFiles);
		}
	}

	/**
	 * Adds the failed message back to the 'toBeReceived' queue if there is room.
	 * @param failedMessage the {@link Message} that failed
	 */
	public void onFailure(Message<File> failedMessage) {
		logger.warn(() -> "Failed to send: " + failedMessage);
		String relativePath = failedMessage.getHeaders().get(FileHeaders.RELATIVE_PATH, String.class);
		File file = failedMessage.getPayload();
		File root;
		if (relativePath != null) {
			String absolutePath = file.getAbsolutePath();
			String rootPath = absolutePath.substring(0, absolutePath.length() - relativePath.length());
			root = new File(rootPath);
		}
		else {
			root = file.getParentFile();
		}
		this.toBeReceived.offer(new DirFile(file, root));
	}

	public enum WatchEventType {

		CREATE(StandardWatchEventKinds.ENTRY_CREATE),

		MODIFY(StandardWatchEventKinds.ENTRY_MODIFY),

		DELETE(StandardWatchEventKinds.ENTRY_DELETE);

		private final WatchEvent.Kind<Path> kind;

		WatchEventType(WatchEvent.Kind<Path> kind) {
			this.kind = kind;
		}

	}

	private final class WatchServiceDirectoryScanner extends DefaultDirectoryScanner implements ManageableLifecycle {

		private final ConcurrentMap<Path, WatchKey> pathKeys = new ConcurrentHashMap<>();

		private final WatchEvent.Kind<?>[] kinds;

		private final Set<File> filesToPoll = ConcurrentHashMap.newKeySet();

		@SuppressWarnings("NullAway.Init")
		private WatchService watcher;

		@SuppressWarnings("NullAway.Init")
		private volatile File directory;

		WatchServiceDirectoryScanner() {
			this.kinds =
					Arrays.stream(FileReadingMessageSource.this.watchEvents)
							.map(watchEventType -> watchEventType.kind)
							.toArray(WatchEvent.Kind<?>[]::new);
		}

		@Override
		public void setFilter(FileListFilter<File> filter) {
			if (filter instanceof DiscardAwareFileListFilter<File> discardAwareFileListFilter) {
				discardAwareFileListFilter.addDiscardCallback(this.filesToPoll::add);
			}
			super.setFilter(filter);
		}

		@Override
		public void start() {
			if (this.directory == null) {
				File directoryToSet =
						FileReadingMessageSource.this.directoryExpression.getValue(getEvaluationContext(), File.class);
				Assert.notNull(directoryToSet, "'directoryExpression' must not evaluate to null");
				this.directory = directoryToSet;
			}
			try {
				this.watcher = FileSystems.getDefault().newWatchService();
				Set<File> initialFiles = walkDirectory(this.directory.toPath(), null);
				initialFiles.addAll(filesFromEvents());
				this.filesToPoll.addAll(initialFiles);
			}
			catch (IOException ex) {
				logger.error(ex, () -> "Failed to create watcher for " + this.directory);
			}
		}

		@Override
		public void stop() {
			try {
				this.pathKeys.forEach((path, watchKey) -> watchKey.cancel());
				this.watcher.close();
				this.pathKeys.clear();
				this.filesToPoll.clear();
			}
			catch (IOException ex) {
				logger.error(ex, () -> "Failed to close watcher for " + this.directory);
			}
		}

		@Override
		public boolean isRunning() {
			return true;
		}

		@Override
		protected File[] listEligibleFiles(File directory) {
			Set<File> files = new LinkedHashSet<>();

			for (Iterator<File> iterator = this.filesToPoll.iterator(); iterator.hasNext(); ) {
				files.add(iterator.next());
				iterator.remove();
			}

			files.addAll(filesFromEvents());

			return files.toArray(new File[0]);
		}

		private Set<File> filesFromEvents() {
			WatchKey key = this.watcher.poll();
			Set<File> files = new LinkedHashSet<>();
			while (key != null) {
				File parentDir = ((Path) key.watchable()).toAbsolutePath().toFile();
				for (WatchEvent<?> event : key.pollEvents()) {
					WatchEvent.Kind<?> watchEventKind = event.kind();
					if (StandardWatchEventKinds.ENTRY_CREATE.equals(watchEventKind) ||
							StandardWatchEventKinds.ENTRY_MODIFY.equals(watchEventKind) ||
							StandardWatchEventKinds.ENTRY_DELETE.equals(watchEventKind)) {

						processFilesFromNormalEvent(files, parentDir, event);
					}
					else if (StandardWatchEventKinds.OVERFLOW.equals(watchEventKind)) {
						processFilesFromOverflowEvent(files, event);
					}
				}
				key.reset();
				key = this.watcher.poll();
			}
			return files;
		}

		private void processFilesFromNormalEvent(Set<File> files, File parentDir, WatchEvent<?> event) {
			Path item = (Path) event.context();
			File file = new File(parentDir, item.toFile().getName());
			logger.debug(() -> "Watch event [" + event.kind() + "] for file [" + file + "]");

			if (StandardWatchEventKinds.ENTRY_DELETE.equals(event.kind())) {
				Path filePath = file.toPath();
				if (this.pathKeys.containsKey(filePath)) {
					WatchKey watchKey = this.pathKeys.remove(filePath);
					watchKey.cancel();
				}
				if (getFilter() instanceof ResettableFileListFilter<File> resettableFileListFilter) {
					resettableFileListFilter.remove(file);
				}
				boolean fileRemoved = files.remove(file);
				if (fileRemoved) {
					logger.debug(() -> "The file [" + file +
							"] has been removed from the queue because of DELETE event.");
				}
			}
			else {
				if (file.exists()) {
					if (file.isDirectory()) {
						files.addAll(walkDirectory(file.toPath(), event.kind()));
					}
					else {
						files.remove(file);
						files.add(file);
					}
				}
				else {
					logger.debug(() -> "A file [" + file + "] for the event [" + event.kind() +
							"] doesn't exist. Ignored. Maybe DELETE event is not watched ?");
				}
			}
		}

		private void processFilesFromOverflowEvent(Set<File> files, WatchEvent<?> event) {
			logger.debug(() -> "Watch event [" + StandardWatchEventKinds.OVERFLOW +
					"] with context [" + event.context() + "]");

			for (WatchKey watchKey : this.pathKeys.values()) {
				watchKey.cancel();
			}
			this.pathKeys.clear();

			if (event.context() != null && event.context() instanceof Path path) {
				files.addAll(walkDirectory(path, event.kind()));
			}
			else {
				files.addAll(walkDirectory(this.directory.toPath(), event.kind()));
			}
		}

		private Set<File> walkDirectory(Path directoryToWalk, WatchEvent.@Nullable Kind<?> kind) {
			final Set<File> walkedFiles = new LinkedHashSet<>();
			try {
				registerWatch(directoryToWalk);
				Files.walkFileTree(directoryToWalk, Collections.emptySet(), FileReadingMessageSource.this.watchMaxDepth,
						new SimpleFileVisitor<>() {

							@Override
							public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
									throws IOException {

								if (FileReadingMessageSource.this.watchDirPredicate.test(dir)) {
									registerWatch(dir);
									return FileVisitResult.CONTINUE;
								}
								else {
									return FileVisitResult.SKIP_SUBTREE;
								}
							}

							@Override
							public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
								FileVisitResult fileVisitResult = super.visitFile(file, attrs);
								if (!StandardWatchEventKinds.ENTRY_MODIFY.equals(kind)) {
									walkedFiles.add(file.toFile());
								}
								return fileVisitResult;
							}

						});
			}
			catch (IOException ex) {
				logger.error(ex, () -> "Failed to walk directory: " + directoryToWalk);
			}
			return walkedFiles;
		}

		private void registerWatch(Path dir) throws IOException {
			if (!this.pathKeys.containsKey(dir)) {
				logger.debug(() -> "registering: " + dir + " for file events");
				WatchKey watchKey = dir.register(this.watcher, this.kinds);
				this.pathKeys.putIfAbsent(dir, watchKey);
			}
		}

	}

	private record DirFile(File file, File root) implements Comparable<DirFile> {

		@Override
		public int compareTo(DirFile other) {
			return this.file.compareTo(other.file);
		}

	}

}
