package org.springframework.integration.file;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * {@link FileListFilter} that passes files only one time. This can conveniently
 * be used to prevent duplication of files, as is done in
 * {@link PollableFileSource}
 * 
 * @author Iwein Fuld
 * 
 */
public class AcceptOnceFileFilter implements FileListFilter {

	private final Queue<File> seen;

	private final Object monitor = new Object();

	/**
	 * Creates an AcceptOnceFileFilter that is based on a bounded queue. If the
	 * queue overflows files that fall out will be pass this filter again if
	 * passed to the {@link #filterFiles(File[])} method
	 * @param maxCapacity
	 */
	public AcceptOnceFileFilter(int maxCapacity) {
		seen = new LinkedBlockingQueue<File>(maxCapacity);
	}

	/**
	 * Creates an AcceptOnceFileFilter based on an unbounded queue
	 */
	public AcceptOnceFileFilter() {
		seen = new LinkedBlockingQueue<File>();
	}

	/**
	 * Returns the list of files that have not been filtered by this instance
	 * before
	 */
	public List<File> filterFiles(File[] files) {
		List<File> accepted = new ArrayList<File>();
		for (File file : files) {
			if (accept(file)) {
				accepted.add(file);
			}
		}
		return accepted;
	}

	private boolean accept(File pathname) {
		synchronized (monitor) {
			if (!seen.contains(pathname)) {
				if (!seen.offer(pathname)) {
					seen.poll();
					seen.add(pathname);
				}
				return true;
			}
			return false;
		}
	}
}
