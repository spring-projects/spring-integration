/*
 * Copyright 2018-2019 the original author or authors.
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

import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.acks.AckUtils;
import org.springframework.integration.acks.AcknowledgmentCallback;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;

/**
 * A {@link PollingOperations} used to ad-hoc poll a {@link MessageSource}.
 * If the source supports deferred acknowledgment, it will be ACCEPTed
 * or REJECTed if necessary.
 *
 * @author Gary Russell
 *
 * @since 5.0.1
 *
 */
public class MessageSourcePollingTemplate implements PollingOperations {

	private final MessageSource<?> source;

	public MessageSourcePollingTemplate(MessageSource<?> source) {
		Assert.notNull(source, "'source' cannot be null");
		this.source = source;
	}

	@Override
	public boolean poll(MessageHandler handler) {
		Assert.notNull(handler, "'handler' cannot be null");
		Message<?> message = this.source.receive();
		if (message != null) {
			AcknowledgmentCallback ackCallback = StaticMessageHeaderAccessor.getAcknowledgmentCallback(message);
			try {
				handler.handleMessage(message);
				AckUtils.autoAck(ackCallback);
			}
			catch (Exception e) {
				AckUtils.autoNack(ackCallback);
				throw IntegrationUtils.wrapInHandlingExceptionIfNecessary(message,
						() -> "error occurred during handling message in 'MessageSourcePollingTemplate' ["
								+ this + "]", e);
			}
			return true;
		}
		return false;
	}

}
