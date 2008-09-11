package org.springframework.integration.file;

import java.io.File;
import java.io.FileFilter;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

class AcceptOnceFileFilter implements FileFilter {

	private final Queue<File> seen;

	private final Object monitor = new Object();

	public AcceptOnceFileFilter(int maxCapacity) {
		seen = new LinkedBlockingQueue<File>(maxCapacity);
	}

	public AcceptOnceFileFilter() {
		seen = new LinkedBlockingQueue<File>();
	}

	public boolean accept(File pathname) {
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
