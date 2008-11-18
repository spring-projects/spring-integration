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

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.MethodParameterMessageMapper;
import org.springframework.integration.message.OutboundMessageMapper;
import org.springframework.integration.util.DefaultMethodInvoker;
import org.springframework.integration.util.MethodInvoker;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * A base or helper class for any Messaging component that acts as an adapter
 * by invoking a "plain" (not Message-aware) method on a given target object.
 * The target Object is mandatory, and either a {@link Method} reference, a
 * 'methodName', or an Annotation type must be provided.
 * 
 * @author Mark Fisher
 */
public class MessageMappingMethodInvoker implements MethodInvoker {

	protected static final Log logger = LogFactory.getLog(MessageMappingMethodInvoker.class);

	private final Set<Method> methodsExpectingMessage = new HashSet<Method>();

	private volatile Object object;

	private final HandlerMethodResolver methodResolver;

	private final Map<Method, OutboundMessageMapper<Object[]>> messageMappers =
			new HashMap<Method, OutboundMessageMapper<Object[]>>();

	private final Map<Method, MethodInvoker> invokers = new HashMap<Method, MethodInvoker>();


	public MessageMappingMethodInvoker(Object object, Method method) {
		Assert.notNull(object, "object must not be null");
		Assert.notNull(method, "method must not be null");
		this.object = object;
		this.methodResolver = new StaticHandlerMethodResolver(method);
	}

	public MessageMappingMethodInvoker(Object object, Class<? extends Annotation> annotationType) {
		Assert.notNull(object, "object must not be null");
		Assert.notNull(annotationType, "annotation type must not be null");
		this.object = object;
		this.methodResolver = this.createResolverForAnnotation(annotationType);
	}

	public MessageMappingMethodInvoker(Object object, String methodName) {
		Assert.notNull(object, "object must not be null");
		Assert.notNull(methodName, "methodName must not be null");
		this.object = object;
		this.methodResolver = this.createResolverForMethodName(methodName);
	}


	public Object invokeMethod(Object... args) {
		Assert.notEmpty(args, "argument array must not be empty");
		Assert.isTrue(args.length == 1 && args[0] != null && (args[0] instanceof Message),
				"Argument array must contain a single Message instance.");
		Message<?> message = (Message<?>) args[0];
		if (message.getPayload() == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("received null payload");
			}
			return null;
		}
		Method method = this.methodResolver.resolveHandlerMethod(message);
		args = this.createArgumentArrayFromMessage(method, message);
		try {
			return this.doInvokeMethod(method, args, message);
		}
		catch (InvocationTargetException e) {
			if (e.getCause() != null && e.getCause() instanceof RuntimeException) {
				throw (RuntimeException) e.getCause();
			}
			throw new MessageHandlingException(message,
					"method '" + method + "' threw an Exception.", e.getCause());
		}
		catch (Exception e) {
			if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}
			throw new MessageHandlingException(message, "Failed to invoke method '"
					+ method + "' with arguments: " + ObjectUtils.nullSafeToString(args), e);
		}
	}

	private Object doInvokeMethod(Method method, Object[] args, Message<?> message) throws Exception {
		Object result = null;
		MethodInvoker invoker = null;
		try {
			invoker = this.invokers.get(method);
			if (invoker == null) {
				invoker = new DefaultMethodInvoker(this.object, method);
				this.invokers.put(method, invoker);
			}
			result = invoker.invokeMethod(args);
		}
		catch (IllegalArgumentException e) {
			try {
				if (message != null) {
					result = invoker.invokeMethod(message);
					// TODO: set this on the invoker
					this.methodsExpectingMessage.add(method);
				}
			}
			catch (NoSuchMethodException e2) {
				throw new MessageHandlingException(message, "unable to resolve method for args: "
						+ StringUtils.arrayToCommaDelimitedString(args));
			}
		}
		return result;
	}

	private Object[] createArgumentArrayFromMessage(Method method, Message<?> message) {
		Object args[] = null;
		Object mappingResult = this.methodsExpectingMessage.contains(method)
				? message : this.resolveParameters(method, message);
		if (mappingResult != null && mappingResult.getClass().isArray()
				&& (Object.class.isAssignableFrom(mappingResult.getClass().getComponentType()))) {
			args = (Object[]) mappingResult;
		}
		else {
			args = new Object[] { mappingResult }; 
		}
		return args;
	}

	private Object[] resolveParameters(Method method, Message<?> message) {
		OutboundMessageMapper<Object[]> mapper = this.messageMappers.get(method);
		if (mapper == null) {
			mapper = new MethodParameterMessageMapper(method);
			this.messageMappers.put(method, mapper);
		}
		return mapper.fromMessage(message);
	}

	private HandlerMethodResolver createResolverForMethodName(String methodName) {
		List<Method> methodsWithName = new ArrayList<Method>();
		Method[] defaultCandidateMethods = HandlerMethodUtils.getCandidateHandlerMethods(this.object);
		for (Method method : defaultCandidateMethods) {
			if (method.getName().equals(methodName)) {
				methodsWithName.add(method);
			}
		}
		Assert.notEmpty(methodsWithName, "Failed to find any valid Message-handling methods named '"
				+ methodName + "' on target class [" + this.object.getClass() + "].");
		if (methodsWithName.size() == 1) {
			return new StaticHandlerMethodResolver(methodsWithName.get(0));
		}
		return new PayloadTypeMatchingHandlerMethodResolver(methodsWithName.toArray(new Method[methodsWithName.size()]));
	}

	private HandlerMethodResolver createResolverForAnnotation(Class<? extends Annotation> annotationType) {
		List<Method> methodsWithAnnotation = new ArrayList<Method>();
		Method[] defaultCandidateMethods = HandlerMethodUtils.getCandidateHandlerMethods(this.object);
		for (Method method : defaultCandidateMethods) {
			Annotation annotation = AnnotationUtils.getAnnotation(method, annotationType);
			if (annotation != null) {
				methodsWithAnnotation.add(method);
			}
		}
		Method[] candidateMethods = (methodsWithAnnotation.size() == 0) ? null
				: methodsWithAnnotation.toArray(new Method[methodsWithAnnotation.size()]);
		if (candidateMethods == null) {
			if (logger.isInfoEnabled()) {
				logger.info("Failed to find any valid Message-handling methods with annotation ["
						+ annotationType + "] on target class [" + this.object.getClass() + "]. "
						+ "Method-resolution will be applied to all eligible methods.");
			}
			candidateMethods = defaultCandidateMethods;
		}
		if (candidateMethods.length == 1) {
			return new StaticHandlerMethodResolver(candidateMethods[0]);
		}
		return new PayloadTypeMatchingHandlerMethodResolver(candidateMethods);
	}

}
