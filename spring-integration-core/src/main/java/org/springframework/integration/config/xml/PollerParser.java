/*
 * Copyright 2002-2010 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Parser for the &lt;poller&gt; element.
 * 
 * @author Mark Fisher
 * @author Marius Bogoevici
 * @author Oleg Zhurakousky
 */
public class PollerParser extends AbstractBeanDefinitionParser {

	private static final String MULTIPLE_TRIGGER_DEFINITIONS = "A <poller> cannot specify more than one trigger configuration.";

	private static final String NO_TRIGGER_DEFINITIONS = "A <poller> must have a one and only one trigger configuration.";

	private static final String PERIODIC_TRIGGER_CLASSNAME = "org.springframework.scheduling.support.PeriodicTrigger";

	private static final String CRON_TRIGGER_CLASSNAME = "org.springframework.scheduling.support.CronTrigger";


	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) throws BeanDefinitionStoreException {
		String id = super.resolveId(element, definition, parserContext);
		if (element.getAttribute("default").equals("true")) {
			if (parserContext.getRegistry().isBeanNameInUse(IntegrationContextUtils.DEFAULT_POLLER_METADATA_BEAN_NAME)) {
				parserContext.getReaderContext().error(
						"Only one default <poller/> element is allowed per context.", element);
			}
			if (StringUtils.hasText(id)) {
				parserContext.getRegistry().registerAlias(id, IntegrationContextUtils.DEFAULT_POLLER_METADATA_BEAN_NAME);
			}
			else {
				id = IntegrationContextUtils.DEFAULT_POLLER_METADATA_BEAN_NAME;
			}
		}
		else if (!StringUtils.hasText(id)) {
			parserContext.getReaderContext().error(
					"The 'id' attribute is required for a top-level poller element unless it is the default poller.",
					element);
		}
		return id;
	}

	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder metadataBuilder = BeanDefinitionBuilder.genericBeanDefinition(
				IntegrationNamespaceUtils.BASE_PACKAGE + ".scheduling.PollerMetadata");
		if (element.hasAttribute("ref")) {
			parserContext.getReaderContext().error(
					"the 'ref' attribute must not be present on the top-level 'poller' element", element);
		}
		configureTrigger(element, metadataBuilder, parserContext);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(metadataBuilder, element, "max-messages-per-poll");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(metadataBuilder, element, "receive-timeout");
		
		Element txElement = DomUtils.getChildElementByTagName(element, "transactional");
		
		Element adviceChainElement = DomUtils.getChildElementByTagName(element, "advice-chain");
		
		configureAdviceChain(adviceChainElement, txElement, metadataBuilder, parserContext);
		
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(metadataBuilder, element, "task-executor");
		String errorChannel = element.getAttribute("error-channel");
		if (StringUtils.hasText(errorChannel)){
			BeanDefinitionBuilder errorHandler = BeanDefinitionBuilder.genericBeanDefinition(
					"org.springframework.integration.channel.MessagePublishingErrorHandler");
			errorHandler.addPropertyReference("defaultErrorChannel", errorChannel);
			
			metadataBuilder.addPropertyValue("errorHandler", errorHandler.getBeanDefinition());
		}
		return metadataBuilder.getBeanDefinition();
	}

	private void configureTrigger(Element pollerElement, BeanDefinitionBuilder targetBuilder, ParserContext parserContext) {
		// Polling frequency can be configured either through attributes or by using sub-elements
		// However, since Spring Integration 2.0 the trigger sub-elements are deprecated
		String triggerAttribute = pollerElement.getAttribute("trigger");
		String fixedRateAttribute = pollerElement.getAttribute("fixed-rate");
		String fixedDelayAttribute = pollerElement.getAttribute("fixed-delay");
		String cronAttribute = pollerElement.getAttribute("cron");
		String timeUnit = pollerElement.getAttribute("time-unit");

		List<String> triggerBeanNames = new ArrayList<String>();
		if (StringUtils.hasText(triggerAttribute)) {
			if (StringUtils.hasText(timeUnit)) {
				parserContext.getReaderContext().error("The 'time-unit' attribute cannot be used with a 'trigger' reference.", pollerElement);
			}
			triggerBeanNames.add(triggerAttribute);
		}
		if (StringUtils.hasText(fixedRateAttribute)) {
			BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(PERIODIC_TRIGGER_CLASSNAME);
			builder.addConstructorArgValue(fixedRateAttribute);
			if (StringUtils.hasText(timeUnit)) {
				builder.addConstructorArgValue(timeUnit);
			}
			builder.addPropertyValue("fixedRate", Boolean.TRUE);
			String triggerBeanName = BeanDefinitionReaderUtils.registerWithGeneratedName(
					builder.getBeanDefinition(), parserContext.getRegistry());
			triggerBeanNames.add(triggerBeanName);
		}
		if (StringUtils.hasText(fixedDelayAttribute)) {
			BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(PERIODIC_TRIGGER_CLASSNAME);
			builder.addConstructorArgValue(fixedDelayAttribute);
			if (StringUtils.hasText(timeUnit)) {
				builder.addConstructorArgValue(timeUnit);
			}
			builder.addPropertyValue("fixedRate", Boolean.FALSE);
			String triggerBeanName = BeanDefinitionReaderUtils.registerWithGeneratedName(
					builder.getBeanDefinition(), parserContext.getRegistry());
			triggerBeanNames.add(triggerBeanName);
		}
		if (StringUtils.hasText(cronAttribute)) {
			if (StringUtils.hasText(timeUnit)) {
				parserContext.getReaderContext().error("The 'time-unit' attribute cannot be used with a 'cron' trigger.", pollerElement);
			}
			BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(CRON_TRIGGER_CLASSNAME);
			builder.addConstructorArgValue(cronAttribute);
			String triggerBeanName = BeanDefinitionReaderUtils.registerWithGeneratedName(
					builder.getBeanDefinition(), parserContext.getRegistry());
			triggerBeanNames.add(triggerBeanName);
		}
		Element intervalElement = DomUtils.getChildElementByTagName(pollerElement, "interval-trigger");
		if (intervalElement != null) {
			parserContext.getReaderContext().warning(
					"Poller configuration via 'interval-trigger' subelements is deprecated, " +
					"use the 'fixed-delay' or 'fixed-rate' attribute instead.", pollerElement);
			triggerBeanNames.add(parseIntervalTrigger(intervalElement, parserContext));
		}
		else {
			Element cronElement = DomUtils.getChildElementByTagName(pollerElement, "cron-trigger");
			if (cronElement != null) {
				parserContext.getReaderContext().warning(
						"Poller configuration via 'cron-trigger' subelements is deprecated, " +
						"use the 'cron' attribute instead.", pollerElement);
				triggerBeanNames.add(parseCronTrigger(cronElement, parserContext));
			}
		}
		if (triggerBeanNames.isEmpty()) {
			parserContext.getReaderContext().error(NO_TRIGGER_DEFINITIONS, pollerElement);
		}
		if (triggerBeanNames.size() > 1) {
			parserContext.getReaderContext().error(MULTIPLE_TRIGGER_DEFINITIONS, pollerElement);
		}
		targetBuilder.addPropertyReference("trigger", triggerBeanNames.get(0));
	}

	/**
	 * Parse an "interval-trigger" element
	 */
	private String parseIntervalTrigger(Element element, ParserContext parserContext) {
		String interval = element.getAttribute("interval");
		if (!StringUtils.hasText(interval)) {
			parserContext.getReaderContext().error(
					"the 'interval' attribute is required for an <interval-trigger/>", element);
		}
		TimeUnit timeUnit = TimeUnit.valueOf(element.getAttribute("time-unit"));
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(PERIODIC_TRIGGER_CLASSNAME);
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
		if (!StringUtils.hasText(cronExpression)) {
			parserContext.getReaderContext().error(
					"the 'expression' attribute is required for a <cron-trigger/>", element);
		}
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(CRON_TRIGGER_CLASSNAME);
		builder.addConstructorArgValue(cronExpression);
		return BeanDefinitionReaderUtils.registerWithGeneratedName(builder.getBeanDefinition(), parserContext.getRegistry());
	}

	/**
	 * Parse a "transactional" element and configure TransactionInterceptor with "transactionManager"
	 * and other "transactionDefinition" properties. This advisor will be applied on Polling Task proxy
	 * (see {@link org.springframework.integration.endpoint.AbstractPollingEndpoint}).
	 */
	private BeanDefinition configureTransactionAttributes(Element txElement, BeanDefinitionBuilder targetBuilder, ParserContext parserContext) {
		String TX_PKG_PREFIX = "org.springframework.transaction.interceptor";
		BeanDefinitionBuilder txDefinitionBuilder = 
			BeanDefinitionBuilder.genericBeanDefinition(TX_PKG_PREFIX + ".DefaultTransactionAttribute");
		txDefinitionBuilder.addPropertyValue("propagationBehaviorName", "PROPAGATION_" + txElement.getAttribute("propagation"));
		txDefinitionBuilder.addPropertyValue("isolationLevelName", "ISOLATION_" + txElement.getAttribute("isolation"));
		txDefinitionBuilder.addPropertyValue("timeout", txElement.getAttribute("timeout"));
		txDefinitionBuilder.addPropertyValue("readOnly", txElement.getAttribute("read-only"));
		BeanDefinitionBuilder attributeSourceBuilder = 
			BeanDefinitionBuilder.genericBeanDefinition(TX_PKG_PREFIX + ".MatchAlwaysTransactionAttributeSource");
		attributeSourceBuilder.addPropertyValue("transactionAttribute", txDefinitionBuilder.getBeanDefinition());
		
		BeanDefinitionBuilder txInterceptorBuilder = 
			BeanDefinitionBuilder.genericBeanDefinition(TX_PKG_PREFIX + ".TransactionInterceptor");
		txInterceptorBuilder.addPropertyReference("transactionManager", txElement.getAttribute("transaction-manager"));
		txInterceptorBuilder.addPropertyValue("transactionAttributeSource", attributeSourceBuilder.getBeanDefinition());
		
		return txInterceptorBuilder.getBeanDefinition();
	}

	/**
	 * Parses the 'advice-chain' element's sub-elements.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void configureAdviceChain(Element adviceChainElement, Element txElement, BeanDefinitionBuilder targetBuilder, ParserContext parserContext) {
		ManagedList adviceChain = new ManagedList();
		if (txElement != null){
			adviceChain.add(this.configureTransactionAttributes(txElement, targetBuilder, parserContext));
		}
		if (adviceChainElement != null){
			NodeList childNodes = adviceChainElement.getChildNodes();
			for (int i = 0; i < childNodes.getLength(); i++) {
				Node child = childNodes.item(i);
				if (child.getNodeType() == Node.ELEMENT_NODE) {
					Element childElement = (Element) child;
					String localName = child.getLocalName();
					if ("bean".equals(localName)) {
						BeanDefinitionHolder holder = parserContext.getDelegate().parseBeanDefinitionElement(
								childElement, targetBuilder.getBeanDefinition());
						parserContext.registerBeanComponent(new BeanComponentDefinition(holder));
						adviceChain.add(new RuntimeBeanReference(holder.getBeanName()));
					}
					else if ("ref".equals(localName)) {
						String ref = childElement.getAttribute("bean");
						adviceChain.add(new RuntimeBeanReference(ref));
					}
					else {
						BeanDefinition customBeanDefinition = parserContext.getDelegate().parseCustomElement(
								childElement, targetBuilder.getBeanDefinition());
						if (customBeanDefinition == null) {
							parserContext.getReaderContext().error(
									"failed to parse custom element '" + localName + "'", childElement);
						}
					}
				}
			}
		}
		targetBuilder.addPropertyValue("adviceChain", adviceChain);
	}

}
