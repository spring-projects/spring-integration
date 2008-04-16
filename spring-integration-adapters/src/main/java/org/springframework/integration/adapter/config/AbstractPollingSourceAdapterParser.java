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

package org.springframework.integration.adapter.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.adapter.PollingSourceAdapter;
import org.springframework.integration.message.PollableSource;
import org.springframework.integration.scheduling.PollingSchedule;
import org.springframework.integration.scheduling.Schedule;
import org.springframework.util.StringUtils;

/**
 * Base parser for polling adapters.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractPollingSourceAdapterParser extends AbstractSingleBeanDefinitionParser {

	protected final Class<?> getBeanClass(Element element) {
		return PollingSourceAdapter.class;
	}

	protected boolean shouldGenerateId() {
		return false;
	}

	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	protected boolean isEligibleAttribute(String attributeName) {
		return !ID_ATTRIBUTE.equals(attributeName) && !"channel".equals(attributeName) && !"poll-period".equals(attributeName)
				&& !shouldSkipAttribute(attributeName);
	}

	protected boolean shouldSkipAttribute(String attributeName) {
		return false;
	}

	protected void doPostProcess(BeanDefinitionBuilder beanDefinition, Element element) {
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String channel = element.getAttribute("channel");
		if (!StringUtils.hasText(channel)) {
			throw new ConfigurationException("'channel' is required");
		}
		SourceParser sourceParser = new SourceParser();
		sourceParser.parse(element, parserContext);
		builder.addConstructorArgReference(sourceParser.generatedName);
		builder.addConstructorArgReference(channel);
		builder.addConstructorArgValue(this.parseSchedule(element));
	}

	/**
	 * Subclasses may override this method to control the creation of the {@link Schedule}. The default
	 * implementation creates a {@link PollingSchedule} instance based on the provided "poll-period" attribute. 
	 */
	protected Schedule parseSchedule(Element element) {
		String period = element.getAttribute("poll-period");
		if (!StringUtils.hasText(period)) {
			throw new ConfigurationException("'poll-period' is required");
		}
		PollingSchedule schedule = new PollingSchedule(Long.valueOf(period));
		return schedule;
	}

	protected abstract Class<? extends PollableSource<?>> getSourceBeanClass(Element element);


	private class SourceParser extends AbstractSimpleBeanDefinitionParser {

		private String generatedName;

		@Override
		protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext)
				throws BeanDefinitionStoreException {
			this.generatedName = parserContext.getReaderContext().generateBeanName(definition);
			return this.generatedName;
		}

		@Override
		protected Class<?> getBeanClass(Element element) {
			return getSourceBeanClass(element);
		}

		@Override
		protected boolean isEligibleAttribute(String attributeName) {
			return AbstractPollingSourceAdapterParser.this.isEligibleAttribute(attributeName);
		}

		@Override
		protected void postProcess(BeanDefinitionBuilder beanDefinition, Element element) {
			doPostProcess(beanDefinition, element);
		}
	}

}
