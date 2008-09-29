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
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.scheduling.IntervalTrigger;
import org.springframework.util.Assert;
import org.springframework.util.xml.DomUtils;

/**
 * Base parser for inbound Channel Adapters that poll a source.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractPollingInboundChannelAdapterParser extends AbstractChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition doParse(Element element, ParserContext parserContext, String channelName) {
		String source = this.parseSource(element, parserContext);
		Assert.hasText(source, "failed to parse source");
		Element pollerElement = DomUtils.getChildElementByTagName(element, "poller");
		BeanDefinitionBuilder adapterBuilder = BeanDefinitionBuilder.genericBeanDefinition(SourcePollingChannelAdapter.class);
		adapterBuilder.addPropertyReference("source", source);
		adapterBuilder.addPropertyReference("outputChannel", channelName);
		if (pollerElement != null) {
			IntegrationNamespaceUtils.configureTrigger(pollerElement, adapterBuilder);
			IntegrationNamespaceUtils.setValueIfAttributeDefined(adapterBuilder, pollerElement, "max-messages-per-poll");
			Element txElement = DomUtils.getChildElementByTagName(pollerElement, "transactional");
			if (txElement != null) {
				IntegrationNamespaceUtils.configureTransactionAttributes(txElement, adapterBuilder);
			}
		}
		else {
			adapterBuilder.addPropertyValue("trigger", new IntervalTrigger(this.getDefaultPollInterval()));
		}
		return adapterBuilder.getBeanDefinition();
	}

	/**
	 * Subclasses may override this to provide the default poll interval (when
	 * no 'trigger' is configured). Otherwise, the value will be 1 second.
	 */
	protected int getDefaultPollInterval() {
		return 1000;
	}

	/**
	 * Subclasses must implement this method to parse the PollableSource instance
	 * which the created Channel Adapter will poll.
	 */
	protected abstract String parseSource(Element element, ParserContext parserContext);

}
