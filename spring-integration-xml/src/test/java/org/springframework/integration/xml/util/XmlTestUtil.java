/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xml.util;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import org.springframework.xml.transform.StringResult;

/**
 * Utility class for XML related testing
 *
 * @author Jonas Partner
 * @author Artem Bilan
 */
public class XmlTestUtil {

	private static final TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newInstance();

	private XmlTestUtil() {
		super();
	}

	public static Document getDocumentForString(String strDoc) throws Exception {
		DocumentBuilderFactory builder = DocumentBuilderFactory.newInstance();
		builder.setNamespaceAware(true);

		return builder.newDocumentBuilder().parse(
				new InputSource(new StringReader(strDoc)));
	}

	public static DOMSource getDomSourceForString(String strDoc) throws Exception {
		DOMSource domSource = new DOMSource();
		domSource.setNode(getDocumentForString(strDoc));
		return domSource;
	}

	public static DOMResult getDomResultForString(String strDoc) throws Exception {
		DOMResult res = new DOMResult();
		transform(getDomSourceForString(strDoc), res);
		return res;
	}

	public static StringResult getStringResultForString(String strDoc) throws Exception {
		StringResult res = new StringResult();
		transform(getDomSourceForString(strDoc), res);
		return res;
	}

	public static String docToString(Document doc) throws Exception {
		DOMSource source = new DOMSource(doc);
		StringResult stringResult = new StringResult();
		transform(source, stringResult);
		return stringResult.toString();
	}

	public static void transform(Source source, Result res) throws Exception {
		TRANSFORMER_FACTORY.newTransformer().transform(source, res);
	}

	public static String sourceToString(Source source) throws Exception {
		StringResult res = new StringResult();
		transform(source, res);
		return res.toString();
	}

}
