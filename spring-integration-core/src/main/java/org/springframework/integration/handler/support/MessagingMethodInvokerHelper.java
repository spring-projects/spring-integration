/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.handler.support;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
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
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.logging.LogFactory;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.AopProxyUtils;
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
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.log.LogAccessor;
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
import org.springframework.integration.core.Pausable;
import org.springframework.integration.support.MutableMessage;
import org.springframework.integration.support.NullAwarePayloadArgumentResolver;
import org.springframework.integration.support.converter.ConfigurableCompositeMessageConverter;
import org.springframework.integration.support.json.JsonObjectMapper;
import org.springframework.integration.support.json.JsonObjectMapperProvider;
import org.springframework.integration.support.management.ManageableLifecycle;
import org.springframework.integration.util.AbstractExpressionEvaluator;
import org.springframework.integration.util.AnnotatedMethodFilter;
import org.springframework.integration.util.FixedMethodFilter;
import org.springframework.integration.util.MessagingAnnotationUtils;
import org.springframework.integration.util.UniqueMethodFilter;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;
import org.springframework.messaging.handler.invocation.MethodArgumentResolutionException;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
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
public class MessagingMethodInvokerHelper extends AbstractExpressionEvaluator implements ManageableLifecycle {

	private static final String CANDIDATE_METHODS = "CANDIDATE_METHODS";

	private static final String CANDIDATE_MESSAGE_METHODS = "CANDIDATE_MESSAGE_METHODS";

	private static final LogAccessor LOGGER = new LogAccessor(LogFactory.getLog(MessagingMethodInvokerHelper.class));

	// Number of times to try an InvocableHandlerMethod before giving up in favor of an expression.
	private static final int FAILED_ATTEMPTS_THRESHOLD = 100;

	private static final ExpressionParser EXPRESSION_PARSER_DEFAULT = EXPRESSION_PARSER;

	private static final ExpressionParser EXPRESSION_PARSER_OFF =
			new SpelExpressionParser(new SpelParserConfiguration(SpelCompilerMode.OFF, null));

