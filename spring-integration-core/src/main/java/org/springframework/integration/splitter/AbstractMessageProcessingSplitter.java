/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.splitter;

import java.util.Collection;

import org.springframework.context.Lifecycle;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.support.management.ManageableLifecycle;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Base class for Message Splitter implementations that delegate to a {@link MessageProcessor} instance.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0
 */
abstract class AbstractMessageProcessingSplitter extends AbstractMessageSplitter implements ManageableLifecycle {

	private final MessageProcessor<Collection<?>> processor;

	protected AbstractMessageProcessingSplitter(MessageProcessor<Collection<?>> expressionEvaluatingMessageProcessor) {
		Assert.notNull(expressionEvaluatingMessageProcessor, "messageProcessor must not be null");
		this.processor = expressionEvaluatingMessageProcessor;
	}

	@Override
	protected void doInit() {
		setupMessageProcessor(this.processor);
	}

	@Override
	protected final Object splitMessage(Message<?> message) {
		return this.processor.processMessage(message);
	}

	@Override
	public void start() {
		if (this.processor instanceof Lifecycle lifecycle) {
			lifecycle.start();
		}
	}

	@Override
	public void stop() {
		if (this.processor instanceof Lifecycle lifecycle) {
			lifecycle.stop();
		}
	}

	@Override
	public boolean isRunning() {
		return !(this.processor instanceof Lifecycle) || ((Lifecycle) this.processor).isRunning();
	}

}
