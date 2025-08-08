/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.leader;

import org.springframework.lang.Nullable;

/**
 * Interface that defines the context for candidate leadership.
 * Instances of this object are passed to {@link Candidate candidates}
 * upon granting and revoking of leadership.
 * <p>
 * The {@link Context} is {@link FunctionalInterface}
 * with no-op implementation for the {@link #yield()}.
 *
 * @author Patrick Peralta
 * @author Janne Valkealahti
 * @author Artem Bilan
 * @author Gary Russell
 *
 */
@FunctionalInterface
public interface Context {

	/**
	 * Checks if the {@link Candidate} this context was
	 * passed to is the leader.
	 * @return true if the {@link Candidate} this context was
	 *         passed to is the leader
	 */
	boolean isLeader();

	/**
	 * Causes the {@link Candidate} this context was passed to
	 * to relinquish leadership. This method has no effect
	 * if the candidate is not currently the leader.
	 */
	default void yield() {
		// no-op
	}

	/**
	 * Get the role for the {@link Candidate}.
	 * @return the role.
	 * @since 5.0.6
	 */
	@Nullable
	default String getRole() {
		return null;
	}

}
