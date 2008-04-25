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

package org.springframework.integration.gateway;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.RendezvousChannel;
import org.springframework.integration.config.MessageBusParser;
import org.springframework.integration.endpoint.EndpointRegistry;
import org.springframework.integration.endpoint.HandlerEndpoint;
import org.springframework.integration.handler.ResponseCorrelator;
import org.springframework.integration.message.DefaultMessageCreator;
import org.springframework.integration.message.DefaultMessageMapper;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageCreator;
import org.springframework.integration.message.MessageMapper;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.scheduling.Subscription;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Generates a proxy for the provided service interface to enable interaction
 * with messaging components without application code being aware of them.
 * 
 * @author Mark Fisher
 */
public class GatewayProxyFactoryBean implements FactoryBean, MethodInterceptor, InitializingBean, ApplicationContextAware, BeanClassLoaderAware {

	private Class<?> serviceInterface;

	private MessageChannel requestChannel;

	private MessageChannel responseChannel;

	private long requestTimeout = 0;

	private long responseTimeout = 1000;

	private ResponseCorrelator responseCorrelator;

	private MessageCreator messageCreator = new DefaultMessageCreator();

	private MessageMapper messageMapper = new DefaultMessageMapper();

	private TypeConverter typeConverter = new SimpleTypeConverter();

	private EndpointRegistry endpointRegistry;

	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private Object serviceProxy;

	private final Object responseCorrelatorMonitor = new Object();


	public void setServiceInterface(Class<?> serviceInterface) {
		this.serviceInterface = serviceInterface;
	}

	public void setRequestChannel(MessageChannel requestChannel) {
		this.requestChannel = requestChannel;
	}

	public void setResponseChannel(MessageChannel responseChannel) {
		this.responseChannel = responseChannel;
	}

	public void setRequestTimeout(long requestTimeout) {
		this.requestTimeout = requestTimeout;
	}

	public void setResponseTimeout(long responseTimeout) {
		this.responseTimeout = responseTimeout;
	}

	public void setMessageCreator(MessageCreator<?, ?> messageCreator) {
		Assert.notNull(messageCreator, "messageCreator must not be null");
		this.messageCreator = messageCreator;
	}

	public void setMessageMapper(MessageMapper<?, ?> messageMapper) {
		Assert.notNull(messageMapper, "messageMapper must not be null");
		this.messageMapper = messageMapper;
	}

	public void setTypeConverter(TypeConverter typeConverter) {
		Assert.notNull(typeConverter, "typeConverter must not be null");
		this.typeConverter = typeConverter;
	}

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		if (applicationContext.containsBean(MessageBusParser.MESSAGE_BUS_BEAN_NAME)) {
			this.endpointRegistry = (EndpointRegistry) applicationContext.getBean(MessageBusParser.MESSAGE_BUS_BEAN_NAME);
		}
	}

	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}

	public void afterPropertiesSet() {
		this.serviceProxy = new ProxyFactory(this.serviceInterface, this).getProxy(this.beanClassLoader);
	}

	public Object getObject() throws Exception {
		return this.serviceProxy;
	}

	public Class<?> getObjectType() {
		return this.serviceInterface;
	}

	public boolean isSingleton() {
		return true;
	}

	public Object invoke(MethodInvocation invocation) throws Throwable {
		Class<?> returnType = invocation.getMethod().getReturnType();
		int params = invocation.getMethod().getParameterTypes().length;
		Message<?> response = null;
		if (params == 0) {
			if (this.responseChannel == null) {
				throw new MessagingException("No response channel available. Cannot support methods with no arguments.");
			}
			response = this.receiveResponse(this.responseChannel);
		}
		else {
			if (this.requestChannel == null) {
				throw new MessagingException("No request channel available. Cannot support methods with arguments.");
			}
			Object payload = (params == 1) ? invocation.getArguments()[0] : invocation.getArguments();
			Message<?> message = this.messageCreator.createMessage(payload);
			if (returnType.equals(void.class)) {
				this.sendRequest(message);
				return null;
			}
			if (this.responseChannel != null) {
				response = this.sendAndReceiveWithResponseCorrelator(message);
			}
			else {
				response = this.sendAndReceiveWithTemporaryChannel(message);
			}
		}
		if (returnType.isAssignableFrom(response.getClass())) {
			return response;
		}
		Object responseObject = (response != null) ? this.messageMapper.mapMessage(response) : null;
		return this.typeConverter.convertIfNecessary(responseObject, returnType);
	}

	private Message<?> sendAndReceiveWithResponseCorrelator(Message<?> message) {
		if (this.responseCorrelator == null) {
			this.registerResponseCorrelator();
		}
		message.getHeader().setReturnAddress(this.responseChannel);
		this.sendRequest(message);
		return (this.responseTimeout >= 0) ? this.responseCorrelator.getResponse(message.getId(), this.responseTimeout) :
			this.responseCorrelator.getResponse(message.getId());
	}

	private Message<?> sendAndReceiveWithTemporaryChannel(Message<?> message) {
		RendezvousChannel temporaryChannel = new RendezvousChannel();
		message.getHeader().setReturnAddress(temporaryChannel);
		this.sendRequest(message);
		return this.receiveResponse(temporaryChannel);
	}

	private void sendRequest(Message<?> message) {
		if (message == null) {
			throw new MessagingException("Created Message is null, cannot be sent.");
		}
		boolean sent = (this.requestTimeout >= 0) ?
				this.requestChannel.send(message, this.requestTimeout) : this.requestChannel.send(message);
		if (!sent) {
			throw new MessagingException("Failed to send request message.");
		}
	}

	private Message<?> receiveResponse(MessageChannel channel) {
		return (this.responseTimeout >= 0) ? channel.receive(this.responseTimeout) : channel.receive();
	}

	private void registerResponseCorrelator() {
		synchronized (this.responseCorrelatorMonitor) {
			if (this.responseCorrelator != null) {
				return;
			}
			if (this.endpointRegistry == null) {
				throw new ConfigurationException("No EndpointRegistry available. Cannot register ResponseCorrelator.");
			}
			ResponseCorrelator correlator = new ResponseCorrelator(10);
			HandlerEndpoint endpoint = new HandlerEndpoint(correlator);
			endpoint.setSubscription(new Subscription(this.responseChannel));
			this.endpointRegistry.registerEndpoint(
					this.serviceInterface.getName() + "-" + this.responseChannel + "-correlator", endpoint);
			this.responseCorrelator = correlator;
		}
	}

}
