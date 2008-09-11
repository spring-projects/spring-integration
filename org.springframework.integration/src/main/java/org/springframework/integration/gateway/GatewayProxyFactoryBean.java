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

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
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
public class GatewayProxyFactoryBean extends SimpleMessagingGateway
		implements FactoryBean, MethodInterceptor, InitializingBean, BeanClassLoaderAware, BeanFactoryAware {

	private volatile Class<?> serviceInterface;

	private volatile TypeConverter typeConverter = new SimpleTypeConverter();

	private volatile ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private volatile Object serviceProxy;

	private volatile boolean initialized;

	private final Object initializationMonitor = new Object();


	public void setServiceInterface(Class<?> serviceInterface) {
		if (serviceInterface != null && !serviceInterface.isInterface()) {
			throw new IllegalArgumentException("'serviceInterface' must be an interface");
		}
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
		this.setMessageBus(
				(MessageBus) beanFactory.getBean(MessageBusParser.MESSAGE_BUS_BEAN_NAME));
	}

	public void afterPropertiesSet() {
		synchronized (this.initializationMonitor) {
			if (this.initialized) {
				return;
			}
			if (this.serviceInterface == null) {
				throw new IllegalArgumentException("'serviceInterface' must not be null");
			}
			this.serviceProxy = new ProxyFactory(this.serviceInterface, this).getProxy(this.beanClassLoader);
			this.initialized = true;
		}
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
		Method method = invocation.getMethod();
		if (AopUtils.isToStringMethod(method)) {
			return "gateway proxy for service interface [" + this.serviceInterface + "]";
		}
		if (method.getDeclaringClass().equals(this.serviceInterface)) {
			return this.invokeGatewayMethod(invocation);
		}
		return invocation.proceed();
	}

	private Object invokeGatewayMethod(MethodInvocation invocation) throws Exception {
		if (!this.initialized) {
			this.afterPropertiesSet();
		}
		Method method = invocation.getMethod();
		Class<?> returnType = method.getReturnType();
		boolean isReturnTypeMessage = Message.class.isAssignableFrom(returnType);
		boolean shouldReply = returnType != void.class;
		int paramCount = method.getParameterTypes().length;
		Object response = null;
		if (paramCount == 0) {
			if (shouldReply) {
				if (isReturnTypeMessage) {
					return this.receive();
				}
				response = this.receive();
			}
		}
		else {
			Object payload = (paramCount == 1) ? invocation.getArguments()[0] : invocation.getArguments();
			if (shouldReply) {
				response = isReturnTypeMessage ? this.sendAndReceiveMessage(payload) : this.sendAndReceive(payload);
			}
			else {
				this.send(payload);
				response = null;
			}
		}
		return (response != null) ? this.typeConverter.convertIfNecessary(response, returnType) : null;
	}

}