	private static final ExpressionParser EXPRESSION_PARSER_IMMEDIATE =
			new SpelExpressionParser(new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE, null));

	private static final ExpressionParser EXPRESSION_PARSER_MIXED =
			new SpelExpressionParser(new SpelParserConfiguration(SpelCompilerMode.MIXED, null));

	private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER =
			new LocalVariableTableParameterNameDiscoverer();

	private static final Map<SpelCompilerMode, ExpressionParser> SPEL_COMPILERS = new HashMap<>();

	private static final TypeDescriptor MESSAGE_TYPE_DESCRIPTOR = TypeDescriptor.valueOf(Message.class);

	private static final TypeDescriptor MESSAGE_LIST_TYPE_DESCRIPTOR =
			TypeDescriptor.collection(Collection.class, TypeDescriptor.valueOf(Message.class));

	private static final TypeDescriptor MESSAGE_ARRAY_TYPE_DESCRIPTOR = TypeDescriptor.valueOf(Message[].class);

	static {
		SPEL_COMPILERS.put(SpelCompilerMode.OFF, EXPRESSION_PARSER_OFF);
		SPEL_COMPILERS.put(SpelCompilerMode.IMMEDIATE, EXPRESSION_PARSER_IMMEDIATE);
		SPEL_COMPILERS.put(SpelCompilerMode.MIXED, EXPRESSION_PARSER_MIXED);
	}

	private MessageHandlerMethodFactory messageHandlerMethodFactory =
			new DefaultMessageHandlerMethodFactory();

	private final Object targetObject;

	private final JsonObjectMapper<?, ?> jsonObjectMapper;

	private final Map<Class<?>, HandlerMethod> handlerMethods;

	private final Map<Class<?>, HandlerMethod> handlerMessageMethods;

	private final List<Map<Class<?>, HandlerMethod>> handlerMethodsList = new LinkedList<>();

	private final TypeDescriptor expectedType;

	private final boolean canProcessMessageList;

	private final String methodName;

	private final Method method;

	private final Class<? extends Annotation> annotationType;

	private final HandlerMethod handlerMethod;

	private final String displayString;

	private final boolean requiresReply;

	private HandlerMethod defaultHandlerMethod;

	private BeanExpressionResolver resolver = new StandardBeanExpressionResolver();

	private BeanExpressionContext expressionContext;

	private boolean useSpelInvoker;

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

	private MessagingMethodInvokerHelper(Object targetObject, Class<? extends Annotation> annotationType,
			Method method, Class<?> expectedType, boolean canProcessMessageList) {

		this.annotationType = annotationType;
		this.canProcessMessageList = canProcessMessageList;
		Assert.notNull(method, "method must not be null");
		this.method = method;
		this.methodName = null;
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
		this.handlerMethod = createHandlerMethod(this.method);
		this.handlerMethods = null;
		this.handlerMessageMethods = null;
		this.handlerMethodsList.add(
				Collections.singletonMap(this.handlerMethod.targetParameterType, this.handlerMethod));
		this.displayString = buildDisplayString(targetObject, method);
		this.jsonObjectMapper = configureJsonObjectMapperIfAny();
	}

	private MessagingMethodInvokerHelper(Object targetObject, Class<? extends Annotation> annotationType,
			String methodName, Class<?> expectedType, boolean canProcessMessageList) {

		Assert.notNull(targetObject, "targetObject must not be null");
		this.annotationType = annotationType;
		if (methodName == null) {
			if (targetObject instanceof Function) {
				this.methodName = "apply";
			}
			else if (targetObject instanceof Consumer) {
				this.methodName = "accept";
			}
			else {
				this.methodName = null;
			}
		}
		else {
			this.methodName = methodName;
		}

		this.method = null;

		this.canProcessMessageList = canProcessMessageList;
		this.requiresReply = expectedType != null;
		if (expectedType != null) {
			this.expectedType = TypeDescriptor.valueOf(expectedType);
		}
		else {
			this.expectedType = null;
		}
		this.targetObject = targetObject;
		Map<String, Map<Class<?>, HandlerMethod>> handlerMethodsForTarget = findHandlerMethodsForTarget();
		Map<Class<?>, HandlerMethod> methods = handlerMethodsForTarget.get(CANDIDATE_METHODS);
		Map<Class<?>, HandlerMethod> messageMethods = handlerMethodsForTarget.get(CANDIDATE_MESSAGE_METHODS);
		if ((methods.size() == 1 && messageMethods.isEmpty()) ||
				(messageMethods.size() == 1 && methods.isEmpty())) {
			if (methods.size() == 1) {
				this.handlerMethod = methods.values().iterator().next();
			}
			else {
				this.handlerMethod = messageMethods.values().iterator().next();
			}
		}
		else {
			this.handlerMethod = null;
		}

		this.handlerMethods = methods;
		this.handlerMessageMethods = messageMethods;
		//TODO Consider to use global option to determine a precedence of methods
		this.handlerMethodsList.add(this.handlerMethods);
		this.handlerMethodsList.add(this.handlerMessageMethods);

		this.displayString = buildDisplayString(targetObject, methodName);
		this.jsonObjectMapper = configureJsonObjectMapperIfAny();
	}

	private JsonObjectMapper<?, ?> configureJsonObjectMapperIfAny() {
		try {
			return JsonObjectMapperProvider.newInstance();
		}
		catch (IllegalStateException e) {
			return null;
		}
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
		if (beanFactory instanceof ConfigurableListableBeanFactory) {
			BeanExpressionResolver beanExpressionResolver = ((ConfigurableListableBeanFactory) beanFactory)
					.getBeanExpressionResolver();
			if (beanExpressionResolver != null) {
				this.resolver = beanExpressionResolver;
			}
			this.expressionContext = new BeanExpressionContext((ConfigurableListableBeanFactory) beanFactory, null);
		}
	}

	@Nullable
	public Object process(Message<?> message) {
		ParametersWrapper parameters = new ParametersWrapper(message);
		return processInternal(parameters);
	}

	@Nullable
	public Object process(Collection<Message<?>> messages, Map<String, Object> headers) {
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

	private HandlerMethod createHandlerMethod(Method method) {
		try {
			HandlerMethod newHandlerMethod = new HandlerMethod(method, this.canProcessMessageList);
			checkSpelInvokerRequired(getTargetClass(this.targetObject), method, newHandlerMethod);
			return newHandlerMethod;
		}
		catch (IneligibleMethodException e) {
			throw new IllegalArgumentException(e);
		}
	}

	private InvocableHandlerMethod createInvocableHandlerMethod(Method method) {
		return this.messageHandlerMethodFactory.createInvocableHandlerMethod(this.targetObject, method);
	}

	private String buildDisplayString(Object targetObject, Object targetMethod) {
		StringBuilder sb =
				new StringBuilder(targetObject.getClass().getName())
						.append('.');
		if (targetMethod instanceof Method) {
			sb.append(((Method) targetMethod).getName());
		}
		else if (targetMethod instanceof String) {
			sb.append(targetMethod);
		}
		return sb.append(']').toString();
	}

	private void prepareEvaluationContext() {
		StandardEvaluationContext context = getEvaluationContext();
		Class<?> targetType = AopUtils.getTargetClass(this.targetObject);
		if (this.method != null) {
			context.registerMethodFilter(targetType,
					new FixedMethodFilter(ClassUtils.getMostSpecificMethod(this.method, targetType)));
			if (this.expectedType != null) {
				Assert.state(context.getTypeConverter()
								.canConvert(TypeDescriptor.valueOf((this.method).getReturnType()), this.expectedType),
						() -> "Cannot convert to expected type (" + this.expectedType + ") from " + this.method);
			}
		}
		else {
			AnnotatedMethodFilter filter = new AnnotatedMethodFilter(this.annotationType, this.methodName,
					this.requiresReply);
			Assert.state(canReturnExpectedType(filter, targetType, context.getTypeConverter()),
					() -> "Cannot convert to expected type (" + this.expectedType + ") from " + this.methodName);
			context.registerMethodFilter(targetType, filter);
		}
		context.setVariable("target", this.targetObject);
		try {
			context.registerFunction("requiredHeader",
					ParametersWrapper.class.getDeclaredMethod("getHeader", Map.class, String.class));
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private boolean canReturnExpectedType(AnnotatedMethodFilter filter, Class<?> targetType,
			TypeConverter typeConverter) {

		if (this.expectedType == null) {
			return true;
		}

		List<Method> methods = filter.filter(Arrays.asList(ReflectionUtils.getAllDeclaredMethods(targetType)));
		return methods
				.stream()
				.anyMatch(candidate ->
						typeConverter.canConvert(TypeDescriptor.valueOf(candidate.getReturnType()), this.expectedType));
	}

	@Nullable
	private Object processInternal(ParametersWrapper parameters) {
		if (!this.initialized) {
			initialize();
		}
		HandlerMethod candidate = findHandlerMethodForParameters(parameters);
		if (candidate == null) {
			candidate = this.defaultHandlerMethod;
		}
		Assert.notNull(candidate, "No candidate methods found for messages.");
		if (!candidate.initialized) {
			initializeHandler(candidate);
		}
		Object result;
		if (this.useSpelInvoker || candidate.spelOnly) {
			result = invokeExpression(candidate.expression, parameters);
		}
		else {
			result = invokeHandlerMethod(candidate, parameters);
		}

		if (result != null && this.expectedType != null) {
			return getEvaluationContext()
					.getTypeConverter()
					.convertValue(result, TypeDescriptor.forObject(result), this.expectedType);
		}
		else {
			return result;
		}
	}

	private synchronized void initialize() {
		if (isProvidedMessageHandlerFactoryBean()) {
			LOGGER.info("Overriding default instance of MessageHandlerMethodFactory with provided one.");
			this.messageHandlerMethodFactory =
					getBeanFactory()
							.getBean(
									this.canProcessMessageList
											? IntegrationContextUtils.LIST_MESSAGE_HANDLER_FACTORY_BEAN_NAME
											: IntegrationContextUtils.MESSAGE_HANDLER_FACTORY_BEAN_NAME,
									MessageHandlerMethodFactory.class);
		}
		else {
			configureLocalMessageHandlerFactory();
		}

		prepareEvaluationContext();
		this.initialized = true;
	}

	private boolean isProvidedMessageHandlerFactoryBean() {
		BeanFactory beanFactory = getBeanFactory();
		return beanFactory != null
				&& beanFactory.containsBean(
				this.canProcessMessageList
						? IntegrationContextUtils.LIST_MESSAGE_HANDLER_FACTORY_BEAN_NAME
						: IntegrationContextUtils.MESSAGE_HANDLER_FACTORY_BEAN_NAME);
	}

	/*
	 * This should not be needed in production but we have many tests
	 * that don't run in an application context.
	 */

	private void initializeHandler(HandlerMethod candidate) {
		ExpressionParser parser;
		if (candidate.useSpelInvoker == null) {
			parser = EXPRESSION_PARSER_DEFAULT;
		}
		else {
			String compilerMode = resolveExpression(candidate.useSpelInvoker.compilerMode()).toUpperCase();
			parser = !StringUtils.hasText(compilerMode)
					? EXPRESSION_PARSER_DEFAULT
					: SPEL_COMPILERS.get(SpelCompilerMode.valueOf(compilerMode));
		}
		candidate.expression = parser.parseExpression(candidate.expressionString);
		if (!this.useSpelInvoker && !candidate.spelOnly) {
			candidate.setInvocableHandlerMethod(createInvocableHandlerMethod(candidate.method));
		}
		candidate.initialized = true;
	}

	private void configureLocalMessageHandlerFactory() {
		BeanFactory beanFactory = getBeanFactory();

		ConfigurableCompositeMessageConverter messageConverter = new ConfigurableCompositeMessageConverter();
		messageConverter.setBeanFactory(beanFactory);
		messageConverter.afterPropertiesSet();

		List<HandlerMethodArgumentResolver> customArgumentResolvers = new LinkedList<>();
		PayloadExpressionArgumentResolver payloadExpressionArgumentResolver = new PayloadExpressionArgumentResolver();
		PayloadsArgumentResolver payloadsArgumentResolver = new PayloadsArgumentResolver();

		customArgumentResolvers.add(payloadExpressionArgumentResolver);
		customArgumentResolvers.add(new NullAwarePayloadArgumentResolver(messageConverter));
		customArgumentResolvers.add(payloadsArgumentResolver);

		CollectionArgumentResolver collectionArgumentResolver = null;

		if (this.canProcessMessageList) {
			collectionArgumentResolver = new CollectionArgumentResolver(true);
			customArgumentResolvers.add(collectionArgumentResolver);
		}

		MapArgumentResolver mapArgumentResolver = new MapArgumentResolver();
		customArgumentResolvers.add(mapArgumentResolver);
		payloadExpressionArgumentResolver.setBeanFactory(beanFactory);
		payloadsArgumentResolver.setBeanFactory(beanFactory);
		mapArgumentResolver.setBeanFactory(beanFactory);
		if (collectionArgumentResolver != null) {
			collectionArgumentResolver.setBeanFactory(beanFactory);
		}

		DefaultMessageHandlerMethodFactory localHandlerMethodFactory =
				(DefaultMessageHandlerMethodFactory) this.messageHandlerMethodFactory;
		localHandlerMethodFactory.setMessageConverter(messageConverter);
		localHandlerMethodFactory.setCustomArgumentResolvers(customArgumentResolvers);
		localHandlerMethodFactory.afterPropertiesSet();
	}

	@Nullable
	private Object invokeHandlerMethod(HandlerMethod handlerMethod, ParametersWrapper parameters) {
		try {
			return handlerMethod.invoke(parameters);
		}
		catch (MethodArgumentResolutionException | MessageConversionException | IllegalStateException ex) {
			return processInvokeExceptionAndFallbackToExpressionIfAny(handlerMethod, parameters, ex);
		}
		catch (RuntimeException ex) { // NOSONAR no way to handle conditional catch according Sonar rules
			throw ex;
		}
		catch (Exception ex) {
			throw new IllegalStateException("HandlerMethod invocation error", ex);
		}
	}

	private Object processInvokeExceptionAndFallbackToExpressionIfAny(HandlerMethod handlerMethod,
			ParametersWrapper parameters, RuntimeException ex) {

		if (ex instanceof MessageConversionException) {
			if (ex.getCause() instanceof ConversionFailedException &&
					!(ex.getCause().getCause() instanceof ConverterNotFoundException)) {
				throw ex;
			}
		}
		else if (ex instanceof IllegalStateException && // NOSONAR complex boolean expression
				(!(ex.getCause() instanceof IllegalArgumentException) ||
						!ex.getStackTrace()[0].getClassName().equals(InvocableHandlerMethod.class.getName()) ||
						(!"argument type mismatch".equals(ex.getCause().getMessage()) &&
								// JVM generates GeneratedMethodAccessor### after several calls with less error
								// checking
								!ex.getCause().getMessage().startsWith("java.lang.ClassCastException@")))) {
			throw ex;
		}

		return fallbackToInvokeExpression(handlerMethod, parameters);
	}

	private Object fallbackToInvokeExpression(HandlerMethod handlerMethod, ParametersWrapper parameters) {
		Expression expression = handlerMethod.expression;

		if (++handlerMethod.failedAttempts >= FAILED_ATTEMPTS_THRESHOLD) {
			handlerMethod.spelOnly = true;
			LOGGER.info(() -> "Failed to invoke [ " + handlerMethod.invocableHandlerMethod +
					"] with provided arguments [ " + parameters + " ]. \n" +
					"Falling back to SpEL invocation for expression [ " +
					expression.getExpressionString() + " ]");
		}

		return invokeExpression(expression, parameters);
	}

	private Object invokeExpression(Expression expression, ParametersWrapper parameters) {
		try {

			convertJsonPayloadIfNecessary(parameters);
			return evaluateExpression(expression, parameters);
		}
		catch (Exception ex) {
			throw processEvaluationException(ex);
		}
	}

	private RuntimeException processEvaluationException(Exception ex) {
		Throwable evaluationException = ex;
		if ((ex instanceof EvaluationException || ex instanceof MessageHandlingException)
				&& ex.getCause() != null) {
			evaluationException = ex.getCause();
		}
		if (evaluationException instanceof RuntimeException) {
			return (RuntimeException) evaluationException;
		}
		return new IllegalStateException("Cannot process message", evaluationException);
	}


	/*
	 * If there's a single method, it is SpEL only, the content is JSON,
	 * the payload is a String or byte[], the parameter doesn't match the payload,
	 * and there is a Json Object Mapper on the CP, convert.
	 */
	private void convertJsonPayloadIfNecessary(ParametersWrapper parameters) {
		if (parameters.message != null &&
				this.handlerMethod != null &&
				this.handlerMethod.exclusiveMethodParameter != null &&
				this.jsonObjectMapper != null) {

			Class<?> type = this.handlerMethod.targetParameterType;
			if ((parameters.getPayload() instanceof String && !type.equals(String.class) // NOSONAR
					|| parameters.getPayload() instanceof byte[] && !type.equals(byte[].class))
					&& contentTypeIsJson(parameters.message)) {

				doConvertJsonPayload(parameters);
			}
		}
	}

	private void doConvertJsonPayload(ParametersWrapper parameters) {
		try {
			Object targetPayload =
					this.jsonObjectMapper.fromJson(parameters.getPayload(), this.handlerMethod.targetParameterType);

			if (this.handlerMethod.targetParameterTypeDescriptor.isAssignableTo(MESSAGE_TYPE_DESCRIPTOR)) {
				parameters.message =
						getMessageBuilderFactory()
								.withPayload(targetPayload)
								.copyHeaders(parameters.getHeaders())
								.build();
			}
			else {
				parameters.payload = targetPayload;
			}
		}
		catch (Exception ex) {
			LOGGER.debug(ex, "Failed to convert from JSON");
		}
	}

	private boolean contentTypeIsJson(Message<?> message) {
		Object contentType = message.getHeaders().get(MessageHeaders.CONTENT_TYPE);
		return contentType != null && contentType.toString().contains("json");
	}

	private Map<String, Map<Class<?>, HandlerMethod>> findHandlerMethodsForTarget() {
		Map<String, Map<Class<?>, HandlerMethod>> methods = new HashMap<>();
		Map<Class<?>, HandlerMethod> candidateMethods = new HashMap<>();
		Map<Class<?>, HandlerMethod> candidateMessageMethods = new HashMap<>();
		Map<Class<?>, HandlerMethod> fallbackMethods = new HashMap<>();
		Map<Class<?>, HandlerMethod> fallbackMessageMethods = new HashMap<>();
		AtomicReference<Class<?>> ambiguousFallbackType = new AtomicReference<>();
		AtomicReference<Class<?>> ambiguousFallbackMessageGenericType = new AtomicReference<>();
		Class<?> targetClass = getTargetClass(this.targetObject);

		processMethodsFromTarget(candidateMethods, candidateMessageMethods, fallbackMethods, fallbackMessageMethods,
				ambiguousFallbackType, ambiguousFallbackMessageGenericType, targetClass);

		if (!candidateMethods.isEmpty() || !candidateMessageMethods.isEmpty()) {
			methods.put(CANDIDATE_METHODS, candidateMethods);
			methods.put(CANDIDATE_MESSAGE_METHODS, candidateMessageMethods);
			return methods;
		}

		if ((ambiguousFallbackType.get() != null
				|| ambiguousFallbackMessageGenericType.get() != null)
				&& ServiceActivator.class.equals(this.annotationType)) {
			/*
			 * When there are ambiguous fallback methods,
			 * a Service Activator can finally fallback to RequestReplyExchanger.exchange(m).
			 * Ambiguous means > 1 method that takes the same payload type, or > 1 method
			 * that takes a Message with the same generic type.
			 */
			Method frameworkMethod = obtainFrameworkMethod(targetClass);
			if (frameworkMethod != null) {
				HandlerMethod theHandlerMethod = createHandlerMethod(frameworkMethod);
				methods.put(CANDIDATE_METHODS, Collections.singletonMap(Object.class, theHandlerMethod));
				methods.put(CANDIDATE_MESSAGE_METHODS, candidateMessageMethods);
				return methods;
			}
		}

		validateFallbackMethods(fallbackMethods, fallbackMessageMethods, ambiguousFallbackType,
				ambiguousFallbackMessageGenericType);

		methods.put(CANDIDATE_METHODS, fallbackMethods);
		methods.put(CANDIDATE_MESSAGE_METHODS, fallbackMessageMethods);
		return methods;
	}

	private void validateFallbackMethods(Map<Class<?>, HandlerMethod> fallbackMethods,
			Map<Class<?>, HandlerMethod> fallbackMessageMethods, AtomicReference<Class<?>> ambiguousFallbackType,
			AtomicReference<Class<?>> ambiguousFallbackMessageGenericType) {
		Assert.state(!fallbackMethods.isEmpty() || !fallbackMessageMethods.isEmpty(),
				() -> "Target object of type [" + this.targetObject.getClass() +
						"] has no eligible methods for handling Messages.");

		Assert.isNull(ambiguousFallbackType.get(),
				() -> "Found ambiguous parameter type [" + ambiguousFallbackType +
						"] for method match: " + fallbackMethods.values());
		Assert.isNull(ambiguousFallbackMessageGenericType.get(),
				() -> "Found ambiguous parameter type ["
						+ ambiguousFallbackMessageGenericType
						+ "] for method match: "
						+ fallbackMethods.values());
	}

	private void processMethodsFromTarget(Map<Class<?>, HandlerMethod> candidateMethods,
			Map<Class<?>, HandlerMethod> candidateMessageMethods, Map<Class<?>, HandlerMethod> fallbackMethods,
			Map<Class<?>, HandlerMethod> fallbackMessageMethods, AtomicReference<Class<?>> ambiguousFallbackType,
			AtomicReference<Class<?>> ambiguousFallbackMessageGenericType, Class<?> targetClass) {

		ReflectionUtils.doWithMethods(targetClass, method1 -> {
			boolean matchesAnnotation = false;
			if (this.annotationType != null && AnnotationUtils.findAnnotation(method1, this.annotationType) != null) {
				matchesAnnotation = true;
			}
			else if (!Modifier.isPublic(method1.getModifiers())) {
				return;
			}

			HandlerMethod handlerMethod1 = obtainHandlerMethodIfAny(method1);

			if (handlerMethod1 != null) {
				populateHandlerMethod(candidateMethods, candidateMessageMethods, fallbackMethods,
						fallbackMessageMethods,
						ambiguousFallbackType, ambiguousFallbackMessageGenericType, matchesAnnotation, handlerMethod1);
			}

		}, new UniqueMethodFilter(targetClass));

		if (candidateMethods.isEmpty() && candidateMessageMethods.isEmpty() && fallbackMethods.isEmpty()
				&& fallbackMessageMethods.isEmpty()) {
			findSingleSpecificMethodOnInterfacesIfProxy(candidateMessageMethods, candidateMethods);
		}
	}

	@Nullable
	private HandlerMethod obtainHandlerMethodIfAny(Method methodToProcess) {
		HandlerMethod handlerMethodToUse = null;
		if (isMethodEligible(methodToProcess)) {
			try {
				handlerMethodToUse = createHandlerMethod(
						AopUtils.selectInvocableMethod(methodToProcess, ClassUtils.getUserClass(this.targetObject)));
			}
			catch (Exception ex) {
					LOGGER.debug(ex, "Method [" + methodToProcess + "] is not eligible for Message handling.");
				return null;
			}

			if (AnnotationUtils.getAnnotation(methodToProcess, Default.class) != null) {
				Assert.state(this.defaultHandlerMethod == null,
						() -> "Only one method can be @Default, but there are more for: " + this.targetObject);
				this.defaultHandlerMethod = handlerMethodToUse;
			}
		}

		return handlerMethodToUse;
	}

	private boolean isMethodEligible(Method methodToProcess) {
		return !(methodToProcess.isBridge() || // NOSONAR boolean complexity
				isMethodDefinedOnObjectClass(methodToProcess) ||
				methodToProcess.getDeclaringClass().equals(Proxy.class) ||
				(this.requiresReply && void.class.equals(methodToProcess.getReturnType())) ||
				(this.methodName != null && !this.methodName.equals(methodToProcess.getName())) ||
				(this.methodName == null && isPausableMethod(methodToProcess)));
	}

	private boolean isPausableMethod(Method pausableMethod) {
		Class<?> declaringClass = pausableMethod.getDeclaringClass();
		boolean pausable = (Pausable.class.isAssignableFrom(declaringClass)
					|| Lifecycle.class.isAssignableFrom(declaringClass))
				&& ReflectionUtils.findMethod(Pausable.class, pausableMethod.getName(),
						pausableMethod.getParameterTypes()) != null;
		if (pausable && this.logger.isTraceEnabled()) {
			this.logger.trace(pausableMethod + " is not considered a candidate method unless explicitly requested");
		}
		return pausable;
	}

	private void populateHandlerMethod(Map<Class<?>, HandlerMethod> candidateMethods,
			Map<Class<?>, HandlerMethod> candidateMessageMethods, Map<Class<?>, HandlerMethod> fallbackMethods,
			Map<Class<?>, HandlerMethod> fallbackMessageMethods, AtomicReference<Class<?>> ambiguousFallbackType,
			AtomicReference<Class<?>> ambiguousFallbackMessageGenericType, boolean matchesAnnotation,
			HandlerMethod handlerMethod1) {

		Class<?> targetParameterType = handlerMethod1.getTargetParameterType();
		if (matchesAnnotation || this.annotationType == null) {
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
	}

	@Nullable
	private Method obtainFrameworkMethod(Class<?> targetClass) {
		for (Class<?> iface : ClassUtils.getAllInterfacesForClass(targetClass)) {
			try {
				// Can't use real class because of package tangle
				if ("org.springframework.integration.gateway.RequestReplyExchanger".equals(iface.getName())) {
					return ClassUtils.getMostSpecificMethod(
							targetClass.getMethod("exchange", Message.class),
							this.targetObject.getClass());
				}
			}
			catch (Exception ex) {
				throw new IllegalStateException(ex);
			}
		}
		return null;
	}

	private void findSingleSpecificMethodOnInterfacesIfProxy(Map<Class<?>, HandlerMethod> candidateMessageMethods,
			Map<Class<?>, HandlerMethod> candidateMethods) {
		if (AopUtils.isAopProxy(this.targetObject)) {
			final AtomicReference<Method> targetMethod = new AtomicReference<>();
			final AtomicReference<Class<?>> targetClass = new AtomicReference<>();
			Class<?>[] interfaces = ((Advised) this.targetObject).getProxiedInterfaces();
			for (Class<?> clazz : interfaces) {
				ReflectionUtils.doWithMethods(clazz, method1 -> {
					if (targetMethod.get() != null) {
						throw new IllegalStateException(
								"Ambiguous method " + this.methodName + " on " + this.targetObject);
					}
					else {
						targetMethod.set(method1);
						targetClass.set(clazz);
					}
				}, method12 -> method12.getName().equals(this.methodName));
			}
			Method theMethod = targetMethod.get();
			if (theMethod != null) {
				theMethod = ClassUtils.getMostSpecificMethod(theMethod, this.targetObject.getClass());
				HandlerMethod theHandlerMethod = createHandlerMethod(theMethod);
				Class<?> targetParameterType = theHandlerMethod.getTargetParameterType();
				if (theHandlerMethod.isMessageMethod()) {
					if (candidateMessageMethods.containsKey(targetParameterType)) {
						throw new IllegalArgumentException("Found more than one method match for type " +
								"[Message<" + targetParameterType + ">]");
					}
					candidateMessageMethods.put(targetParameterType, theHandlerMethod);
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
					candidateMethods.put(targetParameterType, theHandlerMethod);
				}
			}
		}
	}

	private void checkSpelInvokerRequired(final Class<?> targetClass, Method methodArg, HandlerMethod handlerMethod) {
		UseSpelInvoker useSpel =
				AnnotationUtils.findAnnotation(AopUtils.getMostSpecificMethod(methodArg, targetClass),
						UseSpelInvoker.class);
		if (useSpel == null) {
			useSpel = AnnotationUtils.findAnnotation(targetClass, UseSpelInvoker.class);
		}
		if (useSpel != null) {
			handlerMethod.spelOnly = true;
			handlerMethod.useSpelInvoker = useSpel;
		}
	}

	private String resolveExpression(String value) {
		String resolvedValue = resolve(value);

		if (!(resolvedValue.startsWith("#{") && value.endsWith("}"))) {
			return resolvedValue;
		}

		Object evaluated = this.resolver.evaluate(resolvedValue, this.expressionContext);
		Assert.isInstanceOf(String.class, evaluated, "UseSpelInvoker.compilerMode:");
		return (String) evaluated;
	}

	private String resolve(String value) {
		BeanFactory beanFactory = getBeanFactory();
		if (beanFactory instanceof ConfigurableBeanFactory) {
			return ((ConfigurableBeanFactory) beanFactory).resolveEmbeddedValue(value);
		}
		return value;
	}

	private Class<?> getTargetClass(Object targetObject) {
		Class<?> targetClass = AopProxyUtils.ultimateTargetClass(targetObject);
		// Maybe a proxy with no target - e.g. gateway
		if (targetClass == targetObject.getClass() && targetObject instanceof Advised) {
			Class<?>[] interfaces = ((Advised) targetObject).getProxiedInterfaces();
			if (interfaces.length == 1) {
				targetClass = interfaces[0];
			}
		}
		if (targetClass.getSimpleName().contains("$MockitoMock$")) {
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

		HandlerMethod closestMatch = findClosestMatch(payloadType);
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
		for (Map<Class<?>, HandlerMethod> methods : this.handlerMethodsList) {
			Set<Class<?>> candidates = methods.keySet();
			Class<?> match = null;
			if (!CollectionUtils.isEmpty(candidates)) {
				match = org.springframework.integration.util.ClassUtils.findClosestMatch(payloadType, candidates, true);
			}
			if (match != null) {
				return methods.get(match);
			}
		}
		return null;
	}

	private static boolean isMethodDefinedOnObjectClass(Method method) {
		return method != null && // NOSONAR
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

		private final boolean canProcessMessageList;

		private final Method method;

		private InvocableHandlerMethod invocableHandlerMethod;

		private Expression expression;

		private TypeDescriptor targetParameterTypeDescriptor;

		private Class<?> targetParameterType = Void.class;

		private MethodParameter exclusiveMethodParameter;

		private boolean messageMethod;

		private UseSpelInvoker useSpelInvoker;

		private volatile boolean spelOnly;

		private volatile boolean initialized;

		// The number of times InvocableHandlerMethod was attempted and failed - enables us to eventually
		// give up trying to call it when it just doesn't seem to be possible.
		// Switching to spelOnly afterwards forever.
		private volatile int failedAttempts = 0;

		HandlerMethod(Method method, boolean canProcessMessageList) {
			this.method = method;
			this.canProcessMessageList = canProcessMessageList;
			this.expressionString = generateExpression(this.method);
		}

		void setInvocableHandlerMethod(InvocableHandlerMethod newInvocableHandlerMethod) {
			this.invocableHandlerMethod = newInvocableHandlerMethod;
		}

		@Nullable
		public Object invoke(ParametersWrapper parameters) {
			Message<?> message = parameters.getMessage();
			if (this.canProcessMessageList) {
				message = new MutableMessage<>(parameters.getMessages(), parameters.getHeaders());
			}
			try {
				Object result = this.invocableHandlerMethod.invoke(message);
				if (result != null
						&& org.springframework.integration.util.ClassUtils.isKotlinUnit(result.getClass())) {

					result = null;
				}
				return result;
			}
			catch (RuntimeException ex) { // NOSONAR no way to handle conditional catch according Sonar rules
				throw ex;
			}
			catch (Exception ex) {
				throw new IllegalStateException("InvocableHandlerMethod invoke error", ex);
			}
		}

		Class<?> getTargetParameterType() {
			return this.targetParameterType;
		}

		private boolean isMessageMethod() {
			return this.messageMethod;
		}

		@Override
		public String toString() {
			return this.method.toString();
		}

		private String generateExpression(Method method) {
			StringBuilder sb =
					new StringBuilder("#target.")
							.append(method.getName())
							.append('(');
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
				Type genericParameterType = method.getGenericParameterTypes()[i];
				Annotation mappingAnnotation =
						MessagingAnnotationUtils.findMessagePartAnnotation(parameterAnnotations[i], true);
				hasUnqualifiedMapParameter = processMethodParameterForExpression(sb, hasUnqualifiedMapParameter,
						methodParameter, parameterTypeDescriptor, parameterType, genericParameterType,
						mappingAnnotation);
			}
			sb.append(')');
			if (this.targetParameterTypeDescriptor == null) {
				this.targetParameterTypeDescriptor = TypeDescriptor.valueOf(Void.class);
			}
			return sb.toString();
		}

		private boolean processMethodParameterForExpression(StringBuilder sb, boolean hasUnqualifiedMapParameter,
				MethodParameter methodParameter, TypeDescriptor parameterTypeDescriptor, Class<?> parameterType,
				Type genericParameterType, Annotation mappingAnnotation) {

			if (mappingAnnotation != null) {
				processMappingAnnotationForExpression(sb, methodParameter, parameterTypeDescriptor, parameterType,
						mappingAnnotation);
			}
			else if (parameterTypeDescriptor.isAssignableTo(MESSAGE_TYPE_DESCRIPTOR)) {
				this.messageMethod = true;
				sb.append("message");
				setExclusiveTargetParameterType(parameterTypeDescriptor, methodParameter);
			}
			else if (this.canProcessMessageList &&
					(parameterTypeDescriptor.isAssignableTo(MESSAGE_LIST_TYPE_DESCRIPTOR)
							|| parameterTypeDescriptor.isAssignableTo(MESSAGE_ARRAY_TYPE_DESCRIPTOR))) {
				sb.append("messages");
				setExclusiveTargetParameterType(parameterTypeDescriptor, methodParameter);
			}
			else if (Collection.class.isAssignableFrom(parameterType) || parameterType.isArray()) {
				addCollectionParameterForExpression(sb);
				setExclusiveTargetParameterType(parameterTypeDescriptor, methodParameter);
			}
			else if (Iterator.class.isAssignableFrom(parameterType)) {
				populateIteratorParameterForExpression(sb, genericParameterType);
				setExclusiveTargetParameterType(parameterTypeDescriptor, methodParameter);
			}
			else if (Map.class.isAssignableFrom(parameterType)) {
				Assert.isTrue(!hasUnqualifiedMapParameter,
						"Found more than one Map typed parameter without any qualification. "
								+ "Consider using @Payload or @Headers on at least one of the parameters.");
				populateMapParameterForExpression(sb, parameterType);
				return true;
			}
			else {
				sb.append("payload");
				setExclusiveTargetParameterType(parameterTypeDescriptor, methodParameter);
			}
			return hasUnqualifiedMapParameter;
		}

		private void processMappingAnnotationForExpression(StringBuilder sb, MethodParameter methodParameter,
				TypeDescriptor parameterTypeDescriptor, Class<?> parameterType, Annotation mappingAnnotation) {

			Class<? extends Annotation> annotationType = mappingAnnotation.annotationType();
			if (annotationType.equals(Payload.class)) {
				sb.append("payload");
				String qualifierExpression = (String) AnnotationUtils.getValue(mappingAnnotation);
				if (StringUtils.hasText(qualifierExpression)) {
					sb.append(".")
							.append(qualifierExpression);
				}
				if (!StringUtils.hasText(qualifierExpression)) {
					setExclusiveTargetParameterType(parameterTypeDescriptor, methodParameter);
				}
			}
			if (annotationType.equals(Payloads.class)) {
				Assert.isTrue(this.canProcessMessageList,
						"The @Payloads annotation can only be applied " +
								"if method handler canProcessMessageList.");
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
					setExclusiveTargetParameterType(parameterTypeDescriptor, methodParameter);
				}
			}
			else if (annotationType.equals(Headers.class)) {
				Assert.isTrue(Map.class.isAssignableFrom(parameterType),
						"The @Headers annotation can only be applied to a Map-typed parameter.");
				sb.append("headers");
			}
			else if (annotationType.equals(Header.class)) {
				sb.append(determineHeaderExpression(mappingAnnotation, methodParameter));
			}
		}

		private void addCollectionParameterForExpression(StringBuilder sb) {
			if (this.canProcessMessageList) {
				sb.append("messages.![payload]");
			}
			else {
				sb.append("payload");
			}
		}

		private void populateIteratorParameterForExpression(StringBuilder sb, Type type) {
			if (this.canProcessMessageList) {
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
		}

		private void populateMapParameterForExpression(StringBuilder sb, Class<?> parameterType) {
			if (Properties.class.isAssignableFrom(parameterType)) {
				sb.append("payload instanceof T(java.util.Map) or "
						+ "(payload instanceof T(String) and payload.contains('=')) ? payload : headers");
			}
			else {
				sb.append("(payload instanceof T(java.util.Map) ? payload : headers)");
			}
			if (this.targetParameterType != null && Map.class.isAssignableFrom(this.targetParameterType)) {
				throw new IllegalArgumentException(
						"Unable to determine payload matching parameter due to ambiguous Map typed parameters. "
								+ "Consider adding the @Payload and or @Headers annotations as appropriate.");
			}
		}

		private String determineHeaderExpression(Annotation headerAnnotation, MethodParameter methodParameter) {
			methodParameter.initParameterNameDiscovery(PARAMETER_NAME_DISCOVERER);
			String relativeExpression = "";
			AnnotationAttributes annotationAttributes =
					(AnnotationAttributes) AnnotationUtils.getAnnotationAttributes(headerAnnotation);
			String valueAttribute = annotationAttributes.getString(AnnotationUtils.VALUE);
			int len = valueAttribute.length();
			String headerName = valueAttribute;
			if (!StringUtils.hasText(valueAttribute)) {
				headerName = methodParameter.getParameterName();
			}
			else if (len > 2 && valueAttribute.charAt(0) == '\'' && valueAttribute.charAt(len - 1) == '\'') {
				headerName = valueAttribute.substring(1, len - 1);
			}
			else if (valueAttribute.indexOf('.') != -1) {
				String[] tokens = valueAttribute.split("\\.", 2);
				headerName = tokens[0];
				if (StringUtils.hasText(tokens[1])) {
					relativeExpression = "." + tokens[1];
					this.spelOnly = true;
				}
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
						this.targetParameterTypeDescriptor + "] and [" + targetParameterType + "].\n" +
						"Consider annotating one of the parameters with '@Payload'.");
			}
			this.targetParameterTypeDescriptor = targetParameterType;
			if (Message.class.isAssignableFrom(targetParameterType.getObjectType())) {
				this.targetParameterType = methodParameter.nested().getNestedParameterType();
			}
			else {
				this.targetParameterType = targetParameterType.getObjectType();
			}

			this.exclusiveMethodParameter = methodParameter;
		}

	}

	public static class ParametersWrapper {

		private final Collection<Message<?>> messages;

		private final Map<String, Object> headers;

		private Message<?> message;

		private Object payload;

		ParametersWrapper(Message<?> message) {
			this.message = message;
			this.payload = message.getPayload();
			this.headers = message.getHeaders();
			this.messages = null;
		}

		ParametersWrapper(Collection<Message<?>> messages, Map<String, Object> headers) {
			this.messages = messages;
			this.headers = headers;
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
			Assert.notNull(object, () -> "required header not available: " + header);
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
