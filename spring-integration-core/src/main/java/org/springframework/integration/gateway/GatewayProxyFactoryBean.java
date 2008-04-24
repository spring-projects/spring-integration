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

	private ResponseCorrelator responseCorrelator;

	private EndpointRegistry endpointRegistry;

	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private Object serviceProxy;

	private MessageCreator messageCreator = new DefaultMessageCreator();

	private MessageMapper messageMapper = new DefaultMessageMapper();


	public void setServiceInterface(Class<?> serviceInterface) {
		this.serviceInterface = serviceInterface;
	}

	public void setRequestChannel(MessageChannel requestChannel) {
		this.requestChannel = requestChannel;
	}

	public void setResponseChannel(MessageChannel responseChannel) {
		this.responseChannel = responseChannel;
	}

	public void setMessageCreator(MessageCreator<?, ?> messageCreator) {
		Assert.notNull(messageCreator, "messageCreator must not be null");
		this.messageCreator = messageCreator;
	}

	public void setMessageMapper(MessageMapper<?, ?> messageMapper) {
		Assert.notNull(messageMapper, "messageMapper must not be null");
		this.messageMapper = messageMapper;
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
		this.registerResponseCorrelatorIfNecessary();
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
		boolean returnsVoid = invocation.getMethod().getReturnType().equals(void.class);
		int params = invocation.getMethod().getParameterTypes().length;
		if (params == 0) {
			// TODO: add support for receive-only
			throw new MessagingException("Method invocation contains no arguments. Cannot send a message.");
		}
		if (this.requestChannel == null) {
			throw new MessagingException("No request channel available. Cannot invoke methods with arguments.");
		}
		Object payload = (params == 1) ? invocation.getArguments()[0] : invocation.getArguments();
		Message<?> message = this.messageCreator.createMessage(payload);
		if (returnsVoid) {
			this.requestChannel.send(message);
			return null;
		}
		Message<?> response = null;
		if (this.responseCorrelator != null) {
			message.getHeader().setReturnAddress(this.responseChannel);
			this.requestChannel.send(message);
			response = this.responseCorrelator.getResponse(message.getId());
		}
		else {
			RendezvousChannel temporaryChannel = new RendezvousChannel();
			message.getHeader().setReturnAddress(temporaryChannel);
			this.requestChannel.send(message);
			response = temporaryChannel.receive();
		}
		return (response != null) ? this.messageMapper.mapMessage(response) : null;
	}

	private void registerResponseCorrelatorIfNecessary() {
		if (this.responseChannel != null) {
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
