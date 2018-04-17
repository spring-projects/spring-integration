/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.integration.gateway;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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
import org.springframework.core.convert.ConversionService;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.GatewayHeader;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.channel.BeanFactoryChannelResolver;
import org.springframework.integration.support.management.TrackableComponent;
import org.springframework.integration.support.utils.IntegrationUtils;
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
 * This component is also aware of the {@link ConversionService} set on the enclosing {@link BeanFactory}
 * under the name {@link IntegrationUtils#INTEGRATION_CONVERSION_SERVICE_BEAN_NAME} to
 * perform type conversions when necessary (thanks to Jon Schneider's contribution and suggestion in INT-1230).
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 */
public class GatewayProxyFactoryBean extends AbstractEndpoint
		implements TrackableComponent, FactoryBean<Object>, MethodInterceptor, BeanClassLoaderAware {

	private volatile Class<?> serviceInterface;

	private volatile MessageChannel defaultRequestChannel;

	private volatile String defaultRequestChannelName;

	private volatile MessageChannel defaultReplyChannel;

	private volatile String defaultReplyChannelName;

	private volatile MessageChannel errorChannel;

	private volatile String errorChannelName;

	private volatile Expression defaultRequestTimeout;

	private volatile Expression defaultReplyTimeout;

	private volatile DestinationResolver<MessageChannel> channelResolver;

	private volatile boolean shouldTrack = false;

	private volatile TypeConverter typeConverter = new SimpleTypeConverter();

	private volatile ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private volatile Object serviceProxy;

	private final Map<Method, MethodInvocationGateway> gatewayMap = new HashMap<>();

	private volatile AsyncTaskExecutor asyncExecutor = new SimpleAsyncTaskExecutor();

	private volatile Class<?> asyncSubmitType;

	private volatile Class<?> asyncSubmitListenableType;

	private volatile boolean initialized;

	private final Object initializationMonitor = new Object();

	private volatile Map<String, GatewayMethodMetadata> methodMetadataMap;

	private volatile GatewayMethodMetadata globalMethodMetadata;

	private volatile MethodArgsMessageMapper argsMapper;

	private EvaluationContext evaluationContext = new StandardEvaluationContext();

	/**
	 * Create a Factory whose service interface type can be configured by setter injection.
	 * If none is set, it will fall back to the default service interface type,
	 * {@link RequestReplyExchanger}, upon initialization.
	 */
	public GatewayProxyFactoryBean() {
		// serviceInterface will be determined on demand later
	}

	public GatewayProxyFactoryBean(Class<?> serviceInterface) {
		Assert.notNull(serviceInterface, "'serviceInterface' must not be null");
		Assert.isTrue(serviceInterface.isInterface(), "'serviceInterface' must be an interface");
		this.serviceInterface = serviceInterface;
	}


	/**
	 * Set the interface class that the generated proxy should implement.
	 * If none is provided explicitly, the default is {@link RequestReplyExchanger}.
	 *
	 * @param serviceInterface The service interface.
	 */
	public void setServiceInterface(Class<?> serviceInterface) {
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

	/**
	 * Set the default timeout value for sending request messages. If not explicitly
	 * configured with an annotation, or on a method element, this value will be used.
	 *
	 * @param defaultRequestTimeout the timeout value in milliseconds
	 */
	public void setDefaultRequestTimeout(Long defaultRequestTimeout) {
		this.defaultRequestTimeout = new ValueExpression<>(defaultRequestTimeout);
	}

	/**
	 * Set an expression to be evaluated to determine the default timeout value for
	 * sending request messages. If not explicitly configured with an annotation, or on a
	 * method element, this value will be used.
	 *
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
	 *
	 * @param defaultRequestTimeout the timeout value in milliseconds
	 * @since 5.0
	 */
	public void setDefaultRequestTimeoutExpressionString(String defaultRequestTimeout) {
		if (StringUtils.hasText(defaultRequestTimeout)) {
			this.defaultRequestTimeout = ExpressionUtils.longExpression(defaultRequestTimeout);
		}
	}

	/**
	 * Set the default timeout value for receiving reply messages. If not explicitly
	 * configured with an annotation, or on a method element, this value will be used.
	 *
	 * @param defaultReplyTimeout the timeout value in milliseconds
	 */
	public void setDefaultReplyTimeout(Long defaultReplyTimeout) {
		this.defaultReplyTimeout = new ValueExpression<>(defaultReplyTimeout);
	}

	/**
	 * Set an expression to be evaluated to determine the default timeout value for
	 * receiving reply messages. If not explicitly configured with an annotation, or on a
	 * method element, this value will be used.
	 *
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
	 *
	 * @param defaultReplyTimeout the timeout value in milliseconds
	 * @since 5.0
	 */
	public void setDefaultReplyTimeoutExpressionString(String defaultReplyTimeout) {
		if (StringUtils.hasText(defaultReplyTimeout)) {
			this.defaultReplyTimeout = ExpressionUtils.longExpression(defaultReplyTimeout);
		}
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
	public void setAsyncExecutor(Executor executor) {
		if (executor == null && logger.isInfoEnabled()) {
			logger.info("A null executor disables the async gateway; " +
					"methods returning Future<?> will run on the calling thread");
		}
		this.asyncExecutor = (executor instanceof AsyncTaskExecutor || executor == null) ? (AsyncTaskExecutor) executor
				: new TaskExecutorAdapter(executor);
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

	protected AsyncTaskExecutor getAsyncExecutor() {
		return this.asyncExecutor;
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
			BeanFactory beanFactory = this.getBeanFactory();
			if (this.channelResolver == null && beanFactory != null) {
				this.channelResolver = new BeanFactoryChannelResolver(beanFactory);
			}
			Class<?> proxyInterface = this.determineServiceInterface();
			Method[] methods = ReflectionUtils.getUniqueDeclaredMethods(proxyInterface);
			for (Method method : methods) {
				MethodInvocationGateway gateway = this.createGatewayForMethod(method);
				this.gatewayMap.put(method, gateway);
			}
			this.serviceProxy = new ProxyFactory(proxyInterface, this)
					.getProxy(this.beanClassLoader);
			if (this.asyncExecutor != null) {
				Callable<String> task = () -> null;
				Future<String> submitType = this.asyncExecutor.submit(task);
				this.asyncSubmitType = submitType.getClass();
				if (this.asyncExecutor instanceof AsyncListenableTaskExecutor) {
					submitType = ((AsyncListenableTaskExecutor) this.asyncExecutor).submitListenable(task);
					this.asyncSubmitListenableType = submitType.getClass();
				}
			}
			this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
			this.initialized = true;
		}
	}

	private Class<?> determineServiceInterface() {
		if (this.serviceInterface == null) {
			this.serviceInterface = RequestReplyExchanger.class;
		}
		return this.serviceInterface;
	}

	@Override
	public Class<?> getObjectType() {
		return (this.serviceInterface != null ? this.serviceInterface : null);
	}

	@Override
	public Object getObject() throws Exception {
		if (this.serviceProxy == null) {
			this.onInit();
			Assert.notNull(this.serviceProxy, "failed to initialize proxy");
		}
		return this.serviceProxy;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public Object invoke(final MethodInvocation invocation) throws Throwable {
		final Class<?> returnType = invocation.getMethod().getReturnType();
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
			else if (Future.class.isAssignableFrom(returnType)) {
				if (logger.isDebugEnabled()) {
					logger.debug("AsyncTaskExecutor submit*() return types are incompatible with the method return type; "
							+ "running on calling thread; the downstream flow must return the required Future: "
							+ returnType.getSimpleName());
				}
			}
		}
		if (Mono.class.isAssignableFrom(returnType)) {
			return Mono.fromSupplier(new Invoker(invocation));
		}
		return this.doInvoke(invocation, true);
	}

	protected Object doInvoke(MethodInvocation invocation, boolean runningOnCallerThread) throws Throwable {
		Method method = invocation.getMethod();
		if (AopUtils.isToStringMethod(method)) {
			return "gateway proxy for service interface [" + this.serviceInterface + "]";
		}
		try {
			return this.invokeGatewayMethod(invocation, runningOnCallerThread);
		}
		catch (Throwable e) { //NOSONAR - ok to catch, rethrown below
			this.rethrowExceptionCauseIfPossible(e, invocation.getMethod());
			return null; // preceding call should always throw something
		}
	}

	private Object invokeGatewayMethod(MethodInvocation invocation, boolean runningOnCallerThread) throws Exception {
		if (!this.initialized) {
			this.afterPropertiesSet();
		}
		Method method = invocation.getMethod();
		MethodInvocationGateway gateway = this.gatewayMap.get(method);
		Class<?> returnType = method.getReturnType();
		boolean shouldReturnMessage = Message.class.isAssignableFrom(returnType)
				|| hasReturnParameterizedWithMessage(method, runningOnCallerThread);
		boolean shouldReply = returnType != void.class;
		int paramCount = method.getParameterTypes().length;
		Object response = null;
		boolean hasPayloadExpression = method.isAnnotationPresent(Payload.class);
		if (!hasPayloadExpression) {
			// check for the method metadata next
			if (this.methodMetadataMap != null) {
				GatewayMethodMetadata metadata = this.methodMetadataMap.get(method.getName());
				hasPayloadExpression = (metadata != null) && StringUtils.hasText(metadata.getPayloadExpression());
			}
			else if (this.globalMethodMetadata != null) {
				hasPayloadExpression = StringUtils.hasText(this.globalMethodMetadata.getPayloadExpression());
			}
		}
		if (paramCount == 0 && !hasPayloadExpression) {
			Long receiveTimeout = null;
			if (gateway.getReceiveTimeoutExpression() != null) {
				receiveTimeout = gateway.getReceiveTimeoutExpression().getValue(this.evaluationContext, Long.class);
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
					response = gateway.receive(receiveTimeout);
				}
				else {
					response = gateway.receive();
				}
			}
		}
		else {
			Object[] args = invocation.getArguments();
			if (shouldReply) {
				response = shouldReturnMessage ? gateway.sendAndReceiveMessage(args) : gateway.sendAndReceive(args);
			}
			else {
				gateway.send(args);
				response = null;
			}
		}
		return (response != null) ? this.convert(response, returnType) : null;
	}

	private void rethrowExceptionCauseIfPossible(Throwable originalException, Method method) throws Throwable {
		Class<?>[] exceptionTypes = method.getExceptionTypes();
		Throwable t = originalException;
		while (t != null) {
			for (Class<?> exceptionType : exceptionTypes) {
				if (exceptionType.isAssignableFrom(t.getClass())) {
					throw t;
				}
			}
			if (t instanceof RuntimeException
					&& !(t instanceof MessagingException)
					&& !(t instanceof UndeclaredThrowableException)
					&& !(t instanceof IllegalStateException && ("Unexpected exception thrown").equals(t.getMessage()))) {
				throw t;
			}
			t = t.getCause();
		}
		throw originalException;
	}

	private MethodInvocationGateway createGatewayForMethod(Method method) {
		Gateway gatewayAnnotation = method.getAnnotation(Gateway.class);
		String requestChannelName = null;
		String replyChannelName = null;
		Expression requestTimeout = this.defaultRequestTimeout;
		Expression replyTimeout = this.defaultReplyTimeout;
		String payloadExpression = this.globalMethodMetadata != null
				? this.globalMethodMetadata.getPayloadExpression()
				: null;
		Map<String, Expression> headerExpressions = new HashMap<String, Expression>();
		if (gatewayAnnotation != null) {
			requestChannelName = gatewayAnnotation.requestChannel();
			replyChannelName = gatewayAnnotation.replyChannel();
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
			if (replyTimeout == null || gatewayAnnotation.replyTimeout() != Long.MIN_VALUE) {
				replyTimeout = new ValueExpression<>(gatewayAnnotation.replyTimeout());
			}
			if (StringUtils.hasText(gatewayAnnotation.replyTimeoutExpression())) {
				replyTimeout = ExpressionUtils.longExpression(gatewayAnnotation.replyTimeoutExpression());
			}
			if (payloadExpression == null || StringUtils.hasText(gatewayAnnotation.payloadExpression())) {
				payloadExpression = gatewayAnnotation.payloadExpression();
			}

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
		else if (this.methodMetadataMap != null && this.methodMetadataMap.size() > 0) {
			GatewayMethodMetadata methodMetadata = this.methodMetadataMap.get(method.getName());
			if (methodMetadata != null) {
				if (StringUtils.hasText(methodMetadata.getPayloadExpression())) {
					payloadExpression = methodMetadata.getPayloadExpression();
				}
				if (!CollectionUtils.isEmpty(methodMetadata.getHeaderExpressions())) {
					headerExpressions.putAll(methodMetadata.getHeaderExpressions());
				}
				requestChannelName = methodMetadata.getRequestChannelName();
				replyChannelName = methodMetadata.getReplyChannelName();
				String reqTimeout = methodMetadata.getRequestTimeout();
				if (StringUtils.hasText(reqTimeout)) {
					requestTimeout = ExpressionUtils.longExpression(reqTimeout);
				}
				String repTimeout = methodMetadata.getReplyTimeout();
				if (StringUtils.hasText(repTimeout)) {
					replyTimeout = ExpressionUtils.longExpression(repTimeout);
				}
			}
		}
		Map<String, Object> headers = null;
		// We don't want to eagerly resolve the error channel here
		Object errorChannel = this.errorChannel == null ? this.errorChannelName : this.errorChannel;
		if (errorChannel != null && method.getReturnType().equals(void.class)) {
			headers = new HashMap<>();
			headers.put(MessageHeaders.ERROR_CHANNEL, errorChannel);
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

			for (String header : headerNames) {
				if ((MessageHeaders.ID.equals(header) || MessageHeaders.TIMESTAMP.equals(header))) {
					throw new BeanInitializationException(
							"Messaging Gateway cannot override 'id' and 'timestamp' read-only headers.\n" +
									"Wrong headers configuration for " + getComponentName());
				}
			}
		}

		GatewayMethodInboundMessageMapper messageMapper = new GatewayMethodInboundMessageMapper(method,
				headerExpressions,
				this.globalMethodMetadata != null ? this.globalMethodMetadata.getHeaderExpressions() : null,
				headers, this.argsMapper, this.getMessageBuilderFactory());
		if (StringUtils.hasText(payloadExpression)) {
			messageMapper.setPayloadExpression(payloadExpression);
		}
		messageMapper.setBeanFactory(getBeanFactory());
		MethodInvocationGateway gateway = new MethodInvocationGateway(messageMapper);

		if (this.errorChannel != null) {
			gateway.setErrorChannel(this.errorChannel);
		}
		else if (StringUtils.hasText(this.errorChannelName)) {
			gateway.setErrorChannelName(this.errorChannelName);
		}

		if (this.getTaskScheduler() != null) {
			gateway.setTaskScheduler(this.getTaskScheduler());
		}
		gateway.setBeanName(this.getComponentName());

		if (StringUtils.hasText(requestChannelName)) {
			gateway.setRequestChannelName(requestChannelName);
		}
		else if (StringUtils.hasText(this.defaultRequestChannelName)) {
			gateway.setRequestChannelName(this.defaultRequestChannelName);
		}
		else {
			gateway.setRequestChannel(this.defaultRequestChannel);
		}

		if (StringUtils.hasText(replyChannelName)) {
			gateway.setReplyChannelName(replyChannelName);
		}
		else if (StringUtils.hasText(this.defaultReplyChannelName)) {
			gateway.setReplyChannelName(this.defaultReplyChannelName);
		}
		else {
			gateway.setReplyChannel(this.defaultReplyChannel);
		}

		if (requestTimeout == null) {
			gateway.setRequestTimeout(-1);
		}
		else if (requestTimeout instanceof ValueExpression) {
			gateway.setRequestTimeout(requestTimeout.getValue(Long.class));
		}
		else {
			messageMapper.setSendTimeoutExpression(requestTimeout);
		}
		if (replyTimeout == null) {
			gateway.setReplyTimeout(-1);
		}
		else if (replyTimeout instanceof ValueExpression) {
			gateway.setReplyTimeout(replyTimeout.getValue(Long.class));
		}
		else {
			messageMapper.setReplyTimeoutExpression(replyTimeout);
		}
		if (this.getBeanFactory() != null) {
			gateway.setBeanFactory(this.getBeanFactory());
		}
		if (replyTimeout != null) {
			gateway.setReceiveTimeoutExpression(replyTimeout);
		}
		gateway.setShouldTrack(this.shouldTrack);
		gateway.afterPropertiesSet();
		return gateway;
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

	private static boolean hasReturnParameterizedWithMessage(Method method, boolean runningOnCallerThread) {
		if (!runningOnCallerThread &&
				(Future.class.isAssignableFrom(method.getReturnType())
						|| Mono.class.isAssignableFrom(method.getReturnType()))) {
			Type returnType = method.getGenericReturnType();
			if (returnType instanceof ParameterizedType) {
				Type[] typeArgs = ((ParameterizedType) returnType).getActualTypeArguments();
				if (typeArgs != null && typeArgs.length == 1) {
					Type parameterizedType = typeArgs[0];
					if (parameterizedType instanceof ParameterizedType) {
						Type rawType = ((ParameterizedType) parameterizedType).getRawType();
						if (rawType instanceof Class) {
							return Message.class.isAssignableFrom((Class<?>) rawType);
						}
					}
				}
			}
		}
		return false;
	}


	private static final class MethodInvocationGateway extends MessagingGatewaySupport {

		Expression receiveTimeoutExpression;

		MethodInvocationGateway(GatewayMethodInboundMessageMapper messageMapper) {
			setRequestMapper(messageMapper);
		}

		Expression getReceiveTimeoutExpression() {
			return this.receiveTimeoutExpression;
		}

		void setReceiveTimeoutExpression(Expression receiveTimeoutExpression) {
			this.receiveTimeoutExpression = receiveTimeoutExpression;
		}

	}

	private final class Invoker implements Supplier<Object> {

		private final MethodInvocation invocation;

		Invoker(MethodInvocation methodInvocation) {
			this.invocation = methodInvocation;
		}

		@Override
		public Object get() {
			try {
				return doInvoke(this.invocation, false);
			}
			catch (Error e) { //NOSONAR
				throw e;
			}
			catch (Throwable t) { //NOSONAR
				if (t instanceof RuntimeException) {
					throw (RuntimeException) t;
				}
				throw new MessagingException("Asynchronous gateway invocation failed", t);
			}
		}

	}

}
