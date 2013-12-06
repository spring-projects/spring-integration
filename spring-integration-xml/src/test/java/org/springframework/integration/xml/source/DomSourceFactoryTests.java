/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.xml.source;

import static org.junit.Assert.assertNotNull;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.junit.Assert.assertEquals;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import org.springframework.messaging.MessagingException;
import org.springframework.xml.transform.StringResult;

/**
 * @author Jonas Partner
 */
public class DomSourceFactoryTests {

	Document doc;

	DomSourceFactory sourceFactory;

	String docContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root>testValue</root>";

	@Before
	public void setUp() throws Exception {
		StringReader reader = new StringReader(docContent);
		doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(reader));
		sourceFactory = new DomSourceFactory();
	}

	@Test
	public void testWithDocumentPayload() throws Exception {
		Source source = sourceFactory.createSource(doc);
		assertNotNull("Returned source was null", source);
		assertEquals("Expected DOMSource", DOMSource.class, source.getClass());
		assertXMLEqual("Wrong content in source ", docContent, getAsString(source));
	}

	@Test
	public void testWithStringPayload() throws Exception {
		Source source = sourceFactory.createSource(docContent);
		assertNotNull("Returned source was null", source);
		assertEquals("Expected DOMSource", DOMSource.class, source.getClass());
		assertXMLEqual("Wrong content in source ", docContent, getAsString(source));
	}

	@Test(expected = MessagingException.class)
	public void testWithUnsupportedPayload() throws Exception {
		sourceFactory.createSource(new Integer(12));
	}


	private String getAsString(Source source) throws Exception {
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		StringResult res = new StringResult();
		transformer.transform(source, res);
		return res.toString();
	}

}
