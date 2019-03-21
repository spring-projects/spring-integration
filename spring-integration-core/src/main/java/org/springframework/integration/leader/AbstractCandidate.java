/*
 * Copyright 2015-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
