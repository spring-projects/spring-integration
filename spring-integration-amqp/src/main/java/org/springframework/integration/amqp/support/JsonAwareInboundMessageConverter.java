/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.amqp.support;

import java.io.UnsupportedEncodingException;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.ClassMapper;
import org.springframework.amqp.support.converter.JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.util.Assert;

/**
 * MessageConverter used on inbound endpoints that delegates either to a SimpleMessageConverter,
 * or a JsonMessageConverter, with a custom {@link ClassMapper} that uses a configured class name
 * rather than looking for the type information in the message headers.
 * <p/>
 * To provide backwards compatibility,
 * if the contentType property contains json (but does not start with text), we delegate to
 * the JsonMessageConverter. Otherwise we delegate to the SimpleMessageConverter.
 * <p/>
 * If the className property is set to <code>java.lang.String</code> the message
 * body is returned as a String.
 * @author Gary Russell
 * @since 2.2
 *
 */
public class JsonAwareInboundMessageConverter implements MessageConverter {

	private final SimpleMessageConverter simpleMessageConverter = new SimpleMessageConverter();

	private final JsonMessageConverter jsonMessageConverter = new JsonMessageConverter();

	public static final String DEFAULT_CHARSET = "UTF-8";

	private volatile String charset = DEFAULT_CHARSET;

	private volatile Class<?> clazz;

	public JsonAwareInboundMessageConverter(String className) {
		Assert.notNull(className, "'className' cannot be null");
		jsonMessageConverter.setClassMapper(new ClassMapper() {

			public void fromClass(Class<?> clazz, MessageProperties properties) {
				throw new UnsupportedOperationException("This converter can only be used to map inbound requests");
			}

			public Class<?> toClass(MessageProperties properties) {
				return clazz;
			}
		});
		try {
			this.clazz = Class.forName(className);
		}
		catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("Invalid class name", e);
		}
	}

	public void setCharset(String charset) {
		Assert.notNull(charset, "'charset' cannot be null");
		this.charset = charset;
	}

	public Message toMessage(Object object, MessageProperties messageProperties) throws MessageConversionException {
		return this.simpleMessageConverter.toMessage(object, messageProperties);
	}

	public Object fromMessage(Message message) throws MessageConversionException {
		Object content = null;
		MessageProperties properties = message.getMessageProperties();
		if (properties != null) {
			String contentType = properties.getContentType();
			if (contentType != null && contentType.contains("json") && !(contentType.startsWith("text"))) {
				String encoding = properties.getContentEncoding();
				if (encoding == null) {
					encoding = this.charset;
				}
				if (clazz == String.class) {
					try {
						content = new String(message.getBody(), encoding);
					}
					catch (UnsupportedEncodingException e) {
						throw new MessageConversionException("Failed to convert byte[] to String", e);
					}
				}
				else {
					content = this.jsonMessageConverter.fromMessage(message);
				}
			}
			else {
				content = this.simpleMessageConverter.fromMessage(message);
			}
		}
		if (content == null) {
			content = message.getBody();
		}
		return content;
	}

}
