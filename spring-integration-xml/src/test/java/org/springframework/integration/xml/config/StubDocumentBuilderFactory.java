/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xml.config;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * @author Jonas Partner
 */
public class StubDocumentBuilderFactory extends DocumentBuilderFactory {

	@Override
	public Object getAttribute(String name) throws IllegalArgumentException {
		return null;
	}

	@Override
	public boolean getFeature(String name) throws ParserConfigurationException {
		return false;
	}

	@Override
	public DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
		return null;
	}

	@Override
	public void setAttribute(String name, Object value) throws IllegalArgumentException {
	}

	@Override
	public void setFeature(String name, boolean value) throws ParserConfigurationException {
	}

}
