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

import org.jspecify.annotations.Nullable;
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

	private void processSubElement(@Nullable Element element, ParserContext parserContext, BeanDefinitionBuilder expressionProcessor, String elementPrefix) {
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

	private boolean elementsNotDefined(@Nullable Element... elements) {
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
