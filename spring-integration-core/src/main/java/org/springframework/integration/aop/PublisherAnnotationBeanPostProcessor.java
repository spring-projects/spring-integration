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

package org.springframework.integration.aop;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
import org.springframework.util.ClassUtils;

/**
 * Post-processes beans that contain the method-level @{@link Publisher} annotation.
 *
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 * @author Rick Hogge
 *
 * @since 2.0
 */
@SuppressWarnings("serial")
public class PublisherAnnotationBeanPostProcessor extends ProxyConfig
		implements BeanPostProcessor, BeanClassLoaderAware, BeanFactoryAware, InitializingBean, Ordered {

	private final Set<Class<?>> nonApplicableCache =
			Collections.newSetFromMap(new ConcurrentHashMap<Class<?>, Boolean>(256));

	private volatile String defaultChannelName;

	private volatile Integer metadataCacheLimit;

	private volatile PublisherAnnotationAdvisor advisor;

	private volatile int order = Ordered.LOWEST_PRECEDENCE;

	private volatile BeanFactory beanFactory;

	private volatile ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	/**
	 * Set the default channel where Messages should be sent if the annotation
	 * itself does not provide a channel.
	 * @param defaultChannelName the publisher interceptor defaultChannel
	 * @since 4.0.3
	 */
	public void setDefaultChannelName(String defaultChannelName) {
		this.defaultChannelName = defaultChannelName;
	}

	/**
	 * Specify a limit for the method metadata cache.
	 * @param metadataCacheLimit the cache limit to use.
	 * @since 5.0.4
	 */
	public void setMetadataCacheLimit(int metadataCacheLimit) {
		this.metadataCacheLimit = metadataCacheLimit;
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

	@Override
	public void afterPropertiesSet() {
		this.advisor = new PublisherAnnotationAdvisor();
		this.advisor.setBeanFactory(this.beanFactory);
		this.advisor.setDefaultChannelName(this.defaultChannelName);
		if (this.metadataCacheLimit != null) {
			this.advisor.setMetadataCacheLimit(this.metadataCacheLimit);
		}
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		Class<?> targetClass = AopUtils.getTargetClass(bean);

		// the set will hold records of prior class scans and will contain the bean classes that can not
		// be assigned to the Advisor interface and therefore can be short circuited
		if (this.nonApplicableCache.contains(targetClass)) {
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
			this.nonApplicableCache.add(targetClass);
			return bean;
		}
	}

}
