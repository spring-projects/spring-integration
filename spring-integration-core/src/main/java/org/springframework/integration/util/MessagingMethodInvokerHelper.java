/*
 * Copyright 2002-2017 the original author or authors.
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
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.Lifecycle;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.integration.annotation.Payloads;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.handler.support.CollectionArgumentResolver;
import org.springframework.integration.handler.support.PayloadExpressionArgumentResolver;
import org.springframework.integration.handler.support.PayloadsArgumentResolver;
import org.springframework.integration.support.MutableMessage;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodFilter;
import org.springframework.util.StringUtils;

/**
 * A helper class for processors that invoke a method on a target Object using
 * a combination of message payload(s) and headers as arguments.
 * The Method instance or method name may be provided as a constructor argument.
 * If a method name is provided, and more than one declared method has that name,
 * the method-selection will be dynamic, based on the underlying SpEL method resolution.
 * Alternatively, an annotation type may be provided so that the candidates for SpEL's
 * method resolution are determined by the presence of that annotation rather than the method name.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Dave Syer
 * @author Gunnar Hillert
 * @author Soby Chacko
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class MessagingMethodInvokerHelper<T> extends AbstractExpressionEvaluator implements Lifecycle {

	private static final String CANDIDATE_METHODS = "CANDIDATE_METHODS";

	private static final String CANDIDATE_MESSAGE_METHODS = "CANDIDATE_MESSAGE_METHODS";

	private static final Log logger = LogFactory.getLog(MessagingMethodInvokerHelper.class);

	private final DefaultMessageHandlerMethodFactory messageHandlerMethodFactory =
			new DefaultMessageHandlerMethodFactory();

	private final Object targetObject;

	private volatile String displayString;

	private final Map<Class<?>, HandlerMethod> handlerMethods;

	private final Map<Class<?>, HandlerMethod> handlerMessageMethods;

	private final List<Map<Class<?>, HandlerMethod>> handlerMethodsList;

	private final HandlerMethod handlerMethod;

	private final TypeDescriptor expectedType;

	private final boolean canProcessMessageList;

	private Class<? extends Annotation> annotationType;

	private volatile boolean initialized;

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


	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		super.setBeanFactory(beanFactory);
		this.messageHandlerMethodFactory.setBeanFactory(beanFactory);
	}

	public T process(Message<?> message) throws Exception {
		ParametersWrapper parameters = new ParametersWrapper(message);
		return processInternal(parameters);
	}

	public T process(Collection<Message<?>> messages, Map<String, Object> headers) throws Exception {
		ParametersWrapper parameters = new ParametersWrapper(messages, headers);
		return processInternal(parameters);
	}

	@Override
	public String toString() {
		return this.displayString;
	}

	@Override
	public void start() {
		if (this.targetObject instanceof Lifecycle) {
			((Lifecycle) this.targetObject).start();
		}

		if (!this.initialized) {
			PayloadExpressionArgumentResolver payloadExpressionArgumentResolver =
					new PayloadExpressionArgumentResolver();
			payloadExpressionArgumentResolver.setBeanFactory(getBeanFactory());

			PayloadsArgumentResolver payloadsArgumentResolver = new PayloadsArgumentResolver();
			payloadsArgumentResolver.setBeanFactory(getBeanFactory());

			CollectionArgumentResolver collectionArgumentResolver =
					new CollectionArgumentResolver(this.canProcessMessageList);
			collectionArgumentResolver.setBeanFactory(getBeanFactory());

			List<HandlerMethodArgumentResolver> customArgumentResolvers = new LinkedList<>();
			customArgumentResolvers.add(payloadExpressionArgumentResolver);
			customArgumentResolvers.add(payloadsArgumentResolver);
			customArgumentResolvers.add(collectionArgumentResolver);

			this.messageHandlerMethodFactory.setCustomArgumentResolvers(customArgumentResolvers);
			this.messageHandlerMethodFactory.afterPropertiesSet();
			this.initialized = true;
		}
	}

	@Override
	public void stop() {
		if (this.targetObject instanceof Lifecycle) {
			((Lifecycle) this.targetObject).stop();
		}
	}

	@Override
	public boolean isRunning() {
		return !(this.targetObject instanceof Lifecycle) || ((Lifecycle) this.targetObject).isRunning();
	}

	/*
	 * Private constructors for internal use
	 */

	private MessagingMethodInvokerHelper(Object targetObject, Class<? extends Annotation> annotationType,
			Method method, Class<?> expectedType, boolean canProcessMessageList) {
		this.annotationType = annotationType;
		this.canProcessMessageList = canProcessMessageList;
		Assert.notNull(method, "method must not be null");

		if (expectedType != null) {
			Assert.isTrue(method.getReturnType() != Void.class && method.getReturnType() != Void.TYPE,
					"method must have a return type");
			this.expectedType = TypeDescriptor.valueOf(expectedType);
		}
		else {
			this.expectedType = null;
		}

		Assert.notNull(targetObject, "targetObject must not be null");
		this.targetObject = targetObject;
		try {
			InvocableHandlerMethod invocableHandlerMethod =
					this.messageHandlerMethodFactory.createInvocableHandlerMethod(targetObject, method);
			this.handlerMethod = new HandlerMethod(invocableHandlerMethod, canProcessMessageList);
		}
		catch (IneligibleMethodException e) {
			throw new IllegalArgumentException(e);
		}
		this.handlerMethods = null;
		this.handlerMessageMethods = null;
		this.handlerMethodsList = null;
		this.setDisplayString(targetObject, method);
	}

	private MessagingMethodInvokerHelper(Object targetObject, Class<? extends Annotation> annotationType,
			String methodName, Class<?> expectedType, boolean canProcessMessageList) {
		this.annotationType = annotationType;
		this.canProcessMessageList = canProcessMessageList;
		Assert.notNull(targetObject, "targetObject must not be null");
		if (expectedType != null) {
			this.expectedType = TypeDescriptor.valueOf(expectedType);
		}
		else {
			this.expectedType = null;
		}
		this.targetObject = targetObject;
		Map<String, Map<Class<?>, HandlerMethod>> handlerMethodsForTarget =
				findHandlerMethodsForTarget(targetObject, annotationType, methodName, expectedType != null);
		Map<Class<?>, HandlerMethod> handlerMethods = handlerMethodsForTarget.get(CANDIDATE_METHODS);
		Map<Class<?>, HandlerMethod> handlerMessageMethods = handlerMethodsForTarget.get(CANDIDATE_MESSAGE_METHODS);
		if ((handlerMethods.size() == 1 && handlerMessageMethods.isEmpty()) ||
				(handlerMessageMethods.size() == 1 && handlerMethods.isEmpty())) {
			if (handlerMethods.size() == 1) {
				this.handlerMethod = handlerMethods.values().iterator().next();
			}
			else {
				this.handlerMethod = handlerMessageMethods.values().iterator().next();
			}
			this.handlerMethods = null;
			this.handlerMessageMethods = null;
			this.handlerMethodsList = null;
		}
		else {
			this.handlerMethod = null;
			this.handlerMethods = handlerMethods;
			this.handlerMessageMethods = handlerMessageMethods;
			this.handlerMethodsList = new LinkedList<>();

			//TODO Consider to use global option to determine a precedence of methods
			this.handlerMethodsList.add(this.handlerMethods);
			this.handlerMethodsList.add(this.handlerMessageMethods);
		}
		setDisplayString(targetObject, methodName);
	}

	private void setDisplayString(Object targetObject, Object targetMethod) {
		StringBuilder sb = new StringBuilder(targetObject.getClass().getName());
		if (targetMethod instanceof Method) {
			sb.append(".")
					.append(((Method) targetMethod).getName());
		}
		else if (targetMethod instanceof String) {
			sb.append(".")
					.append(targetMethod);
		}
		this.displayString = sb.toString() + "]";
	}

	@SuppressWarnings("unchecked")
	private T processInternal(ParametersWrapper parameters) throws Exception {
		HandlerMethod candidate = this.findHandlerMethodForParameters(parameters);
		Assert.notNull(candidate, "No candidate methods found for messages.");

		T result = candidate.invoke(parameters);
		if (result != null && this.expectedType != null) {
			return (T) getEvaluationContext(true)
					.getTypeConverter()
					.convertValue(result, TypeDescriptor.forObject(result), this.expectedType);
		}
		else {
			return result;
		}
	}

	private Map<String, Map<Class<?>, HandlerMethod>> findHandlerMethodsForTarget(final Object targetObject,
			final Class<? extends Annotation> annotationType, final String methodName, final boolean requiresReply) {

		Map<String, Map<Class<?>, HandlerMethod>> handlerMethods = new HashMap<>();

		final Map<Class<?>, HandlerMethod> candidateMethods = new HashMap<>();
		final Map<Class<?>, HandlerMethod> candidateMessageMethods = new HashMap<>();
		final Map<Class<?>, HandlerMethod> fallbackMethods = new HashMap<>();
		final Map<Class<?>, HandlerMethod> fallbackMessageMethods = new HashMap<>();
		final AtomicReference<Class<?>> ambiguousFallbackType = new AtomicReference<>();
		final AtomicReference<Class<?>> ambiguousFallbackMessageGenericType = new AtomicReference<>();
		final Class<?> targetClass = getTargetClass(targetObject);
		MethodFilter methodFilter = new UniqueMethodFilter(targetClass);
		ReflectionUtils.doWithMethods(targetClass, method1 -> {
			boolean matchesAnnotation = false;
			if (method1.isBridge()) {
				return;
			}
			if (isMethodDefinedOnObjectClass(method1)) {
				return;
			}
			if (method1.getDeclaringClass().equals(Proxy.class)) {
				return;
			}
			if (annotationType != null && AnnotationUtils.findAnnotation(method1, annotationType) != null) {
				matchesAnnotation = true;
			}
			else if (!Modifier.isPublic(method1.getModifiers())) {
				return;
			}
			if (requiresReply && void.class.equals(method1.getReturnType())) {
				return;
			}
			if (methodName != null && !methodName.equals(method1.getName())) {
				return;
			}
			if (methodName == null
					&& ObjectUtils.containsElement(new String[] { "start", "stop", "isRunning" }, method1.getName())) {
				return;
			}
			HandlerMethod handlerMethod1;
			try {
				InvocableHandlerMethod invocableHandlerMethod =
						this.messageHandlerMethodFactory.createInvocableHandlerMethod(targetObject, method1);
				handlerMethod1 = new HandlerMethod(invocableHandlerMethod, this.canProcessMessageList);
			}
			catch (IneligibleMethodException e) {
				if (logger.isDebugEnabled()) {
					logger.debug("Method [" + method1 + "] is not eligible for Message handling "
							+ e.getMessage() + ".");
				}
				return;
			}
			catch (Exception e) {
				if (logger.isDebugEnabled()) {
					logger.debug("Method [" + method1 + "] is not eligible for Message handling.", e);
				}
				return;
			}
			Class<?> targetParameterType = handlerMethod1.getTargetParameterType();
			if (matchesAnnotation || annotationType == null) {
				if (handlerMethod1.isMessageMethod()) {
					if (candidateMessageMethods.containsKey(targetParameterType)) {
						throw new IllegalArgumentException("Found more than one method match for type " +
								"[Message<" + targetParameterType + ">]");
					}
					candidateMessageMethods.put(targetParameterType, handlerMethod1);
				}
				else {
					if (candidateMethods.containsKey(targetParameterType)) {
						String exceptionMessage = "Found more than one method match for ";
						if (Void.class.equals(targetParameterType)) {
							exceptionMessage += "empty parameter for 'payload'";
						}
						else {
							exceptionMessage += "type [" + targetParameterType + "]";
						}
						throw new IllegalArgumentException(exceptionMessage);
					}
					candidateMethods.put(targetParameterType, handlerMethod1);
				}
			}
			else {
				if (handlerMethod1.isMessageMethod()) {
					if (fallbackMessageMethods.containsKey(targetParameterType)) {
						// we need to check for duplicate type matches,
						// but only if we end up falling back
						// and we'll only keep track of the first one
						ambiguousFallbackMessageGenericType.compareAndSet(null, targetParameterType);
					}
					fallbackMessageMethods.put(targetParameterType, handlerMethod1);
				}
				else {
					if (fallbackMethods.containsKey(targetParameterType)) {
						// we need to check for duplicate type matches,
						// but only if we end up falling back
						// and we'll only keep track of the first one
						ambiguousFallbackType.compareAndSet(null, targetParameterType);
					}
					fallbackMethods.put(targetParameterType, handlerMethod1);
				}
			}
		}, methodFilter);

		if (candidateMethods.isEmpty() && candidateMessageMethods.isEmpty() && fallbackMethods.isEmpty()
				&& fallbackMessageMethods.isEmpty()) {
			findSingleSpecifMethodOnInterfacesIfProxy(targetObject, methodName, candidateMessageMethods,
					candidateMethods);
		}

		if (!candidateMethods.isEmpty() || !candidateMessageMethods.isEmpty()) {
			handlerMethods.put(CANDIDATE_METHODS, candidateMethods);
			handlerMethods.put(CANDIDATE_MESSAGE_METHODS, candidateMessageMethods);
			return handlerMethods;
		}
		if ((ambiguousFallbackType.get() != null
				|| ambiguousFallbackMessageGenericType.get() != null)
				&& ServiceActivator.class.equals(annotationType)) {
			/*
			 * When there are ambiguous fallback methods,
			 * a Service Activator can finally fallback to RequestReplyExchanger.exchange(m).
			 * Ambiguous means > 1 method that takes the same payload type, or > 1 method
			 * that takes a Message with the same generic type.
			 */
			List<Method> frameworkMethods = new ArrayList<>();
			Class<?>[] allInterfaces = org.springframework.util.ClassUtils.getAllInterfacesForClass(targetClass);
			for (Class<?> iface : allInterfaces) {
				try {
					if ("org.springframework.integration.gateway.RequestReplyExchanger".equals(iface.getName())) {
						frameworkMethods.add(targetClass.getMethod("exchange", Message.class));
						if (logger.isDebugEnabled()) {
							logger.debug(targetObject.getClass() +
									": Ambiguous fallback methods; using RequestReplyExchanger.exchange()");
						}
					}
				}
				catch (Exception e) {
					// should never happen (but would fall through to errors below)
				}
			}
			if (frameworkMethods.size() == 1) {
				InvocableHandlerMethod invocableHandlerMethod =
						this.messageHandlerMethodFactory.createInvocableHandlerMethod(targetObject,
								frameworkMethods.get(0));
				HandlerMethod handlerMethod = new HandlerMethod(invocableHandlerMethod, this.canProcessMessageList);
				handlerMethods.put(CANDIDATE_METHODS, Collections.singletonMap(Object.class, handlerMethod));
				handlerMethods.put(CANDIDATE_MESSAGE_METHODS, candidateMessageMethods);
				return handlerMethods;
			}
		}

		Assert.state(!fallbackMethods.isEmpty() || !fallbackMessageMethods.isEmpty(),
				"Target object of type [" + this.targetObject.getClass() +
						"] has no eligible methods for handling Messages.");

		Assert.isNull(ambiguousFallbackType.get(), "Found ambiguous parameter type [" + ambiguousFallbackType
				+ "] for method match: " + fallbackMethods.values());
		Assert.isNull(ambiguousFallbackMessageGenericType.get(),
				"Found ambiguous parameter type ["
						+ ambiguousFallbackMessageGenericType
						+ "] for method match: "
						+ fallbackMethods.values());

		handlerMethods.put(CANDIDATE_METHODS, fallbackMethods);
		handlerMethods.put(CANDIDATE_MESSAGE_METHODS, fallbackMessageMethods);
		return handlerMethods;
	}

	private void findSingleSpecifMethodOnInterfacesIfProxy(final Object targetObject, final String methodName,
			Map<Class<?>, HandlerMethod> candidateMessageMethods,
			Map<Class<?>, HandlerMethod> candidateMethods) {
		if (AopUtils.isAopProxy(targetObject)) {
			final AtomicReference<Method> targetMethod = new AtomicReference<>();
			Class<?>[] interfaces = ((Advised) targetObject).getProxiedInterfaces();
			for (Class<?> clazz : interfaces) {
				ReflectionUtils.doWithMethods(clazz, method1 -> {
					if (targetMethod.get() != null) {
						throw new IllegalStateException("Ambiguous method " + methodName + " on " + targetObject);
					}
					else {
						targetMethod.set(method1);
					}
				}, method12 -> method12.getName().equals(methodName));
			}
			Method method = targetMethod.get();
			if (method != null) {
				InvocableHandlerMethod invocableHandlerMethod =
						this.messageHandlerMethodFactory.createInvocableHandlerMethod(targetObject, method);
				HandlerMethod handlerMethod = new HandlerMethod(invocableHandlerMethod, this.canProcessMessageList);
				Class<?> targetParameterType = handlerMethod.getTargetParameterType();
				if (handlerMethod.isMessageMethod()) {
					if (candidateMessageMethods.containsKey(targetParameterType)) {
						throw new IllegalArgumentException("Found more than one method match for type " +
								"[Message<" + targetParameterType + ">]");
					}
					candidateMessageMethods.put(targetParameterType, handlerMethod);
				}
				else {
					if (candidateMethods.containsKey(targetParameterType)) {
						String exceptionMessage = "Found more than one method match for ";
						if (Void.class.equals(targetParameterType)) {
							exceptionMessage += "empty parameter for 'payload'";
						}
						else {
							exceptionMessage += "type [" + targetParameterType + "]";
						}
						throw new IllegalArgumentException(exceptionMessage);
					}
					candidateMethods.put(targetParameterType, handlerMethod);
				}
			}
		}
	}

	private Class<?> getTargetClass(Object targetObject) {
		Class<?> targetClass = targetObject.getClass();
		if (AopUtils.isAopProxy(targetObject)) {
			targetClass = AopUtils.getTargetClass(targetObject);
			if (targetClass == targetObject.getClass()) {
				try {
					// Maybe a proxy with no target - e.g. gateway
					Class<?>[] interfaces = ((Advised) targetObject).getProxiedInterfaces();
					if (interfaces != null && interfaces.length == 1) {
						targetClass = interfaces[0];
					}
				}
				catch (Exception e) {
					if (logger.isDebugEnabled()) {
						logger.debug("Exception trying to extract interface", e);
					}
				}
			}
		}
		else if (org.springframework.util.ClassUtils.isCglibProxyClass(targetClass)
				|| targetClass.getSimpleName().contains("$MockitoMock$")) {
			Class<?> superClass = targetObject.getClass().getSuperclass();
			if (!Object.class.equals(superClass)) {
				targetClass = superClass;
			}
		}
		return targetClass;
	}

	private HandlerMethod findHandlerMethodForParameters(ParametersWrapper parameters) {
		if (this.handlerMethod != null) {
			return this.handlerMethod;
		}

		final Class<?> payloadType = parameters.getFirstParameterType();

		HandlerMethod closestMatch = this.findClosestMatch(payloadType);
		if (closestMatch != null) {
			return closestMatch;

		}

		if (Iterable.class.isAssignableFrom(payloadType) && this.handlerMethods.containsKey(Iterator.class)) {
			return this.handlerMethods.get(Iterator.class);
		}
		else {
			return this.handlerMethods.get(Void.class);
		}

	}

	private HandlerMethod findClosestMatch(Class<?> payloadType) {
		for (Map<Class<?>, HandlerMethod> handlerMethods : this.handlerMethodsList) {
			Set<Class<?>> candidates = handlerMethods.keySet();
			Class<?> match = null;
			if (!CollectionUtils.isEmpty(candidates)) {
				match = ClassUtils.findClosestMatch(payloadType, candidates, true);
			}
			if (match != null) {
				return handlerMethods.get(match);
			}
		}
		return null;
	}

	private static boolean isMethodDefinedOnObjectClass(Method method) {
		return method != null &&
				(method.getDeclaringClass().equals(Object.class) || ReflectionUtils.isEqualsMethod(method) ||
						ReflectionUtils.isHashCodeMethod(method) || ReflectionUtils.isToStringMethod(method) ||
						AopUtils.isFinalizeMethod(method) || (method.getName().equals("clone")
						&& method.getParameterTypes().length == 0));
	}

	/**
	 * Helper class for generating and exposing metadata for a candidate handler method. The metadata includes the SpEL
	 * expression and the expected payload type.
	 */
	private static class HandlerMethod {

		private static final TypeDescriptor messageTypeDescriptor = TypeDescriptor.valueOf(Message.class);

		private static final TypeDescriptor messageListTypeDescriptor = new TypeDescriptor(
				ReflectionUtils.findField(HandlerMethod.class, "dummyMessages"));

		private static final TypeDescriptor messageArrayTypeDescriptor = TypeDescriptor.valueOf(Message[].class);

		@SuppressWarnings("unused")
		private static final Collection<Message<?>> dummyMessages = Collections.emptyList();


		private final InvocableHandlerMethod invocableHandlerMethod;

		private final boolean canProcessMessageList;

		private volatile TypeDescriptor targetParameterTypeDescriptor;

		private volatile Class<?> targetParameterType = Void.class;

		private volatile boolean messageMethod;

		HandlerMethod(InvocableHandlerMethod invocableHandlerMethod, boolean canProcessMessageList) {
			this.invocableHandlerMethod = invocableHandlerMethod;
			this.canProcessMessageList = canProcessMessageList;
			analyzeMethod();
		}

		@SuppressWarnings("unchecked")
		public <T> T invoke(ParametersWrapper parameters) throws Exception {
			Message<?> message = parameters.getMessage();
			if (this.canProcessMessageList) {
				message = new MutableMessage<>(parameters.getMessages(), parameters.getHeaders());
			}
			return (T) this.invocableHandlerMethod.invoke(message, parameters.getHeaders());
		}

		Class<?> getTargetParameterType() {
			return this.targetParameterType;
		}

		private boolean isMessageMethod() {
			return this.messageMethod;
		}

		@Override
		public String toString() {
			return this.invocableHandlerMethod.toString();
		}

		private void analyzeMethod() {

			MethodParameter[] methodParameters = this.invocableHandlerMethod.getMethodParameters();

			boolean hasUnqualifiedMapParameter = false;

			for (MethodParameter methodParameter : methodParameters) {
				TypeDescriptor parameterTypeDescriptor = new TypeDescriptor(methodParameter);
				Class<?> parameterType = parameterTypeDescriptor.getObjectType();
				Annotation mappingAnnotation =
						MessagingAnnotationUtils.findMessagePartAnnotation(methodParameter.getParameterAnnotations(),
								true);
				if (mappingAnnotation != null) {
					Class<? extends Annotation> annotationType = mappingAnnotation.annotationType();
					if (annotationType.equals(Payload.class)) {
						String qualifierExpression = (String) AnnotationUtils.getValue(mappingAnnotation);
						if (!StringUtils.hasText(qualifierExpression)) {
							setExclusiveTargetParameterType(parameterTypeDescriptor, methodParameter);
						}
					}
					else if (annotationType.equals(Payloads.class)) {
						Assert.isTrue(canProcessMessageList,
								"The @Payloads annotation can only be applied if method handler canProcessMessageList.");
						Assert.isTrue(Collection.class.isAssignableFrom(parameterType),
								"The @Payloads annotation can only be applied to a Collection-typed parameter.");
						String qualifierExpression = ((Payloads) mappingAnnotation).value();
						if (!StringUtils.hasText(qualifierExpression)) {
							setExclusiveTargetParameterType(parameterTypeDescriptor, methodParameter);
						}
					}
					else if (annotationType.equals(Headers.class)) {
						Assert.isTrue(Map.class.isAssignableFrom(parameterType),
								"The @Headers annotation can only be applied to a Map-typed parameter.");
					}
				}
				else if (parameterTypeDescriptor.isAssignableTo(messageTypeDescriptor)) {
					this.messageMethod = true;
					setExclusiveTargetParameterType(parameterTypeDescriptor, methodParameter);
				}
				else if (this.canProcessMessageList &&
						(parameterTypeDescriptor.isAssignableTo(messageListTypeDescriptor)
								|| parameterTypeDescriptor.isAssignableTo(messageArrayTypeDescriptor))) {
					setExclusiveTargetParameterType(parameterTypeDescriptor, methodParameter);
				}
				else if (Collection.class.isAssignableFrom(parameterType) || parameterType.isArray()) {
					setExclusiveTargetParameterType(parameterTypeDescriptor, methodParameter);
				}
				else if (Iterator.class.isAssignableFrom(parameterType)) {
					setExclusiveTargetParameterType(parameterTypeDescriptor, methodParameter);
				}
				else if (Map.class.isAssignableFrom(parameterType)) {
					Assert.isTrue(!hasUnqualifiedMapParameter,
							"Found more than one Map typed parameter without any qualification. "
									+ "Consider using @Payload or @Headers on at least one of the parameters.");
					hasUnqualifiedMapParameter = true;
				}
				else {
					setExclusiveTargetParameterType(parameterTypeDescriptor, methodParameter);
				}
			}
			if (hasUnqualifiedMapParameter) {
				if (this.targetParameterType != null && Map.class.isAssignableFrom(this.targetParameterType)) {
					throw new IllegalArgumentException(
							"Unable to determine payload matching parameter due to ambiguous Map typed parameters. "
									+ "Consider adding the @Payload and or @Headers annotations as appropriate.");
				}
			}
			if (this.targetParameterTypeDescriptor == null) {
				this.targetParameterTypeDescriptor = TypeDescriptor.valueOf(Void.class);
			}
		}

		private void setExclusiveTargetParameterType(TypeDescriptor targetParameterType,
				MethodParameter methodParameter) {
			if (this.targetParameterTypeDescriptor != null) {
				throw new IneligibleMethodException("Found more than one parameter type candidate: [" +
						this.targetParameterTypeDescriptor + "] and [" + targetParameterType + "]");
			}
			this.targetParameterTypeDescriptor = targetParameterType;
			if (Message.class.isAssignableFrom(targetParameterType.getObjectType())) {
				methodParameter.increaseNestingLevel();
				this.targetParameterType = methodParameter.getNestedParameterType();
				methodParameter.decreaseNestingLevel();
			}
			else {
				this.targetParameterType = targetParameterType.getObjectType();
			}
		}
	}

	private static class ParametersWrapper {

		private final Object payload;

		private final Collection<Message<?>> messages;

		private final Map<String, Object> headers;

		private final Message<?> message;

		ParametersWrapper(Message<?> message) {
			this.message = message;
			this.payload = message.getPayload();
			this.headers = message.getHeaders();
			this.messages = null;
		}

		ParametersWrapper(Collection<Message<?>> messages, Map<String, Object> headers) {
			this.payload = null;
			this.messages = messages;
			this.headers = headers;
			this.message = null;
		}

		public Object getPayload() {
			Assert.state(this.payload != null,
					"Invalid method parameter for payload: was expecting collection.");
			return this.payload;
		}

		public Collection<Message<?>> getMessages() {
			Assert.state(this.messages != null,
					"Invalid method parameter for messages: was expecting a single payload.");
			return this.messages;
		}

		public Map<String, Object> getHeaders() {
			return this.headers;
		}

		public Message<?> getMessage() {
			return this.message;
		}

		public Class<?> getFirstParameterType() {
			if (this.payload != null) {
				return this.payload.getClass();
			}
			return this.messages.getClass();
		}

	}

	@SuppressWarnings("serial")
	private static final class IneligibleMethodException extends RuntimeException {

		IneligibleMethodException(String message) {
			super(message);
		}

	}

}
