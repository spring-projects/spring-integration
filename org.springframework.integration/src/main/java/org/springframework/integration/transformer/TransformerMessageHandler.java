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

import java.util.Map;
import java.util.Properties;

import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageHandlingException;

/**
 * @author Mark Fisher
 */
public class TransformerMessageHandler extends AbstractMessageHandler {

	@Override
	protected Message<?> createReplyMessage(Object result, Message<?> requestMessage) {
		if (result instanceof Properties && !(requestMessage.getPayload() instanceof Properties)) {
			Properties propertiesToSet = (Properties) result;
			MessageBuilder<?> builder = MessageBuilder.fromMessage(requestMessage);
			for (Object keyObject : propertiesToSet.keySet()) {
				String key = (String) keyObject;
				builder.setHeader(key, propertiesToSet.getProperty(key));
			}
			return builder.build();
		}
		else if (result instanceof Map && !(requestMessage.getPayload() instanceof Map)) {
			Map<?, ?> attributesToSet = (Map <?, ?>) result;
			MessageBuilder<?> builder = MessageBuilder.fromMessage(requestMessage);
			for (Object key : attributesToSet.keySet()) {
				if (!(key instanceof String)) {
					throw new MessageHandlingException(requestMessage,
							"Map returned from a Transformer method must have String-typed keys");
				}
				builder.setHeader((String) key, attributesToSet.get(key));
			}
			return builder.build();
		}
		else {
			return MessageBuilder.fromPayload(result).copyHeaders(requestMessage.getHeaders()).build();
		}
	}

	@Override
	protected Message<?> postProcessReplyMessage(Message<?> replyMessage, Message<?> requestMessage) {
		return replyMessage;
	} 

}
