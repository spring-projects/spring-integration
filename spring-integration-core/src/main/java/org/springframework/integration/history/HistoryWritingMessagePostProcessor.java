/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.history;

import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class HistoryWritingMessagePostProcessor implements MessagePostProcessor {

	private volatile TrackableComponent trackableComponent;

	private volatile boolean shouldTrack;

	private volatile MessageBuilderFactory messageBuilderFactory = new DefaultMessageBuilderFactory();

	public HistoryWritingMessagePostProcessor() {
	}

	public HistoryWritingMessagePostProcessor(TrackableComponent trackableComponent) {
		Assert.notNull(trackableComponent, "trackableComponent must not be null");
		this.trackableComponent = trackableComponent;
	}

	public void setMessageBuilderFactory(MessageBuilderFactory messageBuilderFactory) {
		Assert.notNull(messageBuilderFactory, "'messageBuilderFactory' cannot be null");
		this.messageBuilderFactory = messageBuilderFactory;
	}

	public void setTrackableComponent(TrackableComponent trackableComponent) {
		this.trackableComponent = trackableComponent;
	}

	public void setShouldTrack(boolean shouldTrack) {
		this.shouldTrack = shouldTrack;
	}

	@Override
	public Message<?> postProcessMessage(Message<?> message) {
		if (this.shouldTrack && this.trackableComponent != null) {
			return MessageHistory.write(message, this.trackableComponent, this.messageBuilderFactory);
		}
		return message;
	}

}
