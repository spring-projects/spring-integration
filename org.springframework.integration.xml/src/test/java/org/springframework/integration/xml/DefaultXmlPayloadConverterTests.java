/*
 * Copyright 2002-2007 the original author or authors.
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
package org.springframework.integration.xml;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.Assert;

import org.custommonkey.xmlunit.XMLAssert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class DefaultXmlPayloadConverterTests {

	DefaultXmlPayloadConverter converter;

	Document testDocument;

	String testDocumentAsString = "<test>hello</test>";

	@Before
	public void setUp() throws Exception{
		converter = new DefaultXmlPayloadConverter();
		testDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
				new InputSource(new StringReader(testDocumentAsString)));
	}

	@Test
	public void testWithString() {
		Document doc = converter.convertToDocument("<test>hello</test>");
		XMLAssert.assertXMLEqual(testDocument, doc);
	}
	
	@Test
	public void testWithDocument() {
		Document doc = converter.convertToDocument(testDocument);
		Assert.assertTrue(doc == testDocument);
	}

}
