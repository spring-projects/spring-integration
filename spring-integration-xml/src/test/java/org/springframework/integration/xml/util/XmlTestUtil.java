/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
