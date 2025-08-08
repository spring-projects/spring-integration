/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.aggregator.ResequencingMessageGroupProcessor;
import org.springframework.integration.aggregator.ResequencingMessageHandler;

/**
 * Parser for the &lt;resequencer&gt; element.
 *
 * @author Marius Bogoevici
 * @author Dave Syer
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 */
public class ResequencerParser extends AbstractCorrelatingMessageHandlerParser {

	private static final String RELEASE_PARTIAL_SEQUENCES_ATTRIBUTE = "release-partial-sequences";

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ResequencingMessageHandler.class);
		BeanDefinitionBuilder processorBuilder = BeanDefinitionBuilder.genericBeanDefinition(ResequencingMessageGroupProcessor.class);

		builder.addConstructorArgValue(processorBuilder.getBeanDefinition());

		this.doParse(builder, element, null, parserContext);

		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, RELEASE_PARTIAL_SEQUENCES_ATTRIBUTE);

		return builder;
	}

}
