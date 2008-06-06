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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanNameReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.context.ApplicationContext;

/**
 * Helper to configure the per {@link ApplicationContext}
 * {@link SecurityPropagatingBeanPostProcessor} which determines
 * {@link SecurityContext} propagation.
 * 
 * @author Jonas Partner
 */
public class SecurityPropagatingBeanPostProcessorDefinitionHelper {

	private static final String CHANNELS_TO_INCLUDE = "channelsToInclude";

	private static final String CHANNELS_TO_EXCLUDE = "channelsToExclude";

	private static final String PROPAGATE_BY_DEFAULT = "propagateByDefault";


	public static void setPropagationDefault(boolean valueForPropagationDefault, ParserContext context) {
		BeanDefinition beanDefintion = getOrCreateSecurityPropagatingBeanPostProcessor(context);
		beanDefintion.getPropertyValues().addPropertyValue(PROPAGATE_BY_DEFAULT, Boolean.valueOf(valueForPropagationDefault));
	}

	@SuppressWarnings("unchecked")
	public static void addToExcludeChannelList(String channelName, ParserContext context) {
		BeanDefinition beanDefintion = getOrCreateSecurityPropagatingBeanPostProcessor(context);
		List channelsToExclude;
		if (beanDefintion.getPropertyValues().contains(CHANNELS_TO_EXCLUDE)) {
			channelsToExclude = (List) beanDefintion.getPropertyValues().getPropertyValue(CHANNELS_TO_EXCLUDE).getValue();
		}
		else {
			channelsToExclude = new ArrayList<RuntimeBeanNameReference>();
			beanDefintion.getPropertyValues().addPropertyValue(CHANNELS_TO_EXCLUDE, channelsToExclude);
		}
		channelsToExclude.add(channelName);
	}

	@SuppressWarnings("unchecked")
	public static void addToIncludeChannelList(String channelName, ParserContext context){
		BeanDefinition beanDefintion = getOrCreateSecurityPropagatingBeanPostProcessor(context);
		List channelsToExclude;
		if (beanDefintion.getPropertyValues().contains(CHANNELS_TO_INCLUDE)) {
			channelsToExclude = (List) beanDefintion.getPropertyValues().getPropertyValue(CHANNELS_TO_INCLUDE).getValue();
		}
		else {
			channelsToExclude = new ArrayList<RuntimeBeanNameReference>();
			beanDefintion.getPropertyValues().addPropertyValue(CHANNELS_TO_INCLUDE,channelsToExclude);
		}
		channelsToExclude.add(channelName);
	}

	private static BeanDefinition getOrCreateSecurityPropagatingBeanPostProcessor(ParserContext context) {
		BeanDefinition beanDefinition = null;
		String postProcessorBeanName = SecurityPropagatingBeanPostProcessor.SECURITY_PROPAGATING_BEAN_POST_PROCESSOR_NAME;
		if (context.getRegistry().containsBeanDefinition(postProcessorBeanName)) {
			beanDefinition = context.getRegistry().getBeanDefinition(postProcessorBeanName);
		}
		if (beanDefinition == null) {
			beanDefinition = new RootBeanDefinition(SecurityPropagatingBeanPostProcessor.class);
			context.registerBeanComponent(new BeanComponentDefinition(beanDefinition, postProcessorBeanName));
		}
		return beanDefinition;
	}

}
