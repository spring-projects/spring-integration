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

package org.springframework.integration.gateway;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.Lifecycle;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.handler.ArgumentArrayMessageMapper;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Generates a proxy for the provided service interface to enable interaction
 * with messaging components without application code being aware of them.
 * 
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class GatewayProxyFactoryBean extends AbstractEndpoint implements FactoryBean<Object>, MethodInterceptor, BeanClassLoaderAware {

	private volatile Class<?> serviceInterface;

	private volatile MessageChannel defaultRequestChannel;

	private volatile MessageChannel defaultReplyChannel;

	private volatile long defaultRequestTimeout = -1;

	private volatile long defaultReplyTimeout = -1;

	private volatile TypeConverter typeConverter = new SimpleTypeConverter();

	private volatile ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private volatile Object serviceProxy;

	private final Map<Method, SimpleMessagingGateway> gatewayMap = new HashMap<Method, SimpleMessagingGateway>();

	private volatile boolean initialized;

	private final Object initializationMonitor = new Object();

	private Map<String, GatewayMethodDefinition> methodToChannelMap;


	/**
	 * Create a Factory whose service interface type can be configured by setter injection.
	 * If none is set, it will fall back to the default service interface type,
	 * {@link RequestReplyExchanger}, upon initialization.
	 */
	public GatewayProxyFactoryBean() {
		// serviceInterface will be determined on demand later
	}

	public GatewayProxyFactoryBean(Class<?> serviceInterface) {
		this.serviceInterface = serviceInterface;
	}


	/**
	 * Set the interface class that the generated proxy should implement.
	 * If none is provided explicitly, the default is MessageHandler.
	 */
	public void setServiceInterface(Class<?> serviceInterface) {
		Assert.notNull(serviceInterface, "'serviceInterface' must not be null");
		Assert.isTrue(serviceInterface.isInterface(), "'serviceInterface' must be an interface");
		this.serviceInterface = serviceInterface;
	}


	/**
	 * Set the default request channel.
	 * 
	 * @param defaultRequestChannel the channel to which request messages will
	 * be sent if no request channel has been configured with an annotation
	 */
	public void setDefaultRequestChannel(MessageChannel defaultRequestChannel) {
		this.defaultRequestChannel = defaultRequestChannel;
	}

	/**
	 * Set the default reply channel. If no default reply channel is provided,
	 * and no reply channel is configured with annotations, an anonymous,
	 * temporary channel will be used for handling replies.
	 * 
	 * @param defaultReplyChannel the channel from which reply messages will be
	 * received if no reply channel has been configured with an annotation
	 */
	public void setDefaultReplyChannel(MessageChannel defaultReplyChannel) {
		this.defaultReplyChannel = defaultReplyChannel;
	}

	/**
	 * Set the default timeout value for sending request messages. If not
	 * explicitly configured with an annotation, this value will be used.
	 * 
	 * @param defaultRequestTimeout the timeout value in milliseconds
	 */
	public void setDefaultRequestTimeout(long defaultRequestTimeout) {
		this.defaultRequestTimeout = defaultRequestTimeout;
	}

	/**
	 * Set the default timeout value for receiving reply messages. If not
	 * explicitly configured with an annotation, this value will be used.
	 * 
	 * @param defaultReplyTimeout the timeout value in milliseconds
	 */
	public void setDefaultReplyTimeout(long defaultReplyTimeout) {
		this.defaultReplyTimeout = defaultReplyTimeout;
	}

	public void setTypeConverter(TypeConverter typeConverter) {
		Assert.notNull(typeConverter, "typeConverter must not be null");
		this.typeConverter = typeConverter;
	}

	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}

	@Override
	protected void onInit() {
		synchronized (this.initializationMonitor) {
			if (this.initialized) {
				return;
			}
			Class<?> proxyInterface = this.determineServiceInterface();
			Method[] methods = proxyInterface.getDeclaredMethods();
			for (Method method : methods) {
				SimpleMessagingGateway gateway = this.createGatewayForMethod(method);
				this.gatewayMap.put(method, gateway);
			}
			this.serviceProxy = new ProxyFactory(proxyInterface, this).getProxy(this.beanClassLoader);
			this.start();
			this.initialized = true;
		}
	}

	private Class<?> determineServiceInterface() {
		if (this.serviceInterface == null) {
			this.serviceInterface = RequestReplyExchanger.class;
		}
		return this.serviceInterface;
	}

	public Class<?> getObjectType() {
		return (this.serviceInterface != null ? this.serviceInterface : null);
	}

	public Object getObject() throws Exception {
		if (this.serviceProxy == null) {
			this.onInit();
			Assert.notNull(this.serviceProxy, "failed to initialize proxy");
		}
		return this.serviceProxy;
	}

	public boolean isSingleton() {
		return true;
	}

	public Object invoke(MethodInvocation invocation) throws Throwable {
		Method method = invocation.getMethod();
		if (AopUtils.isToStringMethod(method)) {
			return "gateway proxy for service interface [" + this.serviceInterface + "]";
		}
		if (method.getDeclaringClass().equals(this.serviceInterface)) {
			try {
				return this.invokeGatewayMethod(invocation);
			}
			catch (Exception e) {
				rethrowExceptionInThrowsClauseIfPossible(e, invocation.getMethod());
			}
		}
		return invocation.proceed();
	}

	private Object invokeGatewayMethod(MethodInvocation invocation) throws Exception {
		if (!this.initialized) {
			this.afterPropertiesSet();
		}
		Method method = invocation.getMethod();
		SimpleMessagingGateway gateway = this.gatewayMap.get(method);
		Class<?> returnType = method.getReturnType();
		boolean isReturnTypeMessage = Message.class.isAssignableFrom(returnType);
		boolean shouldReply = returnType != void.class;
		int paramCount = method.getParameterTypes().length;
		Object response = null;
		if (paramCount == 0) {
			if (shouldReply) {
				if (isReturnTypeMessage) {
					return gateway.receive();
				}
				response = gateway.receive();
			}
		}
		else {
			Object[] args = invocation.getArguments();
			if (shouldReply) {
				response = isReturnTypeMessage ? gateway.sendAndReceiveMessage(args) : gateway.sendAndReceive(args);
			}
			else {
				gateway.send(args);
				response = null;
			}
		}
		return (response != null) ? this.typeConverter.convertIfNecessary(response, returnType) : null;
	}

	private void rethrowExceptionInThrowsClauseIfPossible(Throwable originalException, Method method) throws Throwable {
		List<Class<?>> exceptionTypes = Arrays.asList(method.getExceptionTypes());
		Throwable t = originalException;
		while (t != null) {
			if (exceptionTypes.contains(t.getClass())) {
				throw t;
			}
			t = t.getCause();
		}
		throw originalException;
	}

	private SimpleMessagingGateway createGatewayForMethod(Method method) {
		Gateway gatewayAnnotation = method.getAnnotation(Gateway.class);
		MessageChannel requestChannel = this.defaultRequestChannel;
		MessageChannel replyChannel = this.defaultReplyChannel;
		long requestTimeout = this.defaultRequestTimeout;
		long replyTimeout = this.defaultReplyTimeout;
		Map<String, Object> staticHeaders = null;
		if (gatewayAnnotation != null) {
			Assert.state(this.getChannelResolver() != null, "ChannelResolver is required");
			String requestChannelName = gatewayAnnotation.requestChannel();
			requestChannel = this.resolveChannel(requestChannel, requestChannelName);
			String replyChannelName = gatewayAnnotation.replyChannel();
			replyChannel = this.resolveChannel(replyChannel, replyChannelName);
			requestTimeout = gatewayAnnotation.requestTimeout();
			replyTimeout = gatewayAnnotation.replyTimeout();
		}
		else if (methodToChannelMap != null && methodToChannelMap.size() > 0) {	
			Assert.state(this.getChannelResolver() != null, "ChannelResolver is required");
			GatewayMethodDefinition gatewayDefinition = methodToChannelMap.get(method.getName());	
			if (gatewayDefinition != null) {
				staticHeaders = gatewayDefinition.getStaticHeaders();
				String requestChannelName = gatewayDefinition.getRequestChannelName();
				requestChannel = this.resolveChannel(requestChannel, requestChannelName);
				String replyChannelName = gatewayDefinition.getReplyChannelName();
				replyChannel = this.resolveChannel(replyChannel, replyChannelName);
				String reqTimeout = gatewayDefinition.getRequestTimeout();
				if (StringUtils.hasText(reqTimeout)){
					requestTimeout = typeConverter.convertIfNecessary(reqTimeout, Long.class);
				}
				String repTimeout = gatewayDefinition.getReplyTimeout();
				if (StringUtils.hasText(repTimeout)){
					replyTimeout = typeConverter.convertIfNecessary(repTimeout, Long.class);
				}
			}
		}
		ArgumentArrayMessageMapper messageMapper = new ArgumentArrayMessageMapper(method, staticHeaders);
		SimpleMessagingGateway gateway = new SimpleMessagingGateway(messageMapper, new SimpleMessageMapper());
		if (this.getTaskScheduler() != null) {
			gateway.setTaskScheduler(this.getTaskScheduler());
		}
		gateway.setBeanName(this.getComponentName());
		gateway.setRequestChannel(requestChannel);
		gateway.setReplyChannel(replyChannel);
		gateway.setRequestTimeout(requestTimeout);
		gateway.setReplyTimeout(replyTimeout);
		if (this.getBeanFactory() != null) {
			gateway.setBeanFactory(this.getBeanFactory());
		}
		return gateway;
	}
	
	private MessageChannel resolveChannel(MessageChannel channel, String channelName) {
		if (StringUtils.hasText(channelName)) {
			channel = this.getChannelResolver().resolveChannelName(channelName);
			Assert.notNull(channel, "failed to resolve channel '" + channelName + "'");
		}
		return channel;
	}

	// Lifecycle implementation

	@Override // guarded by super#lifecycleLock
	protected void doStart() {
		for (SimpleMessagingGateway gateway : this.gatewayMap.values()) {
			if (gateway instanceof Lifecycle) {
				((Lifecycle) gateway).start();
			}
		}
	}

	@Override // guarded by super#lifecycleLock
	protected void doStop() {
		for (SimpleMessagingGateway gateway : this.gatewayMap.values()) {
			if (gateway instanceof Lifecycle) {
				((Lifecycle) gateway).stop();
			}
		}
	}
	
	public Map<String, GatewayMethodDefinition> getMethodToChannelMap() {
		return methodToChannelMap;
	}

	public void setMethodToChannelMap(Map<String, GatewayMethodDefinition> methodToChannelMap) {
		this.methodToChannelMap = methodToChannelMap;
	}

}
