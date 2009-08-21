/*
 * Copyright 2002-2009 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;

import org.springframework.integration.core.Message;
import org.springframework.integration.handler.MessageMappingMethodInvoker;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageHandlingException;

/**
 * @author Mark Fisher
 */
public class MethodInvokingTransformer implements Transformer {

	private final MessageProcessor messageProcessor;


	public MethodInvokingTransformer(Object object, Method method) {
		this.messageProcessor = new MessageMappingMethodInvoker(object, method);
	}

	public MethodInvokingTransformer(Object object, String methodName) {
		this.messageProcessor = new MessageMappingMethodInvoker(object, methodName);
	}

	public MethodInvokingTransformer(Object object) {
		this.messageProcessor = new MessageMappingMethodInvoker(object,
				org.springframework.integration.annotation.Transformer.class);
	}


	public Message<?> transform(Message<?> message) {
		Object result = this.messageProcessor.processMessage(message);
		if (result == null) {
			return null;
		}
		if (result instanceof Message) {
			return (Message<?>) result;
		}
		if (result instanceof Properties && !(message.getPayload() instanceof Properties)) {
			Properties propertiesToSet = (Properties) result;
			MessageBuilder<?> builder = MessageBuilder.fromMessage(message);
			for (Object keyObject : propertiesToSet.keySet()) {
				String key = (String) keyObject;
				builder.setHeader(key, propertiesToSet.getProperty(key));
			}
			return builder.build();
		}
		if (result instanceof Map && !(message.getPayload() instanceof Map)) {
			Map<?, ?> attributesToSet = (Map <?, ?>) result;
			MessageBuilder<?> builder = MessageBuilder.fromMessage(message);
			for (Object key : attributesToSet.keySet()) {
				if (!(key instanceof String)) {
					throw new MessageHandlingException(message,
							"Map returned from a Transformer method must have String-typed keys");
				}
				builder.setHeader((String) key, attributesToSet.get(key));
			}
			return builder.build();
		}
		return MessageBuilder.withPayload(result).copyHeaders(message.getHeaders()).build();
	}

}
