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

import org.springframework.integration.leader.Context;

/**
 * Interface for publishing leader based application events.
 *
 * @author Janne Valkealahti
 * @author Gary Russell
 * @author Glenn Renfro
 *
 */
public interface LeaderEventPublisher {

	/**
	 * Publish a granted event.
	 * @param source the component generated this event
	 * @param context the context associated with event
	 * @param role the role of the leader
	 */
	void publishOnGranted(Object source, Context context, String role);

	/**
	 * Publish a revoked event.
	 * @param source the component generated this event
	 * @param context the context associated with event
	 * @param role the role of the leader
	 */
	void publishOnRevoked(Object source, Context context, String role);

	/**
	 * Publish a failure to acquire event.
	 * @param source the component generated this event
	 * @param context the context associated with event
	 * @param role the role of the leader
	 * @since 5.0
	 */
	void publishOnFailedToAcquire(Object source, Context context, String role);

}
