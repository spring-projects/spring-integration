/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.event;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageTarget;

/**
 * A message target for publishing {@link MessagingEvent MessagingEvents}. The
 * {@link MessagingEvent} is a subclass of Spring's {@link ApplicationEvent}
 * used by this adapter to wrap any {@link Message} sent to this target.
 * 
 * @author Mark Fisher
 */
public class ApplicationEventTarget<T> implements MessageTarget, ApplicationEventPublisherAware {

	private ApplicationEventPublisher applicationEventPublisher;


	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	public boolean send(Message<?> message) {
		this.applicationEventPublisher.publishEvent(
				new MessagingEvent((Message<?>) message));
		return true;
	}

}
