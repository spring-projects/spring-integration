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

package org.springframework.integration.channel.interceptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.NotReadablePropertyException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.OrderComparator;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.ChannelInterceptor;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

/**
 * Will apply global interceptors to channels (&lt;channel-interceptor&gt;). 
 * 
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @since 2.0
 */
final class GlobalChannelInterceptorBeanPostProcessor implements BeanPostProcessor, InitializingBean {

	private static final Log logger = LogFactory.getLog(GlobalChannelInterceptorBeanPostProcessor.class);


	private final OrderComparator comparator = new OrderComparator();

	private volatile List<GlobalChannelInterceptorWrapper> channelInterceptors;

	private final Set<GlobalChannelInterceptorWrapper> positiveOrderInterceptors = new LinkedHashSet<GlobalChannelInterceptorWrapper>();

	private final Set<GlobalChannelInterceptorWrapper> negativeOrderInterceptors = new LinkedHashSet<GlobalChannelInterceptorWrapper>();


	GlobalChannelInterceptorBeanPostProcessor(List<GlobalChannelInterceptorWrapper> channelInterceptors) {
		this.channelInterceptors = channelInterceptors;
	}


	public void afterPropertiesSet() throws Exception {
		for (GlobalChannelInterceptorWrapper channelInterceptor : this.channelInterceptors) {
			if (channelInterceptor.getOrder() >= 0) {
				this.positiveOrderInterceptors.add(channelInterceptor);
			} 
			else {
				this.negativeOrderInterceptors.add(channelInterceptor);
			}
		}
	}

	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof MessageChannel) {
			if (logger.isDebugEnabled()) {
				logger.debug("Applying global interceptors on channel '" + beanName + "'");
			}
			this.addMatchingInterceptors((MessageChannel) bean, beanName);
		} 
		return bean;
	}

	/**
	 * Adds any interceptor whose pattern matches against the channel's name. 
	 */
	private void addMatchingInterceptors(MessageChannel channel, String beanName) {
		List<ChannelInterceptor> interceptors = this.getExistingInterceptors(channel);
		if (interceptors != null) {
			List<GlobalChannelInterceptorWrapper> tempInterceptors = new ArrayList<GlobalChannelInterceptorWrapper>();
			for (GlobalChannelInterceptorWrapper globalChannelInterceptorWrapper : this.positiveOrderInterceptors) {
				String[] patterns = globalChannelInterceptorWrapper.getPatterns();
				patterns = StringUtils.trimArrayElements(patterns);
				if (PatternMatchUtils.simpleMatch(patterns, beanName)) {
					tempInterceptors.add(globalChannelInterceptorWrapper);
				}
			}
			Collections.sort(tempInterceptors, this.comparator);
			for (GlobalChannelInterceptorWrapper next : tempInterceptors) {
				interceptors.add(next.getChannelInterceptor());
			}
			tempInterceptors = new ArrayList<GlobalChannelInterceptorWrapper>();
			for (GlobalChannelInterceptorWrapper globalChannelInterceptorWrapper : this.negativeOrderInterceptors) {
				String[] patterns = globalChannelInterceptorWrapper.getPatterns();
				patterns = StringUtils.trimArrayElements(patterns);
				if (PatternMatchUtils.simpleMatch(patterns, beanName)) {
					tempInterceptors.add(globalChannelInterceptorWrapper);
				}
			}
			Collections.sort(tempInterceptors, comparator);
			if (!tempInterceptors.isEmpty()) {
				for (int i = tempInterceptors.size() - 1; i >= 0; i--) {
					interceptors.add(0, tempInterceptors.get(i).getChannelInterceptor());
				}
			}
		}
		else if (logger.isDebugEnabled()) {
			logger.debug("Global Channel interceptors will not be applied to Channel: " + beanName);
		}
	}

	@SuppressWarnings("unchecked")
	private List<ChannelInterceptor> getExistingInterceptors(MessageChannel channel) {	
		try {
			MessageChannel targetChannel = channel;
			if (AopUtils.isAopProxy(channel)) {
				Object target = ((Advised) channel).getTargetSource().getTarget();
				if (target instanceof MessageChannel) {
					targetChannel = (MessageChannel) target;
				}
			}
			DirectFieldAccessor channelAccessor = new DirectFieldAccessor(targetChannel);
			Object interceptorListWrapper = channelAccessor.getPropertyValue("interceptors");
			if (interceptorListWrapper != null) {
				return (List<ChannelInterceptor>) new DirectFieldAccessor(interceptorListWrapper).getPropertyValue("interceptors");
			}
		}
		catch (NotReadablePropertyException e) {
			// Channel doesn't support interceptors - null return logged by caller
		}
		catch (Exception e) {
			// interceptors not supported, will return null
			if (logger.isDebugEnabled() && channel != null) {
				logger.debug("interceptors not supported by channel '" + channel + "'", e);
			}
		}
		return null;
	}

}
