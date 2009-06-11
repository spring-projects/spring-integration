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

package org.springframework.integration.message;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.springframework.core.GenericTypeResolver;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.integration.annotation.Header;
import org.springframework.integration.annotation.Headers;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Prepares arguments for handler methods. The method parameters are matched
 * against the Message payload as well as its headers. If a method parameter is
 * annotated with {@link Header @Header}, the annotation's value will be used as
 * a header name. If such an annotation contains no value, then the parameter
 * name will be used as long as the information is available in the class file
 * (requires compilation with debug settings for parameter names). If the
 * {@link Header @Header} annotation is not present, then the parameter will
 * typically match the Message payload. However, if a Map or Properties object
 * is expected, and the paylaod is not itself assignable to that type, then the
 * MessageHeaders' values will be passed in the case of a Map-typed parameter,
 * or the MessageHeaders' String-based values will be passed in the case of a
 * Properties-typed parameter.
 * 
 * @author Mark Fisher
 * @author Iwein Fuld
 */
public class MethodParameterMessageMapper implements InboundMessageMapper<Object[]>, OutboundMessageMapper<Object[]> {

	private final Method method;

	private final MethodParameterMetadata[] parameterMetadata;

	private final MethodParameterMetadata payloadParameterMetadata;

	private final ParameterNameDiscoverer parameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();

	public MethodParameterMessageMapper(Method method) {
		Assert.notNull(method, "method must not be null");
		this.method = method;
		parameterMetadata = this.initializeParameterMetadata();
		payloadParameterMetadata = getPayloadParameterFrom(parameterMetadata);
	}

	private MethodParameterMetadata getPayloadParameterFrom(MethodParameterMetadata[] parameterMetadata) {
		Set<MethodParameterMetadata> payloadCandidates = new HashSet<MethodParameterMetadata>();
		int messagesFound = 0;
		for (MethodParameterMetadata metadata : parameterMetadata) {
			if (metadata.getHeaderAnnotation() == null && !metadata.hasHeadersAnnotation()
					&& !Message.class.isAssignableFrom(metadata.getParameterType())) {
				payloadCandidates.add(metadata);
			} else if (Message.class.isAssignableFrom(metadata.getParameterType())){
				messagesFound++;
			}
		}
		if (payloadCandidates.size() > 1) {
			Iterator<MethodParameterMetadata> iterator = payloadCandidates.iterator();
			while (iterator.hasNext()) {
				Class<?> type = iterator.next().getParameterType();
				if (Map.class.isAssignableFrom(type) || Properties.class.isAssignableFrom(type)) {
					iterator.remove();
				}
			}
		}
		Assert.isTrue(payloadCandidates.size()+messagesFound <= 1, "Could not find at most one message or payload parameter among ["
				+ parameterMetadata + "] ended up with the candidates [" + payloadCandidates + "]");
		MethodParameterMetadata methodParameterMetadata = payloadCandidates.toArray(new MethodParameterMetadata[1])[0];
		return methodParameterMetadata;
	}

	private MethodParameterMetadata[] initializeParameterMetadata() {
		Class<?>[] paramTypes = this.method.getParameterTypes();
		MethodParameterMetadata[] parameterMetadata = new MethodParameterMetadata[paramTypes.length];
		for (int i = 0; i < paramTypes.length; i++) {
			MethodParameterMetadata metadata = new MethodParameterMetadata(this.method, i);
			metadata.initParameterNameDiscovery(this.parameterNameDiscoverer);
			GenericTypeResolver.resolveParameterType(metadata, this.method.getDeclaringClass());
			parameterMetadata[i] = metadata;
		}
		return parameterMetadata;
	}

