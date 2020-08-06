/*
 * Copyright 2017-2020 the original author or authors.
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
