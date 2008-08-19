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

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.endpoint.InboundChannelAdapter;
import org.springframework.integration.endpoint.OutboundChannelAdapter;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Parser for the &lt;channel-adapter/&gt; element.
 * 
 * @author Mark Fisher
 */
public class ChannelAdapterParser extends AbstractBeanDefinitionParser {

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) throws BeanDefinitionStoreException {
		String id = element.getAttribute("id");
		if (!element.hasAttribute("channel")) {
			// the created channel will get the 'id', so the adapter's bean name includes a suffix
			id = id + ".adapter";
		}
		else if (!StringUtils.hasText(id)) {
			id = parserContext.getReaderContext().generateBeanName(definition);
		}
		return id;
	}

	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		String source = element.getAttribute("source");
		String target = element.getAttribute("target");
		String channelName = element.getAttribute("channel");
		Element pollerElement = DomUtils.getChildElementByTagName(element, "poller");
		BeanDefinitionBuilder adapterBuilder = null;
		if (StringUtils.hasText(source)) {
			if (StringUtils.hasText(target)) {
				throw new ConfigurationException("both 'source' and 'target' are not allowed, provide only one");
			}
			adapterBuilder =  BeanDefinitionBuilder.genericBeanDefinition(InboundChannelAdapter.class);
			if (pollerElement != null) {
				String pollerBeanName = IntegrationNamespaceUtils.parsePoller(source, pollerElement, parserContext);
				adapterBuilder.addPropertyReference("source", pollerBeanName);
			}
			else {
				adapterBuilder.addPropertyReference("source", source);
			}
			if (StringUtils.hasText(channelName)) {
				adapterBuilder.addPropertyReference("target", channelName);
			}
			else {
				adapterBuilder.addPropertyReference("target",
						this.createDirectChannel(element, parserContext));
			}
		}
		else if (StringUtils.hasText(target)) {
			adapterBuilder =  BeanDefinitionBuilder.genericBeanDefinition(OutboundChannelAdapter.class);
			adapterBuilder.addPropertyReference("target", target);
			if (pollerElement != null) {
				if (!StringUtils.hasText(channelName)) {
					throw new ConfigurationException("outbound channel-adapter with a 'poller' requires a 'channel' to poll");
				}
				String pollerBeanName = IntegrationNamespaceUtils.parsePoller(channelName, pollerElement, parserContext);
				adapterBuilder.addPropertyReference("source", pollerBeanName);
			}
			else if (StringUtils.hasText(channelName)) {
				adapterBuilder.addPropertyReference("source", channelName);
			}
			else {
				adapterBuilder.addPropertyReference("source",
						this.createDirectChannel(element, parserContext));
			}
		}
		else {
			throw new ConfigurationException("either 'source' or 'target' is required");
		}
		return adapterBuilder.getBeanDefinition();
	}

	private String createDirectChannel(Element element, ParserContext parserContext) {
		String channelId = element.getAttribute("id");
		if (!StringUtils.hasText(channelId)) {
			throw new ConfigurationException("The channel-adapter's 'id' attribute is required when no 'channel' "
					+ "reference has been provided, because that 'id' would be used for the created channel.");
		}
		BeanDefinitionBuilder channelBuilder = BeanDefinitionBuilder.genericBeanDefinition(DirectChannel.class);
		BeanDefinitionHolder holder = new BeanDefinitionHolder(channelBuilder.getBeanDefinition(), channelId);
		BeanDefinitionReaderUtils.registerBeanDefinition(holder, parserContext.getRegistry());
		return channelId;
	}

}
