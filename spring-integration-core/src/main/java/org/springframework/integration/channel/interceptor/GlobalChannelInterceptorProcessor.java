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
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
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

	private volatile List<GlobalChannelInterceptorWrapper> channelInterceptors;

	private final Set<GlobalChannelInterceptorWrapper> positiveOrderInterceptors = new LinkedHashSet<GlobalChannelInterceptorWrapper>();

	private final Set<GlobalChannelInterceptorWrapper> negativeOrderInterceptors = new LinkedHashSet<GlobalChannelInterceptorWrapper>();

	private BeanFactory beanFactory;

	private volatile boolean processed;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public synchronized void start() {
		if (!processed && this.beanFactory instanceof ListableBeanFactory) {
			setUp();
			Map<String, MessageChannel> channels = ((ListableBeanFactory) this.beanFactory).getBeansOfType(MessageChannel.class);
			for (Entry<String, MessageChannel> entry : channels.entrySet()) {
				this.applyGlobalInterceptors(entry.getValue(), entry.getKey());
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

	public void setUp() {
		Map<String, GlobalChannelInterceptorWrapper> interceptorBeans = null;
		if (this.beanFactory instanceof ListableBeanFactory) {
			interceptorBeans = ((ListableBeanFactory) this.beanFactory)
					.getBeansOfType(GlobalChannelInterceptorWrapper.class);
		}
		this.channelInterceptors = new ArrayList<GlobalChannelInterceptorWrapper>(interceptorBeans.values());
		if (interceptorBeans != null) {
			for (GlobalChannelInterceptorWrapper channelInterceptor : this.channelInterceptors) {
				if (channelInterceptor.getOrder() >= 0) {
					this.positiveOrderInterceptors.add(channelInterceptor);
				}
				else {
					this.negativeOrderInterceptors.add(channelInterceptor);
				}
			}
		}
	}

	public void applyGlobalInterceptors(MessageChannel channel, String beanName) throws BeansException {
		if (channel instanceof ChannelInterceptorAware && channel instanceof MessageChannel) {
			if (channelInterceptors == null) {
				setUp();
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Applying global interceptors on channel '" + beanName + "'");
			}
			this.addMatchingInterceptors((ChannelInterceptorAware) channel, beanName);
		}
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
