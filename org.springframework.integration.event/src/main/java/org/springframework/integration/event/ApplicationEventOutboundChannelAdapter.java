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
import org.springframework.integration.endpoint.AbstractMessageConsumer;
import org.springframework.integration.message.Message;
import org.springframework.util.Assert;

/**
 * An outbound Channel Adapter that publishes each {@link Message} it receives
 * as a {@link MessagingEvent}. The {@link MessagingEvent} is a subclass of Spring's
 * {@link ApplicationEvent} used by this adapter to simply wrap the {@link Message}.
 * 
 * @author Mark Fisher
 */
public class ApplicationEventOutboundChannelAdapter<T> extends AbstractMessageConsumer implements ApplicationEventPublisherAware {

	private ApplicationEventPublisher applicationEventPublisher;


	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	protected void onMessageInternal(Message<?> message) {
		Assert.notNull(this.applicationEventPublisher, "applicationEventPublisher is required");
		this.applicationEventPublisher.publishEvent(new MessagingEvent((Message<?>) message));		
	}

}
