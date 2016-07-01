/*
 * Copyright 2002-2016 the original author or authors.
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import org.custommonkey.xmlunit.XMLAssert;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import org.springframework.core.io.ClassPathResource;
import org.springframework.messaging.MessagingException;
import org.springframework.xml.transform.StringSource;

/**
 *
 * @author Jonas Partner
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
public class DefaultXmlPayloadConverterTests {

	private static final String TEST_DOCUMENT_AS_STRING = "<test>hello</test>";

	private DefaultXmlPayloadConverter converter;

	private Document testDocument;

	@Before
	public void setUp() throws Exception {
		converter = new DefaultXmlPayloadConverter();
		testDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
				new InputSource(new StringReader(TEST_DOCUMENT_AS_STRING)));
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
	public void testGetSourcePassingDocument() throws Exception {
		Source source = converter.convertToSource(testDocument);
		assertEquals(DOMSource.class, source.getClass());
	}

	@Test
	public void testGetSourcePassingString() throws Exception {
		Source source = converter.convertToSource(TEST_DOCUMENT_AS_STRING);
		assertEquals(StringSource.class, source.getClass());
	}

	@Test
	public void testGetSourcePassingSource() throws Exception {
		SAXSource passedInSource = new SAXSource();
		Source source = converter.convertToSource(passedInSource);
		assertEquals(source, passedInSource);
	}

	@Test(expected = MessagingException.class)
	public void testInvalidPayload() {
		converter.convertToSource(12);
	}

    @Test
    public void testGetNodePassingDOMSource() {
        Node element = testDocument.getElementsByTagName("test").item(0);
		Node n = converter.convertToNode(new DOMSource(element));
		assertTrue("Wrong node returned", element == n);
    }

	@Test
	public void testConvertNodeToDocument() {
		Node element = testDocument.getElementsByTagName("test").item(0);
		Document doc = converter.convertToDocument(element);
		NodeList childNodes = doc.getChildNodes();
		assertEquals(1, childNodes.getLength());
		assertEquals("test", childNodes.item(0).getNodeName());
		assertEquals("hello", childNodes.item(0).getTextContent());
	}

	@Test
	public void testConvertSourceToDocument() throws Exception {
		Node element = testDocument.getElementsByTagName("test").item(0);
		DOMSource domSource = new DOMSource(element);
		Document doc = converter.convertToDocument(domSource);
		NodeList childNodes = doc.getChildNodes();
		assertEquals(1, childNodes.getLength());
		assertEquals("test", childNodes.item(0).getNodeName());
		assertEquals("hello", childNodes.item(0).getTextContent());
	}

	@Test
	public void testConvertBytesToDocument() throws Exception {
		Document doc = converter.convertToDocument("<test>hello</test>".getBytes());
		XMLAssert.assertXMLEqual(testDocument, doc);
	}

	@Test
	public void testConvertFileToDocument() throws Exception {
		File file = new ClassPathResource("org/springframework/integration/xml/customSource.data").getFile();
		Document doc = converter.convertToDocument(file);
		XMLAssert.assertXMLEqual(testDocument, doc);
	}

	@Test
	public void testConvertInputStreamToDocument() throws Exception {
		InputStream inputStream = new ClassPathResource("org/springframework/integration/xml/customSource.data")
				.getInputStream();
		Document doc = converter.convertToDocument(inputStream);
		XMLAssert.assertXMLEqual(testDocument, doc);
	}

	@Test
	public void testConvertStreamSourceToDocument() throws Exception {
		ClassPathResource resource = new ClassPathResource("org/springframework/integration/xml/customSource.data");
		StreamSource source = new StreamSource(resource.getInputStream());
		Document doc = converter.convertToDocument(source);
		XMLAssert.assertXMLEqual(testDocument, doc);
	}

	@Test
	public void testConvertCustomSourceToDocument() throws Exception {
		Document doc = converter.convertToDocument(new MySource());
		XMLAssert.assertXMLEqual(testDocument, doc);
	}

	private static class MySource implements Source {

		@Override
		public void setSystemId(String systemId) {
		}

		@Override
		public String getSystemId() {
			try {
				return new ClassPathResource("org/springframework/integration/xml/customSource.data")
						.getFile()
						.getPath();
			}
			catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}

	}

}
