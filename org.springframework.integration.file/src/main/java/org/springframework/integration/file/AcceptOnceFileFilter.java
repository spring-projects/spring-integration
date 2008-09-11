package org.springframework.integration.file;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class AcceptOnceFileFilter implements FileListFilter {

	private final Queue<File> seen;

	private final Object monitor = new Object();

	public AcceptOnceFileFilter(int maxCapacity) {
		seen = new LinkedBlockingQueue<File>(maxCapacity);
	}

	public AcceptOnceFileFilter() {
		seen = new LinkedBlockingQueue<File>();
	}

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
