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

package org.springframework.integration.ws;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.MimeHeaders;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import org.junit.Test;
import org.w3c.dom.NodeList;

import org.springframework.messaging.MessageHeaders;
import org.springframework.ws.soap.SoapHeader;
import org.springframework.ws.soap.SoapHeaderElement;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.soap.saaj.SaajSoapMessage;
import org.springframework.ws.transport.TransportConstants;
import org.springframework.xml.namespace.QNameUtils;
import org.springframework.xml.transform.StringSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Gary Russell
 * @author Mauro Molinari
 * @author Artem Bilan
 *
 * @since 2.1
 *
 */
public class DefaultSoapHeaderMapperTests {

	@Test
	public void testNullSoapHeader() {
		DefaultSoapHeaderMapper mapper = new DefaultSoapHeaderMapper();
		SoapMessage soapMessage = mock(SoapMessage.class);
		Map<String, Object> headers = mapper.toHeadersFromReply(soapMessage);
		assertThat(headers.size()).isEqualTo(0);
	}

	@Test
	public void testCustomSoapHeader() {
		DefaultSoapHeaderMapper mapper = new DefaultSoapHeaderMapper();
		mapper.setReplyHeaderNames("x:attr", "x:elem");
		SoapMessage soapMessage = mock(SoapMessage.class);
		SoapHeader soapHeader = mock(SoapHeader.class);
		@SuppressWarnings("unchecked")
		Iterator<QName> attrIterator = mock(Iterator.class);
		QName attribute = new QName("https://x", "attr", "x");
		@SuppressWarnings("unchecked")
		Iterator<SoapHeaderElement> elementIterator = mock(Iterator.class);
		SoapHeaderElement soapHeaderElement = mock(SoapHeaderElement.class);
		QName element = new QName("https://x", "elem", "x");

		when(soapMessage.getSoapHeader()).thenReturn(soapHeader);
		when(soapHeader.getAllAttributes()).thenReturn(attrIterator);
		when(attrIterator.hasNext()).thenReturn(true).thenReturn(false);
		when(attrIterator.next()).thenReturn(attribute);
		when(soapHeader.getAttributeValue(attribute)).thenReturn("attrValue");
		when(soapHeader.examineAllHeaderElements()).thenReturn(elementIterator);
		when(elementIterator.hasNext()).thenReturn(true).thenReturn(false);
		when(elementIterator.next()).thenReturn(soapHeaderElement);
		when(soapHeaderElement.getName()).thenReturn(element);

		Map<String, Object> headers = mapper.toHeadersFromReply(soapMessage);
		assertThat(headers.size()).isEqualTo(2);
		assertThat(headers.get("x:attr")).isEqualTo("attrValue");
		assertThat(headers.get("x:elem")).isSameAs(soapHeaderElement);
	}

	@Test
	public void testRealSoapHeader() throws Exception {
		String soap =
				"<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">"
						+ "<soapenv:Header>"
						+ "<auth>"
						+ "<username>user</username>"
						+ "<password>pass</password>"
						+ "</auth>"
						+ "<bar>BAR</bar>"
						+ "<baz>BAZ</baz>"
						+ "<qux>qux</qux>"
						+ "</soapenv:Header>"
						+ "<soapenv:Body>"
						+ "<foo>foo</foo>"
						+ "</soapenv:Body>"
						+ "</soapenv:Envelope>";
		SOAPMessage message = MessageFactory.newInstance()
				.createMessage(new MimeHeaders(), new ByteArrayInputStream(soap.getBytes("UTF-8")));
		SoapMessage soapMessage = new SaajSoapMessage(message);
		DefaultSoapHeaderMapper mapper = new DefaultSoapHeaderMapper();
		String authHeader = "auth";
		mapper.setRequestHeaderNames(authHeader, "ba*");
		Map<String, Object> headers = mapper.toHeadersFromRequest(soapMessage);
		assertThat(headers.get(authHeader)).isNotNull();
		assertThat(headers.get(authHeader)).isInstanceOf(SoapHeaderElement.class);
		SoapHeaderElement header = (SoapHeaderElement) headers.get(authHeader);
		DOMSource source = (DOMSource) header.getSource();
		NodeList nodeList = source.getNode().getChildNodes();
		assertThat(nodeList.item(0).getNodeName()).isEqualTo("username");
		assertThat(nodeList.item(0).getFirstChild().getNodeValue()).isEqualTo("user");
		assertThat(nodeList.item(1).getNodeName()).isEqualTo("password");
		assertThat(nodeList.item(1).getFirstChild().getNodeValue()).isEqualTo("pass");
		header = (SoapHeaderElement) headers.get("bar");
		assertThat(header).isNotNull();
		source = (DOMSource) header.getSource();
		nodeList = source.getNode().getChildNodes();
		assertThat(nodeList.item(0).getNodeValue()).isEqualTo("BAR");
		header = (SoapHeaderElement) headers.get("baz");
		assertThat(header).isNotNull();
		source = (DOMSource) header.getSource();
		nodeList = source.getNode().getChildNodes();
		assertThat(nodeList.item(0).getNodeValue()).isEqualTo("BAZ");
		assertThat(headers.get("qux")).isNull();
	}

