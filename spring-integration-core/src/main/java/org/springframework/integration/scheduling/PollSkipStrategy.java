/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
