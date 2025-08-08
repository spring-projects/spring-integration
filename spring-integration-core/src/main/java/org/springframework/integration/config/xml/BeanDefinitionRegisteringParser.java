/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.xml.ParserContext;

/**
 * Simple strategy interface for parsers that are responsible
 * for parsing an element, creating a bean definition, and then
 * registering the bean. The {@link #parse(Element, ParserContext)}
 * method should return the name of the registered bean.
 *
 * @author Mark Fisher
 */
public interface BeanDefinitionRegisteringParser {

	String parse(Element element, ParserContext parserContext);

}
