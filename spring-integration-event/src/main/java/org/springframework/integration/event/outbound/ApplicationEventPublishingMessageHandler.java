/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.integration.event.outbound;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.integration.event.core.MessagingEvent;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * A {@link org.springframework.messaging.MessageHandler} that publishes each {@link Message}
 * it receives as a {@link MessagingEvent}. The {@link MessagingEvent} is a subclass of
 * Spring's {@link ApplicationEvent} used by this adapter to simply wrap the
 * {@link Message}.
 * <p>
 * If the {@link #publishPayload} flag is specified to {@code true}, the {@code payload}
 * will be published as is without wrapping to any {@link ApplicationEvent}.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class ApplicationEventPublishingMessageHandler extends AbstractMessageHandler
		implements ApplicationEventPublisherAware {

	private ApplicationEventPublisher applicationEventPublisher;

	private boolean publishPayload;

	/**
	 * Specify if {@code payload} should be published as is
	 * or the whole {@code message} must be wrapped to the {@link MessagingEvent}.
	 * @param publishPayload the {@code boolean} flag to wrap the {@code message}
	 *                       to the {@link MessagingEvent} or publish {@code payload}
	 *                       as is. Defaults to {@code false}.
	 * @since 4.2
	 * @see ApplicationEventPublisher#publishEvent(Object)
	 */
	public void setPublishPayload(boolean publishPayload) {
		this.publishPayload = publishPayload;
	}

	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	protected void handleMessageInternal(Message<?> message) {
		Assert.notNull(this.applicationEventPublisher, "applicationEventPublisher is required");
		if (message.getPayload() instanceof ApplicationEvent) {
			this.applicationEventPublisher.publishEvent((ApplicationEvent) message.getPayload());
		}
		else if (this.publishPayload) {
			this.applicationEventPublisher.publishEvent(message.getPayload());
		}
		else {
			this.applicationEventPublisher.publishEvent(new MessagingEvent(message));
		}
	}

}
