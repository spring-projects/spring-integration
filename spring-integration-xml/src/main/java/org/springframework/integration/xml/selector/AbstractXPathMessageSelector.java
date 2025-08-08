/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xml.selector;

import java.util.HashMap;
import java.util.Map;

import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.xml.DefaultXmlPayloadConverter;
import org.springframework.integration.xml.XmlPayloadConverter;
import org.springframework.xml.xpath.XPathExpression;
import org.springframework.xml.xpath.XPathExpressionFactory;

/**
 * Base class for XPath {@link MessageSelector} implementations.
 *
 * @author Jonas Partner
 */
public abstract class AbstractXPathMessageSelector implements MessageSelector {

	private final XPathExpression xPathExpresion;

	private volatile XmlPayloadConverter converter = new DefaultXmlPayloadConverter();

	/**
	 * @param xPathExpression XPath expression as a String
	 */
	public AbstractXPathMessageSelector(String xPathExpression) {
		this.xPathExpresion = XPathExpressionFactory.createXPathExpression(xPathExpression);
	}

	/**
	 * @param xPathExpression XPath expression as a String
	 * @param prefix namespace prefix
	 * @param namespace namespace URI
	 */
	public AbstractXPathMessageSelector(String xPathExpression, String prefix, String namespace) {
		Map<String, String> namespaces = new HashMap<String, String>();
		namespaces.put(prefix, namespace);
		this.xPathExpresion = XPathExpressionFactory.createXPathExpression(xPathExpression, namespaces);
	}

	/**
	 * @param xPathExpression XPath expression as a String
	 * @param namespaces Map of namespaces with prefixes as the Map keys
	 */
	public AbstractXPathMessageSelector(String xPathExpression, Map<String, String> namespaces) {
		this.xPathExpresion = XPathExpressionFactory.createXPathExpression(xPathExpression, namespaces);
	}

	/**
	 * @param xPathExpression XPath expression
	 */
	public AbstractXPathMessageSelector(XPathExpression xPathExpression) {
		this.xPathExpresion = xPathExpression;
	}

	/**
	 * Specify the converter used to convert payloads prior to XPath testing.
	 *
	 * @param converter The payload converter.
	 */
	public void setConverter(XmlPayloadConverter converter) {
		this.converter = converter;
	}

	protected XmlPayloadConverter getConverter() {
		return this.converter;
	}

	protected XPathExpression getXPathExpresion() {
		return this.xPathExpresion;
	}

}
