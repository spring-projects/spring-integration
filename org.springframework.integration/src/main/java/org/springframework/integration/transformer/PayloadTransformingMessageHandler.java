/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.transformer;

import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessagingException;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 */
@SuppressWarnings("unchecked")
public class PayloadTransformingMessageHandler implements MessageHandler {

	private final PayloadTransformer transformer;


	public PayloadTransformingMessageHandler(PayloadTransformer transformer) {
		Assert.notNull(transformer, "transformer must not be null");
		this.transformer = transformer;
	}


	public Message<?> handle(Message<?> message) {
		try {
	        Object result = this.transformer.transform(message.getPayload());
	        return MessageBuilder.fromPayload(result).copyHeaders(message.getHeaders()).build();
        } catch (Exception e) {
        	throw new MessagingException(message, "failed to transform message payload", e);
        }
	}

}