	@Test
	public void testExtractStandardHeadersNullSoapAction() {
		DefaultSoapHeaderMapper mapper = new DefaultSoapHeaderMapper();
		SoapMessage soapMessage = mock(SoapMessage.class);
		when(soapMessage.getSoapAction()).thenReturn(null);

		assertThat(mapper.extractStandardHeaders(soapMessage).isEmpty()).isTrue();
	}

	@Test
	public void testExtractStandardHeadersEmptySoapAction() {
		DefaultSoapHeaderMapper mapper = new DefaultSoapHeaderMapper();
		SoapMessage soapMessage = mock(SoapMessage.class);
		when(soapMessage.getSoapAction()).thenReturn("");

		assertThat(mapper.extractStandardHeaders(soapMessage).isEmpty()).isTrue();
	}

	@Test
	public void testExtractStandardHeadersNonEmptySoapAction() {
		DefaultSoapHeaderMapper mapper = new DefaultSoapHeaderMapper();
		SoapMessage soapMessage = mock(SoapMessage.class);
		when(soapMessage.getSoapAction()).thenReturn("foo");

		Map<String, Object> standardHeaders = mapper.toHeadersFromRequest(soapMessage);

		assertThat(standardHeaders.size()).isEqualTo(1);
		assertThat(standardHeaders.containsKey(WebServiceHeaders.SOAP_ACTION)).isTrue();
		assertThat(standardHeaders.get(WebServiceHeaders.SOAP_ACTION)).isEqualTo("foo");
	}

	@Test
	public void testFromHeadersToRequest() throws SOAPException {
		DefaultSoapHeaderMapper mapper = new DefaultSoapHeaderMapper();
		mapper.setReplyHeaderNames("foo", "auth", "myHeader");

		Map<String, Object> headers = new HashMap<>();
		headers.put("foo", "bar");

		String docString =
				"<auth xmlns='http://test.auth.org'>"
						+ "<username>user</username>"
						+ "<password>pass</password>"
						+ "</auth>";
		Source source = new StringSource(docString);
		headers.put("auth", source);

		headers.put("myHeader", new StringSource("<test xmlns='http://test.org'>TEST</test>"));

		SaajSoapMessage message = new SaajSoapMessage(MessageFactory.newInstance().createMessage());

		mapper.fromHeadersToReply(new MessageHeaders(headers), message);

		SoapHeader soapHeader = message.getSoapHeader();
		assertThat(soapHeader.getAttributeValue(QNameUtils.parseQNameString("foo"))).isEqualTo("bar");

		Iterator<SoapHeaderElement> authIterator =
				soapHeader.examineHeaderElements(QNameUtils.parseQNameString("{http://test.auth.org}auth"));

		assertThat(authIterator.hasNext()).isTrue();

		SoapHeaderElement auth = authIterator.next();
		DOMSource authSource = (DOMSource) auth.getSource();

		NodeList nodeList = authSource.getNode().getChildNodes();
		assertThat(nodeList.item(0).getNodeName()).isEqualTo("username");
		assertThat(nodeList.item(0).getFirstChild().getNodeValue()).isEqualTo("user");

		assertThat(nodeList.item(1).getNodeName()).isEqualTo("password");
		assertThat(nodeList.item(1).getFirstChild().getNodeValue()).isEqualTo("pass");

		Iterator<SoapHeaderElement> testIterator =
				soapHeader.examineHeaderElements(QNameUtils.parseQNameString("{http://test.org}test"));

		assertThat(testIterator.hasNext()).isTrue();

		/*StringResult stringResult = new StringResult();
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.transform(message.getEnvelope().getSource(), stringResult);
		System. out. println(stringResult.toString());*/
	}

	@Test
	public void testDoNotOverriderSoapAction() throws Exception {
		MimeHeaders mimeHeaders = new MimeHeaders();

		String testSoapAction = "fooAction";

		mimeHeaders.setHeader(TransportConstants.HEADER_SOAP_ACTION, testSoapAction);

		String soap =
				"<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"></soapenv:Envelope>";

		SOAPMessage message = MessageFactory.newInstance()
				.createMessage(mimeHeaders, new ByteArrayInputStream(soap.getBytes()));

		SaajSoapMessage soapMessage = new SaajSoapMessage(message);

		DefaultSoapHeaderMapper headerMapper = new DefaultSoapHeaderMapper();

		headerMapper.fromHeadersToRequest(new MessageHeaders(null), soapMessage);

		assertThat(soapMessage.getSoapAction()).isEqualTo(testSoapAction);
	}

}
