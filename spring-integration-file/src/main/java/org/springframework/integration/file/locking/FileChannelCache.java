/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.file.locking;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.lang.Nullable;

/**
 * Static cache of FileLocks that can be used to ensure that only a single lock is used inside this ClassLoader.
 *
 * @author Iwein Fuld
 * @author Gary Russell
 * @author Emmanuel Roux
 * @author Artem Bilan
 *
 * @since 2.0
 */
final class FileChannelCache {

	private static ConcurrentMap<File, FileChannel> channelCache = new ConcurrentHashMap<>();

	private FileChannelCache() {
	}

	/**
	 * Try to get a lock for this file while guaranteeing that the same channel will be used for all file locks in this
	 * VM. If the lock could not be acquired this method will return {@code null}.
	 * <p>
	 * Locks acquired through this method should be passed back to #closeChannelFor to prevent memory leaks.
	 * <p>
	 * Thread safe.
	 */
	@Nullable
	public static FileLock tryLockFor(File fileToLock) throws IOException {
		FileChannel channel = channelCache.get(fileToLock);
		if (channel == null && fileToLock.exists()) {
			@SuppressWarnings("resource")
			FileChannel newChannel = new RandomAccessFile(fileToLock, "rw").getChannel();
			FileChannel original = channelCache.putIfAbsent(fileToLock, newChannel);
			if (original != null) {
				channel = original;
				try {
					newChannel.close();
				}
				catch (IOException e) {
					// ignore
				}
			}
			else {
				channel = newChannel;
			}
		}
		FileLock lock = null;
		if (channel != null) {
			try {
				lock = channel.tryLock();
			}
			catch (OverlappingFileLockException e) {
				// File is already locked in this thread or virtual machine
			}
		}
		return lock;
	}

	/**
	 * Close the channel for the file passed in.
	 * <p>
	 * Thread safe.
	 */
	public static void closeChannelFor(File fileToUnlock) {
		FileChannel fileChannel = channelCache.remove(fileToUnlock);
		if (fileChannel != null) {
			try {
				fileChannel.close();
			}
			catch (IOException e) {
				// ignore
			}
		}
	}

	public static boolean isLocked(File file) {
		return channelCache.containsKey(file);
	}

}
