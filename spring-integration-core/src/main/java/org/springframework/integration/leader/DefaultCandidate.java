/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.leader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Simple {@link Candidate} for leadership.
 * This implementation simply logs when it is elected and when its leadership is revoked.
 *
 * @author Janne Valkealahti
 * @author Artem Bilan
 *
 * @since 4.2
 */
public class DefaultCandidate extends AbstractCandidate {

	private final Log logger = LogFactory.getLog(getClass());

	private volatile Context leaderContext;

	/**
	 * Instantiate a default candidate.
	 */
	public DefaultCandidate() {
	}

	/**
	 * Instantiate a default candidate.
	 *
	 * @param id the identifier
	 * @param role the role
	 */
	public DefaultCandidate(String id, String role) {
		super(id, role);
	}

	@Override
	public void onGranted(Context ctx) {
		if (this.logger.isInfoEnabled()) {
			this.logger.info(this + " has been granted leadership; context: " + ctx);
		}
		this.leaderContext = ctx;
	}

	@Override
	public void onRevoked(Context ctx) {
		if (this.logger.isInfoEnabled()) {
			this.logger.info(this + " leadership has been revoked: " + ctx);
		}
	}

	/**
	 * Voluntarily yield leadership if held. If leader context is not
	 * yet known this method does nothing. Leader context becomes available
	 * only after {@link #onGranted(Context)} method is called by the
	 * leader initiator.
	 */
	public void yieldLeadership() {
		if (this.leaderContext != null) {
			this.leaderContext.yield();
		}
	}

	@Override
	public String toString() {
		return String.format("DefaultCandidate{role=%s, id=%s}", getRole(), getId());
	}

}
