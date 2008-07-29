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

import java.util.HashMap;
import java.util.Map;

import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessagingException;
import org.springframework.util.Assert;

/**
 * A {@link MessageHandler} implementation that delegates to an instance
 * of the {@link HeaderTransformer} strategy interface to modify the
 * header values of a {@link Message}.
 * 
 * @author Mark Fisher
 */
public class HeaderTransformingMessageHandler implements MessageHandler {

	private final HeaderTransformer transformer;


	public HeaderTransformingMessageHandler(HeaderTransformer transformer) {
		Assert.notNull(transformer, "transformer must not be null");
		this.transformer = transformer;
	}


	public Message<?> handle(Message<?> message) {
		try {
			Map<String, Object> headerMap = new HashMap<String, Object>(message.getHeaders());
	        this.transformer.transform(headerMap);
	        return MessageBuilder.fromPayload(message.getPayload()).copyHeaders(headerMap).build();
        } catch (Exception e) {
        	throw new MessagingException(message, "failed to transform message headers", e);
        }
	}

}
