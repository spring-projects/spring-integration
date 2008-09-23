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

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.endpoint.OutboundChannelAdapter;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.message.MethodInvokingConsumer;
import org.springframework.integration.message.MethodInvokingSource;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Parser for the &lt;channel-adapter/&gt; element.
 * 
 * @author Mark Fisher
 */
public class ChannelAdapterParser extends AbstractChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition doParse(Element element, ParserContext parserContext, String channelName) {
		String source = element.getAttribute("source");
		String target = element.getAttribute("target");
		String methodName = element.getAttribute("method");
		Element pollerElement = DomUtils.getChildElementByTagName(element, "poller");
		BeanDefinitionBuilder adapterBuilder = null;
		if (StringUtils.hasText(source)) {
			if (StringUtils.hasText(target)) {
				throw new ConfigurationException("both 'source' and 'target' are not allowed, provide only one");
			}
			if (StringUtils.hasText(methodName)) {
				BeanDefinitionBuilder invokerBuilder = BeanDefinitionBuilder.genericBeanDefinition(MethodInvokingSource.class);
				invokerBuilder.addPropertyReference("object", source);
				invokerBuilder.addPropertyValue("methodName", methodName);
				source = BeanDefinitionReaderUtils.registerWithGeneratedName(invokerBuilder.getBeanDefinition(), parserContext.getRegistry());
			}
			adapterBuilder =  BeanDefinitionBuilder.genericBeanDefinition(SourcePollingChannelAdapter.class);
			adapterBuilder.addPropertyReference("source", source);
			adapterBuilder.addPropertyReference("outputChannel", channelName);
			if (pollerElement != null) {
				IntegrationNamespaceUtils.configureSchedule(pollerElement, adapterBuilder);
				IntegrationNamespaceUtils.setValueIfAttributeDefined(adapterBuilder, pollerElement, "max-messages-per-poll");
				Element txElement = DomUtils.getChildElementByTagName(pollerElement, "transactional");
				if (txElement != null) {
					IntegrationNamespaceUtils.configureTransactionAttributes(txElement, adapterBuilder);
				}
			}
		}
		else if (StringUtils.hasText(target)) {
			if (StringUtils.hasText(methodName)) {
				BeanDefinitionBuilder invokerBuilder = BeanDefinitionBuilder.genericBeanDefinition(MethodInvokingConsumer.class);
				invokerBuilder.addConstructorArgReference(target);
				invokerBuilder.addConstructorArgValue(methodName);
				target = BeanDefinitionReaderUtils.registerWithGeneratedName(invokerBuilder.getBeanDefinition(), parserContext.getRegistry());
			}
			adapterBuilder =  BeanDefinitionBuilder.genericBeanDefinition(OutboundChannelAdapter.class);
			adapterBuilder.addConstructorArgReference(target);
			if (pollerElement != null) {
				if (!StringUtils.hasText(channelName)) {
					throw new ConfigurationException("outbound channel-adapter with a 'poller' requires a 'channel' to poll");
				}
				IntegrationNamespaceUtils.configureSchedule(pollerElement, adapterBuilder);
				Element txElement = DomUtils.getChildElementByTagName(pollerElement, "transactional");
				if (txElement != null) {
					IntegrationNamespaceUtils.configureTransactionAttributes(txElement, adapterBuilder);
				}
			}
			adapterBuilder.addPropertyReference("inputChannel", channelName);
		}
		else {
			throw new ConfigurationException("either 'source' or 'target' is required");
		}
		return adapterBuilder.getBeanDefinition();
	}

}
