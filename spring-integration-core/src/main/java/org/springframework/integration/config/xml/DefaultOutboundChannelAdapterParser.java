/*
 * Copyright 2002-2009 the original author or authors.
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

import org.springframework.beans.factory.support.RootBeanDefinition;
import org.w3c.dom.Element;

import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;outbound-channel-adapter/&gt; element.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 */
public class DefaultOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	protected String parseAndRegisterConsumer(Element element, ParserContext parserContext) {
		BeanComponentDefinition innerConsumerDefinition = IntegrationNamespaceUtils.parseInnerHandlerDefinition(element, parserContext);
		String consumerRef = element.getAttribute(IntegrationNamespaceUtils.REF_ATTRIBUTE);
		String methodName = element.getAttribute(IntegrationNamespaceUtils.METHOD_ATTRIBUTE);
		String consumerExpressionString = element.getAttribute(IntegrationNamespaceUtils.EXPRESSION_ATTRIBUTE);

		boolean isInnerConsumer = innerConsumerDefinition != null;
		boolean isRef = StringUtils.hasText(consumerRef);
		boolean isExpression = StringUtils.hasText(consumerExpressionString);
		boolean hasMethod = StringUtils.hasText(methodName);

		if (!(isInnerConsumer ^ (isRef ^ isExpression)) | (isInnerConsumer & (isRef & isExpression))) {
			parserContext.getReaderContext().error(
					"Exactly one of the 'ref', 'expression' or inner bean is required.", element);
		}

		if (hasMethod & isExpression) {
			parserContext.getReaderContext().error(
					"The 'method' attribute can't be used with 'expression' attribute.", element);
		}


		if (hasMethod | isExpression) {
			BeanDefinitionBuilder consumerBuilder = null;

			if (hasMethod) {
				consumerBuilder = BeanDefinitionBuilder.genericBeanDefinition(IntegrationNamespaceUtils.BASE_PACKAGE + ".handler.MethodInvokingMessageHandler");
				if (isRef) {
					consumerBuilder.addConstructorArgReference(consumerRef);
				}
				else {
					consumerBuilder.addConstructorArgValue(innerConsumerDefinition);
				}
				consumerBuilder.addConstructorArgValue(methodName);
			}
			else {
				consumerBuilder = BeanDefinitionBuilder.genericBeanDefinition(IntegrationNamespaceUtils.BASE_PACKAGE + ".handler.ExpressionEvaluatingMessageHandler");
				RootBeanDefinition expressionDef = new RootBeanDefinition("org.springframework.integration.config.ExpressionFactoryBean");
				expressionDef.getConstructorArgumentValues().addGenericArgumentValue(consumerExpressionString);
				consumerBuilder.addConstructorArgValue(expressionDef);
			}

			consumerBuilder.addPropertyValue("componentType", "outbound-channel-adapter");

			String order = element.getAttribute(IntegrationNamespaceUtils.ORDER);
			if (StringUtils.hasText(order)) {
				consumerBuilder.addPropertyValue(IntegrationNamespaceUtils.ORDER, order);
			}

			consumerRef = BeanDefinitionReaderUtils.registerWithGeneratedName(consumerBuilder.getBeanDefinition(), parserContext.getRegistry());
		}
		else if (isInnerConsumer) {
			consumerRef = innerConsumerDefinition.getBeanName();
		}

		Assert.hasText(consumerRef, "Can not determine consumer for 'outbound-channel-adapter'");
		return consumerRef;
	}

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		throw new UnsupportedOperationException();
	}
}
