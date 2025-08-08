/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.ExpressionFactoryBean;
import org.springframework.integration.transaction.DefaultTransactionSynchronizationFactory;
import org.springframework.integration.transaction.ExpressionEvaluatingTransactionSynchronizationProcessor;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Parser for transaction-synchronization-factory element.
 *
 * @author Oleg Zhurakousky
 *
 * @since 2.2
 *
 */
public class TransactionSynchronizationFactoryParser extends
		AbstractBeanDefinitionParser {

	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder syncFactoryBuilder =
				BeanDefinitionBuilder.genericBeanDefinition(DefaultTransactionSynchronizationFactory.class);

		Element beforeCommitElement = DomUtils.getChildElementByTagName(element, "before-commit");
		Element afterCommitElement = DomUtils.getChildElementByTagName(element, "after-commit");
		Element afterRollbackElement = DomUtils.getChildElementByTagName(element, "after-rollback");

		if (this.elementsNotDefined(beforeCommitElement, afterCommitElement, afterRollbackElement)) {
			parserContext.getReaderContext().error("At least one sub-element " +
					"('before-commit', 'after-commit' and/or 'after-rollback') must be defined", element);
		}
		BeanDefinitionBuilder expressionProcessor =
				BeanDefinitionBuilder.genericBeanDefinition(ExpressionEvaluatingTransactionSynchronizationProcessor.class);

		this.processSubElement(beforeCommitElement, parserContext, expressionProcessor, "beforeCommit");
		this.processSubElement(afterCommitElement, parserContext, expressionProcessor, "afterCommit");
		this.processSubElement(afterRollbackElement, parserContext, expressionProcessor, "afterRollback");

		syncFactoryBuilder.addConstructorArgValue(expressionProcessor.getBeanDefinition());

		return syncFactoryBuilder.getBeanDefinition();
	}

	private void processSubElement(Element element, ParserContext parserContext, BeanDefinitionBuilder expressionProcessor, String elementPrefix) {
		if (element != null) {
			String expression = element.getAttribute("expression");
			String channel = element.getAttribute("channel");
			if (this.attributesNotDefined(expression, channel)) {
				parserContext.getReaderContext().error("At least one attribute " +
						"('expression' and/or 'channel') must be defined", element);
			}

			if (StringUtils.hasText(expression)) {
				RootBeanDefinition expressionDef = new RootBeanDefinition(ExpressionFactoryBean.class);
				expressionDef.getConstructorArgumentValues().addGenericArgumentValue(expression);
				expressionProcessor.addPropertyValue(elementPrefix + "Expression", expressionDef);
			}
			if (StringUtils.hasText(channel)) {
				expressionProcessor.addPropertyReference(elementPrefix + "Channel", channel);
			}
			else {
				expressionProcessor.addPropertyReference(elementPrefix + "Channel", "nullChannel");
			}
		}
	}

	private boolean elementsNotDefined(Element... elements) {
		for (Object element : elements) {
			if (element != null) {
				return false;
			}
		}
		return true;
	}

	private boolean attributesNotDefined(String... attributes) {
		for (String attribute : attributes) {
			if (StringUtils.hasText(attribute)) {
				return false;
			}
		}
		return true;
	}

}
