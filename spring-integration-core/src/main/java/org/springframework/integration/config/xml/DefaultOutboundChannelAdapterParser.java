/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.ExpressionFactoryBean;
import org.springframework.integration.handler.ExpressionEvaluatingMessageHandler;
import org.springframework.integration.handler.MethodInvokingMessageHandler;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Parser for the &lt;outbound-channel-adapter/&gt; element.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 */
public class DefaultOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		Object source = parserContext.extractSource(element);
		BeanComponentDefinition innerConsumerDefinition =
				IntegrationNamespaceUtils.parseInnerHandlerDefinition(element, parserContext);

		String consumerRef = element.getAttribute(IntegrationNamespaceUtils.REF_ATTRIBUTE);
		String methodName = element.getAttribute(IntegrationNamespaceUtils.METHOD_ATTRIBUTE);
		String consumerExpressionString = element.getAttribute(IntegrationNamespaceUtils.EXPRESSION_ATTRIBUTE);
		Element scriptElement = DomUtils.getChildElementByTagName(element, "script");

		boolean isInnerConsumer = innerConsumerDefinition != null;
		boolean isRef = StringUtils.hasText(consumerRef);
		boolean isExpression = StringUtils.hasText(consumerExpressionString);
		boolean hasMethod = StringUtils.hasText(methodName);
		boolean hasScript = scriptElement != null;

		if (!isInnerConsumer & !isRef & !isExpression & !hasScript) {
			parserContext.getReaderContext().error(
					"Exactly one of the 'ref', 'expression', <script> or inner bean is required.", source);
		}

		if (hasScript && (isRef | isExpression)) {
			parserContext.getReaderContext().error(
					"Neither 'ref' nor 'expression' are permitted when an inner script element is configured.",
					source);
		}

		if (hasMethod & isExpression) {
			parserContext.getReaderContext().error(
					"The 'method' attribute cannot be used with the 'expression' attribute.", element);
		}

		BeanDefinitionBuilder consumerBuilder = null;

		if (isExpression) {
			consumerBuilder = BeanDefinitionBuilder.genericBeanDefinition(ExpressionEvaluatingMessageHandler.class);
			RootBeanDefinition expressionDef = new RootBeanDefinition(ExpressionFactoryBean.class);
			expressionDef.getConstructorArgumentValues().addGenericArgumentValue(consumerExpressionString);
			consumerBuilder.addConstructorArgValue(expressionDef);
		}
		else if (hasScript) {
			consumerBuilder = BeanDefinitionBuilder.genericBeanDefinition(MethodInvokingMessageHandler.class);
			BeanDefinition scriptBeanDefinition = parserContext.getDelegate().parseCustomElement(scriptElement,
					consumerBuilder.getBeanDefinition());
			consumerBuilder.addConstructorArgValue(scriptBeanDefinition);
			consumerBuilder.addConstructorArgValue("processMessage");
		}
		else {
			consumerBuilder = BeanDefinitionBuilder.genericBeanDefinition(MethodInvokingMessageHandler.class);
			if (isRef) {
				consumerBuilder.addConstructorArgReference(consumerRef);
			}
			else {
				consumerBuilder.addConstructorArgValue(innerConsumerDefinition);
			}
			consumerBuilder.addConstructorArgValue(hasMethod ? methodName : "handleMessage");
		}

		consumerBuilder.addPropertyValue("componentType", "outbound-channel-adapter");
		return consumerBuilder.getBeanDefinition();
	}

}
