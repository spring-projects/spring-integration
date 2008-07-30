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

package org.springframework.integration.bus;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.integration.channel.ChannelInterceptor;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.channel.factory.ChannelFactory;
import org.springframework.integration.channel.factory.QueueChannelFactory;
import org.springframework.util.Assert;

/**
 * Creates a channel by delegating to the current message bus' configured
 * ChannelFactory. Tries to retrieve the {@link ChannelFactory} from the
 * single {@link MessageBus} defined in the {@link ApplicationContext}.
 * As a {@link FactoryBean}, this class is solely intended to be used within
 * an ApplicationContext.
 *
 * @author Marius Bogoevici
 * @author Mark Fisher
 */
public class DefaultChannelFactoryBean implements ApplicationContextAware, FactoryBean, BeanNameAware, InitializingBean {

	private volatile String beanName;

	private volatile List<ChannelInterceptor> interceptors;

	private volatile ApplicationContext applicationContext;

	private volatile boolean initialized;

	private final Object initializationMonitor = new Object();

	private volatile Object proxyBean;


	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	public void setInterceptors(List<ChannelInterceptor> interceptors) {
		this.interceptors = interceptors;
	}

	public void afterPropertiesSet() throws Exception {
		synchronized (this.initializationMonitor) {
			if (!initialized) {
				this.proxyBean = Proxy.newProxyInstance(
						getClass().getClassLoader(),
						new Class[] { PollableChannel.class },
						new DefaultChannelInvocationHandler());
				this.initialized = true;
			}
		}
	}

	public Object getObject() throws Exception {
		if (!this.initialized) {
			afterPropertiesSet();
		}
		return proxyBean;
	}

	public Class<?> getObjectType() {
		return PollableChannel.class;
	}

	public boolean isSingleton() {
		return true;
	}


	private class DefaultChannelInvocationHandler implements InvocationHandler {

		private volatile AtomicReference<MessageChannel> targetChannelReference = new AtomicReference<MessageChannel>();


		private MessageChannel getTargetChannel() {
			if (targetChannelReference.get() == null) {
				targetChannelReference.compareAndSet(null, createMessageChannel());
			}
			return targetChannelReference.get();
		}

		private MessageChannel createMessageChannel() {
			ChannelFactory channelFactory;
			Map map = DefaultChannelFactoryBean.this.applicationContext.getBeansOfType(MessageBus.class);
			Assert.state(map.size() <= 1, "There is more than one MessageBus in the ApplicationContext");
			if (map.isEmpty()) {
				channelFactory = new QueueChannelFactory();
			}
			else {
				channelFactory = ((MessageBus) map.values().iterator().next()).getChannelFactory();
			}
			return channelFactory.getChannel(beanName, interceptors);
		}

		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (method.getName().equals("equals")) {
				return proxy == args[0];
			}
			else if (method.getName().equals("hashCode")) {
				return System.identityHashCode(proxy);
			}
			else {
				try {
					return method.invoke(getTargetChannel(), args);
				}
				catch (InvocationTargetException e) {
					throw e.getCause();
				}
			}
		}

	}

}
