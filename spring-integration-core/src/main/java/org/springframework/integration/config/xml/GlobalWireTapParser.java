/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;

/**
 * Parser for the top level 'wire-tap' element.
 *
 * @author David Turanski
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 2.1
 *
 */
public class GlobalWireTapParser extends GlobalChannelInterceptorParser {

	@Override
	protected Object getBeanDefinitionBuilderConstructorValue(Element element, ParserContext parserContext) {
		String wireTapBeanName = new WireTapParser().parse(element, parserContext);
		return new RuntimeBeanReference(wireTapBeanName);
	}

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext)
			throws BeanDefinitionStoreException {
		RuntimeBeanReference wireTapBean =
				(RuntimeBeanReference) definition.getConstructorArgumentValues()
						.getIndexedArgumentValues()
						.values()
						.iterator()
						.next()
						.getValue();
		return wireTapBean.getBeanName() + ".globalChannelInterceptor"; // NOSONAR never null
	}

}
