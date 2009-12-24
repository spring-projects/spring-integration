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

package org.springframework.integration.handler;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.support.AopUtils;
import org.springframework.context.expression.MapAccessor;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.ConversionServiceFactory;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;
import org.springframework.integration.annotation.Header;
import org.springframework.integration.annotation.Headers;
import org.springframework.integration.annotation.Payload;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessagingException;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.util.ClassUtils;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.ReflectionUtils.MethodCallback;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class MethodInvokingMessageProcessor implements MessageProcessor {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final Object targetObject;

	private volatile String displayString;

	private volatile boolean requiresReply;

	private final Map<Class<?>, HandlerMethod> handlerMethods;

	private final EvaluationContext evaluationContext;


	public MethodInvokingMessageProcessor(Object targetObject, Method method) {
		this(targetObject, null, method);
	}

	public MethodInvokingMessageProcessor(Object targetObject, String methodName) {
		this(targetObject, null, methodName);
	}

	public MethodInvokingMessageProcessor(Object targetObject, String methodName, boolean requiresReply) {
		this(targetObject, null, methodName, requiresReply);
	}

	public MethodInvokingMessageProcessor(Object targetObject, Class<? extends Annotation> annotationType) {
		this(targetObject, annotationType, (String) null);
	}


	/*
	 * Private constructors for internal use
	 */

	private MethodInvokingMessageProcessor(Object targetObject, Class<? extends Annotation> annotationType, Method method) {
		Assert.notNull(method, "method must not be null");
		HandlerMethod handlerMethod = new HandlerMethod(method);
		Assert.notNull(targetObject, "targetObject must not be null");
		this.targetObject = targetObject;
		this.handlerMethods = Collections.<Class<?>, HandlerMethod>singletonMap(handlerMethod.getTargetParameterType(), handlerMethod);
		this.evaluationContext = this.createEvaluationContext(targetObject, method, annotationType);
		this.setDisplayString(targetObject, method);
	}

	private MethodInvokingMessageProcessor(Object targetObject, Class<? extends Annotation> annotationType, String methodName) {
		this(targetObject, annotationType, methodName, false);
	}

	private MethodInvokingMessageProcessor(Object targetObject, Class<? extends Annotation> annotationType, String methodName, boolean requiresReply) {
		Assert.notNull(targetObject, "targetObject must not be null");
		this.targetObject = targetObject;
		this.requiresReply = requiresReply;
		this.handlerMethods = this.findHandlerMethodsForTarget(targetObject, annotationType, methodName, requiresReply);
		this.evaluationContext = this.createEvaluationContext(targetObject, methodName, annotationType);
		this.setDisplayString(targetObject, methodName);
	}


	private void setDisplayString(Object targetObject, Object targetMethod) {
		StringBuilder sb = new StringBuilder(targetObject.getClass().getName());
		if (targetMethod instanceof Method) {
			sb.append("." + ((Method) targetMethod).getName());
		}
		else if (targetMethod instanceof String) {
			sb.append("." + (String) targetMethod);
		}
		this.displayString = sb.toString() + "]";
	}

	private EvaluationContext createEvaluationContext(Object targetObject, Object method, Class<? extends Annotation> annotationType) {
		StandardEvaluationContext context = new StandardEvaluationContext();
		// TODO: StandardEvaluationContext may soon provide a better way to *replace* the MethodResolver
		context.getMethodResolvers().clear();
		Class<?> targetType = AopUtils.getTargetClass(this.targetObject);
		if (method instanceof Method) {
			context.getMethodResolvers().add(new FilteringReflectiveMethodResolver(
					new FixedHandlerMethodFilter((Method) method), targetType));
		}
		else if (method == null || method instanceof String) {
			context.getMethodResolvers().add(new FilteringReflectiveMethodResolver(
					new HandlerMethodFilter(annotationType, (String) method, this.requiresReply), targetType));
		}
		context.addPropertyAccessor(new MapAccessor());
		// TODO: Enable configuration of an integration ConversionService bean to be used here,
		//       but then fallback to this same default if no such bean has been defined.
		ConversionService conversionService = ConversionServiceFactory.createDefaultConversionService();
		context.setTypeConverter(new StandardTypeConverter(conversionService));
		context.setVariable("target", targetObject);
		return context;
	}


	public String toString() {
		return this.displayString;
	}

	public Object processMessage(Message<?> message) {
		EvaluationException evaluationException = null;
		List<HandlerMethod> candidates = this.findHandlerMethodsForMessage(message);
		for (HandlerMethod candidate : candidates) {
			try {
				Object result = candidate.getExpression().getValue(this.evaluationContext, message);
				if (this.requiresReply) {
					// TODO: remove this if SpEL is modified to throw an EvaluationException instead
					// e.g. we can invoke getValue(this.evaluationContext, message, candidate.getReturnType);
					Assert.notNull(result, "Expression evaluation result was null, but this processor requires a reply.");
				}
				return result;
			}
			catch (EvaluationException e) {
				if (evaluationException == null) {
					// keep the first exception
					evaluationException = e;
				}
			}
		}
		if (evaluationException == null) {
			throw new MessageHandlingException(message, "Failed to find a suitable Message-handling " +
					"method on target of type [" + targetObject.getClass() + "].");
		}
		throw new MessageHandlingException(message, "Failed to process Message.", evaluationException);
	}

	private Map<Class<?>, HandlerMethod> findHandlerMethodsForTarget(final Object targetObject,
			final Class<? extends Annotation> annotationType, final String methodName, final boolean requiresReply) {

		final Map<Class<?>, HandlerMethod> candidateMethods = new HashMap<Class<?>, HandlerMethod>();
		final Map<Class<?>, HandlerMethod> fallbackMethods = new HashMap<Class<?>, HandlerMethod>();
		final AtomicReference<Class<?>> ambiguousFallbackType = new AtomicReference<Class<?>>();
		ReflectionUtils.doWithMethods(targetObject.getClass(), new MethodCallback() {
			public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
				boolean matchesAnnotation = false;
				if (method.isBridge()) {
					return;
				}
				if (isMethodDefinedOnObjectClass(method)) {
					return;
				}
				if (!Modifier.isPublic(method.getModifiers())) {
					return;
				}
				if (requiresReply && void.class.equals(method.getReturnType())) {
					return;
				}
				if (methodName != null && !methodName.equals(method.getName())) {
					return;
				}
				if (annotationType != null && AnnotationUtils.findAnnotation(method, annotationType) != null) {
					matchesAnnotation = true;
				}
				HandlerMethod handlerMethod = null;
				try {
					handlerMethod = new HandlerMethod(method);
				}
				catch (Exception e) {
					if (logger.isDebugEnabled()) {
						logger.debug("Method [" + method + "] is not eligible for Message handling.", e);
					}
					return;
				}
				Class<?> targetParameterType = handlerMethod.getTargetParameterType();
				if (matchesAnnotation || annotationType == null) {
					Assert.isTrue(!candidateMethods.containsKey(targetParameterType),
							"Found more than one method match for type [" + targetParameterType + "]");
					candidateMethods.put(targetParameterType, handlerMethod);
				}
				else {
					if (fallbackMethods.containsKey(targetParameterType)) {
						// we need to check for duplicate type matches,
						// but only if we end up falling back
						// and we'll only keep track of the first one
						ambiguousFallbackType.compareAndSet(null, targetParameterType);
					}
					fallbackMethods.put(targetParameterType, handlerMethod);
				}
			}
		});
		if (!candidateMethods.isEmpty()) {
			return candidateMethods;
		}
		Assert.notEmpty(fallbackMethods, "Target object [" + this.targetObject +
				"] has no eligible methods for handling Messages.");
		Assert.isNull(ambiguousFallbackType.get(),
				"Found more than one method match for type [" + ambiguousFallbackType + "]");
		return fallbackMethods;
	}

	private List<HandlerMethod> findHandlerMethodsForMessage(Message<?> message) {
		final Class<?> payloadType = message.getPayload().getClass();
		HandlerMethod closestMatch = this.findClosestMatch(payloadType);
		if (closestMatch != null) {
			return Collections.singletonList(closestMatch);
		}
		return new ArrayList<HandlerMethod>(this.handlerMethods.values());
	}

	private HandlerMethod findClosestMatch(Class<?> payloadType) {
		Set<Class<?>> candidates = this.handlerMethods.keySet();
		Class<?> match = null; 
		if (candidates != null && !candidates.isEmpty()) {
			match = ClassUtils.findClosestMatch(payloadType, candidates, true);
		}
		return (match != null) ? this.handlerMethods.get(match) : null;
	}

	private static boolean isMethodDefinedOnObjectClass(Method method) {
		if (method == null) {
			return false;
		}
		if (method.getDeclaringClass().equals(Object.class)) {
			return true;
		}
		if (ReflectionUtils.isEqualsMethod(method) ||
				ReflectionUtils.isHashCodeMethod(method) ||
				ReflectionUtils.isToStringMethod(method) ||
				AopUtils.isFinalizeMethod(method)) {
			return true;
		}
		return (method.getName().equals("clone") && method.getParameterTypes().length == 0);
	}


	/**
	 * Helper class for generating and exposing metadata for a candidate handler method.
	 * The metadata includes the SpEL expression and the expected payload type. 
	 */
	private static class HandlerMethod {

		private static final SpelExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();

		private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER =
				new LocalVariableTableParameterNameDiscoverer();


		private final Method method;

		private final Expression expression;

		private volatile Class<?> targetParameterType;


		HandlerMethod(Method method) {
			this.method = method;
			this.expression = this.generateExpression(method);
		}

		Expression getExpression() {
			return this.expression;
		}

		Class<?> getTargetParameterType() {
			return this.targetParameterType;
		}

		public String toString() {
			return this.method.toString();
		}

		private Expression generateExpression(Method method) {
			StringBuilder sb = new StringBuilder("#target." + method.getName() + "(");
			Class<?>[] parameterTypes = method.getParameterTypes();
			Annotation[][] parameterAnnotations = method.getParameterAnnotations();
			boolean hasUnqualifiedMapParameter = false;
			for (int i = 0; i < parameterTypes.length; i++) {
				if (i != 0) {
					sb.append(", ");
				}
				Class<?> parameterType = parameterTypes[i];
				Annotation mappingAnnotation = findMappingAnnotation(parameterAnnotations[i]);
				if (mappingAnnotation != null) {
					Class<? extends Annotation> annotationType = mappingAnnotation.annotationType();
					if (annotationType.equals(Payload.class)) {
						sb.append("payload");
						String qualifierExpression = ((Payload) mappingAnnotation).value();
						if (StringUtils.hasText(qualifierExpression)) {
							sb.append("." + qualifierExpression);
						}
						if (!StringUtils.hasText(qualifierExpression)) {
							this.setExclusiveTargetParameterType(parameterType);
						}
					}
					else if (annotationType.equals(Headers.class)) {
						Assert.isTrue(Map.class.isAssignableFrom(parameterType),
								"The @Headers annotation can only be applied to a Map-typed parameter.");
						sb.append("headers");
					}
					else if (annotationType.equals(Header.class)) {
						Header headerAnnotation = (Header) mappingAnnotation;
						String headerName = this.determineHeaderName(headerAnnotation, new MethodParameter(method, i));
						String headerExpression = "headers." + headerName;
						if (headerAnnotation.required()) {
							sb.append(headerExpression);
						}
						else {
							sb.append("headers[" + headerName + "] != null ? " + headerExpression + " : null");
						}
					}
				}
				else if (Message.class.isAssignableFrom(parameterType)) {
					sb.append("#root");
					this.setExclusiveTargetParameterType(Message.class);
				}
				else if (Map.class.isAssignableFrom(parameterType)) {
					if (Properties.class.isAssignableFrom(parameterType)) {
						sb.append("payload instanceof T(java.util.Map) or " +
								"(payload instanceof T(String) and payload.contains('=')) ? payload : headers");
					}
					else {
						sb.append("(payload instanceof T(java.util.Map) ? payload : headers)");
					}
					Assert.isTrue(!hasUnqualifiedMapParameter,
							"Found more than one Map typed parameter without any qualification. " +
							"Consider using @Payload or @Headers on at least one of the parameters.");
					hasUnqualifiedMapParameter = true;
				}
				else {
					sb.append("payload");
					this.setExclusiveTargetParameterType(parameterType);
				}
			}
			if (hasUnqualifiedMapParameter) {
				if (targetParameterType != null && Map.class.isAssignableFrom(this.targetParameterType)) {
					throw new IllegalArgumentException(
							"Unable to determine payload matching parameter due to ambiguous Map typed parameters. " +
							"Consider adding the @Payload and or @Headers annotations as appropriate.");
				}
			}
			sb.append(")");
			if (this.targetParameterType == null) {
				this.targetParameterType = Message.class;
			}
			return EXPRESSION_PARSER.parseExpression(sb.toString());
		}

		private Annotation findMappingAnnotation(Annotation[] annotations) {
			if (annotations == null || annotations.length == 0) {
				return null;
			}
			Annotation match = null;
			for (Annotation annotation : annotations) {
				Class<? extends Annotation> type = annotation.annotationType();
				if (type.equals(Payload.class) || type.equals(Header.class) || type.equals(Headers.class)) {
					if (match != null) {
						throw new MessagingException("At most one parameter annotation can be provided for message mapping, " +
								"but found two: [" + match.annotationType().getName() + "] and [" + annotation.annotationType().getName() + "]");
					}
					match = annotation;
				}
			}
			return match;
		}

		private String determineHeaderName(Header headerAnnotation, MethodParameter methodParameter) {
			methodParameter.initParameterNameDiscovery(PARAMETER_NAME_DISCOVERER);
			String valueAttribute = headerAnnotation.value();
			String headerName = StringUtils.hasText(valueAttribute) ? valueAttribute : methodParameter.getParameterName();
			Assert.notNull(headerName, "Cannot determine header name. Possible reasons: -debug is " +
					"disabled or header name is not explicitly provided via @Header annotation.");
			return headerName;
		}

		private synchronized void setExclusiveTargetParameterType(Class<?> targetParameterType) {
			Assert.isNull(this.targetParameterType, "Found more than one parameter type candidate: [" +
					this.targetParameterType + "] and [" + targetParameterType + "]");
			this.targetParameterType = targetParameterType;
		}
	}

}
