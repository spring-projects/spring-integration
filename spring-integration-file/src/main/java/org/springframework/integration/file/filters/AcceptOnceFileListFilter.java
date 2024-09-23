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

package org.springframework.integration.file.filters;

import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.lang.Nullable;

/**
 * {@link FileListFilter} that passes files only one time. This can
 * conveniently be used to prevent duplication of files, as is done in
 * {@link org.springframework.integration.file.FileReadingMessageSource}.
 * <p>
 * This implementation is thread safe.
 *
 * @param <F> the file type.
 *
 * @author Iwein Fuld
 * @author Josh Long
 * @author Gary Russell
 * @author Artem Bilan
 * @author Christian Tzolov
 * @author Ngoc Nhan
 */
public class AcceptOnceFileListFilter<F> extends AbstractFileListFilter<F> implements ReversibleFileListFilter<F>,
		ResettableFileListFilter<F> {

	@Nullable
	private final Queue<F> seen;

	private final Set<F> seenSet = new HashSet<>();

	private final Lock monitor = new ReentrantLock();

	/**
	 * Creates an AcceptOnceFileListFilter that is based on a bounded queue. If the queue overflows,
	 * files that fall out will be passed through this filter again if passed to the
	 * {@link #filterFiles(Object[])}
	 * @param maxCapacity the maximum number of Files to maintain in the 'seen' queue.
	 */
	public AcceptOnceFileListFilter(int maxCapacity) {
		this.seen = new LinkedBlockingQueue<>(maxCapacity);
	}

	/**
	 * Creates an AcceptOnceFileListFilter based on an unbounded queue.
	 */
	public AcceptOnceFileListFilter() {
		this.seen = null;
	}

	@Override
	public boolean accept(F file) {
		this.monitor.lock();
		try {
			if (this.seenSet.contains(file)) {
				return false;
			}
			if (this.seen != null && !this.seen.offer(file)) {
				F removed = this.seen.poll();
				this.seenSet.remove(removed);
				this.seen.add(file);
			}
			this.seenSet.add(file);
			return true;
		}
		finally {
			this.monitor.unlock();
		}
	}

	@Override
	public void rollback(F file, List<F> files) {
		this.monitor.lock();
		try {
			boolean rollingBack = false;
			for (F fileToRollback : files) {
				if (fileToRollback.equals(file)) {
					rollingBack = true;
				}
				if (rollingBack) {
					remove(fileToRollback);
				}
			}
		}
		finally {
			this.monitor.unlock();
		}
	}

	@Override
	public boolean remove(F fileToRemove) {
		boolean removed = this.seenSet.remove(fileToRemove);
		if (this.seen != null) {
			this.seen.remove(fileToRemove);
		}
		return removed;
	}

}
