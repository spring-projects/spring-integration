/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.scheduling;

/**
 * Implementations determine whether a particular poll should be skipped.
 *
 * @author Gary Russell
 * @since 4.1
 *
 */
public interface PollSkipStrategy {

	/**
	 * Return true if this poll should be skipped.
	 * @return true to skip.
	 */
	boolean skipPoll();

}
