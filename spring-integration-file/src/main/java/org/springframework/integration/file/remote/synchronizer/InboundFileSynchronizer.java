/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.file.remote.synchronizer;

import java.io.File;

/**
 * Strategy for synchronizing from a remote File system to a local directory.
 *
 * @author Mark Fisher
 * @author Gary Russell
 *
 * @since 2.0
 */
@FunctionalInterface
public interface InboundFileSynchronizer {

	/**
	 * Synchronize all available files to the local directory.
	 * @param localDirectory the directory.
	 */
	void synchronizeToLocalDirectory(File localDirectory);

	/**
	 * Synchronize up to maxFetchSize files to the local directory.
	 * @param localDirectory the directory.
	 * @param maxFetchSize the maximum files to fetch.
	 */
	default void synchronizeToLocalDirectory(File localDirectory, int maxFetchSize) {
		synchronizeToLocalDirectory(localDirectory);
	}

}
