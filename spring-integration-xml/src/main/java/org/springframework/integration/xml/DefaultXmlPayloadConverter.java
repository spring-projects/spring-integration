/*
 * Copyright 2002-2010 the original author or authors.
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

import java.io.File;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import org.springframework.messaging.MessagingException;
import org.springframework.xml.transform.StringSource;

/**
 * Default implementation of {@link XmlPayloadConverter}. Supports
 * {@link Document}, {@link File} and {@link String} payloads.
 * 
 * @author Jonas Partner
 */
public class DefaultXmlPayloadConverter implements XmlPayloadConverter {

	private DocumentBuilderFactory documentBuilderFactory;


	public DefaultXmlPayloadConverter() {
		this.documentBuilderFactory = DocumentBuilderFactory.newInstance();
		this.documentBuilderFactory.setNamespaceAware(true);
	}

	public DefaultXmlPayloadConverter(DocumentBuilderFactory documentBuilderFactory) {
		this.documentBuilderFactory = documentBuilderFactory;
	}


	public Document convertToDocument(Object object) {
		if (object instanceof Document) {
			return (Document) object;
		}
		if (object instanceof File) {
			try {
				return getDocumentBuilder().parse((File) object);
			}
			catch (Exception e) {
				throw new MessagingException("failed to parse File payload '" + object + "'", e);
			}
		}
		if (object instanceof String) {
			try {
				return getDocumentBuilder().parse(new InputSource(new StringReader((String) object)));
			}
			catch (Exception e) {
				throw new MessagingException("failed to parse String payload '" + object + "'", e);
			}
		}
		throw new MessagingException("unsupported payload type [" + object.getClass().getName() + "]");
	}

	public Node convertToNode(Object object) {
		Node node = null;
		if (object instanceof Node) {
			node = (Node) object;
		}
		else if (object instanceof DOMSource) {
			node = ((DOMSource) object).getNode();
		}
		else {
			node = convertToDocument(object);
		}
		return node;
	}

	public Source convertToSource(Object object) {
		Source source = null;
		if (object instanceof Source) {
			source = (Source) object;
		}
		else if (object instanceof Document) {
			source = new DOMSource((Document) object);
		}
		else if (object instanceof String) {
			source = new StringSource((String) object);
		}
		else {
			throw new MessagingException("unsupported payload type [" + object.getClass().getName() + "]");
		}
		return source;
	}

	protected synchronized DocumentBuilder getDocumentBuilder() {
		try {
			return this.documentBuilderFactory.newDocumentBuilder();
		}
		catch (ParserConfigurationException e) {
			throw new MessagingException("failed to create a new DocumentBuilder", e);
		}
	}

}
