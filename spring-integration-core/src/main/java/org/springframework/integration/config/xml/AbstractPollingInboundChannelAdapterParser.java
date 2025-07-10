/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.SourcePollingChannelAdapterFactoryBean;
import org.springframework.util.xml.DomUtils;

/**
 * Base parser for inbound Channel Adapters that poll a source.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Ngoc Nhan
 */
public abstract class AbstractPollingInboundChannelAdapterParser extends AbstractChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition doParse(Element element, ParserContext parserContext, String channelName) {
		BeanMetadataElement source = this.parseSource(element, parserContext);
		if (source == null) {
			parserContext.getReaderContext().error("failed to parse source", element);
		}
		BeanDefinitionBuilder adapterBuilder = BeanDefinitionBuilder
				.genericBeanDefinition(SourcePollingChannelAdapterFactoryBean.class);

		String sourceBeanName = null;

		if (source instanceof BeanDefinition beanDefinition) {
			String channelAdapterId = this.resolveId(element, adapterBuilder.getRawBeanDefinition(), parserContext);
			sourceBeanName = channelAdapterId + ".source";
			parserContext.getRegistry().registerBeanDefinition(sourceBeanName, beanDefinition);
		}
		else if (source instanceof RuntimeBeanReference runtimeBeanReference) {
			sourceBeanName = runtimeBeanReference.getBeanName();
		}
		else {
			parserContext.getReaderContext().error("Wrong 'source' type: must be 'BeanDefinition' or 'RuntimeBeanReference'", source);
			throw new IllegalStateException("This dummy exception is meant to signify to  NullAway that an the error method throws an exception");
		}
		adapterBuilder.addPropertyReference("source", sourceBeanName);
		adapterBuilder.addPropertyReference("outputChannel", channelName);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(adapterBuilder, element, "send-timeout");
		Element pollerElement = DomUtils.getChildElementByTagName(element, "poller");
		if (pollerElement != null) {
			IntegrationNamespaceUtils.configurePollerMetadata(pollerElement, adapterBuilder, parserContext);
		}
		return adapterBuilder.getBeanDefinition();
	}

	/**
	 * Subclasses must implement this method to parse the PollableSource instance
	 * which the created Channel Adapter will poll.
	 *
	 * @param element The element.
	 * @param parserContext The parser context.
	 * @return The bean metadata element.
	 */
	protected abstract BeanMetadataElement parseSource(Element element, ParserContext parserContext);

}
