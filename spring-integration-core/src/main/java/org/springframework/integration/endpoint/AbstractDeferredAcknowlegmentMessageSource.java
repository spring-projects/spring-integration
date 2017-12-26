/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.integration.endpoint;

import org.springframework.context.MessageSource;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.core.DeferredAcknowlegmentMessageSource;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;

/**
 * A {@link MessageSource} that populates a
 * {@link IntegrationMessageHeaderAccessor#ACKNOWLEDGMENT_CALLBACK} header used to later
 * acknowledge or reject/requeue the message.
 * <p>
 * When used as a message source for an inbound channel adapter, if the user application
 * doesn't ack the message, it will be acknowledged if the flow ends normally
 * and rejected if an exception is thrown.
 *
 * @param <T> the payload type.
 *
 * @author Gary Russell
 * @since 5.0.1
 *
 */
public abstract class AbstractDeferredAcknowlegmentMessageSource<T> extends AbstractMessageSource<T>
		implements DeferredAcknowlegmentMessageSource<T> {

	@Override
	protected Object doReceive() {
		return receiveMessageBuilder();
	}

	/**
	 * Return a message builder with the
	 * {@link IntegrationMessageHeaderAccessor#ACKNOWLEDGMENT_CALLBACK} header populated.
	 * @return the builder.
	 */
	protected abstract AbstractIntegrationMessageBuilder<Object> receiveMessageBuilder();

}
