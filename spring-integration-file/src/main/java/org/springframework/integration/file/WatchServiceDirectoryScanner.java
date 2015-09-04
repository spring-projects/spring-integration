/*
 * Copyright 2015 the original author or authors.
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

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.SmartLifecycle;
import org.springframework.lang.UsesJava7;
import org.springframework.util.Assert;

/**
 * Directory scanner that uses Java 7 {@link WatchService}.
 *
 * The initial state of the directory is collected during {@link #start()}. Subsequent
 * polls return new files as reported by {@code ENTRY_CREATE} events.
 * <p>
 * While initially walking the directory, any subdirectories encountered are registered
 * to watch for creation events.
 * <p>
 * If subdirectories are subsequently added, they are walked and registered for
 * new creation events, too.
 * <p>
 * When a {@link StandardWatchEventKinds#OVERFLOW} {@link WatchKey} event is occurred,
 * the {@link #directory} is rescanned to avoid the loss for any new entries according
 * to the "missed events" logic around {@link StandardWatchEventKinds#OVERFLOW}.
 *
 * @author Hezi Schrager
 * @author Gary Russell
 * @author Artem Bilan
 * @since 4.2
 *
 */
@UsesJava7
public class WatchServiceDirectoryScanner extends DefaultDirectoryScanner implements SmartLifecycle {

	private final static Log logger = LogFactory.getLog(WatchServiceDirectoryScanner.class);

	private final Path directory;

	private volatile WatchService watcher;

	private volatile int phase;

	private volatile boolean running;

	private volatile boolean autoStartup;

	private volatile Collection<File> initialFiles;

	/**
	 * Construct an instance for the given directory.
	 * @param directory the directory.
	 */
	public WatchServiceDirectoryScanner(String directory) {
		this.directory = Paths.get(directory);
	}

	@Override
	public int getPhase() {
		return this.phase;
	}

	/**
	 * see {@link #getPhase()}
	 * @param phase the phase.
	 */
	public void setPhase(int phase) {
		this.phase = phase;
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	/**
	 * @see #isRunning()
	 * @param running true if running.
	 */
	public void setRunning(boolean running) {
		this.running = running;
	}

	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	/**
	 * @see #isAutoStartup()
	 * @param autoStartup true to auto start.
	 */
	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	@Override
	public synchronized void start() {
		if (!this.running) {
			try {
				this.watcher = FileSystems.getDefault().newWatchService();
			}
			catch (IOException e) {
				logger.error("Failed to create watcher for " + this.directory.toString(), e);
			}
			final Set<File> initialFiles = walkDirectory(this.directory);
			initialFiles.addAll(filesFromEvents());
			this.initialFiles = initialFiles;
			this.running = true;
		}
	}

	@Override
	public synchronized void stop() {
		if (this.running) {
			try {
				this.watcher.close();
			}
			catch (IOException e) {
				logger.error("Failed to close watcher for " + this.directory.toString(), e);
			}
			this.running = false;
		}
	}

	@Override
	public void stop(Runnable callback) {
		stop();
		callback.run();
	}

	@Override
	protected File[] listEligibleFiles(File directory) {
		Assert.state(this.watcher != null, "Scanner needs to be started");
		if (this.initialFiles != null) {
			File[] initial = this.initialFiles.toArray(new File[this.initialFiles.size()]);
			this.initialFiles = null;
			return initial;
		}
		Collection<File> files = filesFromEvents();
		return files.toArray(new File[files.size()]);
	}

	private Set<File> filesFromEvents() {
		WatchKey key = watcher.poll();
		Set<File> files = new LinkedHashSet<File>();
		while (key != null) {
			for (WatchEvent<?> event : key.pollEvents()) {
				if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
					Path item = (Path) event.context();
					File file = new File(((Path) key.watchable()).toAbsolutePath() + File.separator + item.getFileName());
					if (logger.isDebugEnabled()) {
						logger.debug("Watch Event: " + event.kind() + ": " + file);
					}
					if (file.isDirectory()) {
						files.addAll(walkDirectory(file.toPath()));
					}
					else {
						files.add(file);
					}
				}
				else if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
					if (logger.isDebugEnabled()) {
						logger.debug("Watch Event: " + event.kind() + ": context: " + event.context());
					}
					if (event.context() != null && event.context() instanceof Path) {
						files.addAll(walkDirectory((Path) event.context()));
					}
					else {
						files.addAll(walkDirectory(this.directory));
					}
				}
				else {
					if (logger.isDebugEnabled()) {
						logger.debug("Watch Event: " + event.kind() + ": context: " + event.context());
					}
				}
			}
			key.reset();
			key = watcher.poll();
		}
		return files;
	}

	private Set<File> walkDirectory(Path directory) {
		final Set<File> walkedFiles = new LinkedHashSet<File>();
		try {
			registerWatch(directory);
			Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					FileVisitResult fileVisitResult = super.preVisitDirectory(dir, attrs);
					registerWatch(dir);
					return fileVisitResult;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					FileVisitResult fileVisitResult = super.visitFile(file, attrs);
					walkedFiles.add(file.toFile());
					return fileVisitResult;
				}

			});
		}
		catch (IOException e) {
			logger.error("Failed to walk directory: " + directory.toString(), e);
		}
		return walkedFiles;
	}

	private void registerWatch(Path dir) throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("registering: " + dir + " for file creation events");
		}
		dir.register(this.watcher, StandardWatchEventKinds.ENTRY_CREATE);
	}

}
