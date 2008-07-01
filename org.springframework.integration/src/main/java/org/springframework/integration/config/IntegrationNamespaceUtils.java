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

import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.endpoint.ConcurrencyPolicy;
import org.springframework.integration.endpoint.interceptor.ConcurrencyInterceptor;
import org.springframework.transaction.interceptor.MatchAlwaysTransactionAttributeSource;
import org.springframework.transaction.interceptor.NoRollbackRuleAttribute;
import org.springframework.transaction.interceptor.RollbackRuleAttribute;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.util.StringUtils;

/**
 * Shared utility methods for integration namespace parsers.
 * 
 * @author Mark Fisher
 * @author Marius Bogoevici
 */
public abstract class IntegrationNamespaceUtils {

	private static final String CORE_SIZE_ATTRIBUTE = "core";

	private static final String MAX_SIZE_ATTRIBUTE = "max";

	private static final String QUEUE_CAPACITY_ATTRIBUTE = "queue-capacity";

	private static final String KEEP_ALIVE_ATTRIBUTE = "keep-alive";


	public static ConcurrencyPolicy parseConcurrencyPolicy(Element element) {
		ConcurrencyPolicy policy = new ConcurrencyPolicy();
		String coreSize = element.getAttribute(CORE_SIZE_ATTRIBUTE);
		String maxSize = element.getAttribute(MAX_SIZE_ATTRIBUTE);
		String queueCapacity = element.getAttribute(QUEUE_CAPACITY_ATTRIBUTE);
		String keepAlive = element.getAttribute(KEEP_ALIVE_ATTRIBUTE);
		if (StringUtils.hasText(coreSize)) {
			policy.setCoreSize(Integer.parseInt(coreSize));
		}
		if (StringUtils.hasText(maxSize)) {
			policy.setMaxSize(Integer.parseInt(maxSize));
		}
		if (StringUtils.hasText(queueCapacity)) {
			policy.setQueueCapacity(Integer.parseInt(queueCapacity));
		}
		if (StringUtils.hasText(keepAlive)) {
			policy.setKeepAliveSeconds(Integer.parseInt(keepAlive));
		}
		return policy;
	}

