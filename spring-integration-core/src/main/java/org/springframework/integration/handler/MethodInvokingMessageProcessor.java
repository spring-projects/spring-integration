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

package org.springframework.integration.handler;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.MessagingException;
import org.springframework.integration.annotation.Header;
import org.springframework.integration.annotation.Headers;
import org.springframework.integration.annotation.Payload;
import org.springframework.integration.util.ClassUtils;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.ReflectionUtils.MethodCallback;
import org.springframework.util.ReflectionUtils.MethodFilter;

/**
 * A MessageProcessor implementation that invokes a method on a target Object.
 * The Method instance or method name may be provided as a constructor argument.
 * If a method name is provided, and more than one declared method has that name,
 * the method-selection will be dynamic, based on the underlying SpEL method
 * resolution. Alternatively, an annotation type may be provided so that the
 * candidates for SpEL's method resolution are determined by the presence of that
 * annotation rather than the method name.
 * 
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class MethodInvokingMessageProcessor extends AbstractMessageProcessor {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final Object targetObject;

	private volatile String displayString;

	private volatile boolean requiresReply;

	private final Map<Class<?>, HandlerMethod> handlerMethods;


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
		this.prepareEvaluationContext(this.getEvaluationContext(), method, annotationType);
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
		this.prepareEvaluationContext(this.getEvaluationContext(), methodName, annotationType);
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

	private void prepareEvaluationContext(StandardEvaluationContext context, Object method, Class<? extends Annotation> annotationType) {
		Class<?> targetType = AopUtils.getTargetClass(this.targetObject);
		if (method instanceof Method) {
			context.registerMethodFilter(targetType, new FixedHandlerMethodFilter((Method) method));
		}
		else if (method == null || method instanceof String) {
			context.registerMethodFilter(targetType,
					new HandlerMethodFilter(annotationType, (String) method, this.requiresReply));
		}
		context.addPropertyAccessor(new MapAccessor());
		context.setVariable("target", targetObject);
	}


	public String toString() {
		return this.displayString;
	}

	public Object processMessage(Message<?> message) {
		Throwable evaluationException = null;
		List<HandlerMethod> candidates = this.findHandlerMethodsForMessage(message);
		for (HandlerMethod candidate : candidates) {
			try {
				Expression expression = candidate.getExpression();
				Class<?> expectedType = candidate.method.getReturnType();
				Object result = this.evaluateExpression(expression, message, expectedType);
				if (this.requiresReply) {
					Assert.notNull(result, "Expression evaluation result was null, but this processor requires a reply.");
				}
				return result;
			}
			catch (MessageHandlingException e) {
				if (evaluationException == null) {
					// keep the first exception
					evaluationException = e.getCause();
				}
			}
		}
		throw new MessageHandlingException(message, "Failed to process Message.", evaluationException);
	}

	private Map<Class<?>, HandlerMethod> findHandlerMethodsForTarget(final Object targetObject,
			final Class<? extends Annotation> annotationType, final String methodName, final boolean requiresReply) {

		final Map<Class<?>, HandlerMethod> candidateMethods = new HashMap<Class<?>, HandlerMethod>();
		final Map<Class<?>, HandlerMethod> fallbackMethods = new HashMap<Class<?>, HandlerMethod>();
		final AtomicReference<Class<?>> ambiguousFallbackType = new AtomicReference<Class<?>>();
		final Class<?> targetClass = this.getTargetClass(targetObject);
		MethodFilter methodFilter = new UniqueMethodFilter(targetClass);
		ReflectionUtils.doWithMethods(targetClass, new MethodCallback() {
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
		}, methodFilter);
		if (!candidateMethods.isEmpty()) {
			return candidateMethods;
		}
		Assert.notEmpty(fallbackMethods, "Target object of type [" + this.targetObject.getClass() +
				"] has no eligible methods for handling Messages.");
		Assert.isNull(ambiguousFallbackType.get(),
				"Found more than one method match for type [" + ambiguousFallbackType + "]");
		return fallbackMethods;
	}

	private Class<?> getTargetClass(Object targetObject) {
		Class<?> targetClass = targetObject.getClass();
		if (AopUtils.isAopProxy(targetObject)) {
			targetClass = AopUtils.getTargetClass(targetObject);
		}
		else if(AopUtils.isCglibProxyClass(targetClass)) {
			Class<?> superClass = targetObject.getClass().getSuperclass();
			if (!Object.class.equals(superClass)) {
				targetClass = superClass;
			}
		}
		return targetClass;
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
						sb.append(this.determineHeaderExpression(headerAnnotation, new MethodParameter(method, i)));
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

		private String determineHeaderExpression(Header headerAnnotation, MethodParameter methodParameter) {
			methodParameter.initParameterNameDiscovery(PARAMETER_NAME_DISCOVERER);
			String headerName = null;
			String relativeExpression = "";
			String valueAttribute = headerAnnotation.value();
			if (!StringUtils.hasText(valueAttribute)) {
				headerName = methodParameter.getParameterName();
			}
			else if (valueAttribute.indexOf('.') != -1) {
				String tokens[] = valueAttribute.split("\\.", 2);
				headerName = tokens[0];
				if (StringUtils.hasText(tokens[1])) {
					relativeExpression = "." + tokens[1];
				}
			}
			else {
				headerName = valueAttribute;
			}
			Assert.notNull(headerName, "Cannot determine header name. Possible reasons: -debug is " +
					"disabled or header name is not explicitly provided via @Header annotation.");
			String headerExpression = "headers." + headerName + relativeExpression;
			return (headerAnnotation.required()) ? headerExpression
					: "headers['" + headerName + "'] != null ? " + headerExpression + " : null";
		}

		private synchronized void setExclusiveTargetParameterType(Class<?> targetParameterType) {
			Assert.isNull(this.targetParameterType, "Found more than one parameter type candidate: [" +
					this.targetParameterType + "] and [" + targetParameterType + "]");
			this.targetParameterType = targetParameterType;
		}
	}


	/**
	 * @author Oleg Zhurakousky
	 * @since 2.0
	 */
	private static class UniqueMethodFilter implements MethodFilter {

		private List<Method> uniqueMethods = new ArrayList<Method>();

		public UniqueMethodFilter(Class<?> targetClass) {
			ArrayList<Method> allMethods = new ArrayList<Method>(Arrays.asList(targetClass.getMethods()));
			for (Method method : allMethods) {
				uniqueMethods.add(org.springframework.util.ClassUtils.getMostSpecificMethod(method, targetClass));
			}
		}

		public boolean matches(Method method) {
			return uniqueMethods.contains(method);
		}
	}

}
