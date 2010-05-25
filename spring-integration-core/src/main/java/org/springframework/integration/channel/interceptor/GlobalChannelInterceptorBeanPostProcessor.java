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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.OrderComparator;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.ChannelInterceptor;
import org.springframework.util.CollectionUtils;

/**
 * Will apply global interceptors to channels (<channel-interceptor-chain>). Since global interceptors
 * could be Ordered or un-Ordered they will be sorted before merged with other interceptors in the channel.
 * Sorting will only be done within the given interceptor chain which itself defines 'order' attribute 
 * essentially creating a group of ordered interceptors which are ordered internally and then these chain 
 * groups are also ordered. For example:
 * <pre>
 * channel-interceptor-chain channel-name-pattern="foo" order="5" - positive order value means AFTER local channel interceptors
 * 			Ordered-global interceptor (4)
 * 			Ordered-global interceptor (1)
 * channel-interceptor-chain
 * channel-interceptor-chain channel-name-pattern="foo" order="-1" - negative order value means AFTER local channel interceptors
 * 			Ordered-global interceptor (3)
 * 			Ordered-global interceptor (10)
 * channel-interceptor-chain
 * 
 * channel id="foo"
 * 		Ordered-in-channel interceptor (1)
 * channel
 * 
 * will result in channel with the following interceptors
 * Channel "foo"
 * 		Ordered-global interceptor (3)
 * 		Ordered-global interceptor (10)
 * 		Ordered-in-channel interceptor (1)
 * 		Ordered-global interceptor (1)
 * 		Ordered-global interceptor (4)
 * </pre>
 * 
 * @author Oleg Zhurakousky
 * @since 2.0
 */
final class GlobalChannelInterceptorBeanPostProcessor implements BeanPostProcessor, InitializingBean{
	private final static Log logger = LogFactory.getLog(GlobalChannelInterceptorBeanPostProcessor.class);
	private final OrderComparator comparator = new OrderComparator();
	private List<String> allAvailablePatters;
	private List<GlobalChannelInterceptorChain> globalInterceptors;
	private final Map<String, Pattern> compiledPatterns = new HashMap<String, Pattern>();
	
	private List<GlobalChannelInterceptorChain> positiveOrderChains = new ArrayList<GlobalChannelInterceptorChain>();
	private List<GlobalChannelInterceptorChain> negativeOrderChains = new ArrayList<GlobalChannelInterceptorChain>();
	/**
	 * 
	 * @param globalInterceptors
	 */
	GlobalChannelInterceptorBeanPostProcessor(List<GlobalChannelInterceptorChain> globalInterceptors){
		this.globalInterceptors = globalInterceptors;
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
		if (channelPatternMatches(beanName)){
			if (bean instanceof AbstractMessageChannel){
				logger.debug("Applying global interceptors on channel '" + beanName + "'");
				this.mergeInterceptorsToChannel((AbstractMessageChannel) bean, beanName);
			} else {
				logger.warn("Attempt to add channel interceptors is unsuccessfull. Global channel interceptors " +
						"can only be added to AbstractMessageChannel. Current implementation is: " + bean.getClass() +
						" This might happen becouse you specified a single wild-card '*' in 'channel-name-pattern'");
			}		
		}
		return bean;
	}
	/**
	 * 
	 * @param channel
	 * @param channelName
	 */
	private void mergeInterceptorsToChannel(AbstractMessageChannel channel, String channelName){
		List<ChannelInterceptor> tInt = null;
		List<ChannelInterceptor> interceptors = this.getExistingInterceptors(channel);
		// POSITIVE
		List<GlobalChannelInterceptorChain> tempPositiveInterceptorChains = new ArrayList<GlobalChannelInterceptorChain>();
		for (GlobalChannelInterceptorChain positiveOrderChain : positiveOrderChains) {
			if (channelPatternMatches(channelName, positiveOrderChain.getPatterns())){
				tempPositiveInterceptorChains.add(positiveOrderChain);
			}
		}
		// sort chain
	    Collections.sort(tempPositiveInterceptorChains, comparator);
	    
	    for (GlobalChannelInterceptorChain globalChannelInterceptorChain : tempPositiveInterceptorChains) {
	    	tInt = globalChannelInterceptorChain.getInterceptors();
	    	// sort within the chain
	    	Collections.sort(tInt, comparator);
	    	interceptors.addAll(tInt);
		}
	    // NEGATIVE
	    List<GlobalChannelInterceptorChain> tempNegativeInterceptorChains = new ArrayList<GlobalChannelInterceptorChain>();
		for (GlobalChannelInterceptorChain negativeOrderChain : negativeOrderChains) {
			if (channelPatternMatches(channelName, negativeOrderChain.getPatterns())){
				tempNegativeInterceptorChains.add(negativeOrderChain);
			}
		}
		// sort chains
	    Collections.sort(tempNegativeInterceptorChains, comparator);
	    
	    for (GlobalChannelInterceptorChain globalChannelInterceptorChain : tempNegativeInterceptorChains) {
	    	tInt = globalChannelInterceptorChain.getInterceptors();
	    	// sort within the chain
	    	Collections.sort(tInt, comparator);
	    	interceptors.addAll(0, tInt);
		}
	}
	/*
	 * 
	 */
	private void filterPositiveNegativeOrderChains(){
		for (GlobalChannelInterceptorChain globalInterceptorChain : globalInterceptors) {
			if (globalInterceptorChain.getOrder() < 0){
				negativeOrderChains.add(globalInterceptorChain);
			} else {
				positiveOrderChains.add(globalInterceptorChain);
			}
		}
	}
	/*
	 * 
	 */
	@SuppressWarnings("unchecked")
	private List<ChannelInterceptor> getExistingInterceptors(AbstractMessageChannel channel){
		DirectFieldAccessor channelAccessor = new DirectFieldAccessor(channel);
		Object iWrapper = channelAccessor.getPropertyValue("interceptors");
		DirectFieldAccessor iWrapperAccessor = new DirectFieldAccessor(iWrapper);
		List<ChannelInterceptor> interceptors = (List<ChannelInterceptor>) iWrapperAccessor.getPropertyValue("interceptors");
		return interceptors;
	}
	/*
	 * 
	 */
	private boolean channelPatternMatches(String beanName, String... patternsToMatch){
		String[] patterns = null;
		if (patternsToMatch.length > 0){
			patterns = patternsToMatch;
		} else {
			patterns = allAvailablePatters.toArray(new String[]{});
		}
		for (String channelPattern : patterns) {
			channelPattern = channelPattern.trim();
			if (channelPattern.trim().equals("*")){
				return true;
			}
			Pattern p = compiledPatterns.get(channelPattern);
			if (p == null){
				p = Pattern.compile(channelPattern);
				compiledPatterns.put(channelPattern, p);
			}
			Matcher m = p.matcher(beanName);
			if (m.find()){
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public void afterPropertiesSet() throws Exception {
		allAvailablePatters = new ArrayList<String>();
		for (GlobalChannelInterceptorChain globalInterceptorchain : globalInterceptors) {
			allAvailablePatters.addAll(CollectionUtils.arrayToList(globalInterceptorchain.getPatterns()));
		}
		this.filterPositiveNegativeOrderChains();
		logger.info("Initialized: '" + this.getClass().getSimpleName() + "' to apply global channel interceptors");
	}
}
