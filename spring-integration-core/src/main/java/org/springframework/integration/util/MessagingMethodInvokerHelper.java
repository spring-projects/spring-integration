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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.Lifecycle;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.spel.SpelCompilerMode;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.annotation.Default;
import org.springframework.integration.annotation.Payloads;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.UseSpelInvoker;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.handler.support.CollectionArgumentResolver;
import org.springframework.integration.handler.support.MapArgumentResolver;
import org.springframework.integration.handler.support.PayloadExpressionArgumentResolver;
import org.springframework.integration.handler.support.PayloadsArgumentResolver;
import org.springframework.integration.support.MutableMessage;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;
import org.springframework.messaging.handler.invocation.MethodArgumentResolutionException;
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

	// Number of times to try an InvocableHandlerMethod before giving up in favor of an expression.
	private static final int FAILED_ATTEMPTS_THRESHOLD = 100;

	private static final ExpressionParser EXPRESSION_PARSER_DEFAULT = EXPRESSION_PARSER;

	private static final ExpressionParser EXPRESSION_PARSER_OFF = new SpelExpressionParser(
			new SpelParserConfiguration(SpelCompilerMode.OFF, null));

	private static final ExpressionParser EXPRESSION_PARSER_IMMEDIATE = new SpelExpressionParser(
			new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE, null));

	private static final ExpressionParser EXPRESSION_PARSER_MIXED = new SpelExpressionParser(
			new SpelParserConfiguration(SpelCompilerMode.MIXED, null));

	private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER =
			new LocalVariableTableParameterNameDiscoverer();

	private static final Map<SpelCompilerMode, ExpressionParser> SPEL_COMPILERS = new HashMap<>();

	private static final TypeDescriptor messageTypeDescriptor = TypeDescriptor.valueOf(Message.class);

	@SuppressWarnings("unused")
	private static final Collection<Message<?>> dummyMessages = Collections.emptyList();

	private static final TypeDescriptor messageListTypeDescriptor = new TypeDescriptor(
			ReflectionUtils.findField(MessagingMethodInvokerHelper.class, "dummyMessages"));

	private static final TypeDescriptor messageArrayTypeDescriptor = TypeDescriptor.valueOf(Message[].class);

	static {
		SPEL_COMPILERS.put(SpelCompilerMode.OFF, EXPRESSION_PARSER_OFF);
		SPEL_COMPILERS.put(SpelCompilerMode.IMMEDIATE, EXPRESSION_PARSER_IMMEDIATE);
		SPEL_COMPILERS.put(SpelCompilerMode.MIXED, EXPRESSION_PARSER_MIXED);
	}

	private final DefaultMessageHandlerMethodFactory messageHandlerMethodFactory =
			new DefaultMessageHandlerMethodFactory();

	private final Object targetObject;

	private volatile String displayString;

	private volatile boolean requiresReply;

	private final Map<Class<?>, HandlerMethod> handlerMethods;

	private final Map<Class<?>, HandlerMethod> handlerMessageMethods;

	private final List<Map<Class<?>, HandlerMethod>> handlerMethodsList;

	private final HandlerMethod handlerMethod;

	private final TypeDescriptor expectedType;

	private final boolean canProcessMessageList;

	private Class<? extends Annotation> annotationType;

	private volatile boolean initialized;

	private String methodName;

	private Method method;

	private boolean useSpelInvoker;

	private HandlerMethod defaultHandlerMethod;

	private BeanExpressionResolver resolver = new StandardBeanExpressionResolver();

	private BeanExpressionContext expressionContext;


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

	/**
	 * A {@code boolean} flag to use SpEL Expression evaluation or {@link InvocableHandlerMethod}
	 * for target method invocation.
	 * @param useSpelInvoker to use SpEL Expression evaluation or not.
	 * @since 5.0
	 */
	public void setUseSpelInvoker(boolean useSpelInvoker) {
		this.useSpelInvoker = useSpelInvoker;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		super.setBeanFactory(beanFactory);
		this.messageHandlerMethodFactory.setBeanFactory(beanFactory);
		if (beanFactory instanceof ConfigurableListableBeanFactory) {
			BeanExpressionResolver beanExpressionResolver = ((ConfigurableListableBeanFactory) beanFactory)
					.getBeanExpressionResolver();
			if (beanExpressionResolver != null) {
				this.resolver = beanExpressionResolver;
			}
			this.expressionContext = new BeanExpressionContext((ConfigurableListableBeanFactory) beanFactory, null);
		}
	}

	@Override
	public void setConversionService(ConversionService conversionService) {
		super.setConversionService(conversionService);
		if (conversionService != null) {
			this.messageHandlerMethodFactory.setConversionService(conversionService);
		}
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
		this.method = method;
		this.requiresReply = expectedType != null;
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
			this.defaultHandlerMethod = null;
			checkSpelInvokerRequired(getTargetClass(targetObject), method, this.handlerMethod);
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
		this.methodName = methodName;
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

	private void prepareEvaluationContext() throws Exception {
		StandardEvaluationContext context = getEvaluationContext(false);
		Class<?> targetType = AopUtils.getTargetClass(this.targetObject);
		if (this.method != null) {
			context.registerMethodFilter(targetType, new FixedMethodFilter(this.method));
			if (this.expectedType != null) {
				Assert.state(context.getTypeConverter()
								.canConvert(TypeDescriptor.valueOf((this.method).getReturnType()), this.expectedType),
						"Cannot convert to expected type (" + this.expectedType + ") from " + this.method);
			}
		}
		else {
			AnnotatedMethodFilter filter = new AnnotatedMethodFilter(this.annotationType, this.methodName,
					this.requiresReply);
			Assert.state(canReturnExpectedType(filter, targetType, context.getTypeConverter()),
					"Cannot convert to expected type (" + this.expectedType + ") from " + this.method);
			context.registerMethodFilter(targetType, filter);
		}
		context.setVariable("target", this.targetObject);
		context.registerFunction("requiredHeader", ParametersWrapper.class.getDeclaredMethod("getHeader",
				Map.class, String.class));
	}

	private boolean canReturnExpectedType(AnnotatedMethodFilter filter, Class<?> targetType,
			TypeConverter typeConverter) {
		if (this.expectedType == null) {
			return true;
		}
		List<Method> methods = filter.filter(Arrays.asList(ReflectionUtils.getAllDeclaredMethods(targetType)));
		for (Method method : methods) {
			if (typeConverter.canConvert(TypeDescriptor.valueOf(method.getReturnType()), this.expectedType)) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private T processInternal(ParametersWrapper parameters) throws Exception {
		if (!this.initialized) {
			initialize();
		}
		HandlerMethod candidate = this.findHandlerMethodForParameters(parameters);
		if (candidate == null) {
			candidate = this.defaultHandlerMethod;
		}
		Assert.notNull(candidate, "No candidate methods found for messages.");
		if (!candidate.initialized) {
			initializeHandler(candidate);
		}
		Expression expression = candidate.expression;

		T result;
		if (this.useSpelInvoker || candidate.spelOnly) {
			result = invokeExpression(expression, parameters);
		}
		else {
			result = invokeHandlerMethod(candidate, parameters);
		}

		if (result != null && this.expectedType != null) {
			return (T) getEvaluationContext(true)
					.getTypeConverter()
					.convertValue(result, TypeDescriptor.forObject(result), this.expectedType);
		}
		else {
			return result;
		}
	}

	private void initializeHandler(HandlerMethod candidate) {
		ExpressionParser parser;
		if (candidate.useSpelInvoker == null) {
			parser = EXPRESSION_PARSER_DEFAULT;
		}
		else {
			String compilerMode = resolveExpression(candidate.useSpelInvoker.compilerMode(),
					"UseSpelInvoker.compilerMode:").toUpperCase();
			parser = !StringUtils.hasText(compilerMode)
				? EXPRESSION_PARSER_DEFAULT
				: SPEL_COMPILERS.get(SpelCompilerMode.valueOf(compilerMode));
		}
		candidate.expression = parser.parseExpression(candidate.expressionString);
		candidate.initialized = true;
	}

	private synchronized void initialize() throws Exception {
		if (!this.initialized) {
			PayloadExpressionArgumentResolver payloadExpressionArgumentResolver =
					new PayloadExpressionArgumentResolver();
			payloadExpressionArgumentResolver.setBeanFactory(getBeanFactory());

			PayloadsArgumentResolver payloadsArgumentResolver = new PayloadsArgumentResolver();
			payloadsArgumentResolver.setBeanFactory(getBeanFactory());

			CollectionArgumentResolver collectionArgumentResolver =
					new CollectionArgumentResolver(this.canProcessMessageList);
			collectionArgumentResolver.setBeanFactory(getBeanFactory());

			MapArgumentResolver mapArgumentResolver = new MapArgumentResolver();
			mapArgumentResolver.setBeanFactory(getBeanFactory());

			List<HandlerMethodArgumentResolver> customArgumentResolvers = new LinkedList<>();
			customArgumentResolvers.add(payloadExpressionArgumentResolver);
			customArgumentResolvers.add(payloadsArgumentResolver);
			customArgumentResolvers.add(collectionArgumentResolver);
			customArgumentResolvers.add(mapArgumentResolver);

			this.messageHandlerMethodFactory.setCustomArgumentResolvers(customArgumentResolvers);

			if (getBeanFactory() != null &&
					getBeanFactory()
							.containsBean(IntegrationContextUtils.ARGUMENT_RESOLVER_MESSAGE_CONVERTER_BEAN_NAME)) {
				this.messageHandlerMethodFactory
						.setMessageConverter(getBeanFactory()
								.getBean(IntegrationContextUtils.ARGUMENT_RESOLVER_MESSAGE_CONVERTER_BEAN_NAME,
										MessageConverter.class));
			}

			this.messageHandlerMethodFactory.afterPropertiesSet();
			prepareEvaluationContext();
			this.initialized = true;
		}
	}

	@SuppressWarnings("unchecked")
	private T invokeHandlerMethod(HandlerMethod handlerMethod, ParametersWrapper parameters) throws Exception {
		try {
			return (T) handlerMethod.invoke(parameters);
		}
		catch (MethodArgumentResolutionException | MessageConversionException | IllegalStateException e) {
			if (e instanceof MessageConversionException) {
				if (e.getCause() instanceof ConversionFailedException &&
						!(e.getCause().getCause() instanceof ConverterNotFoundException)) {
					throw e;
				}
			}
			else if (e instanceof IllegalStateException) {
				if (!(e.getCause() instanceof IllegalArgumentException) ||
						!e.getStackTrace()[0].getClassName().equals(InvocableHandlerMethod.class.getName()) ||
						(!"argument type mismatch".equals(e.getCause().getMessage()) &&
								// JVM generates GeneratedMethodAccessor### after several calls with less error checking
								!e.getCause().getMessage().startsWith("java.lang.ClassCastException@"))) {
					throw e;
				}
			}

			Expression expression = handlerMethod.expression;

			if (++handlerMethod.failedAttempts >= FAILED_ATTEMPTS_THRESHOLD) {
				handlerMethod.spelOnly = true;
				if (logger.isInfoEnabled()) {
					logger.info("Failed to invoke [ " + handlerMethod.invocableHandlerMethod +
							"] with provided arguments [ " + parameters + " ]. \n" +
							"Falling back to SpEL invocation for expression [ " +
							expression.getExpressionString() + " ]");
				}
			}

			return invokeExpression(expression, parameters);
		}
	}

	@SuppressWarnings("unchecked")
	private T invokeExpression(Expression expression, ParametersWrapper parameters) throws Exception {
		try {
			return (T) evaluateExpression(expression, parameters);
		}
		catch (Exception e) {
			Throwable evaluationException = e;
			if ((e instanceof EvaluationException || e instanceof MessageHandlingException)
					&& e.getCause() != null) {
				evaluationException = e.getCause();
			}
			if (evaluationException instanceof Exception) {
				throw (Exception) evaluationException;
			}
			else {
				throw new IllegalStateException("Cannot process message", evaluationException);
			}
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
				method1 = org.springframework.util.ClassUtils.getMostSpecificMethod(method1, targetObject.getClass());
				InvocableHandlerMethod invocableHandlerMethod =
						this.messageHandlerMethodFactory.createInvocableHandlerMethod(targetObject, method1);
				handlerMethod1 = new HandlerMethod(invocableHandlerMethod, this.canProcessMessageList);
				checkSpelInvokerRequired(targetClass, method1, handlerMethod1);
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
			if (AnnotationUtils.getAnnotation(method1, Default.class) != null) {
				Assert.state(this.defaultHandlerMethod == null,
						() -> "Only one method can be @Default, but there are more for: " + targetObject);
				this.defaultHandlerMethod = handlerMethod1;
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
				Method method = org.springframework.util.ClassUtils.getMostSpecificMethod(frameworkMethods.get(0),
						targetObject.getClass());
				InvocableHandlerMethod invocableHandlerMethod =
						this.messageHandlerMethodFactory.createInvocableHandlerMethod(targetObject,
								method);
				HandlerMethod handlerMethod = new HandlerMethod(invocableHandlerMethod, this.canProcessMessageList);
				checkSpelInvokerRequired(targetClass, method, handlerMethod);
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
			final AtomicReference<Class<?>> targetClass = new AtomicReference<>();
			Class<?>[] interfaces = ((Advised) targetObject).getProxiedInterfaces();
			for (Class<?> clazz : interfaces) {
				ReflectionUtils.doWithMethods(clazz, method1 -> {
					if (targetMethod.get() != null) {
						throw new IllegalStateException("Ambiguous method " + methodName + " on " + targetObject);
					}
					else {
						targetMethod.set(method1);
						targetClass.set(clazz);
					}
				}, method12 -> method12.getName().equals(methodName));
			}
			Method theMethod = targetMethod.get();
			if (theMethod != null) {
				theMethod = org.springframework.util.ClassUtils.getMostSpecificMethod(theMethod, targetObject.getClass());
				InvocableHandlerMethod invocableHandlerMethod =
						this.messageHandlerMethodFactory.createInvocableHandlerMethod(targetObject, theMethod);
				HandlerMethod handlerMethod = new HandlerMethod(invocableHandlerMethod, this.canProcessMessageList);
				checkSpelInvokerRequired(targetClass.get(), theMethod, handlerMethod);
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

	private void checkSpelInvokerRequired(final Class<?> targetClass, Method methodArg, HandlerMethod handlerMethod) {
		Method method = AopUtils.getMostSpecificMethod(methodArg, targetClass);
		UseSpelInvoker useSpel = AnnotationUtils.findAnnotation(method, UseSpelInvoker.class);
		if (useSpel == null) {
			useSpel = AnnotationUtils.findAnnotation(targetClass, UseSpelInvoker.class);
		}
		if (useSpel != null) {
			handlerMethod.spelOnly = true;
			handlerMethod.useSpelInvoker = useSpel;
		}
	}

	private String resolveExpression(String value, String msg) {
		String resolvedValue = resolve(value);

		if (!(resolvedValue.startsWith("#{") && value.endsWith("}"))) {
			return resolvedValue;
		}

		Object evaluated = this.resolver.evaluate(resolvedValue, this.expressionContext);
		Assert.isInstanceOf(String.class, evaluated, msg);
		return (String) evaluated;
	}

	private String resolve(String value) {
		if (getBeanFactory() != null && getBeanFactory() instanceof ConfigurableBeanFactory) {
			return ((ConfigurableBeanFactory) getBeanFactory()).resolveEmbeddedValue(value);
		}
		return value;
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

		private final String expressionString;

		private final InvocableHandlerMethod invocableHandlerMethod;

		private final boolean canProcessMessageList;

		private volatile Expression expression;

		private volatile TypeDescriptor targetParameterTypeDescriptor;

		private volatile Class<?> targetParameterType = Void.class;

		private volatile boolean messageMethod;

		private volatile boolean spelOnly;

		private volatile UseSpelInvoker useSpelInvoker;

		private volatile boolean initialized;

		// The number of times InvocableHandlerMethod was attempted and failed - enables us to eventually
		// give up trying to call it when it just doesn't seem to be possible.
		// Switching to spelOnly afterwards forever.
		private volatile int failedAttempts = 0;

		HandlerMethod(InvocableHandlerMethod invocableHandlerMethod, boolean canProcessMessageList) {
			this.invocableHandlerMethod = invocableHandlerMethod;
			this.canProcessMessageList = canProcessMessageList;
			this.expressionString = generateExpression(this.invocableHandlerMethod.getMethod());
		}


		@SuppressWarnings("unchecked")
		public <T> T invoke(ParametersWrapper parameters) throws Exception {
			Message<?> message = parameters.getMessage();
			if (this.canProcessMessageList) {
				message = new MutableMessage<>(parameters.getMessages(), parameters.getHeaders());
			}
			return (T) this.invocableHandlerMethod.invoke(message);
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

		private String generateExpression(Method method) {
			StringBuilder sb = new StringBuilder("#target." + method.getName() + "(");
			Class<?>[] parameterTypes = method.getParameterTypes();
			Annotation[][] parameterAnnotations = method.getParameterAnnotations();
			boolean hasUnqualifiedMapParameter = false;
			for (int i = 0; i < parameterTypes.length; i++) {
				if (i != 0) {
					sb.append(", ");
				}
				MethodParameter methodParameter = new MethodParameter(method, i);
				TypeDescriptor parameterTypeDescriptor = new TypeDescriptor(methodParameter);
				Class<?> parameterType = parameterTypeDescriptor.getObjectType();
				Annotation mappingAnnotation =
						MessagingAnnotationUtils.findMessagePartAnnotation(parameterAnnotations[i], true);
				if (mappingAnnotation != null) {
					Class<? extends Annotation> annotationType = mappingAnnotation.annotationType();
					if (annotationType.equals(Payload.class)) {
						sb.append("payload");
						String qualifierExpression = (String) AnnotationUtils.getValue(mappingAnnotation);
						if (StringUtils.hasText(qualifierExpression)) {
							sb.append(".")
									.append(qualifierExpression);
						}
						if (!StringUtils.hasText(qualifierExpression)) {
							this.setExclusiveTargetParameterType(parameterTypeDescriptor, methodParameter);
						}
					}
					if (annotationType.equals(Payloads.class)) {
						Assert.isTrue(this.canProcessMessageList,
								"The @Payloads annotation can only be applied if method handler canProcessMessageList.");
						Assert.isTrue(Collection.class.isAssignableFrom(parameterType),
								"The @Payloads annotation can only be applied to a Collection-typed parameter.");
						sb.append("messages.![payload");
						String qualifierExpression = ((Payloads) mappingAnnotation).value();
						if (StringUtils.hasText(qualifierExpression)) {
							sb.append(".")
									.append(qualifierExpression);
						}
						sb.append("]");
						if (!StringUtils.hasText(qualifierExpression)) {
							this.setExclusiveTargetParameterType(parameterTypeDescriptor, methodParameter);
						}
					}
					else if (annotationType.equals(Headers.class)) {
						Assert.isTrue(Map.class.isAssignableFrom(parameterType),
								"The @Headers annotation can only be applied to a Map-typed parameter.");
						sb.append("headers");
					}
					else if (annotationType.equals(Header.class)) {
						sb.append(this.determineHeaderExpression(mappingAnnotation, methodParameter));
					}
				}
				else if (parameterTypeDescriptor.isAssignableTo(messageTypeDescriptor)) {
					this.messageMethod = true;
					sb.append("message");
					this.setExclusiveTargetParameterType(parameterTypeDescriptor, methodParameter);
				}
				else if (this.canProcessMessageList &&
						(parameterTypeDescriptor.isAssignableTo(messageListTypeDescriptor)
								|| parameterTypeDescriptor.isAssignableTo(messageArrayTypeDescriptor))) {
					sb.append("messages");
					this.setExclusiveTargetParameterType(parameterTypeDescriptor, methodParameter);
				}
				else if (Collection.class.isAssignableFrom(parameterType) || parameterType.isArray()) {
					if (this.canProcessMessageList) {
						sb.append("messages.![payload]");
					}
					else {
						sb.append("payload");
					}
					this.setExclusiveTargetParameterType(parameterTypeDescriptor, methodParameter);
				}
				else if (Iterator.class.isAssignableFrom(parameterType)) {
					if (this.canProcessMessageList) {
						Type type = method.getGenericParameterTypes()[i];
						Type parameterizedType = null;
						if (type instanceof ParameterizedType) {
							parameterizedType = ((ParameterizedType) type).getActualTypeArguments()[0];
							if (parameterizedType instanceof ParameterizedType) {
								parameterizedType = ((ParameterizedType) parameterizedType).getRawType();
							}
						}
						if (parameterizedType != null && Message.class.isAssignableFrom((Class<?>) parameterizedType)) {
							sb.append("messages.iterator()");
						}
						else {
							sb.append("messages.![payload].iterator()");
						}
					}
					else {
						sb.append("payload.iterator()");
					}
					this.setExclusiveTargetParameterType(parameterTypeDescriptor, methodParameter);
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
					this.setExclusiveTargetParameterType(parameterTypeDescriptor, methodParameter);
				}
			}
			if (hasUnqualifiedMapParameter) {
				if (this.targetParameterType != null && Map.class.isAssignableFrom(this.targetParameterType)) {
					throw new IllegalArgumentException(
							"Unable to determine payload matching parameter due to ambiguous Map typed parameters. "
									+ "Consider adding the @Payload and or @Headers annotations as appropriate.");
				}
			}
			sb.append(")");
			if (this.targetParameterTypeDescriptor == null) {
				this.targetParameterTypeDescriptor = TypeDescriptor.valueOf(Void.class);
			}
			return sb.toString();
		}

		private String determineHeaderExpression(Annotation headerAnnotation, MethodParameter methodParameter) {
			methodParameter.initParameterNameDiscovery(PARAMETER_NAME_DISCOVERER);
			String headerName = null;
			String relativeExpression = "";
			AnnotationAttributes annotationAttributes =
					(AnnotationAttributes) AnnotationUtils.getAnnotationAttributes(headerAnnotation);
			String valueAttribute = annotationAttributes.getString(AnnotationUtils.VALUE);
			if (!StringUtils.hasText(valueAttribute)) {
				headerName = methodParameter.getParameterName();
			}
			else if (valueAttribute.indexOf('.') != -1) {
				String[] tokens = valueAttribute.split("\\.", 2);
				headerName = tokens[0];
				if (StringUtils.hasText(tokens[1])) {
					relativeExpression = "." + tokens[1];
					this.spelOnly = true;
				}
			}
			else {
				headerName = valueAttribute;
			}
			Assert.notNull(headerName, "Cannot determine header name. Possible reasons: -debug is "
					+ "disabled or header name is not explicitly provided via @Header annotation.");
			String headerRetrievalExpression = "headers['" + headerName + "']";
			String fullHeaderExpression = headerRetrievalExpression + relativeExpression;
			if (annotationAttributes.getBoolean("required")
					&& !methodParameter.getParameterType().equals(Optional.class)) {
				return "#requiredHeader(headers, '" + headerName + "')" + relativeExpression;
			}
			else if (!StringUtils.hasLength(relativeExpression)) {
				return headerRetrievalExpression + " ?: null";
			}
			else {
				return headerRetrievalExpression + " != null ? " + fullHeaderExpression + " : null";
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

	public static class ParametersWrapper {

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

		/**
		 * SpEL Function to retrieve a required header.
		 * @param headers the headers.
		 * @param header the header name
		 * @return the header
		 * @throws IllegalArgumentException if the header does not exist
		 */
		public static Object getHeader(Map<?, ?> headers, String header) {
			Object object = headers.get(header);
			if (object == null) {
				throw new IllegalArgumentException("required header not available: " + header);
			}
			return object;
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

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder("ParametersWrapper{");
			if (this.messages != null) {
				sb.append("messages=").append(this.messages)
						.append(", headers=").append(this.headers);
			}
			else {
				sb.append("message=").append(this.message);
			}
			return sb.append('}')
					.toString();
		}

	}

	@SuppressWarnings("serial")
	private static final class IneligibleMethodException extends RuntimeException {

		IneligibleMethodException(String message) {
			super(message);
		}

	}

}
