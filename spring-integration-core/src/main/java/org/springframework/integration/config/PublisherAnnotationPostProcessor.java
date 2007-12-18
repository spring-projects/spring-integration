/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.config;

import java.lang.annotation.Annotation;

import org.springframework.aop.Advisor;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.integration.aop.Publisher;
import org.springframework.integration.aop.PublisherAnnotationAdvisor;
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.util.Assert;

/**
 * A {@link BeanPostProcessor} that adds a message publishing interceptor when
 * it discovers annotated methods.
 * 
 * @author Mark Fisher
 */
public class PublisherAnnotationPostProcessor implements BeanPostProcessor, BeanClassLoaderAware {

	private Class<? extends Annotation> publisherAnnotationType = Publisher.class;

	private String channelNameAttribute = "channel";

	private ChannelRegistry channelRegistry;

	private Advisor advisor;

	private ClassLoader beanClassLoader;


	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		Assert.notNull(beanClassLoader, "'beanClassLoader' must not be null");
		this.beanClassLoader = beanClassLoader;
	}

	public void setPublisherAnnotationType(Class<? extends Annotation> publisherAnnotationType) {
		Assert.notNull(publisherAnnotationType, "'publisherAnnotationType' must not be null");
		this.publisherAnnotationType = publisherAnnotationType;
	}

	public void setChannelNameAttribute(String channelNameAttribute) {
		Assert.notNull(channelNameAttribute, "'channelNameAttribute' must not be null");
		this.channelNameAttribute = channelNameAttribute;
	}

	public void setChannelRegistry(ChannelRegistry channelRegistry) {
		Assert.notNull(channelRegistry, "'channelRegistry' must not be null");
		this.channelRegistry = channelRegistry;
	}

	private void createAdvisor() {
		if (this.channelRegistry == null) {
			throw new IllegalStateException("'channelRegistry' is required");
		}
		this.advisor = new PublisherAnnotationAdvisor(this.publisherAnnotationType, this.channelNameAttribute,
				this.channelRegistry);
	}

	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		Class<?> targetClass = bean instanceof Advised ? 
				((Advised) bean).getTargetSource().getTargetClass() : bean.getClass();
		if (targetClass == null) {
			return bean;
		}
		if (advisor == null) {
			createAdvisor();
		}
		if (AopUtils.canApply(this.advisor, targetClass)) {
			if (bean instanceof Advised) {
				((Advised) bean).addAdvisor(this.advisor);
				return bean;
			}
			else {
				ProxyFactory pf = new ProxyFactory(bean);
				pf.addAdvisor(this.advisor);
				return pf.getProxy(this.beanClassLoader);
			}
		}
		else {
			return bean;
		}
	}

}
