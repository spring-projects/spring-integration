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

package org.springframework.integration.security.config;

import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.messaging.MessageChannel;
import org.springframework.integration.security.channel.ChannelSecurityMetadataSource;
import org.springframework.integration.security.channel.ChannelSecurityInterceptor;
import org.springframework.util.Assert;

/**
 * A {@link BeanPostProcessor} that proxies {@link MessageChannel}s to apply a {@link ChannelSecurityInterceptor}.
 * 
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class ChannelSecurityInterceptorBeanPostProcessor implements BeanPostProcessor {

	private final ChannelSecurityInterceptor interceptor;


	public ChannelSecurityInterceptorBeanPostProcessor(ChannelSecurityInterceptor interceptor) {
		Assert.notNull(interceptor, "interceptor must not be null");
		this.interceptor = interceptor;
	}


	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		ChannelSecurityMetadataSource channelSecurityMetadataSource =
				(ChannelSecurityMetadataSource) this.interceptor.obtainSecurityMetadataSource();
		if (bean instanceof MessageChannel && 
				shouldProxy(beanName, (MessageChannel) bean, channelSecurityMetadataSource)) {
			ProxyFactory proxyFactory = new ProxyFactory(bean);
			proxyFactory.addAdvisor(new DefaultPointcutAdvisor(this.interceptor));
			return proxyFactory.getProxy();
		}
		return bean;
	}

	private boolean shouldProxy(String beanName, MessageChannel channel, ChannelSecurityMetadataSource channelSecurityMetadataSource) {
		Set<Pattern> patterns = channelSecurityMetadataSource.getPatterns();
		for (Pattern pattern : patterns) {
			if (pattern.matcher(beanName).matches()) {
				return true;
			}
		}
		return false;
	}

}
