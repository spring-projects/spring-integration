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

package org.springframework.integration.message;

import java.lang.annotation.Annotation;
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
 * against the Message, its payload as well as its headers. A message or
 * payload parameter must not be annotated, and there can be at most one of
 * these. In certain special cases, more than one non-annotated parameter can
 * be used (more on this later), but there should always be at most one
 * parameter that is expecting the message or its payload.
 * <p/>
 * If a method parameter is annotated with {@link Header @Header}, the
 * annotation's value will be used as a header name. If such an annotation
 * contains no value, then the parameter name will be used as long as the
 * information is available in the class file (requires compilation with debug
 * settings for parameter names).
 * <p/>
 * In addition a Map or Properties parameter can receive multiple message
 * headers. In the case of a Map argument, all headers will be passed, but in
 * the case of a Properties argument, only the headers with String-typed values
 * will be passed. These parameters can be labeled explicitly with the
 * {@link Headers @Headers} annotation, or matched implicitly by using a non-
 * ambiguous method signature. There can be as many parameters annotated with
 * @Header as necessary, but typically there should be only one parameter
 * expecting multiple headers (with or without the @Headers annotation).
 * <p/>
 * If a Map or Properties object is expected, and the payload is not itself
 * assignable to that type or capable of being converted to that type, then
 * the MessageHeaders' values will be passed in the case of a Map-typed
 * parameter, or the MessageHeaders' String-based values will be passed in the
 * case of a Properties-typed parameter. In these cases multiple unannotated
 * parameters are legal. If, however, the actual payload type is a Map or
 * Properties instance, then this ambiguity cannot be resolved. For that
 * reason, it is highly recommended to use the explicit
 * {@link Headers @Headers} annotation whenever possible.
 * <p/>
 * Some examples of legal method signatures:<br/>
 * <tt>public void dealWith(Object payload);</tt><br/>
 * <tt>public void dealWith(Message message);</tt><br/>
 * <tt>public void dealWith(@Header String myHeader, Object payload);</tt><br/>
 * <tt>public void dealWith(@Header String myHeader, @Header String anotherHeader);</tt>
 * <br/>
 * <tt>public void dealWith(@Headers Map headers, Object payload);</tt><br/>
 * <tt>public void dealWith(@Headers Properties headers, Map payload);</tt><br/>
 * <tt>public void dealWith(Properties headers, Object payload);</tt><br/>
 * <p/>
 * Some examples of illegal method signatures: <br/>
 * <tt>public void dealWith(Object payload, String payload);</tt><br/>
 * <tt>public void dealWith(Message message, Object payload);</tt><br/>
 * <tt>public void dealWith(Properties headers, Map payload);</tt><br/>
 * 
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
		this.parameterMetadata = this.initializeParameterMetadata();
		this.payloadParameterMetadata = getPayloadParameterFrom(this.parameterMetadata);
	}


	private MethodParameterMetadata getPayloadParameterFrom(MethodParameterMetadata[] mpm) {
		Set<MethodParameterMetadata> payloadCandidates = new HashSet<MethodParameterMetadata>();
		int messageTypedParameterCount = 0;
		for (MethodParameterMetadata metadata : mpm) {
			if (Message.class.isAssignableFrom(metadata.getParameterType())) {
				// expecting Message, not a payload candidate
				messageTypedParameterCount++;
			}
			else if (metadata.getHeaderAnnotation() == null && !metadata.hasHeadersAnnotation()) {
				// not expecting Message, and not explicitly annotated for headers
				payloadCandidates.add(metadata);
			}
		}
		if (payloadCandidates.size() > 1) {
			Iterator<MethodParameterMetadata> iterator = payloadCandidates.iterator();
			while (iterator.hasNext()) {
				Class<?> type = iterator.next().getParameterType();
				if (Map.class.isAssignableFrom(type)) {
					// Map (or Properties) may accept headers rather than payload
					iterator.remove();
				}
			}
		}
		if (payloadCandidates.size() + messageTypedParameterCount > 1) {
			// too many candidates, create a helpful error message
			int count = 0;
			String[] candidateTypes = new String[payloadCandidates.size()];
			for (MethodParameterMetadata candidate : payloadCandidates) {
				candidateTypes[count++] = candidate.getParameterType().getName();
			}
			throw new IllegalArgumentException("At most one message or payload parameter " +
					"is allowed on handler method [" + this.method.getName() +
					"], but the following payload candidate types were found [" +
					StringUtils.arrayToCommaDelimitedString(candidateTypes) +
					"] and " + messageTypedParameterCount + " Message type(s).");
		}
		return (payloadCandidates.isEmpty() ? null : payloadCandidates.iterator().next());
	}

	private MethodParameterMetadata[] initializeParameterMetadata() {
		Class<?>[] paramTypes = this.method.getParameterTypes();
		MethodParameterMetadata[] parameterMetadata = new MethodParameterMetadata[paramTypes.length];
		for (int i = 0; i < paramTypes.length; i++) {
			MethodParameterMetadata metadata = new MethodParameterMetadata(this.method, i);
			metadata.initParameterNameDiscovery(this.parameterNameDiscoverer);
			GenericTypeResolver.resolveParameterType(metadata.parameter, this.method.getDeclaringClass());
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
			else if (metadata.hasHeadersAnnotation() || metadata.isMapOrProperties()) {
				if (value != null) {
					this.addHeadersAnnotatedParameterToMap(value, headers);
				}
			}
			else {
				Assert.isTrue(Message.class.isAssignableFrom(metadata.getParameterType()));
				message = (Message<?>) value;
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
			if (metadata == this.payloadParameterMetadata) {
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

	private static class MethodParameterMetadata {

		private final MethodParameter parameter;

		private volatile Header _headerAnnotation;

		private volatile boolean _hasHeadersAnnotation;

		private MethodParameterMetadata(Method method, int index) {
			this.parameter = new MethodParameter(method, index);
			Annotation[] annotations = this.parameter.getParameterAnnotations();
			for (Object o : annotations) {
				if (o instanceof Header) {
					this._headerAnnotation = (Header) o;
				}
				else if (Headers.class.isInstance(o)) {
					Assert.isAssignable(Map.class, this.parameter.getParameterType(),
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
			if (Properties.class.isAssignableFrom(this.parameter.getParameterType())
					|| Map.class.isAssignableFrom(this.parameter.getParameterType())) {
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
				paramName = this.parameter.getParameterName();
				Assert.state(paramName != null,
						"No parameter name specified on @Header and unable to discover in class file.");
			}
			return paramName;
		}

		private void initParameterNameDiscovery(ParameterNameDiscoverer parameterNameDiscoverer) {
			this.parameter.initParameterNameDiscovery(parameterNameDiscoverer);
		}

		private Class<?> getParameterType() {
			return this.parameter.getParameterType();
		}
	}

}
