/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.router.HeaderValueRouter;

/**
 * Parser for the &lt;header-value-router/&gt; element.
 *
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @since 1.0.3
 */
public class HeaderValueRouterParser extends AbstractRouterParser {

	@Override
	protected BeanDefinition doParseRouter(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder headerValueRouterBuilder = BeanDefinitionBuilder.genericBeanDefinition(HeaderValueRouter.class);
		headerValueRouterBuilder.addConstructorArgValue(element.getAttribute("header-name"));
		return headerValueRouterBuilder.getBeanDefinition();
	}

}
