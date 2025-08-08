/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.springframework.integration.config.TransformerFactoryBean;

/**
 * Parser for the &lt;transformer/&gt; element.
 *
 * @author Mark Fisher
 * @author Gary Russell
 */
public class TransformerParser extends AbstractDelegatingConsumerEndpointParser {

	@Override
	String getFactoryBeanClassName() {
		return TransformerFactoryBean.class.getName();
	}

	@Override
	boolean hasDefaultOption() {
		return false;
	}

}
