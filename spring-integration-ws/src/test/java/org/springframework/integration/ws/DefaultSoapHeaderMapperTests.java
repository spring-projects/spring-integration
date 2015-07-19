/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.integration.ws;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.QName;

import org.junit.Test;
import org.springframework.ws.soap.SoapHeader;
import org.springframework.ws.soap.SoapHeaderElement;
import org.springframework.ws.soap.SoapMessage;

/**
 * @author Gary Russell
 * @author Mauro Molinari
 * @since 2.1
 *
 */
public class DefaultSoapHeaderMapperTests {

	@Test
	public void testNullSoapHeader() {
		DefaultSoapHeaderMapper mapper = new DefaultSoapHeaderMapper();
		SoapMessage soapMessage = mock(SoapMessage.class);
		Map<String, Object> headers = mapper.toHeadersFromReply(soapMessage);
		assertEquals(0, headers.size());
	}

	@Test
	public void testCustomSoapHeader() {
		DefaultSoapHeaderMapper mapper = new DefaultSoapHeaderMapper();
		mapper.setReplyHeaderNames("x:attr", "x:elem");
		SoapMessage soapMessage = mock(SoapMessage.class);
		SoapHeader soapHeader = mock(SoapHeader.class);
		@SuppressWarnings("unchecked")
		Iterator<QName> attrIterator = mock(Iterator.class);
		QName attribute = new QName("http://x", "attr", "x");
		@SuppressWarnings("unchecked")
		Iterator<SoapHeaderElement> elementIterator = mock(Iterator.class);
		SoapHeaderElement soapHeaderElement = mock(SoapHeaderElement.class);
		QName element = new QName("http://x", "elem", "x");

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
		assertEquals(2, headers.size());
		assertEquals("attrValue", headers.get("x:attr"));
		assertSame(soapHeaderElement, headers.get("x:elem"));
	}

	@Test
	public void testExtractStandardHeadersNullSoapAction() {
		DefaultSoapHeaderMapper mapper = new DefaultSoapHeaderMapper();
		SoapMessage soapMessage = mock(SoapMessage.class);
		when(soapMessage.getSoapAction()).thenReturn(null);

		assertTrue(mapper.extractStandardHeaders(soapMessage).isEmpty());
	}

	@Test
	public void testExtractStandardHeadersEmptySoapAction() {
		DefaultSoapHeaderMapper mapper = new DefaultSoapHeaderMapper();
		SoapMessage soapMessage = mock(SoapMessage.class);
		when(soapMessage.getSoapAction()).thenReturn("");

		assertTrue(mapper.extractStandardHeaders(soapMessage).isEmpty());
	}

	@Test
	public void testExtractStandardHeadersNonEmptySoapAction() {
		DefaultSoapHeaderMapper mapper = new DefaultSoapHeaderMapper();
		SoapMessage soapMessage = mock(SoapMessage.class);
		when(soapMessage.getSoapAction()).thenReturn("foo");

		Map<String, Object> standardHeaders = mapper.toHeadersFromRequest(soapMessage);

		assertEquals(1, standardHeaders.size());
		assertTrue(standardHeaders.containsKey(WebServiceHeaders.SOAP_ACTION));
		assertEquals("foo", standardHeaders.get(WebServiceHeaders.SOAP_ACTION));
	}

}
