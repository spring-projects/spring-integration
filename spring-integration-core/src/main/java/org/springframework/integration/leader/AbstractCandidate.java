/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.leader;

import java.util.UUID;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Base implementation of a {@link Candidate}.
 *
 * @author Janne Valkealahti
 * @author Gary Russell
 *
 */
public abstract class AbstractCandidate implements Candidate {

	private static final String DEFAULT_ROLE = "leader";

	private final String id;

	private final String role;

	/**
	 * Instantiate a abstract candidate.
	 */
	public AbstractCandidate() {
		this(null, null);
	}

	/**
	 * Instantiate a abstract candidate.
	 *
	 * @param id the identifier
	 * @param role the role
	 */
	public AbstractCandidate(@Nullable String id, @Nullable String role) {
		this.id = StringUtils.hasText(id) ? id : UUID.randomUUID().toString();
		this.role = StringUtils.hasText(role) ? role : DEFAULT_ROLE;
	}

	@Override
	public String getRole() {
		return this.role;
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public abstract void onGranted(Context ctx) throws InterruptedException;

	@Override
	public abstract void onRevoked(Context ctx);

}
