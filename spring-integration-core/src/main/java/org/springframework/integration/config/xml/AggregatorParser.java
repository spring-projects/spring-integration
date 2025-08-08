/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.aggregator.DefaultAggregatingMessageGroupProcessor;
import org.springframework.integration.aggregator.ExpressionEvaluatingMessageGroupProcessor;
import org.springframework.integration.config.AggregatorFactoryBean;
import org.springframework.util.StringUtils;

/**
 * Parser for the <em>aggregator</em> element of the integration namespace. Registers the annotation-driven
 * post-processors.
 *
 * @author Marius Bogoevici
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Dave Syer
 * @author Stefan Ferstl
 * @author Gary Russell
 * @author Artem Bilan
 */
public class AggregatorParser extends AbstractCorrelatingMessageHandlerParser {

	private static final String EXPIRE_GROUPS_UPON_COMPLETION = "expire-groups-upon-completion";

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanComponentDefinition innerHandlerDefinition = IntegrationNamespaceUtils.parseInnerHandlerDefinition(element,
				parserContext);
		String ref = element.getAttribute(REF_ATTRIBUTE);
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(AggregatorFactoryBean.class);
		String headersFunction = element.getAttribute("headers-function");
		BeanMetadataElement processor = null;

		if (innerHandlerDefinition != null || StringUtils.hasText(ref)) {
			if (innerHandlerDefinition != null) {
				processor = innerHandlerDefinition;
			}
			else {
				processor = new RuntimeBeanReference(ref);
			}
			builder.addPropertyValue("processorBean", processor);
			if (StringUtils.hasText(headersFunction)) {
				builder.addPropertyReference("headersFunction", headersFunction);
			}
		}
		else {
			BeanDefinitionBuilder groupProcessorBuilder;
			if (StringUtils.hasText(element.getAttribute(EXPRESSION_ATTRIBUTE))) {
				String expression = element.getAttribute(EXPRESSION_ATTRIBUTE);
				groupProcessorBuilder =
						BeanDefinitionBuilder.genericBeanDefinition(ExpressionEvaluatingMessageGroupProcessor.class);
				groupProcessorBuilder.addConstructorArgValue(expression);
			}
			else {
				groupProcessorBuilder =
						BeanDefinitionBuilder.genericBeanDefinition(DefaultAggregatingMessageGroupProcessor.class);
			}
			builder.addPropertyValue("processorBean", groupProcessorBuilder.getBeanDefinition());
			if (StringUtils.hasText(headersFunction)) {
				groupProcessorBuilder.addPropertyReference("headersFunction", headersFunction);
			}
		}

		if (StringUtils.hasText(element.getAttribute(METHOD_ATTRIBUTE))) {
			String method = element.getAttribute(METHOD_ATTRIBUTE);
			builder.addPropertyValue("methodName", method);
		}

		doParse(builder, element, processor, parserContext);

		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, EXPIRE_GROUPS_UPON_COMPLETION);

		return builder;
	}

}
