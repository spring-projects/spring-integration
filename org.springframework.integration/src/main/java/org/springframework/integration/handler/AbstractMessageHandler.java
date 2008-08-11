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

package org.springframework.integration.handler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.handler.annotation.Header;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.MessageHeaders;
import org.springframework.integration.util.DefaultMethodInvoker;
import org.springframework.integration.util.MethodInvoker;
import org.springframework.integration.util.NameResolvingMethodInvoker;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * A base class for any {@link MessageHandler} that may act as an adapter
 * by invoking a "plain" (not Message-aware) method for a given target object.
 * When used as an adapter, the target Object is mandatory and either a
 * {@link Method} reference or a 'methodName' must be provided. If no Object
 * and Method are provided, the handler will simply process the request
 * Message's payload.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractMessageHandler implements MessageHandler, InitializingBean {

	private static final Log logger = LogFactory.getLog(AbstractMessageHandler.class);

	private volatile boolean methodExpectsMessage;

	private volatile Object object;

	private volatile Method method;

	private volatile String methodName;

	private volatile MethodInvoker invoker;

	private volatile MethodParameterMetadata[] parameterMetadata;

	private final ParameterNameDiscoverer parameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();

	private volatile boolean initialized;

	private final Object initializationMonitor = new Object();


	public AbstractMessageHandler(Object object, Method method) {
		Assert.notNull(object, "object must not be null");
		Assert.notNull(method, "method must not be null");
		this.object = object;
		this.method = method;
		this.methodName = method.getName();
	}

	public AbstractMessageHandler(Object object, String methodName) {
		Assert.notNull(object, "object must not be null");
		Assert.notNull(methodName, "methodName must not be null");
		this.object = object;
		this.methodName = methodName;
	}

	public AbstractMessageHandler() {
	}


	public void setObject(Object object) {
		this.object = object;
	}

	public void setMethod(Method method) {
		Assert.notNull(method, "method must not be null");
		this.method = method;
		this.methodName = method.getName();
	}

	public void setMethodName(String methodName) {
		Assert.notNull(methodName, "methodName must not be null");
		if (this.method != null && !this.method.getName().equals(methodName)) {
			this.method = null;
		}
		this.methodName = methodName;
	}

	public void afterPropertiesSet() {
		synchronized (this.initializationMonitor) {
			if (this.initialized) {
				return;
			}
			if (this.object != null) {
				if (this.method == null) {
					if (this.methodName == null) {
						throw new IllegalStateException("either the 'method' or 'methodName' must be provided when the 'object' property has been set");
					}
					final List<Method> candidates = new ArrayList<Method>();
					ReflectionUtils.doWithMethods(this.object.getClass(), new ReflectionUtils.MethodCallback() {
						public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
							if (method.getName().equals(AbstractMessageHandler.this.methodName)) {
								candidates.add(method);
							}
						}
					});
					if (candidates.size() == 0) {
						throw new ConfigurationException("no such method '" + this.methodName
								+ "' on target class [" + this.object.getClass() + "]"); 
					}
					if (candidates.size() == 1) {
						this.method = candidates.get(0);
					}
				}
				if (this.method != null) {
					this.invoker = new DefaultMethodInvoker(this.object, this.method);
				}
				else {
					// TODO: resolve the candidate method and/or create a dynamic resolver
					this.invoker = new NameResolvingMethodInvoker(this.object, this.methodName);
				}
				this.configureParameterMetadata();
			}
			this.initialized = true;
		}
	}

	private void configureParameterMetadata() {
		if (this.method == null) {
			return;
		}
		Class<?>[] paramTypes = this.method.getParameterTypes();			
		this.parameterMetadata = new MethodParameterMetadata[paramTypes.length];
		for (int i = 0; i < parameterMetadata.length; i++) {
			MethodParameter methodParam = new MethodParameter(this.method, i);
			methodParam.initParameterNameDiscovery(this.parameterNameDiscoverer);
			GenericTypeResolver.resolveParameterType(methodParam, this.method.getDeclaringClass());
			Object[] paramAnnotations = methodParam.getParameterAnnotations();
			String headerName = null;
			for (int j = 0; j < paramAnnotations.length; j++) {
				if (Header.class.isInstance(paramAnnotations[j])) {
					Header headerAnnotation = (Header) paramAnnotations[j];
					headerName = this.resolveParameterNameIfNecessary(headerAnnotation.value(), methodParam);
					parameterMetadata[i] = new MethodParameterMetadata(Header.class, headerName, headerAnnotation.required());
				}
			}
			if (headerName == null) {
				parameterMetadata[i] = new MethodParameterMetadata(methodParam.getParameterType(), null, false);
			}
		}
	}

	public Message<?> handle(Message<?> message) {
		if (message == null || message.getPayload() == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("message handler received a null message");
			}
			return null;
		}
		if (!this.initialized) {
			this.afterPropertiesSet();
		}
		Object result = (this.invoker != null) ? this.invokeHandlerMethod(message) : message.getPayload();
		if (result == null) {
			return null;
		}
		return this.createReplyMessage(result, message.getHeaders());
	}

	/**
	 * Subclasses must implement this method to generate the reply Message.
	 * 
	 * @param result the return value from an adapter method, or the Message payload if not acting as an adapter
	 * @param requestHeaders the MessageHeaders of the original request Message
	 * @return the Message to be sent to the reply MessageTarget
	 */
	protected abstract Message<?> createReplyMessage(Object result, MessageHeaders requestHeaders);


	private Object invokeHandlerMethod(Message<?> message) {
		if (this.invoker == null) {
			throw new IllegalStateException("cannot invoke method, invoker is null");
		}
		Object args[] = null;
		Object mappingResult = this.methodExpectsMessage ? message
				: this.mapMessageToMethodArguments(message);
		if (mappingResult != null && mappingResult.getClass().isArray()
				&& (Object.class.isAssignableFrom(mappingResult.getClass().getComponentType()))) {
			args = (Object[]) mappingResult;
		}
		else {
			args = new Object[] { mappingResult }; 
		}
		try {
			Object result = null;
			try {
				result = this.invoker.invokeMethod(args);
			}
			catch (NoSuchMethodException e) {
				try {
					result = this.invoker.invokeMethod(args);
					this.methodExpectsMessage = true;
				}
				catch (NoSuchMethodException e2) {
					throw new MessageHandlingException(message, "unable to determine method match");
				}
			}
			if (result == null) {
				return null;
			}
			return result;
		}
		catch (InvocationTargetException e) {
			throw new MessageHandlingException(message, "Handler method '"
					+ this.methodName + "' threw an Exception.", e.getTargetException());
		}
		catch (Throwable e) {
			throw new MessageHandlingException(message, "Failed to invoke handler method '"
					+ this.methodName + "' with arguments: " + ObjectUtils.nullSafeToString(args), e);
		}
	}


	private Object[] mapMessageToMethodArguments(Message<?> message) {
		if (message == null) {
			return null;
		}
		if (message.getPayload() == null) {
			throw new IllegalArgumentException("Message payload must not be null.");
		}
		if (ObjectUtils.isEmpty(this.parameterMetadata)) {
			return new Object[] { message.getPayload() };
		}
		Object[] args = new Object[this.parameterMetadata.length];
		for (int i = 0; i < this.parameterMetadata.length; i++) {
			MethodParameterMetadata metadata = this.parameterMetadata[i];
			Class<?> expectedType = metadata.type;
			if (expectedType.equals(Header.class)) {
				Object value = message.getHeaders().get(metadata.key);
				if (value == null && metadata.required) {
					throw new MessageHandlingException(message,
							"required header '" + metadata.key + "' not available");
				}
				args[i] = value;
			}
			else if (expectedType.isAssignableFrom(message.getClass())) {
				args[i] = message;
			}
			else if (expectedType.isAssignableFrom(message.getPayload().getClass())) {
				args[i] = message.getPayload();
			}
			else if (expectedType.equals(Map.class)) {
				args[i] = message.getHeaders();
			}
			else if (expectedType.equals(Properties.class)) {
				args[i] = this.getStringTypedHeaders(message);
			}
			else {
				args[i] = message.getPayload();
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
