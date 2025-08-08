/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ws.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.WebServiceMessageFactory;
import org.springframework.ws.pox.dom.DomPoxMessage;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Mark Fisher
 */
public class StubMessageFactory implements WebServiceMessageFactory {

	public WebServiceMessage createWebServiceMessage() {
		WebServiceMessage message = mock(WebServiceMessage.class);
		Source source = mock(Source.class);
		when(message.getPayloadSource()).thenReturn(source);
		return message;
	}

	public WebServiceMessage createWebServiceMessage(InputStream inputStream) throws IOException {
		try {
			Transformer transformer = TransformerFactory.newInstance().newTransformer();

			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			InputSource is = new InputSource(new InputStreamReader(inputStream));
			Document document = builder.parse(is);
			return new DomPoxMessage(document, transformer, "text/xml");
		}
		catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}

}