	@SuppressWarnings("unchecked")
	public static ManagedList parseEndpointInterceptors(Element element, ParserContext parserContext) {
		ManagedList interceptors = new ManagedList();
		NodeList childNodes = element.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node child = childNodes.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				Element childElement = (Element) child;
				String localName = child.getLocalName();
				if ("bean".equals(localName)) {
					BeanDefinitionParserDelegate beanParser = new BeanDefinitionParserDelegate(parserContext.getReaderContext());
					beanParser.initDefaults(childElement.getOwnerDocument().getDocumentElement());
					BeanDefinitionHolder beanDefinitionHolder = beanParser.parseBeanDefinitionElement(childElement);
					parserContext.registerBeanComponent(new BeanComponentDefinition(beanDefinitionHolder));
					interceptors.add(new RuntimeBeanReference(beanDefinitionHolder.getBeanName()));
				}
				else if ("ref".equals(localName)) {
					String ref = childElement.getAttribute("bean");
					interceptors.add(new RuntimeBeanReference(ref));
				}
				else if ("transaction-interceptor".equals(localName)) {
					String txInterceptorBeanName = parseTransactionInterceptor(childElement, parserContext);
					interceptors.add(new RuntimeBeanReference(txInterceptorBeanName));
				}
				else if ("concurrency-interceptor".equals(localName)) {
					String concurrencyInterceptorBeanName = parseConcurrencyInterceptor(childElement, parserContext);
					interceptors.add(new RuntimeBeanReference(concurrencyInterceptorBeanName));
				}
			}
		}
		return interceptors;
	}

	private static String parseTransactionInterceptor(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(TransactionInterceptor.class);
		String txManagerRef = element.getAttribute("transaction-manager");
		if (!StringUtils.hasText(txManagerRef)) {
			txManagerRef = "transactionManager";
		}
		builder.addPropertyReference("transactionManager", txManagerRef);
		RuleBasedTransactionAttribute attribute = new RuleBasedTransactionAttribute();
		String propagation = element.getAttribute("propagation");
		String isolation = element.getAttribute("isolation");
		String timeout = element.getAttribute("timeout");
		String readOnly = element.getAttribute("read-only");
		if (StringUtils.hasText(propagation)) {
			attribute.setPropagationBehaviorName(RuleBasedTransactionAttribute.PREFIX_PROPAGATION + propagation);
		}
		if (StringUtils.hasText(isolation)) {
			attribute.setIsolationLevelName(RuleBasedTransactionAttribute.PREFIX_ISOLATION + isolation);
		}
		if (StringUtils.hasText(timeout)) {
			try {
				attribute.setTimeout(Integer.parseInt(timeout));
			}
			catch (NumberFormatException ex) {
				parserContext.getReaderContext().error("Timeout must be an integer value: [" + timeout + "]", element);
			}
		}
		if (StringUtils.hasText(readOnly)) {
			attribute.setReadOnly(Boolean.valueOf(readOnly).booleanValue());
		}
		List rollbackRules = new LinkedList();
		if (element.hasAttribute("rollback-for")) {
			String rollbackForValue = element.getAttribute("rollback-for");
			String[] exceptionTypeNames = StringUtils.commaDelimitedListToStringArray(rollbackForValue);
			for (int i = 0; i < exceptionTypeNames.length; i++) {
				rollbackRules.add(new RollbackRuleAttribute(StringUtils.trimWhitespace(exceptionTypeNames[i])));
			}
		}
		if (element.hasAttribute("no-rollback-for")) {
			String noRollbackForValue = element.getAttribute("no-rollback-for");
			String[] exceptionTypeNames = StringUtils.commaDelimitedListToStringArray(noRollbackForValue);
			for (int i = 0; i < exceptionTypeNames.length; i++) {
				rollbackRules.add(new NoRollbackRuleAttribute(StringUtils.trimWhitespace(exceptionTypeNames[i])));
			}
		}
		attribute.setRollbackRules(rollbackRules);
		RootBeanDefinition attributeSourceDefinition = new RootBeanDefinition(MatchAlwaysTransactionAttributeSource.class);
		attributeSourceDefinition.setSource(parserContext.extractSource(element));
		attributeSourceDefinition.getPropertyValues().addPropertyValue("transactionAttribute", attribute);
		String attributeSourceBeanName = BeanDefinitionReaderUtils.registerWithGeneratedName(
				attributeSourceDefinition, parserContext.getRegistry());
		builder.addPropertyReference("transactionAttributeSource", attributeSourceBeanName);
		return BeanDefinitionReaderUtils.registerWithGeneratedName(builder.getBeanDefinition(), parserContext.getRegistry());
	}

	private static String parseConcurrencyInterceptor(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ConcurrencyInterceptor.class);
		String taskExecutorRef = element.getAttribute("task-executor");
		if (StringUtils.hasText(taskExecutorRef)) {
			if (element.getAttributes().getLength() != 1) {
				parserContext.getReaderContext().error("No other attributes are permitted when "
					+ "specifying a 'task-executor' reference on the <concurrency-interceptor/> element.",
					parserContext.extractSource(element));
			}
			builder.addConstructorArgReference(taskExecutorRef);
		}
		else {
			ConcurrencyPolicy policy = IntegrationNamespaceUtils.parseConcurrencyPolicy(element);
			builder.addConstructorArgValue(policy);
		}
		return BeanDefinitionReaderUtils.registerWithGeneratedName(builder.getBeanDefinition(), parserContext.getRegistry());
	}

	/**
	 * Populates the property identified by propertyName on the bean definition
	 * to the value of the attribute specified by attributeName, if that
	 * attribute is defined in the element
	 * 
	 * @param beanDefinition - the bean definition to be configured
	 * @param propertyName - the name of the bean property to be set
	 * @param element - the XML element where the attribute should be defined
	 * @param attributeName - the name of the attribute whose value will be set
	 * on the property
	 */
	public static void setValueIfAttributeDefined(RootBeanDefinition beanDefinition, String propertyName,
			Element element, String attributeName) {
		final String attributeValue = element.getAttribute(attributeName);
		if (StringUtils.hasText(attributeValue)) {
			beanDefinition.getPropertyValues().addPropertyValue(propertyName, attributeValue);
		}
	}

	/**
	 * Populates the property given by propertyName on the given bean definition
	 * to a reference to a bean identified by the value of the attribute
	 * specified by attributeName, if that attribute is defined in the element
	 * 
	 * @param beanDefinition - the bean definition to be configured
	 * @param propertyName - the name of the bean property to be set
	 * @param element - the XML element where the attribute should be defined
	 * @param attributeName - the id of the bean which will be used to populate
	 * the property
	 */
	public static void setBeanReferenceIfAttributeDefined(RootBeanDefinition beanDefinition, String propertyName,
			Element element, String attributeName) {
		final String attributeValue = element.getAttribute(attributeName);
		if (StringUtils.hasText(attributeValue)) {
			beanDefinition.getPropertyValues().addPropertyValue(propertyName, new RuntimeBeanReference(attributeValue));
		}
	}

}
