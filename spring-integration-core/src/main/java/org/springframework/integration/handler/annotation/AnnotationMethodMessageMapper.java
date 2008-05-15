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

package org.springframework.integration.handler.annotation;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.springframework.core.GenericTypeResolver;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.MessageHeader;
import org.springframework.integration.message.MessageMapper;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Mark Fisher
 */
public class AnnotationMethodMessageMapper implements MessageMapper {

	private ParameterNameDiscoverer parameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();

	private final Method method;

	private MethodParameterMetadata[] parameterMetadata;

	private volatile boolean initialized;

	private final Object initializationMonitor = new Object();


	public AnnotationMethodMessageMapper(Method method) {
		Assert.notNull(method, "method must not be null");
		this.method = method;
	}


	public void initialize() {
		synchronized (this.initializationMonitor) {
			if (this.initialized) {
				return;
			}
			Class<?>[] paramTypes = this.method.getParameterTypes();			
			this.parameterMetadata = new MethodParameterMetadata[paramTypes.length];
			for (int i = 0; i < parameterMetadata.length; i++) {
				MethodParameter methodParam = new MethodParameter(this.method, i);
				methodParam.initParameterNameDiscovery(this.parameterNameDiscoverer);
				GenericTypeResolver.resolveParameterType(methodParam, this.method.getDeclaringClass());
				Object[] paramAnns = methodParam.getParameterAnnotations();
				String attributeName = null;
				String propertyName = null;
				for (int j = 0; j < paramAnns.length; j++) {
					Object paramAnn = paramAnns[j];
					if (HeaderAttribute.class.isInstance(paramAnn)) {
						HeaderAttribute headerAttribute = (HeaderAttribute) paramAnn;
						attributeName = this.resolveParameterNameIfNecessary(headerAttribute.value(), methodParam);
						parameterMetadata[i] = new MethodParameterMetadata(HeaderAttribute.class, attributeName, headerAttribute.required());
					}
					else if (HeaderProperty.class.isInstance(paramAnn)) {
						HeaderProperty headerProperty = (HeaderProperty) paramAnn;
						propertyName = this.resolveParameterNameIfNecessary(headerProperty.value(), methodParam);
						parameterMetadata[i] = new MethodParameterMetadata(HeaderProperty.class, propertyName, headerProperty.required());
					}
				}
				if (attributeName != null && propertyName != null) {
					throw new ConfigurationException("The @HeaderAttribute and @HeaderProperty annotations " +
							"are mutually exclusive. They should not both be provided on the same parameter.");
				}
				if (attributeName == null && propertyName == null) {
					parameterMetadata[i] = new MethodParameterMetadata(methodParam.getParameterType(), null, false);
				}
			}
			this.initialized = true;
		}
	}

	public Object[] mapMessage(Message message) {
		if (message == null) {
			return null;
		}
		if (!this.initialized) {
			this.initialize();
		}
		Object[] args = new Object[this.parameterMetadata.length];
		for (int i = 0; i < this.parameterMetadata.length; i++) {
			MethodParameterMetadata metadata = this.parameterMetadata[i];
			Class<?> type = metadata.type;
			if (type.equals(HeaderAttribute.class)) {
				Object value = message.getHeader().getAttribute(metadata.key);
				if (value == null && metadata.required) {
					throw new MessageHandlingException(message,
							"required attribute '" + metadata.key + "' not available");
				}
				args[i] = value;
			}
			else if (type.equals(HeaderProperty.class)) {
				Object value = message.getHeader().getProperty(metadata.key);
				if (value == null && metadata.required) {
					throw new MessageHandlingException(message,
							"required property '" + metadata.key + "' not available");
				}
				args[i] = value;
			}
			else if (Message.class.isAssignableFrom(type)) {
				args[i] = message;
			}
			else if (Map.class.isAssignableFrom(type)) {
				args[i] = this.getHeaderAttributes(message);
			}
			else if (Properties.class.isAssignableFrom(type)) {
				args[i] = this.getHeaderProperties(message);
			}
			else {
				args[i] = message.getPayload();
			}
		}
		return args;
	}

	private Map<String, Object> getHeaderAttributes(Message<?> message) {
		Map<String, Object> attributes = new HashMap<String, Object>();
		MessageHeader header = message.getHeader();
		Set<String> attributeNames = header.getAttributeNames();
		for (String name : attributeNames) {
			attributes.put(name, header.getAttribute(name));
		}
		return attributes;
	}

	private Properties getHeaderProperties(Message<?> message) {
		Properties properties = new Properties();
		MessageHeader header = message.getHeader();
		Set<String> propertyNames = header.getPropertyNames();
		for (String name : propertyNames) {
			properties.setProperty(name, header.getProperty(name));
		}
		return properties;
	}

	private String resolveParameterNameIfNecessary(String paramName, MethodParameter methodParam) {
		if (!StringUtils.hasText(paramName)) {
			paramName = methodParam.getParameterName();
			if (paramName == null) {
				throw new IllegalStateException("No parameter name specified and not available in class file.");
			}
		}
		return paramName;
	}


	private static class MethodParameterMetadata {

		private final Class<?> type;

		private final String key;

		private final boolean required;


		MethodParameterMetadata(Class<?> type, String key, boolean required) {
			this.type = type;
			this.key = key;
			this.required = required;
		}

	}

}
