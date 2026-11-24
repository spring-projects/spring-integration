/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.router.PayloadTypeRouter;

/**
 * Parser for the &lt;payload-type-router/&gt; element.
 *
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @since 1.0.3
 */
public class PayloadTypeRouterParser extends AbstractRouterParser {

	@Override
	protected String getMappingKeyAttributeName() {
		return "type";
	}

	@Override
	protected BeanDefinition doParseRouter(Element element, ParserContext parserContext) {
		return new RootBeanDefinition(PayloadTypeRouter.class);
	}

}
