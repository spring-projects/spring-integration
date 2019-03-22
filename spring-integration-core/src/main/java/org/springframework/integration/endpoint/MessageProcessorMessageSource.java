/*
 * Copyright 2016-2019 the original author or authors.
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

import org.springframework.integration.handler.MessageProcessor;

/**
 * The {@link org.springframework.integration.core.MessageSource} strategy implementation
 * to produce a {@link org.springframework.messaging.Message} from underlying
 * {@linkplain #messageProcessor} for polling endpoints.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
public class MessageProcessorMessageSource extends AbstractMessageSource<Object> {

	private final MessageProcessor<?> messageProcessor;

	public MessageProcessorMessageSource(MessageProcessor<?> messageProcessor) {
		this.messageProcessor = messageProcessor;
	}

	@Override
	public String getComponentType() {
		return "inbound-channel-adapter";
	}

	@Override
	protected Object doReceive() {
		return this.messageProcessor.processMessage(null);
	}

}
