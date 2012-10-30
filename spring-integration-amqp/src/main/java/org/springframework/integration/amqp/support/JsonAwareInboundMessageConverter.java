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
import org.springframework.util.ClassUtils;

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
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.2
 */
public class JsonAwareInboundMessageConverter extends SimpleMessageConverter {

	private final JsonMessageConverter jsonMessageConverter = new JsonMessageConverter();

	private volatile String charset = DEFAULT_CHARSET;

	private final Class<?> clazz;

	public JsonAwareInboundMessageConverter(Class<?> aClass) {
		Assert.notNull(aClass, "'className' cannot be null");
		this.clazz = aClass;

		jsonMessageConverter.setClassMapper(new ClassMapper() {

			public void fromClass(Class<?> clazz, MessageProperties properties) {
				throw new UnsupportedOperationException("This converter can only be used to map inbound requests");
			}

			public Class<?> toClass(MessageProperties properties) {
				return clazz;
			}
		});
	}

	public void setDefaultCharset(String charset) {
		Assert.notNull(charset, "'charset' cannot be null");
		this.charset = charset;
		super.setDefaultCharset(charset);
		this.jsonMessageConverter.setDefaultCharset(charset);
	}

	public Object fromMessage(Message message) throws MessageConversionException {
		MessageProperties properties = message.getMessageProperties();
		if (properties != null) {
			String contentType = properties.getContentType();
			if (contentType != null && contentType.contains("json") && !(contentType.startsWith("text"))) {
				if (clazz == String.class) {
					String encoding = properties.getContentEncoding();
					if (encoding == null) {
						encoding = this.charset;
					}
					try {
						return new String(message.getBody(), encoding);
					}
					catch (UnsupportedEncodingException e) {
						throw new MessageConversionException("Failed to convert byte[] to String", e);
					}
				}
				else {
					return this.jsonMessageConverter.fromMessage(message);
				}
			}
		}

		return super.fromMessage(message);
	}

}
