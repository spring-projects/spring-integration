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

package org.springframework.integration.security.config;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.security.channel.SecurityContextPropagatingChannelInterceptor;

/**
 * Post processes channels applying appropriate propagation behaviour. If
 * default propagation is specified with a secure-channels tag, that will be
 * applied in the absence of a secured tag for the channel. If the secured tag
 * is specified, it will always determine propagation behaviour.
 * 
 * @author Jonas Partner
 */
public class SecurityPropagatingBeanPostProcessor implements BeanPostProcessor, Ordered {

	protected static final String SECURITY_PROPAGATING_BEAN_POST_PROCESSOR_NAME = SecurityPropagatingBeanPostProcessor.class
			.getName();

	private final SecurityContextPropagatingChannelInterceptor interceptor = new SecurityContextPropagatingChannelInterceptor();

	private boolean propagateByDefault;

	private final Log logger = LogFactory.getLog(this.getClass());

	private List<Pattern> channelsToInclude = new ArrayList<Pattern>();

	private List<Pattern> channelsToExclude = new ArrayList<Pattern>();

	public boolean isPropagateByDefault() {
		return this.propagateByDefault;
	}

	public void setPropagateByDefault(boolean propagateByDefault) {
		this.propagateByDefault = propagateByDefault;
	}

	public List<Pattern> getChannelsToInclude() {
		return this.channelsToInclude;
	}

	public void setChannelsToInclude(List<Pattern> channelsToInclude) {
		this.channelsToInclude = channelsToInclude;
	}

	public List<Pattern> getChannelsToExclude() {
		return this.channelsToExclude;
	}

	public void setChannelsToExclude(List<Pattern> channelsToExclude) {
		this.channelsToExclude = channelsToExclude;
	}

	public int getOrder() {
		return 0;
	}

	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (AbstractMessageChannel.class.isAssignableFrom(bean.getClass())) {
			AbstractMessageChannel channel = (AbstractMessageChannel) bean;
			if (isIncluded(beanName) || (this.propagateByDefault && !isExcluded(beanName))) {
				channel.addInterceptor(this.interceptor);
				if (logger.isDebugEnabled()) {
					logger.debug("Channel '" + beanName + "' will propagate a SecurityContext.");
				}
			}
			else if (logger.isDebugEnabled()) {
				logger.debug("Channel '" + beanName + "' is not configured to propagate a SecurityContext.");
			}
		}
		return bean;
	}

	protected boolean isExcluded(String str) {
		return matchesOnePattern(channelsToExclude, str);
	}

	protected boolean isIncluded(String str) {
		return matchesOnePattern(channelsToInclude, str);
	}

	protected boolean matchesOnePattern(List<Pattern> patterns, String str) {
		for (Pattern pattern : patterns) {
			if (pattern.matcher(str).matches()) {
				return true;
			}
		}
		return false;
	}

}
