/*
 * Copyright 2002-2013 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;

import org.junit.Assert;

import org.custommonkey.xmlunit.XMLAssert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.messaging.MessagingException;
import org.springframework.xml.transform.StringSource;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/**
 *
 * @author Jonas Partner
 * @author Gunnar Hillert
 *
 */
public class DefaultXmlPayloadConverterTests {

	DefaultXmlPayloadConverter converter;

	Document testDocument;

	String testDocumentAsString = "<test>hello</test>";

	@Before
	public void setUp() throws Exception {
		converter = new DefaultXmlPayloadConverter();
		testDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
				new InputSource(new StringReader(testDocumentAsString)));
	}

	@Test
	public void testGetDocumentWithString() {
		Document doc = converter.convertToDocument("<test>hello</test>");
		XMLAssert.assertXMLEqual(testDocument, doc);
	}

	@Test
	public void testGetDocumentWithDocument() {
		Document doc = converter.convertToDocument(testDocument);
		Assert.assertTrue(doc == testDocument);
	}

	@Test
	public void testGetNodePassingNode() {
		Node element = testDocument.getElementsByTagName("test").item(0);
		Node n = converter.convertToNode(element);
		assertTrue("Wrong node returned", element == n);
	}

	@Test
	public void testGetNodePassingString() {
		Node n = converter.convertToNode("<test>hello</test>");
		XMLAssert.assertXMLEqual(testDocument, (Document) n);
	}

	@Test
	public void testGetNodePassingDocument() {
		Node n = converter.convertToNode(testDocument);
		XMLAssert.assertXMLEqual(testDocument, (Document) n);
	}


	@Test
	public void testGetSourcePassingDocumet() throws Exception{
		Source source = converter.convertToSource(testDocument);
		assertEquals(DOMSource.class, source.getClass());
	}

	@Test
	public void testGetSourcePassingString() throws Exception{
		Source source = converter.convertToSource(testDocumentAsString);
		assertEquals(StringSource.class, source.getClass());
	}

	@Test
	public void testGetSourcePassingSource() throws Exception{
		SAXSource passedInSource = new SAXSource();
		Source source = converter.convertToSource(passedInSource);
		assertEquals(source, passedInSource);
	}

	@Test(expected=MessagingException.class)
	public void testInvalidPayload(){
		converter.convertToSource(12);
	}

    @Test
    public void testGetNodePassingDOMSource(){
        Node element = testDocument.getElementsByTagName("test").item(0);
		Node n = converter.convertToNode(new DOMSource(element));
		assertTrue("Wrong node returned", element == n);
    }


}
