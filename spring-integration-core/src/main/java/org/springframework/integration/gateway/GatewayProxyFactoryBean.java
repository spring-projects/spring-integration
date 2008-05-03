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
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.config.MessageBusParser;
import org.springframework.integration.message.Message;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Generates a proxy for the provided service interface to enable interaction
 * with messaging components without application code being aware of them.
 * 
 * @author Mark Fisher
 */
public class GatewayProxyFactoryBean extends MessagingGateway
		implements FactoryBean, MethodInterceptor, InitializingBean, BeanClassLoaderAware, BeanFactoryAware {

	private Class<?> serviceInterface;

	private TypeConverter typeConverter = new SimpleTypeConverter();

	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private Object serviceProxy;


	public void setServiceInterface(Class<?> serviceInterface) {
		this.serviceInterface = serviceInterface;
	}

	public void setTypeConverter(TypeConverter typeConverter) {
		Assert.notNull(typeConverter, "typeConverter must not be null");
		this.typeConverter = typeConverter;
	}

	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}

	public void setBeanFactory(BeanFactory beanFactory) {
		this.getRequestReplyTemplate().setMessageBus(
				(MessageBus) beanFactory.getBean(MessageBusParser.MESSAGE_BUS_BEAN_NAME));
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
		boolean shouldReturnMessage = Message.class.isAssignableFrom(returnType);
		int paramCount = invocation.getMethod().getParameterTypes().length;
		Object response = null;
		if (paramCount == 0) {
			if (shouldReturnMessage) {
				return this.receive();
			}
			response = this.receive();
		}
		else {
			Object payload = (paramCount == 1) ? invocation.getArguments()[0] : invocation.getArguments();
			if (returnType.equals(void.class)) {
				this.send(payload);
				return null;
			}
			response = shouldReturnMessage ? this.sendAndReceiveMessage(payload) : this.sendAndReceive(payload);
		}
		return (response != null) ? this.typeConverter.convertIfNecessary(response, returnType) : null;
	}

}
