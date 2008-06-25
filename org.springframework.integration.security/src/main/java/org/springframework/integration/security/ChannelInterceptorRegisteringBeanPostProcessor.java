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

package org.springframework.integration.security;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.ChannelInterceptor;

/**
 * Registers the provided {@link ChannelInterceptor} instance with any
 * {@link AbstractMessageChannel} with a name matching the provided pattern
 * @author Jonas Partner
 * 
 */
public class ChannelInterceptorRegisteringBeanPostProcessor implements BeanPostProcessor, Ordered {

	private final ChannelInterceptor channelInterceptor;

	private final List<Pattern> regexpPatterns;

	private int order;

	public ChannelInterceptorRegisteringBeanPostProcessor(ChannelInterceptor channelInterceptor, List<String> patterns) {
		this.channelInterceptor = channelInterceptor;

		this.regexpPatterns = new ArrayList<Pattern>();
		for (String stringPattern : patterns) {
			regexpPatterns.add(Pattern.compile(stringPattern));
		}
	}

	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (AbstractMessageChannel.class.isAssignableFrom(bean.getClass()) && matchesPattern(beanName)) {
			((AbstractMessageChannel) bean).addInterceptor(channelInterceptor);
		}
		return bean;
	}

	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	protected boolean matchesPattern(String beanName) {
		for (Pattern regexpPattern : regexpPatterns) {
			if (regexpPattern.matcher(beanName).matches()) {
				return true;
			}
		}
		return false;
	}

}
