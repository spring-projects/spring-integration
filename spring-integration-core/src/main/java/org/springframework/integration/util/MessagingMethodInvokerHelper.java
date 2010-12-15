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

package org.springframework.integration.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.support.AopUtils;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.MessagingException;
import org.springframework.integration.annotation.Header;
import org.springframework.integration.annotation.Headers;
import org.springframework.integration.annotation.Payload;
import org.springframework.integration.annotation.Payloads;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodCallback;
import org.springframework.util.ReflectionUtils.MethodFilter;
import org.springframework.util.StringUtils;

/**
 * A helper class for processors that invoke a method on a target Object using a combination of message payload(s) and
 * headers as arguments. The Method instance or method name may be provided as a constructor argument. If a method name
 * is provided, and more than one declared method has that name, the method-selection will be dynamic, based on the
 * underlying SpEL method resolution. Alternatively, an annotation type may be provided so that the candidates for
 * SpEL's method resolution are determined by the presence of that annotation rather than the method name.
 * 
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Dave Syer
 * @since 2.0
 */
public class MessagingMethodInvokerHelper<T> extends AbstractExpressionEvaluator {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final Object targetObject;

	private volatile String displayString;

	private volatile boolean requiresReply;

	private final Map<Class<?>, HandlerMethod> handlerMethods;

	private final Class<?> expectedType;

	private final boolean canProcessMessageList;


	public MessagingMethodInvokerHelper(Object targetObject, Method method, Class<?> expectedType,
			boolean canProcessMessageList) {
		this(targetObject, null, method, expectedType, canProcessMessageList);
	}

	public MessagingMethodInvokerHelper(Object targetObject, Method method, boolean canProcessMessageList) {
		this(targetObject, method, null, canProcessMessageList);
	}

	public MessagingMethodInvokerHelper(Object targetObject, String methodName, Class<?> expectedType,
			boolean canProcessMessageList) {
		this(targetObject, null, methodName, expectedType, canProcessMessageList);
	}

	public MessagingMethodInvokerHelper(Object targetObject, String methodName, boolean canProcessMessageList) {
		this(targetObject, methodName, null, canProcessMessageList);
	}

	public MessagingMethodInvokerHelper(Object targetObject, Class<? extends Annotation> annotationType,
			boolean canProcessMessageList) {
		this(targetObject, annotationType, null, canProcessMessageList);
	}

	public MessagingMethodInvokerHelper(Object targetObject, Class<? extends Annotation> annotationType,
			Class<?> expectedType, boolean canProcessMessageList) {
		this(targetObject, annotationType, (String) null, expectedType, canProcessMessageList);
	}


	public T process(Message<?> message) throws Exception {
		ParametersWrapper parameters = new ParametersWrapper(message);
		return processInternal(parameters);
	}

	public T process(Collection<Message<?>> messages, Map<String, ?> headers) throws Exception {
		ParametersWrapper parameters = new ParametersWrapper(messages, headers);
		return processInternal(parameters);
	}

	public String toString() {
		return this.displayString;
	}

	/*
	 * Private constructors for internal use
	 */

	private MessagingMethodInvokerHelper(Object targetObject, Class<? extends Annotation> annotationType,
			Method method, Class<?> expectedType, boolean canProcessMessageList) {
		this.canProcessMessageList = canProcessMessageList;
		Assert.notNull(method, "method must not be null");
		this.expectedType = expectedType;
		this.requiresReply = expectedType != null;
		if (expectedType != null) {
			Assert.isTrue(method.getReturnType() != Void.class && method.getReturnType() != Void.TYPE,
					"method must have a return type");
		}
		HandlerMethod handlerMethod = new HandlerMethod(method, canProcessMessageList);
		Assert.notNull(targetObject, "targetObject must not be null");
		this.targetObject = targetObject;
		this.handlerMethods = Collections.<Class<?>, HandlerMethod> singletonMap(handlerMethod.getTargetParameterType()
				.getObjectType(), handlerMethod);
		this.prepareEvaluationContext(this.getEvaluationContext(), method, annotationType);
		this.setDisplayString(targetObject, method);
	}

