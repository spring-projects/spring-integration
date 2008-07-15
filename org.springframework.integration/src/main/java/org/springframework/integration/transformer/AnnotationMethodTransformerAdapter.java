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

import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.handler.annotation.AnnotationMethodMessageMapper;
import org.springframework.integration.message.DefaultMessageMapper;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageMapper;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.util.AbstractMethodInvokingAdapter;

/**
 * @author Mark Fisher
 */
public class AnnotationMethodTransformerAdapter extends AbstractMethodInvokingAdapter implements MessageHandler {

	private volatile MessageMapper mapper;

	private volatile boolean methodExpectsMessage;


	public void setMethodExpectsMessage(boolean methodExpectsMessage) {
		this.methodExpectsMessage = methodExpectsMessage;
	}

	protected void initialize() {
		this.mapper = (this.getMethod() != null)
				? new AnnotationMethodMessageMapper(this.getMethod())
				: new DefaultMessageMapper();
	}

	@SuppressWarnings("unchecked")
	public Message<?> transform(Message<?> message) {
		if (!this.isInitialized()) {
			this.afterPropertiesSet();
		}
		if (message.getPayload() == null) {
			return message;
		}
		Object param = (this.methodExpectsMessage) ? message : this.mapper.mapMessage(message);
		try {
			Object args[] = null;
			if (param != null && param.getClass().isArray()
					&& (Object.class.isAssignableFrom(param.getClass().getComponentType()))) {
				args = (Object[]) param;
			}
			else {
				args = new Object[] { param }; 
			}
			Object result = null;
			try {
				result = this.invokeMethod(args);
			}
			catch (NoSuchMethodException e) {
				result = this.invokeMethod(message);
				this.methodExpectsMessage = true;
			}
			if (result == null) {
				if (logger.isDebugEnabled()) {
					logger.debug("MessageTransformer returned a null result");
				}
				return null;
			}
			if (result instanceof Properties && !(message.getPayload() instanceof Properties)) {
				Properties propertiesToSet = (Properties) result;
				for (Object keyObject : propertiesToSet.keySet()) {
					String key = (String) keyObject;
					message.getHeader().setProperty(key, propertiesToSet.getProperty(key)); 
				}
			}
			else if (result instanceof Map && !(message.getPayload() instanceof Map)) {
				Map<String, ?> attributesToSet = (Map) result;
				for (String key : attributesToSet.keySet()) {
					message.getHeader().setAttribute(key, attributesToSet.get(key));
				}
			}
			else {
				return new GenericMessage(result, message.getHeader());
			}
		}
		catch (Exception e) {
			throw new MessagingException(message, "failed to transform message payload", e);
		}
		return message;
	}

	public Message<?> handle(Message<?> message) {
		return this.transform(message);
	}

}
