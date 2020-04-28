/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.gateway;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.IntegrationPatternType;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.GatewayHeader;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.channel.ChannelResolverUtils;
import org.springframework.integration.support.management.TrackableComponent;
import org.springframework.integration.util.JavaUtils;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import reactor.core.publisher.Mono;


/**
 * Generates a proxy for the provided service interface to enable interaction
 * with messaging components without application code being aware of them allowing
 * for POJO-style interaction.
 * This component is also aware of the
 * {@link org.springframework.core.convert.ConversionService} set on the enclosing {@link BeanFactory}
 * under the name
 * {@link org.springframework.integration.support.utils.IntegrationUtils#INTEGRATION_CONVERSION_SERVICE_BEAN_NAME}
 * to
 * perform type conversions when necessary (thanks to Jon Schneider's contribution and suggestion in INT-1230).
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 */
public class GatewayProxyFactoryBean extends AbstractEndpoint
		implements TrackableComponent, FactoryBean<Object>, MethodInterceptor, BeanClassLoaderAware {

	private static final SpelExpressionParser PARSER = new SpelExpressionParser();

	private final Object initializationMonitor = new Object();

	private final Map<Method, MethodInvocationGateway> gatewayMap = new HashMap<>();

	private final Class<?> serviceInterface;

	private MessageChannel defaultRequestChannel;

	private String defaultRequestChannelName;

	private MessageChannel defaultReplyChannel;

	private String defaultReplyChannelName;

	private MessageChannel errorChannel;

	private String errorChannelName;

	private Expression defaultRequestTimeout;

	private Expression defaultReplyTimeout;

	private DestinationResolver<MessageChannel> channelResolver;

	private boolean shouldTrack = false;

	private TypeConverter typeConverter = new SimpleTypeConverter();

	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private Object serviceProxy;

	private AsyncTaskExecutor asyncExecutor = new SimpleAsyncTaskExecutor();

	private boolean asyncExecutorExplicitlySet;

	private Class<?> asyncSubmitType;

	private Class<?> asyncSubmitListenableType;

	private volatile boolean initialized;

	private Map<String, GatewayMethodMetadata> methodMetadataMap;

	private GatewayMethodMetadata globalMethodMetadata;

	private MethodArgsMessageMapper argsMapper;

	private boolean proxyDefaultMethods;

	private EvaluationContext evaluationContext = new StandardEvaluationContext();

	/**
	 * Create a Factory whose service interface type can be configured by setter injection.
	 * If none is set, it will fall back to the default service interface type,
	 * {@link RequestReplyExchanger}, upon initialization.
	 */
	public GatewayProxyFactoryBean() {
		this.serviceInterface = RequestReplyExchanger.class;
	}

	public GatewayProxyFactoryBean(Class<?> serviceInterface) {
		Assert.notNull(serviceInterface, "'serviceInterface' must not be null");
		Assert.isTrue(serviceInterface.isInterface(), "'serviceInterface' must be an interface");
		this.serviceInterface = serviceInterface;
	}

	/**
	 * Set the default request channel.
	 * @param defaultRequestChannel the channel to which request messages will
	 * be sent if no request channel has been configured with an annotation.
	 */
	public void setDefaultRequestChannel(MessageChannel defaultRequestChannel) {
		this.defaultRequestChannel = defaultRequestChannel;
	}

	/**
	 * Set the default request channel bean name.
	 * @param defaultRequestChannelName the channel name to which request messages will
	 * be sent if no request channel has been configured with an annotation.
	 * @since 4.2.9
	 */
	public void setDefaultRequestChannelName(String defaultRequestChannelName) {
		this.defaultRequestChannelName = defaultRequestChannelName;
	}

	@Nullable
	protected MessageChannel getDefaultRequestChannel() {
		return this.defaultRequestChannel;
	}

	@Nullable
	protected String getDefaultRequestChannelName() {
		return this.defaultRequestChannelName;
	}

	/**
	 * Set the default reply channel. If no default reply channel is provided,
	 * and no reply channel is configured with annotations, an anonymous,
	 * temporary channel will be used for handling replies.
	 * @param defaultReplyChannel the channel from which reply messages will be
	 * received if no reply channel has been configured with an annotation
	 */
	public void setDefaultReplyChannel(MessageChannel defaultReplyChannel) {
		this.defaultReplyChannel = defaultReplyChannel;
	}

	/**
	 * Set the default reply channel bean name. If no default reply channel is provided,
	 * and no reply channel is configured with annotations, an anonymous,
	 * temporary channel will be used for handling replies.
	 * @param defaultReplyChannelName the channel name from which reply messages will be
	 * received if no reply channel has been configured with an annotation
	 * @since 4.2.9
	 */
	public void setDefaultReplyChannelName(String defaultReplyChannelName) {
		this.defaultReplyChannelName = defaultReplyChannelName;
	}

	@Nullable
	protected MessageChannel getDefaultReplyChannel() {
		return this.defaultReplyChannel;
	}

	@Nullable
	protected String getDefaultReplyChannelName() {
		return this.defaultReplyChannelName;
	}

	/**
	 * Set the error channel. If no error channel is provided, this gateway will
	 * propagate Exceptions to the caller. To completely suppress Exceptions, provide
	 * a reference to the "nullChannel" here.
	 * @param errorChannel The error channel.
	 */
	public void setErrorChannel(MessageChannel errorChannel) {
		this.errorChannel = errorChannel;
	}

	/**
	 * Set the error channel name. If no error channel is provided, this gateway will
	 * propagate Exceptions to the caller. To completely suppress Exceptions, provide
	 * a reference to the "nullChannel" here.
	 * @param errorChannelName The error channel bean name.
	 * @since 4.2.9
	 */
	public void setErrorChannelName(String errorChannelName) {
		this.errorChannelName = errorChannelName;
	}

	@Nullable
	protected MessageChannel getErrorChannel() {
		return this.errorChannel;
	}

	@Nullable
	protected String getErrorChannelName() {
		return this.errorChannelName;
	}

	/**
	 * Set the default timeout value for sending request messages. If not explicitly
	 * configured with an annotation, or on a method element, this value will be used.
	 * @param defaultRequestTimeout the timeout value in milliseconds
	 */
	public void setDefaultRequestTimeout(Long defaultRequestTimeout) {
		this.defaultRequestTimeout = new ValueExpression<>(defaultRequestTimeout);
	}

	/**
	 * Set an expression to be evaluated to determine the default timeout value for
	 * sending request messages. If not explicitly configured with an annotation, or on a
	 * method element, this value will be used.
	 * @param defaultRequestTimeout the timeout value in milliseconds
	 * @since 5.0
	 */
	public void setDefaultRequestTimeoutExpression(Expression defaultRequestTimeout) {
		this.defaultRequestTimeout = defaultRequestTimeout;
	}

	/**
	 * Set an expression to be evaluated to determine the default timeout value for
	 * sending request messages. If not explicitly configured with an annotation, or on a
	 * method element, this value will be used.
	 * @param defaultRequestTimeout the timeout value in milliseconds
	 * @since 5.0
	 */
	public void setDefaultRequestTimeoutExpressionString(String defaultRequestTimeout) {
		if (StringUtils.hasText(defaultRequestTimeout)) {
			this.defaultRequestTimeout = ExpressionUtils.longExpression(defaultRequestTimeout);
		}
	}

	@Nullable
	protected Expression getDefaultRequestTimeout() {
		return this.defaultRequestTimeout;
	}

	/**
	 * Set the default timeout value for receiving reply messages. If not explicitly
	 * configured with an annotation, or on a method element, this value will be used.
	 * @param defaultReplyTimeout the timeout value in milliseconds
	 */
	public void setDefaultReplyTimeout(Long defaultReplyTimeout) {
		this.defaultReplyTimeout = new ValueExpression<>(defaultReplyTimeout);
	}

	/**
	 * Set an expression to be evaluated to determine the default timeout value for
	 * receiving reply messages. If not explicitly configured with an annotation, or on a
	 * method element, this value will be used.
	 * @param defaultReplyTimeout the timeout value in milliseconds
	 * @since 5.0
	 */
	public void setDefaultReplyTimeoutExpression(Expression defaultReplyTimeout) {
		this.defaultReplyTimeout = defaultReplyTimeout;
	}

	/**
	 * Set an expression to be evaluated to determine the default timeout value for
	 * receiving reply messages. If not explicitly configured with an annotation, or on a
	 * method element, this value will be used.
	 * @param defaultReplyTimeout the timeout value in milliseconds
	 * @since 5.0
	 */
	public void setDefaultReplyTimeoutExpressionString(String defaultReplyTimeout) {
		if (StringUtils.hasText(defaultReplyTimeout)) {
			this.defaultReplyTimeout = ExpressionUtils.longExpression(defaultReplyTimeout);
		}
	}

	@Nullable
	protected Expression getDefaultReplyTimeout() {
		return this.defaultReplyTimeout;
	}

	@Override
	public void setShouldTrack(boolean shouldTrack) {
		this.shouldTrack = shouldTrack;
		if (!CollectionUtils.isEmpty(this.gatewayMap)) {
			for (MethodInvocationGateway gateway : this.gatewayMap.values()) {
				gateway.setShouldTrack(shouldTrack);
			}
		}
	}

	/**
	 * Set the executor for use when the gateway method returns
	 * {@link java.util.concurrent.Future} or {@link org.springframework.util.concurrent.ListenableFuture}.
	 * Set it to null to disable the async processing, and any
	 * {@link java.util.concurrent.Future} return types must be returned by the downstream flow.
	 * @param executor The executor.
	 */
	public void setAsyncExecutor(@Nullable Executor executor) {
		if (executor == null) {
			logger.info("A null executor disables the async gateway; " +
					"methods returning Future<?> will run on the calling thread");
		}
		this.asyncExecutor =
				(executor instanceof AsyncTaskExecutor || executor == null)
						? (AsyncTaskExecutor) executor
						: new TaskExecutorAdapter(executor);
		this.asyncExecutorExplicitlySet = true;
	}

	public void setTypeConverter(TypeConverter typeConverter) {
		Assert.notNull(typeConverter, "typeConverter must not be null");
		this.typeConverter = typeConverter;
	}

	public void setMethodMetadataMap(Map<String, GatewayMethodMetadata> methodMetadataMap) {
		this.methodMetadataMap = methodMetadataMap;
	}

	public void setGlobalMethodMetadata(GatewayMethodMetadata globalMethodMetadata) {
		this.globalMethodMetadata = globalMethodMetadata;
	}

	@Nullable
	protected GatewayMethodMetadata getGlobalMethodMetadata() {
		return this.globalMethodMetadata;
	}

	@Override
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}

	/**
	 * Provide a custom {@link MethodArgsMessageMapper} to map from a {@link MethodArgsHolder}
	 * to a {@link Message}.
	 * @param mapper the mapper.
	 */
	public final void setMapper(MethodArgsMessageMapper mapper) {
		this.argsMapper = mapper;
	}

	@Nullable
	protected MethodArgsMessageMapper getMapper() {
		return this.argsMapper;
	}

	/**
	 * Indicate if {@code default} methods on the interface should be proxied as well.
	 * If an explicit {@link Gateway} annotation is present on method it is proxied
	 * independently of this option.
	 * Note: default methods in JDK classes (such as {@code Function}) can be proxied, but cannot be invoked
	 * via {@code MethodHandle} by an internal Java security restriction for {@code MethodHandle.Lookup}.
	 * @param proxyDefaultMethods the boolean flag to proxy default methods or invoke via {@code MethodHandle}.
	 * @since 5.3
	 */
	public void setProxyDefaultMethods(boolean proxyDefaultMethods) {
		this.proxyDefaultMethods = proxyDefaultMethods;
	}

	@Nullable
	protected AsyncTaskExecutor getAsyncExecutor() {
		return this.asyncExecutor;
	}

	protected boolean isAsyncExecutorExplicitlySet() {
		return this.asyncExecutorExplicitlySet;
	}

	/**
	 * Return the Map of {@link Method} to {@link MessagingGatewaySupport}
	 * generated by this factory bean.
	 * @return the map.
	 * @since 4.3
	 */
	public Map<Method, MessagingGatewaySupport> getGateways() {
		return Collections.unmodifiableMap(this.gatewayMap);
	}

	@Override
	protected void onInit() {
		synchronized (this.initializationMonitor) {
			if (this.initialized) {
				return;
			}
			BeanFactory beanFactory = getBeanFactory();
			if (this.channelResolver == null && beanFactory != null) {
				this.channelResolver = ChannelResolverUtils.getChannelResolver(beanFactory);
			}

			populateMethodInvocationGateways();

			ProxyFactory gatewayProxyFactory =
					new ProxyFactory(this.serviceInterface, this);
			gatewayProxyFactory.addAdvice(new DefaultMethodInvokingMethodInterceptor());
			this.serviceProxy = gatewayProxyFactory.getProxy(this.beanClassLoader);
			if (this.asyncExecutor != null) {
				Callable<String> task = () -> null;
				Future<String> submitType = this.asyncExecutor.submit(task);
				this.asyncSubmitType = submitType.getClass();
				if (this.asyncExecutor instanceof AsyncListenableTaskExecutor) {
					submitType = ((AsyncListenableTaskExecutor) this.asyncExecutor).submitListenable(task);
					this.asyncSubmitListenableType = submitType.getClass();
				}
			}
			this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(beanFactory);
			this.initialized = true;
		}
	}

	private void populateMethodInvocationGateways() {
		Method[] methods = ReflectionUtils.getUniqueDeclaredMethods(this.serviceInterface);
		for (Method method : methods) {
			if (Modifier.isAbstract(method.getModifiers())
					|| method.getAnnotation(Gateway.class) != null
					|| (method.isDefault() && this.proxyDefaultMethods)) {

				MethodInvocationGateway gateway = createGatewayForMethod(method);
				this.gatewayMap.put(method, gateway);
			}
		}
	}

	@Override
	public Class<?> getObjectType() {
		return this.serviceInterface;
	}

	@Override
	public Object getObject() {
		if (this.serviceProxy == null) {
			this.onInit();
			Assert.notNull(this.serviceProxy, "failed to initialize proxy");
		}
		return this.serviceProxy;
	}

	@Override
	@Nullable
	public Object invoke(final MethodInvocation invocation) throws Throwable { // NOSONAR
		Class<?> returnType = invocation.getMethod().getReturnType();
		MethodInvocationGateway gateway = this.gatewayMap.get(invocation.getMethod());
		if (gateway != null) {
			returnType = gateway.returnType;
		}
		if (this.asyncExecutor != null && !Object.class.equals(returnType)) {
			Invoker invoker = new Invoker(invocation);
			if (returnType.isAssignableFrom(this.asyncSubmitType)) {
				return this.asyncExecutor.submit(invoker::get);
			}
			else if (returnType.isAssignableFrom(this.asyncSubmitListenableType)) {
				return ((AsyncListenableTaskExecutor) this.asyncExecutor).submitListenable(invoker::get);
			}
			else if (CompletableFuture.class.equals(returnType)) { // exact
				return CompletableFuture.supplyAsync(invoker, this.asyncExecutor);
			}
			else if (Future.class.isAssignableFrom(returnType) && logger.isDebugEnabled()) {
				logger.debug("AsyncTaskExecutor submit*() return types are incompatible with the method return " +
						"type; running on calling thread; the downstream flow must return the required Future: "
						+ returnType.getSimpleName());
			}
		}
		if (Mono.class.isAssignableFrom(returnType)) {
			return doInvoke(invocation, false);
		}
		else {
			return doInvoke(invocation, true);
		}
	}

	@Nullable
	protected Object doInvoke(MethodInvocation invocation, boolean runningOnCallerThread) throws Throwable { // NOSONAR
		Method method = invocation.getMethod();
		if (AopUtils.isToStringMethod(method)) {
			return "gateway proxy for service interface [" + this.serviceInterface + "]";
		}
		try {
			return invokeGatewayMethod(invocation, runningOnCallerThread);
		}
		catch (Throwable e) { //NOSONAR - ok to catch, rethrown below
			rethrowExceptionCauseIfPossible(e, invocation.getMethod());
			return null; // preceding call should always throw something
		}
	}

	@Nullable
	private Object invokeGatewayMethod(MethodInvocation invocation, boolean runningOnCallerThread) {
		if (!this.initialized) {
			afterPropertiesSet();
		}
		Method method = invocation.getMethod();
		MethodInvocationGateway gateway = this.gatewayMap.get(method);
		if (gateway == null) {
			try {
				return invocation.proceed();
			}
			catch (Throwable throwable) { // NOSONAR
				throw new IllegalStateException(throwable);
			}
		}
		boolean shouldReturnMessage =
				Message.class.isAssignableFrom(gateway.returnType) || (!runningOnCallerThread && gateway.expectMessage);
		boolean shouldReply = gateway.returnType != void.class;
		int paramCount = method.getParameterTypes().length;
		Object response;
		boolean hasPayloadExpression = findPayloadExpression(method);
		if (paramCount == 0 && !hasPayloadExpression) {
			response = receive(gateway, method, shouldReply, shouldReturnMessage);
		}
		else {
			response = sendOrSendAndReceive(invocation, gateway, shouldReturnMessage, shouldReply);
		}
		return response(gateway.returnType, shouldReturnMessage, response);
	}

	@Nullable
	private Object response(Class<?> returnType, boolean shouldReturnMessage, @Nullable Object response) {
		if (shouldReturnMessage) {
			return response;
		}
		else {
			return response != null ? convert(response, returnType) : null;
		}
	}

	private boolean findPayloadExpression(Method method) {
		boolean hasPayloadExpression = method.isAnnotationPresent(Payload.class);
		if (!hasPayloadExpression) {
			// check for the method metadata next
			if (this.methodMetadataMap != null) {
				GatewayMethodMetadata metadata = this.methodMetadataMap.get(method.getName());
				hasPayloadExpression = (metadata != null) && metadata.getPayloadExpression() != null;
			}
			else if (this.globalMethodMetadata != null) {
				hasPayloadExpression = this.globalMethodMetadata.getPayloadExpression() != null;
			}
		}
		return hasPayloadExpression;
	}

	@Nullable
	private Object receive(MethodInvocationGateway gateway, Method method, boolean shouldReply,
			boolean shouldReturnMessage) {

		Long receiveTimeout = null;
		Expression receiveTimeoutExpression = gateway.getReceiveTimeoutExpression();
		if (receiveTimeoutExpression != null) {
			receiveTimeout = receiveTimeoutExpression.getValue(this.evaluationContext, Long.class);
		}
		if (shouldReply) {
			if (shouldReturnMessage) {
				if (receiveTimeout != null) {
					return gateway.receiveMessage(receiveTimeout);
				}
				else {
					return gateway.receiveMessage();
				}
			}
			if (receiveTimeout != null) {
				return gateway.receive(receiveTimeout);
			}
			else {
				return gateway.receive();
			}
		}
		throw new IllegalArgumentException("The 'void' method without arguments '" + method + "' is not eligible for" +
				" gateway invocation. Consider to use different signature or 'payloadExpression'.");
	}

	@Nullable
	private Object sendOrSendAndReceive(MethodInvocation invocation, MethodInvocationGateway gateway,
			boolean shouldReturnMessage, boolean shouldReply) {

		Object[] args = invocation.getArguments();
		if (shouldReply) {
			if (gateway.isMonoReturn) {
				Mono<Message<?>> messageMono = gateway.sendAndReceiveMessageReactive(args);
				if (!shouldReturnMessage) {
					return messageMono.map(Message::getPayload);
				}
				else {
					return messageMono;
				}
			}
			else {
				return shouldReturnMessage ? gateway.sendAndReceiveMessage(args) : gateway.sendAndReceive(args);
			}
		}
		else {
			gateway.send(args);
		}
		return null;
	}

	private void rethrowExceptionCauseIfPossible(Throwable originalException, Method method)
			throws Throwable { // NOSONAR
		Class<?>[] exceptionTypes = method.getExceptionTypes();
		Throwable t = originalException;
		while (t != null) {
			for (Class<?> exceptionType : exceptionTypes) {
				if (exceptionType.isAssignableFrom(t.getClass())) {
					throw t;
				}
			}
			if (t instanceof RuntimeException // NOSONAR boolean complexity
					&& !(t instanceof MessagingException)
					&& !(t instanceof UndeclaredThrowableException)
					&& !(t instanceof IllegalStateException && "Unexpected exception thrown".equals(t.getMessage()))) {
				throw t;
			}
			t = t.getCause();
		}
		throw originalException;
	}

	private MethodInvocationGateway createGatewayForMethod(Method method) {
		Gateway gatewayAnnotation = method.getAnnotation(Gateway.class);
		GatewayMethodMetadata methodMetadata = null;
		if (!CollectionUtils.isEmpty(this.methodMetadataMap)) {
			methodMetadata = this.methodMetadataMap.get(method.getName());
		}
		Expression payloadExpression =
				extractPayloadExpressionFromAnnotationOrMetadata(gatewayAnnotation, methodMetadata);
		String requestChannelName = extractRequestChannelFromAnnotationOrMetadata(gatewayAnnotation, methodMetadata);
		String replyChannelName = extractReplyChannelFromAnnotationOrMetadata(gatewayAnnotation, methodMetadata);
		Expression requestTimeout = extractRequestTimeoutFromAnnotationOrMetadata(gatewayAnnotation, methodMetadata);
		Expression replyTimeout = extractReplyTimeoutFromAnnotationOrMetadata(gatewayAnnotation, methodMetadata);

		Map<String, Expression> headerExpressions = new HashMap<>();
		if (gatewayAnnotation != null) {
			annotationHeaders(gatewayAnnotation, headerExpressions);
		}
		else if (methodMetadata != null && !CollectionUtils.isEmpty(methodMetadata.getHeaderExpressions())) {
			headerExpressions.putAll(methodMetadata.getHeaderExpressions());
		}

		return doCreateMethodInvocationGateway(method, payloadExpression, headerExpressions,
				requestChannelName, replyChannelName, requestTimeout, replyTimeout);
	}

	@Nullable
	private Expression extractPayloadExpressionFromAnnotationOrMetadata(@Nullable Gateway gatewayAnnotation,
			@Nullable GatewayMethodMetadata methodMetadata) {

		Expression payloadExpression =
				this.globalMethodMetadata != null
						? this.globalMethodMetadata.getPayloadExpression()
						: null;

		if (gatewayAnnotation != null) {
			/*
			 * INT-2636 Unspecified annotation attributes should not
			 * override the default values supplied by explicit configuration.
			 * There is a small risk that someone has used Long.MIN_VALUE explicitly
			 * to indicate an indefinite timeout on a gateway method and that will
			 * no longer work as expected; they will need to use, say, -1 instead.
			 */
			if (payloadExpression == null && StringUtils.hasText(gatewayAnnotation.payloadExpression())) {
				payloadExpression = PARSER.parseExpression(gatewayAnnotation.payloadExpression());
			}
		}
		else if (methodMetadata != null && methodMetadata.getPayloadExpression() != null) {
			payloadExpression = methodMetadata.getPayloadExpression();
		}

		return payloadExpression;
	}

	@Nullable
	private String extractRequestChannelFromAnnotationOrMetadata(@Nullable Gateway gatewayAnnotation,
			@Nullable GatewayMethodMetadata methodMetadata) {

		if (gatewayAnnotation != null) {
			return gatewayAnnotation.requestChannel();
		}
		else if (methodMetadata != null) {
			return methodMetadata.getRequestChannelName();
		}
		return null;
	}

	@Nullable
	private String extractReplyChannelFromAnnotationOrMetadata(@Nullable Gateway gatewayAnnotation,
			@Nullable GatewayMethodMetadata methodMetadata) {

		if (gatewayAnnotation != null) {
			return gatewayAnnotation.replyChannel();
		}
		else if (methodMetadata != null) {
			return methodMetadata.getReplyChannelName();
		}
		return null;
	}

	@Nullable
	private Expression extractRequestTimeoutFromAnnotationOrMetadata(@Nullable Gateway gatewayAnnotation,
			@Nullable GatewayMethodMetadata methodMetadata) {

		Expression requestTimeout = this.defaultRequestTimeout;

		if (gatewayAnnotation != null) {
			/*
			 * INT-2636 Unspecified annotation attributes should not
			 * override the default values supplied by explicit configuration.
			 * There is a small risk that someone has used Long.MIN_VALUE explicitly
			 * to indicate an indefinite timeout on a gateway method and that will
			 * no longer work as expected; they will need to use, say, -1 instead.
			 */
			if (requestTimeout == null || gatewayAnnotation.requestTimeout() != Long.MIN_VALUE) {
				requestTimeout = new ValueExpression<>(gatewayAnnotation.requestTimeout());
			}
			if (StringUtils.hasText(gatewayAnnotation.requestTimeoutExpression())) {
				requestTimeout = ExpressionUtils.longExpression(gatewayAnnotation.requestTimeoutExpression());
			}

		}
		else if (methodMetadata != null) {
			String reqTimeout = methodMetadata.getRequestTimeout();
			if (StringUtils.hasText(reqTimeout)) {
				requestTimeout = ExpressionUtils.longExpression(reqTimeout);
			}
		}
		return requestTimeout;
	}

	@Nullable
	private Expression extractReplyTimeoutFromAnnotationOrMetadata(@Nullable Gateway gatewayAnnotation,
			@Nullable GatewayMethodMetadata methodMetadata) {

		Expression replyTimeout = this.defaultReplyTimeout;

		if (gatewayAnnotation != null) {
			/*
			 * INT-2636 Unspecified annotation attributes should not
			 * override the default values supplied by explicit configuration.
			 * There is a small risk that someone has used Long.MIN_VALUE explicitly
			 * to indicate an indefinite timeout on a gateway method and that will
			 * no longer work as expected; they will need to use, say, -1 instead.
			 */
			if (replyTimeout == null || gatewayAnnotation.replyTimeout() != Long.MIN_VALUE) {
				replyTimeout = new ValueExpression<>(gatewayAnnotation.replyTimeout());
			}
			if (StringUtils.hasText(gatewayAnnotation.replyTimeoutExpression())) {
				replyTimeout = ExpressionUtils.longExpression(gatewayAnnotation.replyTimeoutExpression());
			}

		}
		else if (methodMetadata != null) {
			String repTimeout = methodMetadata.getReplyTimeout();
			if (StringUtils.hasText(repTimeout)) {
				replyTimeout = ExpressionUtils.longExpression(repTimeout);
			}
		}
		return replyTimeout;
	}

	private void annotationHeaders(Gateway gatewayAnnotation, Map<String, Expression> headerExpressions) {
		if (!ObjectUtils.isEmpty(gatewayAnnotation.headers())) {
			for (GatewayHeader gatewayHeader : gatewayAnnotation.headers()) {
				String value = gatewayHeader.value();
				String expression = gatewayHeader.expression();
				String name = gatewayHeader.name();
				boolean hasValue = StringUtils.hasText(value);

				if (hasValue == StringUtils.hasText(expression)) {
					throw new BeanDefinitionStoreException("exactly one of 'value' or 'expression' " +
							"is required on a gateway's header.");
				}
				headerExpressions.put(name, hasValue
						? new LiteralExpression(value)
						: EXPRESSION_PARSER.parseExpression(expression));
			}
		}
	}

	private MethodInvocationGateway doCreateMethodInvocationGateway(Method method,
			@Nullable Expression payloadExpression, Map<String, Expression> headerExpressions,
			@Nullable String requestChannelName, @Nullable String replyChannelName,
			@Nullable Expression requestTimeout, @Nullable Expression replyTimeout) {

		GatewayMethodInboundMessageMapper messageMapper = createGatewayMessageMapper(method, headerExpressions);
		MethodInvocationGateway gateway = new MethodInvocationGateway(messageMapper);
		gateway.setupReturnType(this.serviceInterface, method);

		if (method.getParameterTypes().length == 0 && !findPayloadExpression(method)) {
			gateway.setPollable();
		}

		JavaUtils.INSTANCE
				.acceptIfNotNull(payloadExpression, messageMapper::setPayloadExpression)
				.acceptIfNotNull(getTaskScheduler(), gateway::setTaskScheduler);

		channels(requestChannelName, replyChannelName, gateway);

		timeouts(requestTimeout, replyTimeout, messageMapper, gateway);

		gateway.setBeanName(getComponentName());
		gateway.setBeanFactory(getBeanFactory());
		gateway.setShouldTrack(this.shouldTrack);
		gateway.afterPropertiesSet();

		return gateway;
	}

	private GatewayMethodInboundMessageMapper createGatewayMessageMapper(Method method,
			Map<String, Expression> headerExpressions) {

		Map<String, Object> headers = headers(method, headerExpressions);

		return new GatewayMethodInboundMessageMapper(method,
				headerExpressions,
				this.globalMethodMetadata != null ? this.globalMethodMetadata.getHeaderExpressions() : null,
				headers, this.argsMapper, getMessageBuilderFactory());
	}

	@Nullable
	private Map<String, Object> headers(Method method, Map<String, Expression> headerExpressions) {
		Map<String, Object> headers = null;
		// We don't want to eagerly resolve the error channel here
		Object errorChannelForVoidReturn = this.errorChannel == null ? this.errorChannelName : this.errorChannel;
		if (errorChannelForVoidReturn != null && method.getReturnType().equals(void.class)) {
			headers = new HashMap<>();
			headers.put(MessageHeaders.ERROR_CHANNEL, errorChannelForVoidReturn);
		}

		if (getMessageBuilderFactory() instanceof DefaultMessageBuilderFactory) {
			Set<String> headerNames = new HashSet<>(headerExpressions.keySet());

			if (this.globalMethodMetadata != null) {
				headerNames.addAll(this.globalMethodMetadata.getHeaderExpressions().keySet());
			}

			List<MethodParameter> methodParameters = GatewayMethodInboundMessageMapper.getMethodParameterList(method);

			for (MethodParameter methodParameter : methodParameters) {
				Header header = methodParameter.getParameterAnnotation(Header.class);
				if (header != null) {
					String headerName = GatewayMethodInboundMessageMapper.determineHeaderName(header, methodParameter);
					headerNames.add(headerName);
				}
			}

			validateHeaders(headerNames);
		}
		return headers;
	}

	private void validateHeaders(Set<String> headerNames) {
		for (String header : headerNames) {
			if ((MessageHeaders.ID.equals(header) || MessageHeaders.TIMESTAMP.equals(header))) {
				throw new BeanInitializationException(
						"Messaging Gateway cannot override 'id' and 'timestamp' read-only headers.\n" +
								"Wrong headers configuration for " + getComponentName());
			}
		}
	}

	private void channels(@Nullable String requestChannelName, @Nullable String replyChannelName,
			MethodInvocationGateway gateway) {

		setChannel(this.errorChannel, gateway::setErrorChannel, this.errorChannelName, gateway::setErrorChannelName);
		setChannel(requestChannelName, this.defaultRequestChannelName, gateway::setRequestChannelName,
				this.defaultRequestChannel, gateway::setRequestChannel);
		setChannel(replyChannelName, this.defaultReplyChannelName, gateway::setReplyChannelName,
				this.defaultReplyChannel, gateway::setReplyChannel);
	}

	private void timeouts(@Nullable Expression requestTimeout, @Nullable Expression replyTimeout,
			GatewayMethodInboundMessageMapper messageMapper, MethodInvocationGateway gateway) {
		if (requestTimeout == null) {
			gateway.setRequestTimeout(-1);
		}
		else if (requestTimeout instanceof ValueExpression) {
			Long timeout = requestTimeout.getValue(Long.class);
			if (timeout != null) {
				gateway.setRequestTimeout(timeout);
			}
		}
		else {
			messageMapper.setSendTimeoutExpression(requestTimeout);
		}
		if (replyTimeout == null) {
			gateway.setReplyTimeout(-1);
		}
		else if (replyTimeout instanceof ValueExpression) {
			Long timeout = replyTimeout.getValue(Long.class);
			if (timeout != null) {
				gateway.setReplyTimeout(timeout);
			}
		}
		else {
			messageMapper.setReplyTimeoutExpression(replyTimeout);
		}
		if (replyTimeout != null) {
			gateway.setReceiveTimeoutExpression(replyTimeout);
		}
	}

	private void setChannel(@Nullable MessageChannel channel, Consumer<MessageChannel> channelMethod,
			String channelName, Consumer<String> channelNameMethod) {

		if (channel != null) {
			channelMethod.accept(channel);
		}
		else if (StringUtils.hasText(channelName)) {
			channelNameMethod.accept(channelName);
		}
	}

	private void setChannel(String channelName1, String channelName2, Consumer<String> channelNameMethod,
			MessageChannel channel, Consumer<MessageChannel> channelMethod) {

		if (StringUtils.hasText(channelName1)) {
			channelNameMethod.accept(channelName1);
		}
		else if (StringUtils.hasText(channelName2)) {
			channelNameMethod.accept(channelName2);
		}
		else {
			channelMethod.accept(channel);
		}
	}

	// Lifecycle implementation

	@Override // guarded by super#lifecycleLock
	protected void doStart() {
		for (MethodInvocationGateway gateway : this.gatewayMap.values()) {
			gateway.start();
		}
	}

	@Override // guarded by super#lifecycleLock
	protected void doStop() {
		for (MethodInvocationGateway gateway : this.gatewayMap.values()) {
			gateway.stop();
		}
	}

	@SuppressWarnings("unchecked")
	@Nullable
	private <T> T convert(Object source, Class<T> expectedReturnType) {
		if (Future.class.isAssignableFrom(expectedReturnType)) {
			return (T) source;
		}
		if (Mono.class.isAssignableFrom(expectedReturnType)) {
			return (T) source;
		}
		if (getConversionService() != null) {
			return getConversionService().convert(source, expectedReturnType);
		}
		else {
			return this.typeConverter.convertIfNecessary(source, expectedReturnType);
		}
	}

	private static final class MethodInvocationGateway extends MessagingGatewaySupport {

		private Expression receiveTimeoutExpression;

		private Class<?> returnType;

		private boolean expectMessage;

		private boolean isMonoReturn;

		private boolean isVoidReturn;

		private boolean pollable;

		MethodInvocationGateway(GatewayMethodInboundMessageMapper messageMapper) {
			setRequestMapper(messageMapper);
		}

		@Override
		public IntegrationPatternType getIntegrationPatternType() {
			return this.pollable ? IntegrationPatternType.outbound_channel_adapter
					: this.isVoidReturn
							? IntegrationPatternType.inbound_channel_adapter
							: IntegrationPatternType.inbound_gateway;
		}

		@Nullable
		Expression getReceiveTimeoutExpression() {
			return this.receiveTimeoutExpression;
		}

		void setReceiveTimeoutExpression(Expression receiveTimeoutExpression) {
			this.receiveTimeoutExpression = receiveTimeoutExpression;
		}

		void setupReturnType(Class<?> serviceInterface, Method method) {
			ResolvableType resolvableType;
			if (Function.class.isAssignableFrom(serviceInterface) && "apply".equals(method.getName())) {
				resolvableType = ResolvableType.forClass(Function.class, serviceInterface).getGeneric(1);
			}
			else {
				resolvableType = ResolvableType.forMethodReturnType(method);
			}
			this.returnType = resolvableType.getRawClass();
			if (this.returnType == null) {
				this.returnType = Object.class;
			}
			else {
				this.isMonoReturn = Mono.class.isAssignableFrom(this.returnType);
				this.expectMessage = hasReturnParameterizedWithMessage(resolvableType);
			}
			this.isVoidReturn = isVoidReturnType(resolvableType);
		}

		private boolean hasReturnParameterizedWithMessage(ResolvableType resolvableType) {
			return (Future.class.isAssignableFrom(this.returnType) || Mono.class.isAssignableFrom(this.returnType))
					&& Message.class.isAssignableFrom(resolvableType.getGeneric(0).resolve(Object.class));
		}

		private boolean isVoidReturnType(ResolvableType resolvableType) {
			Class<?> returnTypeToCheck = this.returnType;
			if (Future.class.isAssignableFrom(this.returnType) || Mono.class.isAssignableFrom(this.returnType)) {
				returnTypeToCheck = resolvableType.getGeneric(0).resolve(Object.class);
			}
			return Void.class.isAssignableFrom(returnTypeToCheck);
		}

		private void setPollable() {
			this.pollable = true;
		}

	}

	private final class Invoker implements Supplier<Object> {

		private final MethodInvocation invocation;

		Invoker(MethodInvocation methodInvocation) {
			this.invocation = methodInvocation;
		}

		@Override
		@Nullable
		public Object get() {
			try {
				return doInvoke(this.invocation, false);
			}
			catch (Error e) { //NOSONAR
				throw e;
			}
			catch (Throwable t) { //NOSONAR
				if (t instanceof RuntimeException) { //NOSONAR
					throw (RuntimeException) t;
				}
				throw new MessagingException("Asynchronous gateway invocation failed for: " + this.invocation, t);
			}
		}

	}

}