	private MessagingMethodInvokerHelper(Object targetObject, Class<? extends Annotation> annotationType,
			String methodName, Class<?> expectedType, boolean canProcessMessageList) {
		this.canProcessMessageList = canProcessMessageList;
		Assert.notNull(targetObject, "targetObject must not be null");
		this.expectedType = expectedType;
		this.targetObject = targetObject;
		this.requiresReply = expectedType != null;
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

	private void prepareEvaluationContext(StandardEvaluationContext context, Object method,
			Class<? extends Annotation> annotationType) {
		Class<?> targetType = AopUtils.getTargetClass(this.targetObject);
		if (method instanceof Method) {
			context.registerMethodFilter(targetType, new FixedMethodFilter((Method) method));
			if (expectedType != null) {
				Assert.state(context.getTypeConverter().canConvert(TypeDescriptor.valueOf(((Method) method).getReturnType()), TypeDescriptor.valueOf(expectedType)),
						"Cannot convert to expected type (" + expectedType + ") from " + method);
			}
		}
		else if (method == null || method instanceof String) {
			AnnotatedMethodFilter filter = new AnnotatedMethodFilter(annotationType, (String) method,
					this.requiresReply);
			Assert.state(canReturnExpectedType(filter, targetType, context.getTypeConverter()),
					"Cannot convert to expected type (" + expectedType + ") from " + method);
			context.registerMethodFilter(targetType, filter);
		}
		context.setVariable("target", targetObject);
	}

	private boolean canReturnExpectedType(AnnotatedMethodFilter filter, Class<?> targetType, TypeConverter typeConverter) {
		if (expectedType == null) {
			return true;
		}
		List<Method> methods = filter.filter(Arrays.asList(ReflectionUtils.getAllDeclaredMethods(targetType)));
		for (Method method : methods) {
			if (typeConverter.canConvert(TypeDescriptor.valueOf(method.getReturnType()), TypeDescriptor.valueOf(expectedType))) {
				return true;
			}
		}
		return false;
	}

	private T processInternal(ParametersWrapper parameters) throws Exception {
		Throwable evaluationException = null;
		List<HandlerMethod> candidates = this.findHandlerMethodsForParameters(parameters);
		Assert.state(!candidates.isEmpty(), "No candidate methods found for messages.");
		for (HandlerMethod candidate : candidates) {
			try {
				Expression expression = candidate.getExpression();
				Class<?> expectedType = this.expectedType != null ? this.expectedType : candidate.method.getReturnType();
				@SuppressWarnings("unchecked")
				T result = (T) this.evaluateExpression(expression, parameters, expectedType);
				if (this.requiresReply) {
					Assert.notNull(result,
							"Expression evaluation result was null, but this processor requires a reply.");
				}
				return result;
			}
			// keep the first exception
			catch (EvaluationException e) {
				if (evaluationException == null) {
					evaluationException = e.getCause();
				}
				if (evaluationException == null) {
					evaluationException = e;
				}
			}
			catch (MessageHandlingException e) {
				if (evaluationException == null) {
					evaluationException = e.getCause();
				}
				if (evaluationException == null) {
					evaluationException = e;
				}
			}
			catch (Exception e) {
				if (evaluationException == null) {
					evaluationException = e;
				}
			}
		}
		if (evaluationException instanceof Exception) {
			throw (Exception) evaluationException;
		}
		else if (evaluationException instanceof Error) {
			throw (Error) evaluationException;
		}
		else {
			throw new IllegalStateException("Cannot process message", evaluationException);
		}
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
					handlerMethod = new HandlerMethod(method, canProcessMessageList);
				}
				catch (Exception e) {
					if (logger.isDebugEnabled()) {
						logger.debug("Method [" + method + "] is not eligible for Message handling.", e);
					}
					return;
				}
				Class<?> targetParameterType = handlerMethod.getTargetParameterType().getObjectType();
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
		if ((fallbackMethods.isEmpty() || ambiguousFallbackType.get() != null) && ServiceActivator.class.equals(annotationType)) {
			// a Service Activator can fallback to either MessageHandler.handleMessage(m) or RequestReplyExchanger.exchange(m)
			List<Method> frameworkMethods = new ArrayList<Method>();
			Class<?>[] allInterfaces = org.springframework.util.ClassUtils.getAllInterfacesForClass(targetClass);
			for (Class<?> iface : allInterfaces) {
				try {
					if ("org.springframework.integration.gateway.RequestReplyExchanger".equals(iface.getName())) {
						frameworkMethods.add(targetClass.getMethod("exchange", Message.class));
					}
					else if ("org.springframework.integration.core.MessageHandler".equals(iface.getName()) && !requiresReply) {
						frameworkMethods.add(targetClass.getMethod("handleMessage", Message.class));
					}
				}
				catch (Exception e) {
					// should never happen (but would fall through to errors below)
				}
			}
			if (frameworkMethods.size() == 1) {
				HandlerMethod handlerMethod = new HandlerMethod(frameworkMethods.get(0), canProcessMessageList);
				return Collections.<Class<?>, HandlerMethod>singletonMap(Object.class, handlerMethod);
			}
		}
		Assert.notEmpty(fallbackMethods, "Target object of type [" + this.targetObject.getClass()
				+ "] has no eligible methods for handling Messages.");
		Assert.isNull(ambiguousFallbackType.get(), "Found ambiguous parameter type [" + ambiguousFallbackType
				+ "] for method match: " + fallbackMethods.values());
		return fallbackMethods;
	}

