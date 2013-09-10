/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.support.converter;

import java.lang.reflect.Type;

import org.springframework.integration.mapping.InboundMessageMapper;
import org.springframework.integration.mapping.OutboundMessageMapper;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.converter.MessageConversionException;
import org.springframework.messaging.support.converter.MessageConverter;

/**
 * @author Mark Fisher
 * @since 2.0
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class SimpleMessageConverter implements MessageConverter<Object> {

	private volatile InboundMessageMapper inboundMessageMapper;

	private volatile OutboundMessageMapper outboundMessageMapper;


	public SimpleMessageConverter() {
		this(null, null);
	}

	public SimpleMessageConverter(InboundMessageMapper<?> inboundMessageMapper) {
		this(inboundMessageMapper,
				(inboundMessageMapper instanceof OutboundMessageMapper ? (OutboundMessageMapper<?>) inboundMessageMapper : null));
	}

	public SimpleMessageConverter(OutboundMessageMapper<?> outboundMessageMapper) {
		this(outboundMessageMapper instanceof InboundMessageMapper ? (InboundMessageMapper<?>) outboundMessageMapper : null,
				outboundMessageMapper);
	}

	public SimpleMessageConverter(InboundMessageMapper<?> inboundMessageMapper, OutboundMessageMapper<?> outboundMessageMapper) {
		this.setInboundMessageMapper(inboundMessageMapper);
		this.setOutboundMessageMapper(outboundMessageMapper);
	}


	public void setInboundMessageMapper(InboundMessageMapper<?> inboundMessageMapper) {
		this.inboundMessageMapper = (inboundMessageMapper != null) ? inboundMessageMapper : new DefaultInboundMessageMapper();
	}

	public void setOutboundMessageMapper(OutboundMessageMapper<?> outboundMessageMapper) {
		this.outboundMessageMapper = (outboundMessageMapper != null) ? outboundMessageMapper : new DefaultOutboundMessageMapper();
	}

	public <P> Message<P> toMessage(Object object) {
		try {
			return this.inboundMessageMapper.toMessage(object);
		}
		catch (Exception e) {
			throw new MessageConversionException("failed to convert object to Message", e);
		}
	}

	public Object fromMessage(Message<?> message, Type targetClass) {
		try {
			return this.outboundMessageMapper.fromMessage(message);
		}
		catch (Exception e) {
			throw new MessageConversionException(message, "failed to convert Message to object", e);
		}
	}


	private static class DefaultInboundMessageMapper implements InboundMessageMapper<Object> {

		public Message<?> toMessage(Object object) throws Exception {
			if (object == null) {
				return null;
			}
			if (object instanceof Message<?>) {
				return (Message<?>) object;
			}
			return MessageBuilder.withPayload(object).build();
		}
	}


	private static class DefaultOutboundMessageMapper implements OutboundMessageMapper<Object> {

		public Object fromMessage(Message<?> message) throws Exception {
			return (message != null) ? message.getPayload() : null;
		}
	}

}
