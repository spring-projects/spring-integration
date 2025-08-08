/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.zip.config.xml;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * The namespace handler for the Zip namespace
 *
 * @author Gunnar Hillert
 *
 * @since 6.1
 *
 */
public class ZipNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	@Override
	public void init() {
		this.registerBeanDefinitionParser("zip-transformer", new ZipTransformerParser());
		this.registerBeanDefinitionParser("unzip-transformer", new UnZipTransformerParser());
	}

}
