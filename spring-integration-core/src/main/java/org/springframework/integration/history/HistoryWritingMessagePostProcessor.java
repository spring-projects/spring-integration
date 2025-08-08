/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.history;

import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.integration.support.management.TrackableComponent;
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
