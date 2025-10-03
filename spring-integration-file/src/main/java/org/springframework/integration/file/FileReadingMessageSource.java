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

package org.springframework.integration.file;

import java.io.File;
import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;

import org.jspecify.annotations.Nullable;

import org.springframework.integration.file.filters.FileListFilter;

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
 * If a external {@link DirectoryScanner} is used, then the {@link FileLocker}
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
 * @deprecated since 7.0 in favor {@link org.springframework.integration.file.inbound.FileReadingMessageSource}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class FileReadingMessageSource extends org.springframework.integration.file.inbound.FileReadingMessageSource {

	/**
	 * Create a FileReadingMessageSource with a naturally ordered queue of unbounded capacity.
	 */
	public FileReadingMessageSource() {
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
		super(internalQueueCapacity);
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
		super(receptionOrderComparator);
	}

}
