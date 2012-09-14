/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.integration.config.xml;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.handler.DelayHandler;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * Parser for the &lt;delayer&gt; element.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gunnar Hillert
 *
 * @since 1.0.3
 */
public class DelayerParser extends AbstractConsumerEndpointParser {

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(DelayHandler.class);

		String id = element.getAttribute(ID_ATTRIBUTE);
		String name = element.getAttribute("name");

		if (StringUtils.hasText(id) && StringUtils.hasText(name)) {
			parserContext.getReaderContext().error("Either the 'id' attribute or " +
				"the 'name' attribute is required but not both.", element);
		}

		if (StringUtils.hasText(id)) {
			builder.addConstructorArgValue(id + ".messageGroupId");
		}
		else if (StringUtils.hasText(name)) {
			builder.addConstructorArgValue(name + ".messageGroupId");
		}
		else {
			parserContext.getReaderContext().error("Either the 'id' attribute or " +
				"the 'name' attribute is required.", element);
		}

		String defaultDelay = element.getAttribute("default-delay");
		String delayHeaderName = element.getAttribute("delay-header-name");

		boolean hasDefaultDelay = StringUtils.hasText(defaultDelay);
		boolean hasDelayHeaderName = StringUtils.hasText(delayHeaderName);

		if (!(hasDefaultDelay | hasDelayHeaderName)) {
			parserContext.getReaderContext()
					.error("The 'default-delay' or 'delay-header-name' attributes should be provided.", element);
		}

		String scheduler = element.getAttribute("scheduler");
		if (StringUtils.hasText(scheduler)) {
			builder.addConstructorArgReference(scheduler);
		}

		if (hasDefaultDelay) {
			builder.addPropertyValue("defaultDelay", defaultDelay);
		}
		if (hasDelayHeaderName) {
			builder.addPropertyValue("delayHeaderName", delayHeaderName);
		}

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "message-store");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "send-timeout");

		Element txElement = DomUtils.getChildElementByTagName(element, "transactional");
		Element adviceChainElement = DomUtils.getChildElementByTagName(element, "advice-chain");

		IntegrationNamespaceUtils.configureAndSetAdviceChainIfPresent(adviceChainElement, txElement,
				builder.getRawBeanDefinition(), parserContext, "delayedAdviceChain");

		return builder;
	}

}
