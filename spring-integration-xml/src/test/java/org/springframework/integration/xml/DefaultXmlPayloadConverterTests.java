/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.xml;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.xmlunit.assertj3.XmlAssert.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import org.springframework.core.io.ClassPathResource;
import org.springframework.messaging.MessagingException;

/**
 *
 * @author Jonas Partner
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
public class DefaultXmlPayloadConverterTests {

	private static final String TEST_DOCUMENT_AS_STRING =
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?><test>hello</test>";

	private static final DefaultXmlPayloadConverter converter = new DefaultXmlPayloadConverter();

	private static Document testDocument;

	@BeforeAll
	public static void setUp() throws Exception {
		testDocument =
				DocumentBuilderFactory.newInstance()
						.newDocumentBuilder()
						.parse(new InputSource(new StringReader(TEST_DOCUMENT_AS_STRING)));
	}

	@Test
	public void testGetDocumentWithString() {
		Document doc = converter.convertToDocument(TEST_DOCUMENT_AS_STRING);
		assertThat(doc).and(TEST_DOCUMENT_AS_STRING).areIdentical();
	}

	@Test
	public void testGetDocumentWithDocument() {
		Document doc = converter.convertToDocument(testDocument);
		assertThat(doc).isSameAs(testDocument);
	}

	@Test
	public void testGetNodePassingNode() {
		Node element = testDocument.getElementsByTagName("test").item(0);
		Node n = converter.convertToNode(element);
		assertThat(n).isSameAs(element);
	}

	@Test
	public void testGetNodePassingString() {
		Node n = converter.convertToNode(TEST_DOCUMENT_AS_STRING);
		assertThat(n).and(TEST_DOCUMENT_AS_STRING).areIdentical();
	}

	@Test
	public void testGetNodePassingDocument() throws Exception {
		Node n = converter.convertToNode(testDocument);
		assertThat(n).and(TEST_DOCUMENT_AS_STRING).areIdentical();
	}


	@Test
	public void testGetSourcePassingDocument() {
		Source source = converter.convertToSource(testDocument);
		assertThat(source).isInstanceOf(DOMSource.class);
	}

	@Test
	public void testGetSourcePassingString() {
		Source source = converter.convertToSource(TEST_DOCUMENT_AS_STRING);
		assertThat(source).isInstanceOf(DOMSource.class);
	}

	@Test
	public void testGetSourcePassingSource() {
		SAXSource passedInSource = new SAXSource();
		Source source = converter.convertToSource(passedInSource);
		assertThat(source).isEqualTo(passedInSource);
	}

	@Test
	public void testInvalidPayload() {
		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> converter.convertToSource(12));
	}

	@Test
	public void testGetNodePassingDOMSource() {
		Node element = testDocument.getElementsByTagName("test").item(0);
		Node n = converter.convertToNode(new DOMSource(element));
		assertThat(n).isSameAs(element);
	}

	@Test
	public void testConvertNodeToDocument() {
		Node element = testDocument.getElementsByTagName("test").item(0);
		Document doc = converter.convertToDocument(element);
		NodeList childNodes = doc.getChildNodes();
		assertThat(childNodes.getLength()).isEqualTo(1);
		assertThat(childNodes.item(0).getNodeName()).isEqualTo("test");
		assertThat(childNodes.item(0).getTextContent()).isEqualTo("hello");
	}

	@Test
	public void testConvertSourceToDocument() {
		Node element = testDocument.getElementsByTagName("test").item(0);
		DOMSource domSource = new DOMSource(element);
		Document doc = converter.convertToDocument(domSource);
		NodeList childNodes = doc.getChildNodes();
		assertThat(childNodes.getLength()).isEqualTo(1);
		assertThat(childNodes.item(0).getNodeName()).isEqualTo("test");
		assertThat(childNodes.item(0).getTextContent()).isEqualTo("hello");
	}

	@Test
	public void testConvertBytesToDocument() throws Exception {
		Document doc = converter.convertToDocument(TEST_DOCUMENT_AS_STRING.getBytes());
		assertThat(doc).and(TEST_DOCUMENT_AS_STRING).areIdentical();
	}

	@Test
	public void testConvertFileToDocument() throws Exception {
		File file = new ClassPathResource("org/springframework/integration/xml/customSource.data").getFile();
		Document doc = converter.convertToDocument(file);
		assertThat(doc).and(TEST_DOCUMENT_AS_STRING).areSimilar();
	}

	@Test
	public void testConvertInputStreamToDocument() throws Exception {
		InputStream inputStream = new ClassPathResource("org/springframework/integration/xml/customSource.data")
				.getInputStream();
		Document doc = converter.convertToDocument(inputStream);
		assertThat(doc).and(TEST_DOCUMENT_AS_STRING).areSimilar();
	}

	@Test
	public void testConvertStreamSourceToDocument() throws Exception {
		ClassPathResource resource = new ClassPathResource("org/springframework/integration/xml/customSource.data");
		StreamSource source = new StreamSource(resource.getInputStream());
		Document doc = converter.convertToDocument(source);
		assertThat(doc).and(TEST_DOCUMENT_AS_STRING).areSimilar();
	}

	@Test
	public void testConvertCustomSourceToDocument() {
		Document doc = converter.convertToDocument(new MySource());
		assertThat(doc).and(TEST_DOCUMENT_AS_STRING).areSimilar();
	}

	private static class MySource implements Source {

		MySource() {
			super();
		}

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