	private Class<?> getTargetClass(Object targetObject) {
		Class<?> targetClass = targetObject.getClass();
		if (AopUtils.isAopProxy(targetObject)) {
			targetClass = AopUtils.getTargetClass(targetObject);
		}
		else if (AopUtils.isCglibProxyClass(targetClass)) {
			Class<?> superClass = targetObject.getClass().getSuperclass();
			if (!Object.class.equals(superClass)) {
				targetClass = superClass;
			}
		}
		return targetClass;
	}

	private List<HandlerMethod> findHandlerMethodsForParameters(ParametersWrapper parameters) {
		final Class<?> payloadType = parameters.getFirstParameterType();
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
		if (ReflectionUtils.isEqualsMethod(method) || ReflectionUtils.isHashCodeMethod(method)
				|| ReflectionUtils.isToStringMethod(method) || AopUtils.isFinalizeMethod(method)) {
			return true;
		}
		return (method.getName().equals("clone") && method.getParameterTypes().length == 0);
	}


	/**
	 * Helper class for generating and exposing metadata for a candidate handler method. The metadata includes the SpEL
	 * expression and the expected payload type.
	 */
	private static class HandlerMethod {

		private static final SpelExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();

		private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER = new LocalVariableTableParameterNameDiscoverer();

		private static final TypeDescriptor messageTypeDescriptor = TypeDescriptor.valueOf(Message.class);

		private static final TypeDescriptor messageListTypeDescriptor = new TypeDescriptor(
				ReflectionUtils.findField(HandlerMethod.class, "dummyMessages"));

		private static final TypeDescriptor messageArrayTypeDescriptor = TypeDescriptor.valueOf(Message[].class);

		@SuppressWarnings("unused")
		private static final List<Message<?>> dummyMessages = Collections.emptyList();


		private final Method method;

		private final Expression expression;

		private volatile TypeDescriptor targetParameterType;

		private final boolean canProcessMessageList;


		HandlerMethod(Method method, boolean canProcessMessageList) {
			this.method = method;
			this.canProcessMessageList = canProcessMessageList;
			this.expression = this.generateExpression(method);
		}


		Expression getExpression() {
			return this.expression;
		}

