/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.handler;

import org.springframework.context.Lifecycle;
import org.springframework.integration.IntegrationPattern;
import org.springframework.integration.IntegrationPatternType;
import org.springframework.integration.support.management.ManageableLifecycle;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;

/**
 * The {@link AbstractReplyProducingMessageHandler} wrapper around raw {@link MessageHandler}
 * for request-reply scenarios, e.g. {@code @ServiceActivator} annotation configuration.
 * <p>
 * This class is used internally by Framework in cases when request-reply is important
 * and there is no other way to apply advice chain.
 * <p>
 * The lifecycle control is delegated to the {@code target} {@link MessageHandler}.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class ReplyProducingMessageHandlerWrapper extends AbstractReplyProducingMessageHandler
		implements ManageableLifecycle {

	private final MessageHandler target;

	public ReplyProducingMessageHandlerWrapper(MessageHandler target) {
		Assert.notNull(target, "'target' must not be null");
		this.target = target;
	}

	@Override
	public IntegrationPatternType getIntegrationPatternType() {
		return (this.target instanceof IntegrationPattern)
				? ((IntegrationPattern) this.target).getIntegrationPatternType()
				: IntegrationPatternType.service_activator;
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		this.target.handleMessage(requestMessage);
		return null;
	}

	@Override
	public void start() {
		if (this.target instanceof Lifecycle) {
			((Lifecycle) this.target).start();
		}

	}

	@Override
	public void stop() {
		if (this.target instanceof Lifecycle) {
			((Lifecycle) this.target).stop();
		}
	}

	@Override
	public boolean isRunning() {
		return !(this.target instanceof Lifecycle) || ((Lifecycle) this.target).isRunning();
	}

}
