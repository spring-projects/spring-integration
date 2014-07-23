/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.file.filters;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * {@link FileListFilter} that passes files only one time. This can
 * conveniently be used to prevent duplication of files, as is done in
 * {@link org.springframework.integration.file.FileReadingMessageSource}.
 * <p>
 * This implementation is thread safe.
 *
 * @author Iwein Fuld
 * @author Josh Long
 * @author Gary Russell
 * @since 1.0.0
 */
public class AcceptOnceFileListFilter<F> extends AbstractFileListFilter<F> implements ReversibleFileListFilter<F> {

	private final Queue<F> seen;

	private final Object monitor = new Object();


	/**
	 * Creates an AcceptOnceFileListFilter that is based on a bounded queue. If the queue overflows,
	 * files that fall out will be passed through this filter again if passed to the
	 * {@link #filterFiles(Object[])}
	 *
	 * @param maxCapacity the maximum number of Files to maintain in the 'seen' queue.
	 */
	public AcceptOnceFileListFilter(int maxCapacity) {
		this.seen = new LinkedBlockingQueue<F>(maxCapacity);
	}

	/**
	 * Creates an AcceptOnceFileListFilter based on an unbounded queue.
	 */
	public AcceptOnceFileListFilter() {
		this.seen = new LinkedBlockingQueue<F>();
	}


	@Override
	public boolean accept(F file) {
		synchronized (this.monitor) {
			if (this.seen.contains(file)) {
				return false;
			}
			if (!this.seen.offer(file)) {
				this.seen.poll();
				this.seen.add(file);
			}
			return true;
		}
	}

	/**
	 * {@inheritDoc}
	 * @since 4.0.4
	 */
	@Override
	public void rollback(F file, List<F> files) {
		boolean rollingBack = false;
		for (F fileToRollback : files) {
			if (fileToRollback.equals(file)) {
				rollingBack = true;
			}
			if (rollingBack) {
				this.seen.remove(fileToRollback);
			}
		}
	}

}