		TypeDescriptor getTargetParameterType() {
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
			TypeDescriptor defaultParameterTypeDescriptor = TypeDescriptor.valueOf(List.class);
			for (int i = 0; i < parameterTypes.length; i++) {
				if (i != 0) {
					sb.append(", ");
				}
				TypeDescriptor parameterTypeDescriptor = new TypeDescriptor(new MethodParameter(method, i));
				defaultParameterTypeDescriptor = parameterTypeDescriptor;
				Class<?> parameterType = parameterTypeDescriptor.getObjectType();
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
							this.setExclusiveTargetParameterType(parameterTypeDescriptor);
						}
					}
					if (annotationType.equals(Payloads.class)) {
						sb.append("messages.![payload");
						String qualifierExpression = ((Payloads) mappingAnnotation).value();
						if (StringUtils.hasText(qualifierExpression)) {
							sb.append("." + qualifierExpression);
						}
						sb.append("]");
						if (!StringUtils.hasText(qualifierExpression)) {
							this.setExclusiveTargetParameterType(parameterTypeDescriptor);
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
				else if (parameterTypeDescriptor != null
						&& parameterTypeDescriptor.isAssignableTo(messageTypeDescriptor)) {
					sb.append("message");
					this.setExclusiveTargetParameterType(parameterTypeDescriptor);
				}
				else if (parameterTypeDescriptor != null
						&& (parameterTypeDescriptor.isAssignableTo(messageListTypeDescriptor) || parameterTypeDescriptor
								.isAssignableTo(messageArrayTypeDescriptor))) {
					sb.append("messages");
					this.setExclusiveTargetParameterType(parameterTypeDescriptor);
				}
				else if (Collection.class.isAssignableFrom(parameterType) || parameterType.isArray()) {
					if (canProcessMessageList) {
						sb.append("messages.![payload]");
					}
					else {
						sb.append("payload");
					}
					this.setExclusiveTargetParameterType(parameterTypeDescriptor);
				}
				else if (Iterator.class.isAssignableFrom(parameterType)) {
					if (canProcessMessageList) {
						if (parameterTypeDescriptor.getElementType()!=null && Message.class.isAssignableFrom(parameterTypeDescriptor.getElementType())) {
							sb.append("messages.iterator()");
						}
						else {
							sb.append("messages.![payload].iterator()");
						}
					}
					else {
						sb.append("payload.iterator()");
					}
					this.setExclusiveTargetParameterType(parameterTypeDescriptor);
				}
				else if (Map.class.isAssignableFrom(parameterType)) {
					if (Properties.class.isAssignableFrom(parameterType)) {
						sb.append("payload instanceof T(java.util.Map) or "
								+ "(payload instanceof T(String) and payload.contains('=')) ? payload : headers");
					}
					else {
						sb.append("(payload instanceof T(java.util.Map) ? payload : headers)");
					}
					Assert.isTrue(!hasUnqualifiedMapParameter,
							"Found more than one Map typed parameter without any qualification. "
									+ "Consider using @Payload or @Headers on at least one of the parameters.");
					hasUnqualifiedMapParameter = true;
				}
				else {
					sb.append("payload");
					this.setExclusiveTargetParameterType(parameterTypeDescriptor);
				}
			}
			if (hasUnqualifiedMapParameter) {
				if (targetParameterType != null && Map.class.isAssignableFrom(this.targetParameterType.getObjectType())) {
					throw new IllegalArgumentException(
							"Unable to determine payload matching parameter due to ambiguous Map typed parameters. "
									+ "Consider adding the @Payload and or @Headers annotations as appropriate.");
				}
			}
			sb.append(")");
			if (this.targetParameterType == null) {
				this.targetParameterType = defaultParameterTypeDescriptor;
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
				if (type.equals(Payload.class) || type.equals(Payloads.class) || type.equals(Header.class) || type.equals(Headers.class)) {
					if (match != null) {
						throw new MessagingException(
								"At most one parameter annotation can be provided for message mapping, "
										+ "but found two: [" + match.annotationType().getName() + "] and ["
										+ annotation.annotationType().getName() + "]");
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
			Assert.notNull(headerName, "Cannot determine header name. Possible reasons: -debug is "
					+ "disabled or header name is not explicitly provided via @Header annotation.");
			String headerRetrievalExpression = "headers['" + headerName + "']";
			String fullHeaderExpression = headerRetrievalExpression + relativeExpression;
			String fallbackExpression = (headerAnnotation.required())
					? "T(org.springframework.util.Assert).isTrue(false, 'required header not available:  " + headerName + "')"
					: "null";
			return headerRetrievalExpression + " != null ? " + fullHeaderExpression + " : " + fallbackExpression;
		}

		private synchronized void setExclusiveTargetParameterType(TypeDescriptor targetParameterType) {
			Assert.isNull(this.targetParameterType, "Found more than one parameter type candidate: ["
					+ this.targetParameterType + "] and [" + targetParameterType + "]");
			this.targetParameterType = targetParameterType;
		}
	}

	@SuppressWarnings("unused")
	private static class ParametersWrapper {

		private final Object payload;

		private final Collection<Message<?>> messages;

		private final Map<String, ?> headers;

		private final Message<?> message;

		public ParametersWrapper(Message<?> message) {
			this.message = message;
			this.payload = message.getPayload();
			this.headers = message.getHeaders();
			this.messages = null;
		}

		public ParametersWrapper(Collection<Message<?>> messages, Map<String, ?> headers) {
			this.payload = null;
			this.messages = messages;
			this.headers = headers;
			this.message = null;
		}

		public Object getPayload() {
			Assert.state(payload != null, "Invalid method parameter for payload: was expecting collection.");
			return payload;
		}

		public Collection<Message<?>> getMessages() {
			Assert.state(messages != null, "Invalid method parameter for messages: was expecting a single payload.");
			return messages;
		}

		public Map<String, ?> getHeaders() {
			return headers;
		}

		public Message<?> getMessage() {
			return message;
		}

		public Class<?> getFirstParameterType() {
			if (payload != null) {
				return payload.getClass();
			}
			return Collection.class;
		}

	}

}
