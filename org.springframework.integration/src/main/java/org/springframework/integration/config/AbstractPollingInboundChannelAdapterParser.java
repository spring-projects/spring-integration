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
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.scheduling.PollingSchedule;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Base parser for inbound Channel Adapters that poll a source.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractPollingInboundChannelAdapterParser extends AbstractBeanDefinitionParser {

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
		String source = this.parseSource(element, parserContext);
		if (!StringUtils.hasText(source)) {
			throw new ConfigurationException("failed to parse source");
		}
		String channelName = element.getAttribute("channel");
		Element pollerElement = DomUtils.getChildElementByTagName(element, "poller");
		BeanDefinitionBuilder adapterBuilder = BeanDefinitionBuilder.genericBeanDefinition(SourcePollingChannelAdapter.class);
		adapterBuilder.addPropertyReference("source", source);
		if (StringUtils.hasText(channelName)) {
			adapterBuilder.addPropertyReference("outputChannel", channelName);
		}
		else {
			adapterBuilder.addPropertyReference("outputChannel", this.createDirectChannel(element, parserContext));
		}
		if (pollerElement != null) {
			IntegrationNamespaceUtils.configureSchedule(pollerElement, adapterBuilder);
			IntegrationNamespaceUtils.setValueIfAttributeDefined(adapterBuilder, pollerElement, "max-messages-per-poll");
			Element txElement = DomUtils.getChildElementByTagName(pollerElement, "transactional");
			if (txElement != null) {
				IntegrationNamespaceUtils.configureTransactionAttributes(txElement, adapterBuilder);
			}
		}
		else {
			adapterBuilder.addPropertyValue("schedule", new PollingSchedule(0));
		}
		return adapterBuilder.getBeanDefinition();
	}

	/**
	 * Subclasses must implement this method to parse the PollableSource instance
	 * which the created Channel Adapter will poll.
	 */
	protected abstract String parseSource(Element element, ParserContext parserContext);

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
