/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.router.ErrorMessageExceptionTypeRouter;

/**
 * Parser for the &lt;exception-type-router/&gt; element.
 *
 * @author Oleg Zhurakousky
 * @since 2.0.4
 */
public class ErrorMessageExceptionTypeRouterParser extends AbstractRouterParser {

	@Override
	protected String getMappingKeyAttributeName() {
		return "exception-type";
	}

	@Override
	protected BeanDefinition doParseRouter(Element element, ParserContext parserContext) {
		return new RootBeanDefinition(ErrorMessageExceptionTypeRouter.class);
	}

}
