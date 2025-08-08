/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xml.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;

public class TestXmlApplicationContext extends AbstractXmlApplicationContext {

	private final Resource[] resources;

	public TestXmlApplicationContext(String... xmlStrings) {
		resources = new Resource[xmlStrings.length];
		for (int i = 0; i < xmlStrings.length; i++) {
			resources[i] = new TestResource(xmlStrings[i]);
		}
		refresh();
	}

	@Override
	protected Resource[] getConfigResources() {
		return resources;
	}

	private static class TestResource extends AbstractResource {

		String xmlString;

		TestResource(String xmlString) {
			this.xmlString = xmlString;
		}

		@Override
		public String getDescription() {
			return "test";
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return new ByteArrayInputStream(xmlString.getBytes("UTF-8"));
		}

	}

}
