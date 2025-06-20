/*
 * Copyright 2015-present the original author or authors.
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

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.integration.leader.Context;

/**
 * Default implementation of {@link LeaderEventPublisher}.
 *
 * @author Janne Valkealahti
 * @author Gary Russell
 * @author Glenn Renfro
 *
 */
public class DefaultLeaderEventPublisher implements LeaderEventPublisher, ApplicationEventPublisherAware {

	private ApplicationEventPublisher applicationEventPublisher;

	/**
	 * Instantiates a new leader event publisher.
	 */
	public DefaultLeaderEventPublisher() {
	}

	/**
	 * Instantiates a new leader event publisher.
	 *
	 * @param applicationEventPublisher the application event publisher
	 */
	public DefaultLeaderEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	public void publishOnGranted(Object source, Context context, String role) {
		if (this.applicationEventPublisher != null) {
			this.applicationEventPublisher.publishEvent(new OnGrantedEvent(source, context, role));
		}
	}

	@Override
	public void publishOnRevoked(Object source, Context context, String role) {
		if (this.applicationEventPublisher != null) {
			this.applicationEventPublisher.publishEvent(new OnRevokedEvent(source, context, role));
		}
	}

	@Override
	public void publishOnFailedToAcquire(Object source, Context context, String role) {
		if (this.applicationEventPublisher != null) {
			this.applicationEventPublisher.publishEvent(new OnFailedToAcquireMutexEvent(source, context, role));
		}
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

}
