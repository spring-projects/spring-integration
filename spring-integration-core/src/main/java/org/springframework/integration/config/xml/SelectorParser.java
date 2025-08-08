/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.filter.MethodInvokingSelector;
import org.springframework.util.StringUtils;

/**
 * Parser for a top-level &lt;selector/&gt; element.
 *
 * @author Mark Fisher
 * @since 1.0.4
 */
public class SelectorParser extends AbstractSingleBeanDefinitionParser {

	@Override
	protected String getBeanClassName(Element element) {
		return MethodInvokingSelector.class.getName();
	}

	public void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String id = element.getAttribute("id");
		if (!StringUtils.hasText(id)) {
			parserContext.getReaderContext().error(
					"The 'id' attribute is required for a selector.", element);
		}
		String ref = element.getAttribute("ref");
		if (!StringUtils.hasText(ref)) {
			parserContext.getReaderContext().error(
					"The 'ref' attribute is required for selector '" + id + "'.", element);
		}
		String method = element.getAttribute("method");
		if (!StringUtils.hasText(method)) {
			parserContext.getReaderContext().error(
					"The 'method' attribute is required for selector '" + id + "'.", element);
		}
		builder.addConstructorArgValue(new RuntimeBeanReference(ref));
		builder.addConstructorArgValue(method);
	}

}
