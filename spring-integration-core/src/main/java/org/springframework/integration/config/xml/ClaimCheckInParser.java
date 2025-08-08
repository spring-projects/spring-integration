/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.transformer.ClaimCheckInTransformer;
import org.springframework.util.Assert;

/**
 * Parser for the &lt;claim-check-in/&gt; element.
 *
 * @author Mark Fisher
 * @since 2.0
 */
public class ClaimCheckInParser extends AbstractTransformerParser {

	@Override
	protected String getTransformerClassName() {
		return ClaimCheckInTransformer.class.getName();
	}

	@Override
	protected void parseTransformer(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String messageStoreRef = element.getAttribute("message-store");
		Assert.hasText(messageStoreRef, "The 'message-store' attribute is required.");
		builder.addConstructorArgReference(messageStoreRef);
	}

}
