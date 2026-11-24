/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
