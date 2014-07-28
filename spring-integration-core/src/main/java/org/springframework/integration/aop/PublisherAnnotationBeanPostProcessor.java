/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.aop;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyConfig;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.integration.annotation.Publisher;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.ClassUtils;

/**
 * Post-processes beans that contain the method-level @{@link Publisher} annotation.
 *
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
@SuppressWarnings("serial")
public class PublisherAnnotationBeanPostProcessor extends ProxyConfig
		implements BeanPostProcessor, BeanClassLoaderAware, BeanFactoryAware, InitializingBean, Ordered {

	private volatile MessageChannel defaultChannel;

	private volatile String defaultChannelName;

	private volatile PublisherAnnotationAdvisor advisor;

	private volatile int order = Ordered.LOWEST_PRECEDENCE;

	private volatile BeanFactory beanFactory;

	private volatile ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();


	/**
	 * Set the default channel where Messages should be sent if the annotation
	 * itself does not provide a channel.
	 * @param defaultChannel The default channel.
	 * @deprecated Use {@link #setDefaultChannelName(String)}
	 */
	@Deprecated
	public void setDefaultChannel(MessageChannel defaultChannel){
		this.defaultChannel = defaultChannel;
	}

	/**
	 * Set the default channel where Messages should be sent if the annotation
	 * itself does not provide a channel.
	 * @param defaultChannelName the publisher interceptor defaultChannel
	 * @since 4.0.3
	 */
	public void setDefaultChannelName(String defaultChannelName) {
		this.defaultChannelName = defaultChannelName;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void afterPropertiesSet(){
		this.advisor = new PublisherAnnotationAdvisor();
		this.advisor.setBeanFactory(this.beanFactory);
		if (this.defaultChannel != null) {
			this.advisor.setDefaultChannel(this.defaultChannel);
		}
		else {
			this.advisor.setDefaultChannelName(this.defaultChannelName);
		}
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		Class<?> targetClass = AopUtils.getTargetClass(bean);
		if (targetClass == null) {
			return bean;
		}

		if (AopUtils.canApply(this.advisor, targetClass)) {
			if (bean instanceof Advised) {
				((Advised) bean).addAdvisor(this.advisor);
				return bean;
			}
			else {
				ProxyFactory proxyFactory = new ProxyFactory(bean);
				// Copy our properties (proxyTargetClass etc) inherited from ProxyConfig.
				proxyFactory.copyFrom(this);
				proxyFactory.addAdvisor(this.advisor);
				return proxyFactory.getProxy(this.beanClassLoader);
			}
		}
		else {
			// cannot apply advisor
			return bean;
		}
	}

}
