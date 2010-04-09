/*
 * Copyright 2002-2010 the original author or authors.
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
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * {@link FileListFilter} that passes files only one time. This can
 * conveniently be used to prevent duplication of files, as is done in
 * {@link FileReadingMessageSource}.
 * <p/>
 * This implementation is thread safe.
 * 
 * @author Iwein Fuld
 * @since 1.0.0
 */
public class AcceptOnceFileListFilter extends AbstractFileListFilter {

	private final Queue<File> seen;

	private final Object monitor = new Object();


	/**
	 * Creates an AcceptOnceFileFilter that is based on a bounded queue. If the
	 * queue overflows, files that fall out will be passed through this filter
	 * again if passed to the {@link #filterFiles(File[])} method.
	 * 
	 * @param maxCapacity the maximum number of Files to maintain in the 'seen'
	 * queue.
	 */
	public AcceptOnceFileListFilter(int maxCapacity) {
		this.seen = new LinkedBlockingQueue<File>(maxCapacity);
	}

	/**
	 * Creates an AcceptOnceFileFilter based on an unbounded queue.
	 */
	public AcceptOnceFileListFilter() {
		this.seen = new LinkedBlockingQueue<File>();
	}


	protected boolean accept(File pathname) {
		synchronized (this.monitor) {
			if (seen.contains(pathname)) {
				return false;
			}
			if (!seen.offer(pathname)) {
				seen.poll();
				seen.add(pathname);
			}
			return true;
		}
	}

}
