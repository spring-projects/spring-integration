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

package org.springframework.integration.channel.interceptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.OrderComparator;
import org.springframework.integration.channel.ChannelInterceptorAware;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

/**
 * Will apply global interceptors to channels (&lt;channel-interceptor&gt;).
 *
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Artem Bilan
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


	@Override
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

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof ChannelInterceptorAware && bean instanceof MessageChannel) {
			if (logger.isDebugEnabled()) {
				logger.debug("Applying global interceptors on channel '" + beanName + "'");
			}
			this.addMatchingInterceptors((ChannelInterceptorAware) bean, beanName);
		}
		return bean;
	}

	/**
	 * Adds any interceptor whose pattern matches against the channel's name.
	 */
	private void addMatchingInterceptors(ChannelInterceptorAware channel, String beanName) {
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
			channel.addInterceptor(next.getChannelInterceptor());
		}

		tempInterceptors.clear();
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
				channel.addInterceptor(0, tempInterceptors.get(i).getChannelInterceptor());
			}
		}
	}

}
