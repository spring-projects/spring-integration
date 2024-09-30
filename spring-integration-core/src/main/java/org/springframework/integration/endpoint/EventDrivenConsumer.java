/*
 * Copyright 2002-2024 the original author or authors.
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
		if (this.handler instanceof MessageProducer) {
			setPhase(Integer.MIN_VALUE + 1000);
		}
		else {
			setPhase(Integer.MIN_VALUE);
		}
	}

	@Override
	public MessageChannel getInputChannel() {
		return this.inputChannel;
	}

	@Override
	public MessageChannel getOutputChannel() {
		if (this.handler instanceof MessageProducer messageProducer) {
			return messageProducer.getOutputChannel();
		}
		else if (this.handler instanceof MessageRouter messageRouter) {
			return messageRouter.getDefaultOutputChannel();
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
		if (this.handler instanceof Lifecycle lifecycle) {
			lifecycle.start();
		}
	}

	@Override
	protected void doStop() {
		this.logComponentSubscriptionEvent(false);
		this.inputChannel.unsubscribe(this.handler);
		if (this.handler instanceof Lifecycle lifecycle) {
			lifecycle.stop();
		}
	}

	private void logComponentSubscriptionEvent(boolean add) {
		if (this.handler instanceof NamedComponent namedHandler
				&& this.inputChannel instanceof NamedComponent namedChannel) {

			String componentType = namedHandler.getComponentType();
			componentType = StringUtils.hasText(componentType) ? componentType : "";
			String componentName = getComponentName();
			componentName =
					(StringUtils.hasText(componentName) && componentName.contains("#")) ? "" : ":" + componentName;
			StringBuilder buffer = new StringBuilder();
			buffer.append("{")
					.append(componentType)
					.append(componentName)
					.append("} as a subscriber to the '")
					.append(namedChannel.getComponentName())
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
