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

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.endpoint.SourceEndpoint;
import org.springframework.integration.scheduling.PollingSchedule;
import org.springframework.integration.scheduling.Schedule;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Parser for the <source-endpoint/> element.
 * 
 * @author Mark Fisher
 */
public class SourceEndpointParser extends AbstractSimpleBeanDefinitionParser {

	protected final Class<?> getBeanClass(Element element) {
		return SourceEndpoint.class;
	}

	protected boolean shouldGenerateId() {
		return false;
	}

	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	protected boolean isEligibleAttribute(String name) {
		return (!"source".equals(name) && !"channel".equals(name) && super.isEligibleAttribute(name));
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String source = element.getAttribute("source");
		if (!StringUtils.hasText(source)) {
			throw new ConfigurationException("'source' is required");
		}
		String output = element.getAttribute("channel");
		if (!StringUtils.hasText(output)) {
			throw new ConfigurationException("'channel' is required");
		}
		builder.addConstructorArgReference(source);
		builder.addConstructorArgReference(output);
		Element scheduleElement = this.getScheduleElement(element);
		if (scheduleElement == null) {
			throw new ConfigurationException("The <schedule/> sub-element is required for a <source-endpoint/>.");
		}
		builder.addPropertyValue("schedule", this.parseSchedule(scheduleElement));
		Element adviceChainElement = DomUtils.getChildElementByTagName(element, "advice-chain");
		if (adviceChainElement != null) {
			ManagedList adviceChain = IntegrationNamespaceUtils.parseEndpointAdviceChain(adviceChainElement);
			builder.addPropertyValue("adviceChain", adviceChain);
		}
	}

	/**
	 * Subclasses may override this method to control the creation of the {@link Schedule}. The default
	 * implementation creates a {@link PollingSchedule} instance based on the provided "period" attribute. 
	 */
	protected Schedule parseSchedule(Element element) {
		String period = element.getAttribute("period");
		if (!StringUtils.hasText(period)) {
			throw new ConfigurationException("The 'period' attribute is required for the 'schedule' element.");
		}
		PollingSchedule schedule = new PollingSchedule(Long.valueOf(period));
		return schedule;
	}

	private Element getScheduleElement(Element element) {
		return DomUtils.getChildElementByTagName(element, "schedule");
	}

}
