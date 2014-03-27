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

package org.springframework.integration.security.config;

import java.util.Collection;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.integration.security.channel.ChannelSecurityInterceptor;
import org.springframework.integration.security.channel.ChannelSecurityMetadataSource;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

/**
 * A {@link BeanPostProcessor} that proxies {@link MessageChannel}s to apply a {@link ChannelSecurityInterceptor}.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 */
public class ChannelSecurityInterceptorBeanPostProcessor implements BeanPostProcessor, BeanFactoryAware, InitializingBean {

	private volatile Collection<ChannelSecurityInterceptor> securityInterceptors;

	private ListableBeanFactory beanFactory;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		Assert.isInstanceOf(ListableBeanFactory.class, beanFactory);
		this.beanFactory = (ListableBeanFactory) beanFactory;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.securityInterceptors = this.beanFactory.getBeansOfType(ChannelSecurityInterceptor.class).values();
	}

	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof MessageChannel) {
			for (ChannelSecurityInterceptor securityInterceptor : securityInterceptors) {
				ChannelSecurityMetadataSource channelSecurityMetadataSource =
						(ChannelSecurityMetadataSource) securityInterceptor.obtainSecurityMetadataSource();
				if (this.shouldProxy(beanName, channelSecurityMetadataSource)) {
					if (AopUtils.isAopProxy(bean) && bean instanceof Advised) {
						((Advised) bean).addAdvisor(new DefaultPointcutAdvisor(securityInterceptor));
					}
					else {
						ProxyFactory proxyFactory = new ProxyFactory(bean);
						proxyFactory.addAdvisor(new DefaultPointcutAdvisor(securityInterceptor));
						bean = proxyFactory.getProxy();
					}
				}
			}
		}
		return bean;
	}

	private boolean shouldProxy(String beanName, ChannelSecurityMetadataSource channelSecurityMetadataSource) {
		Set<Pattern> patterns = channelSecurityMetadataSource.getPatterns();
		for (Pattern pattern : patterns) {
			if (pattern.matcher(beanName).matches()) {
				return true;
			}
		}
		return false;
	}
}
