/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.endpoint;

import org.springframework.context.Lifecycle;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.router.MessageRouter;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Message Endpoint that connects any {@link MessageHandler} implementation to a {@link SubscribableChannel}.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 */
public class EventDrivenConsumer extends AbstractEndpoint implements IntegrationConsumer {

	private final SubscribableChannel inputChannel;

	private final MessageHandler handler;

	public EventDrivenConsumer(SubscribableChannel inputChannel, MessageHandler handler) {
		Assert.notNull(inputChannel, "inputChannel must not be null");
		Assert.notNull(handler, "handler must not be null");
		this.inputChannel = inputChannel;
		this.handler = handler;
		this.setPhase(Integer.MIN_VALUE);
	}

	@Override
	public MessageChannel getInputChannel() {
		return this.inputChannel;
	}

	@Override
	public MessageChannel getOutputChannel() {
		if (this.handler instanceof MessageProducer) {
			return ((MessageProducer) this.handler).getOutputChannel();
		}
		else if (this.handler instanceof MessageRouter) {
			return ((MessageRouter) this.handler).getDefaultOutputChannel();
		}
		else {
			return null;
		}
	}

	@Override
	public MessageHandler getHandler() {
		return this.handler;
	}

	@Override
	protected void doStart() {
		this.logComponentSubscriptionEvent(true);
		this.inputChannel.subscribe(this.handler);
		if (this.handler instanceof Lifecycle) {
			((Lifecycle) this.handler).start();
		}
	}

	@Override
	protected void doStop() {
		this.logComponentSubscriptionEvent(false);
		this.inputChannel.unsubscribe(this.handler);
		if (this.handler instanceof Lifecycle) {
			((Lifecycle) this.handler).stop();
		}
	}

	private void logComponentSubscriptionEvent(boolean add) {
		if (this.handler instanceof NamedComponent && this.inputChannel instanceof NamedComponent) {
			String channelName = ((NamedComponent) this.inputChannel).getComponentName();
			String componentType = ((NamedComponent) this.handler).getComponentType();
			componentType = StringUtils.hasText(componentType) ? componentType : "";
			String componentName = getComponentName();
			componentName =
					(StringUtils.hasText(componentName) && componentName.contains("#")) ? "" : ":" + componentName;
			StringBuilder buffer = new StringBuilder();
			buffer.append("{")
					.append(componentType)
					.append(componentName)
					.append("} as a subscriber to the '")
					.append(channelName)
					.append("' channel");
			if (add) {
				buffer.insert(0, "Adding ");
			}
			else {
				buffer.insert(0, "Removing ");
			}
			logger.info(buffer.toString());
		}
	}

}
