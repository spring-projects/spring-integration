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

package org.springframework.integration.leader.event;

import org.springframework.context.ApplicationEvent;
import org.springframework.integration.leader.Context;

/**
 * Base {@link ApplicationEvent} class for leader based events. All custom event
 * classes should be derived from this class.
 *
 * @author Janne Valkealahti
 * @author Gary Russell
 *
 */
@SuppressWarnings("serial")
public abstract class AbstractLeaderEvent extends ApplicationEvent {

	private final Context context;

	private final String role;

	/**
	 * Create a new ApplicationEvent.
	 *
	 * @param source the component that published the event (never {@code null})
	 */
	public AbstractLeaderEvent(Object source) {
		this(source, null, null);
	}

	/**
	 * Create a new ApplicationEvent.
	 *
	 * @param source the component that published the event (never {@code null})
	 * @param context the context associated with this event
	 * @param role the role of the leader
	 */
	public AbstractLeaderEvent(Object source, Context context, String role) {
		super(source);
		this.context = context;
		this.role = role;
	}

	/**
	 * Get the {@link Context} associated with this event.
	 *
	 * @return the context
	 */
	public Context getContext() {
		return this.context;
	}

	/**
	 * Get the role of the leader.
	 *
	 * @return the role
	 */
	public String getRole() {
		return this.role;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " [role=" + this.role + ", context=" + this.context + ", source=" + source
				+ "]";
	}

}
