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

package org.springframework.integration.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.adapter.DefaultTargetAdapter;
import org.springframework.integration.adapter.MethodInvokingSource;
import org.springframework.integration.adapter.MethodInvokingTarget;
import org.springframework.integration.adapter.PollingSourceAdapter;
import org.springframework.integration.endpoint.DefaultMessageEndpoint;
import org.springframework.integration.scheduling.Subscription;
import org.springframework.util.StringUtils;

/**
 * Parser for inbound and outbound channel adapters.
 * 
 * @author Mark Fisher
 */
public class ChannelAdapterParser implements BeanDefinitionParser {

	private static final String ID_ATTRIBUTE = "id";

	private static final String REF_ATTRIBUTE = "ref";

	private static final String METHOD_ATTRIBUTE = "method";

	private static final String CHANNEL_ATTRIBUTE = "channel";

	private static final String PERIOD_ATTRIBUTE = "period";


	private final boolean isInbound; 


	public ChannelAdapterParser(boolean isInbound) {
		this.isInbound = isInbound;
	}


	public BeanDefinition parse(Element element, ParserContext parserContext) {
		String ref = element.getAttribute(REF_ATTRIBUTE);
		String method = element.getAttribute(METHOD_ATTRIBUTE);
		String channel = element.getAttribute(CHANNEL_ATTRIBUTE);
		if (!StringUtils.hasText(ref)) {
			throw new ConfigurationException("'ref' is required");
		}
		if (!StringUtils.hasText(method)) {
			throw new ConfigurationException("'method' is required");
		}
		if (!StringUtils.hasText(channel)) {
			throw new ConfigurationException("'channel' is required");
		}
		RootBeanDefinition adapterDef = null;
		RootBeanDefinition invokerDef = null;
		if (this.isInbound) {
			adapterDef = new RootBeanDefinition(PollingSourceAdapter.class);
			invokerDef = new RootBeanDefinition(MethodInvokingSource.class);
			String period = element.getAttribute(PERIOD_ATTRIBUTE);
			if (StringUtils.hasText(period)) {
				adapterDef.getPropertyValues().addPropertyValue("period", period);
			}
			adapterDef.getPropertyValues().addPropertyValue("channel", new RuntimeBeanReference(channel));
		}
		else {
			adapterDef = new RootBeanDefinition(DefaultTargetAdapter.class);
			invokerDef = new RootBeanDefinition(MethodInvokingTarget.class);
		}
		invokerDef.getPropertyValues().addPropertyValue("object", new RuntimeBeanReference(ref));
		invokerDef.getPropertyValues().addPropertyValue("method", method);
		String invokerBeanName = parserContext.getReaderContext().generateBeanName(invokerDef);
		parserContext.registerBeanComponent(new BeanComponentDefinition(invokerDef, invokerBeanName));
		adapterDef.getConstructorArgumentValues().addGenericArgumentValue(new RuntimeBeanReference(invokerBeanName));
		adapterDef.setSource(parserContext.extractSource(element));
		String beanName = element.getAttribute(ID_ATTRIBUTE);
		if (!StringUtils.hasText(beanName)) {
			beanName = parserContext.getReaderContext().generateBeanName(adapterDef);
		}
		if (!this.isInbound) {
			RootBeanDefinition endpointDef = new RootBeanDefinition(DefaultMessageEndpoint.class);
			RootBeanDefinition subscriptionDef = new RootBeanDefinition(Subscription.class);
			subscriptionDef.getConstructorArgumentValues().addGenericArgumentValue(new RuntimeBeanReference(channel));
			String subscriptionBeanName = parserContext.getReaderContext().generateBeanName(subscriptionDef);
			parserContext.registerBeanComponent(new BeanComponentDefinition(subscriptionDef, subscriptionBeanName));
			endpointDef.getPropertyValues().addPropertyValue("subscription", new RuntimeBeanReference(subscriptionBeanName));
			endpointDef.getPropertyValues().addPropertyValue("handler", new RuntimeBeanReference(beanName));
			String endpointBeanName = parserContext.getReaderContext().generateBeanName(endpointDef);
			parserContext.registerBeanComponent(new BeanComponentDefinition(endpointDef, endpointBeanName));
		}
		parserContext.registerBeanComponent(new BeanComponentDefinition(adapterDef, beanName));
		return adapterDef;
	}

}
