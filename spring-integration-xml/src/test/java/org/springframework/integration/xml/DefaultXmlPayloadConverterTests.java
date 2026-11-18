/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.xml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamSource;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junitpioneer.jupiter.SetSystemProperty;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import org.springframework.core.io.ClassPathResource;
import org.springframework.messaging.MessagingException;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.xmlunit.assertj3.XmlAssert.assertThat;

/**
 *
 * @author Jonas Partner
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Glenn Renfro
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
	void testStaxSourceIsConvertedFromItsContent(@TempDir Path tempDir) throws Exception {
		XMLStreamReader reader = XMLInputFactory.newFactory()
				.createXMLStreamReader(decoySystemId(tempDir), new StringReader(TEST_DOCUMENT_AS_STRING));
		assertConvertsToTestDocument(new StAXSource(reader));
	}

	@Test
	void testStaxSourceAtStartElementIsConvertedFromItsContent(@TempDir Path tempDir) throws Exception {
		XMLStreamReader reader = XMLInputFactory.newFactory()
				.createXMLStreamReader(decoySystemId(tempDir), new StringReader(TEST_DOCUMENT_AS_STRING));
		reader.nextTag();
		assertConvertsToTestDocument(new StAXSource(reader));
	}

	@Test
	void testStaxEventSourceIsConvertedFromItsContent(@TempDir Path tempDir) throws Exception {
		XMLEventReader eventReader = XMLInputFactory.newFactory()
				.createXMLEventReader(decoySystemId(tempDir), new StringReader(TEST_DOCUMENT_AS_STRING));
		assertConvertsToTestDocument(new StAXSource(eventReader));
	}

	@Test
	void testStaxSourceWithoutSystemIdIsConvertedFromItsContent() throws Exception {
		XMLStreamReader reader = XMLInputFactory.newFactory()
				.createXMLStreamReader(new StringReader(TEST_DOCUMENT_AS_STRING));
		assertConvertsToTestDocument(new StAXSource(reader));
	}

	@Test
	void testStaxSourceKeepsNamespaces() throws Exception {
		String xml = "<ns:test xmlns:ns=\"urn:si:test\">hello</ns:test>";
		Document doc = converter.convertToDocument(new StAXSource(XMLInputFactory.newFactory()
				.createXMLStreamReader(new StringReader(xml))));
		assertThat(doc).and(xml).areIdentical();
	}

	@Test
	void testStaxEventSourceAtStartElementIsConvertedFromItsContent() throws Exception {
		XMLEventReader eventReader = XMLInputFactory.newFactory()
				.createXMLEventReader(new StringReader(TEST_DOCUMENT_AS_STRING));
		eventReader.nextEvent();
		assertConvertsToTestDocument(new StAXSource(eventReader));
	}

	@Test
	void testConsumedStaxSourceIsRejected() throws Exception {
		StAXSource source = new StAXSource(XMLInputFactory.newFactory()
				.createXMLStreamReader(new StringReader(TEST_DOCUMENT_AS_STRING)));
		assertConvertsToTestDocument(source);
		assertConsumedStaxSourceIsRejected(source);
	}

	@Test
	void testConsumedStaxEventSourceIsRejected() throws Exception {
		StAXSource source = new StAXSource(XMLInputFactory.newFactory()
				.createXMLEventReader(new StringReader(TEST_DOCUMENT_AS_STRING)));
		assertConvertsToTestDocument(source);
		assertConsumedStaxSourceIsRejected(source);
	}

	@Test
	@SetSystemProperty(key = "javax.xml.transform.TransformerFactory",
			value = "org.springframework.integration.xml.MissingTransformerFactory")
	void testStaxSourceConversionUsesJdkTransformerFactory() throws Exception {
		XMLStreamReader reader = XMLInputFactory.newFactory()
				.createXMLStreamReader(new StringReader(TEST_DOCUMENT_AS_STRING));
		Document doc = new DefaultXmlPayloadConverter().convertToDocument(new StAXSource(reader));
		assertThat(doc).and(TEST_DOCUMENT_AS_STRING).areSimilar();
	}

	@Test
	public void testConvertCustomSourceToDocument() {
		Document doc = converter.convertToDocument(new MySource());
		assertThat(doc).and(TEST_DOCUMENT_AS_STRING).areSimilar();
	}

	private static String decoySystemId(Path tempDir) throws IOException {
		Path decoy = tempDir.resolve("decoy.xml");
		Files.writeString(decoy, "<test>fetched</test>");
		return decoy.toUri().toString();
	}

	private static void assertConvertsToTestDocument(StAXSource source) {
		Document doc = converter.convertToDocument(source);
		assertThat(doc).and(TEST_DOCUMENT_AS_STRING).areSimilar();
	}

	private static void assertConsumedStaxSourceIsRejected(StAXSource source) {
		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> converter.convertToDocument(source))
				.withRootCauseInstanceOf(IllegalStateException.class)
				.havingRootCause()
				.withMessageContaining("consumed");
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
