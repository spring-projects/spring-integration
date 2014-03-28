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

package org.springframework.integration.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.OrderComparator;
import org.springframework.integration.channel.ChannelInterceptorAware;
import org.springframework.integration.channel.interceptor.GlobalChannelInterceptorWrapper;
import org.springframework.integration.channel.interceptor.VetoCapableInterceptor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

/**
 * Will apply global interceptors to channels (&lt;channel-interceptor&gt;).
 *
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 * @since 2.0
 */
final class GlobalChannelInterceptorProcessor implements BeanFactoryAware, SmartLifecycle {

	private static final Log logger = LogFactory.getLog(GlobalChannelInterceptorProcessor.class);


	private final OrderComparator comparator = new OrderComparator();

	private final Set<GlobalChannelInterceptorWrapper> positiveOrderInterceptors = new LinkedHashSet<GlobalChannelInterceptorWrapper>();

	private final Set<GlobalChannelInterceptorWrapper> negativeOrderInterceptors = new LinkedHashSet<GlobalChannelInterceptorWrapper>();

	private ListableBeanFactory beanFactory;

	private volatile boolean processed;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		Assert.isInstanceOf(ListableBeanFactory.class, beanFactory);
		this.beanFactory = (ListableBeanFactory) beanFactory;
	}

	@Override
	public synchronized void start() {
		if (!this.processed) {
			Collection<GlobalChannelInterceptorWrapper> interceptors = this.beanFactory.getBeansOfType(GlobalChannelInterceptorWrapper.class).values();
			if (CollectionUtils.isEmpty(interceptors)) {
				logger.debug("No global channel interceptors.");
			}
			else {
				for (GlobalChannelInterceptorWrapper channelInterceptor : interceptors) {
					if (channelInterceptor.getOrder() >= 0) {
						this.positiveOrderInterceptors.add(channelInterceptor);
					}
					else {
						this.negativeOrderInterceptors.add(channelInterceptor);
					}
				}
				Map<String, ChannelInterceptorAware> channels = this.beanFactory.getBeansOfType(ChannelInterceptorAware.class);
				for (Entry<String, ChannelInterceptorAware> entry : channels.entrySet()) {
					this.addMatchingInterceptors(entry.getValue(), entry.getKey());
				}
			}
			this.processed = true;
		}
	}

	@Override
	public void stop() {
	}

	@Override
	public boolean isRunning() {
		return false;
	}

	@Override
	public int getPhase() {
		return Integer.MIN_VALUE;
	}

	@Override
	public boolean isAutoStartup() {
		return true;
	}

	@Override
	public void stop(Runnable callback) {
	}

	/**
	 * Adds any interceptor whose pattern matches against the channel's name.
	 */
	private void addMatchingInterceptors(ChannelInterceptorAware channel, String beanName) {
		if (logger.isDebugEnabled()) {
			logger.debug("Applying global interceptors on channel '" + beanName + "'");
		}
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
			ChannelInterceptor channelInterceptor = next.getChannelInterceptor();
			if (!(channelInterceptor instanceof VetoCapableInterceptor)
					|| ((VetoCapableInterceptor) channelInterceptor).shouldIntercept(beanName, channel)) {
				channel.addInterceptor(channelInterceptor);
			}
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
				ChannelInterceptor channelInterceptor = tempInterceptors.get(i).getChannelInterceptor();
				if (!(channelInterceptor instanceof VetoCapableInterceptor)
						|| ((VetoCapableInterceptor) channelInterceptor).shouldIntercept(beanName, channel)) {
					channel.addInterceptor(0, channelInterceptor);
				}
			}
		}
	}

}
