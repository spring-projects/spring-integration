/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xml.config;

import javax.xml.transform.Templates;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Jonas Partner
 */
public class TestTemplatesFactory implements FactoryBean<Templates> {

	public Templates getObject() throws Exception {
		org.springframework.core.io.Resource xslResource = new ClassPathResource("test.xsl", getClass());
		return TransformerFactory.newInstance().newTemplates(new StreamSource(xslResource.getInputStream()));
	}

	public Class<Templates> getObjectType() {
		return Templates.class;
	}

	public boolean isSingleton() {
		return false;
	}

}
