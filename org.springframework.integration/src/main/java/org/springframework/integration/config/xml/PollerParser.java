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

package org.springframework.integration.config.xml;

import java.util.concurrent.TimeUnit;

import org.w3c.dom.Element;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.scheduling.CronTrigger;
import org.springframework.integration.scheduling.IntervalTrigger;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.util.Assert;
import org.springframework.util.xml.DomUtils;

/**
 * Parser for the &lt;poller&gt; element.
 * 
 * @author Mark Fisher
 */
public class PollerParser extends AbstractBeanDefinitionParser {

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) throws BeanDefinitionStoreException {
		String id = super.resolveId(element, definition, parserContext);
		if (element.getAttribute("default").equals("true")) {
			Assert.isTrue(!parserContext.getRegistry().isBeanNameInUse(IntegrationContextUtils.DEFAULT_POLLER_METADATA_BEAN_NAME),
					"only one default <poller/> element is allowed per context");
			parserContext.getRegistry().registerAlias(id, IntegrationContextUtils.DEFAULT_POLLER_METADATA_BEAN_NAME);
		}
		return id;
	}

	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder metadataBuilder = BeanDefinitionBuilder.genericBeanDefinition(PollerMetadata.class);
		Assert.isTrue(!element.hasAttribute("ref"),
				"the 'ref' attribute must not be present on a 'poller' element submitted to the parser");
		configureTrigger(element, metadataBuilder, parserContext);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(metadataBuilder, element, "max-messages-per-poll");
		Element txElement = DomUtils.getChildElementByTagName(element, "transactional");
		if (txElement != null) {
			configureTransactionAttributes(txElement, metadataBuilder);
		}
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(metadataBuilder, element, "task-executor");
		return metadataBuilder.getBeanDefinition();
	}

	private void configureTrigger(Element pollerElement, BeanDefinitionBuilder targetBuilder, ParserContext parserContext) {
		String triggerBeanName = null;
		Element intervalElement = DomUtils.getChildElementByTagName(pollerElement, "interval-trigger");
		if (intervalElement != null) {
			triggerBeanName = parseIntervalTrigger(intervalElement, parserContext);
		}
		else {
			Element cronElement = DomUtils.getChildElementByTagName(pollerElement, "cron-trigger");
			Assert.notNull(cronElement,
					"A <poller> element must include either an <interval-trigger/> or <cron-trigger/> child element.");
			triggerBeanName = parseCronTrigger(cronElement, parserContext);
		}
		targetBuilder.addPropertyReference("trigger", triggerBeanName);
	}

	/**
	 * Parse an "interval-trigger" element
	 */
	private String parseIntervalTrigger(Element element, ParserContext parserContext) {
		String interval = element.getAttribute("interval");
		Assert.hasText(interval, "the 'interval' attribute is required for an <interval-trigger/>");
		TimeUnit timeUnit = TimeUnit.valueOf(element.getAttribute("time-unit"));
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(IntervalTrigger.class);
		builder.addConstructorArgValue(interval);
		builder.addConstructorArgValue(timeUnit);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "initial-delay");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "fixed-rate");
		return BeanDefinitionReaderUtils.registerWithGeneratedName(builder.getBeanDefinition(), parserContext.getRegistry());
	}

	/**
	 * Parse a "cron-trigger" element
	 */
	private String parseCronTrigger(Element element, ParserContext parserContext) {
		String cronExpression = element.getAttribute("expression");
		Assert.hasText(cronExpression, "the 'expression' attribute is required for a <cron-trigger/>");
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(CronTrigger.class);
		builder.addConstructorArgValue(cronExpression);
		return BeanDefinitionReaderUtils.registerWithGeneratedName(builder.getBeanDefinition(), parserContext.getRegistry());
	}

	/**
	 * Parse a "transactional" element and configure the "transactionManager" and "transactionDefinition"
	 * properties for the target builder.
	 */
	private void configureTransactionAttributes(Element txElement, BeanDefinitionBuilder targetBuilder) {
		targetBuilder.addPropertyReference("transactionManager", txElement.getAttribute("transaction-manager"));
		DefaultTransactionDefinition txDefinition = new DefaultTransactionDefinition();
		txDefinition.setPropagationBehaviorName(
				DefaultTransactionDefinition.PREFIX_PROPAGATION + txElement.getAttribute("propagation"));
		txDefinition.setIsolationLevelName(
				DefaultTransactionDefinition.PREFIX_ISOLATION + txElement.getAttribute("isolation"));
		txDefinition.setTimeout(Integer.valueOf(txElement.getAttribute("timeout")));
		txDefinition.setReadOnly(txElement.getAttribute("read-only").equalsIgnoreCase("true"));
		targetBuilder.addPropertyValue("transactionDefinition", txDefinition);
	}

}