	public Message<?> toMessage(Object[] parameters) {
		Assert.isTrue(!ObjectUtils.isEmpty(parameters), "parameter array is required");
		Assert.isTrue(parameters.length == this.parameterMetadata.length, "wrong number of parameters: expected "
				+ this.parameterMetadata.length + ", received " + parameters.length);
		Message<?> message = null;
		Object payload = null;
		Map<String, Object> headers = new HashMap<String, Object>();
		for (int i = 0; i < parameters.length; i++) {
			Object value = parameters[i];
			MethodParameterMetadata metadata = this.parameterMetadata[i];
			Header headerAnnotation = metadata.getHeaderAnnotation();
			if (metadata == payloadParameterMetadata) {
				Assert.notNull(value, "payload object must not be null");
				payload = value;
			}
			else if (headerAnnotation != null) {
				String headerName = metadata.getHeaderName();
				boolean required = headerAnnotation.required();
				if (value != null) {
					headers.put(headerName, value);
				}
				else {
					Assert.isTrue(!required, "header '" + headerName + "' is required");
				}
			}
			else if (metadata.hasHeadersAnnotation()) {
				if (value != null) {
					this.addHeadersAnnotatedParameterToMap(value, headers);
				}
			}
			else if (metadata.getParameterType().equals(Message.class)) {
				message = (Message<?>) value;
			}
			else {
				Assert.isTrue(metadata.isMapOrProperties());
				this.addHeadersAnnotatedParameterToMap(value, headers);
			}
		}
		if (message != null) {
			if (headers.isEmpty()) {
				return message;
			}
			return MessageBuilder.fromMessage(message).copyHeadersIfAbsent(headers).build();
		}
		Assert.notNull(payload, "no parameter available for Message or payload");
		return MessageBuilder.withPayload(payload).copyHeaders(headers).build();
	}

	public Object[] fromMessage(Message<?> message) {
		if (message == null) {
			return null;
		}
		Assert.notNull(message.getPayload(), "Message payload must not be null.");
		Object[] args = new Object[this.parameterMetadata.length];
		for (int i = 0; i < this.parameterMetadata.length; i++) {
			MethodParameterMetadata metadata = this.parameterMetadata[i];
			Class<?> expectedType = metadata.getParameterType();
			Header headerAnnotation = metadata.getHeaderAnnotation();
			if (metadata == payloadParameterMetadata) {
				args[i] = message.getPayload();
			}
			else if (headerAnnotation != null) {
				String headerName = metadata.getHeaderName();
				Object value = message.getHeaders().get(headerName);
				if (value == null && headerAnnotation.required()) {
					throw new MessageHandlingException(message, "required header '" + headerName + "' not available");
				}
				args[i] = value;
			}
			else if (metadata.isMapOrProperties()) {
				if (Properties.class.isAssignableFrom(expectedType)) {
					args[i] = this.getStringTypedHeaders(message);
				}
				else {
					args[i] = message.getHeaders();
				}
			}
			else {
				Assert.isTrue(expectedType.isAssignableFrom(message.getClass())
						&& Message.class.isAssignableFrom(expectedType),
						"Argument is neither header or payload, so it should be of type message.");
				args[i] = message;
			}
		}
		return args;
	}

	private Properties getStringTypedHeaders(Message<?> message) {
		Properties properties = new Properties();
		MessageHeaders headers = message.getHeaders();
		for (String key : headers.keySet()) {
			Object value = headers.get(key);
			if (value instanceof String) {
				properties.setProperty(key, (String) value);
			}
		}
		return properties;
	}

	@SuppressWarnings("unchecked")
	private void addHeadersAnnotatedParameterToMap(Object value, Map<String, Object> headers) {
		Map map = (Map) value;
		for (Iterator iter = map.entrySet().iterator(); iter.hasNext();) {
			Map.Entry entry = (Map.Entry) iter.next();
			Assert.isTrue(entry.getKey() instanceof String, "Map annotated with @Headers must have String-typed keys");
			headers.put((String) entry.getKey(), entry.getValue());
		}
	}

	private static class MethodParameterMetadata extends MethodParameter {

		private volatile Header _headerAnnotation;

		private volatile boolean _hasHeadersAnnotation;

		private MethodParameterMetadata(Method method, int index) {
			super(method, index);
			for (Object o : this.getParameterAnnotations()) {
				if (o instanceof Header) {
					this._headerAnnotation = (Header) o;
				}
				else if (Headers.class.isInstance(o)) {
					Assert.isAssignable(Map.class, this.getParameterType(),
							"parameter with the @Headers annotation must be assignable to java.util.Map");
					this._hasHeadersAnnotation = true;
				}
			}
		}

		Header getHeaderAnnotation() {
			return this._headerAnnotation;
		}

		boolean hasHeadersAnnotation() {
			return this._hasHeadersAnnotation;
		}

		boolean isMapOrProperties() {
			if (Properties.class.isAssignableFrom(this.getParameterType())
					|| Map.class.isAssignableFrom(this.getParameterType())) {
				return true;
			}
			return false;
		}

		String getHeaderName() {
			if (this.getHeaderAnnotation() == null) {
				return null;
			}
			String paramName = this.getHeaderAnnotation().value();
			if (!StringUtils.hasText(paramName)) {
				paramName = this.getParameterName();
				Assert.state(paramName != null,
						"No parameter name specified on @Header and unable to discover in class file.");
			}
			return paramName;
		}
	}
}
