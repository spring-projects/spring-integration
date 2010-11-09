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
package org.springframework.integration.channel.interceptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.DirectFieldAccessor;
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
 * 
 * @author Oleg Zhurakousky
 * @since 2.0
 */
final class GlobalChannelInterceptorBeanPostProcessor implements BeanPostProcessor, InitializingBean{
	private final static Log logger = LogFactory.getLog(GlobalChannelInterceptorBeanPostProcessor.class);
	private final OrderComparator comparator = new OrderComparator();
	private List<GlobalChannelInterceptorWrapper> channelInterceptors;
	
	private final Set<GlobalChannelInterceptorWrapper> positiveOrderInterceptors = new LinkedHashSet<GlobalChannelInterceptorWrapper>();
	private final Set<GlobalChannelInterceptorWrapper> negativeOrderInterceptors = new LinkedHashSet<GlobalChannelInterceptorWrapper>();

	GlobalChannelInterceptorBeanPostProcessor(List<GlobalChannelInterceptorWrapper> channelInterceptors){
		this.channelInterceptors = channelInterceptors;
	}
	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.config.BeanPostProcessor#postProcessAfterInitialization(java.lang.Object, java.lang.String)
	 */
	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		return bean;
	}
	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.config.BeanPostProcessor#postProcessBeforeInitialization(java.lang.Object, java.lang.String)
	 */
	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		
		if (bean instanceof MessageChannel){
			
			logger.debug("Applying global interceptors on channel '" + beanName + "'");
			this.addInterceptorsIfExist((MessageChannel) bean, beanName);
		} 
	
		return bean;
	}

	/*
	 * 
	 */
	@SuppressWarnings("unchecked")
	private List<ChannelInterceptor> getExistingInterceptors(MessageChannel channel){
		DirectFieldAccessor channelAccessor = new DirectFieldAccessor(channel);
		try {
			Object iWrapper = channelAccessor.getPropertyValue("interceptors");
			if (iWrapper != null){
				DirectFieldAccessor iWrapperAccessor = new DirectFieldAccessor(iWrapper);
				List<ChannelInterceptor> interceptors = (List<ChannelInterceptor>) iWrapperAccessor.getPropertyValue("interceptors");
				return interceptors;
			}
		} 
		catch (Exception e) {
			logger.warn("Attempted to apply Global Channel iterceptors on the Channel that does not support interceptors");
			return null;
		}	
		return null;
	}
	/*
	 * 
	 */
	private void addInterceptorsIfExist(MessageChannel channel, String beanName){
		List<ChannelInterceptor> interceptors = this.getExistingInterceptors(channel);
		if (interceptors != null){
			List<GlobalChannelInterceptorWrapper> tempInterceptors = new ArrayList<GlobalChannelInterceptorWrapper>();
			for (GlobalChannelInterceptorWrapper globalChannelInterceptorWrapper : positiveOrderInterceptors) {
				String[] patterns = globalChannelInterceptorWrapper.getPatterns();
				patterns = StringUtils.trimArrayElements(patterns);
				if (PatternMatchUtils.simpleMatch(patterns, beanName)){
					tempInterceptors.add(globalChannelInterceptorWrapper);
				}
			}
			Collections.sort(tempInterceptors, comparator);
			interceptors.addAll(tempInterceptors);
			
			tempInterceptors = new ArrayList<GlobalChannelInterceptorWrapper>();
			for (GlobalChannelInterceptorWrapper globalChannelInterceptorWrapper : negativeOrderInterceptors) {
				String[] patterns = globalChannelInterceptorWrapper.getPatterns();
				patterns = StringUtils.trimArrayElements(patterns);
				if (PatternMatchUtils.simpleMatch(patterns, beanName)){
					tempInterceptors.add(globalChannelInterceptorWrapper);
				}
			}
			Collections.sort(tempInterceptors, comparator);
			interceptors.addAll(0, tempInterceptors);
		}
		else {
			logger.warn("Attempted to apply Global Channel iterceptors on the Channel that does not support interceptors");
		}
	}
	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		for (GlobalChannelInterceptorWrapper channelInterceptor : channelInterceptors) {
			if (channelInterceptor.getOrder() >= 0){
				positiveOrderInterceptors.add(channelInterceptor);
			} 
			else {
				negativeOrderInterceptors.add(channelInterceptor);
			}
		}
	}
}
