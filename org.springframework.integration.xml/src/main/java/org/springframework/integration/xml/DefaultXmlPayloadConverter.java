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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.springframework.integration.core.MessagingException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/**
 * Supports {@link Document} and {@link String} 
 * @author Jonas Partner
 *
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
		Document doc;
		if (object instanceof Document) {
			doc = (Document) object;
		}
		else if (object instanceof String) {
			try {
				doc = getDocumentBuilder().parse(new InputSource(new StringReader((String) object)));
			}
			catch (Exception e) {
				throw new MessagingException("Failed to parse String payload " + object, e);
			}
		}
		else {
			throw new MessagingException("Unsupported payload type " + object.getClass().getName());
		}
		return doc;
	}

	public Node convertToNode(Object object) {
		Node node;
		if(object instanceof Node){
			node = (Node)object;
		} else {
			node = convertToDocument(object);
		}
		return node;
	}

	
	protected synchronized DocumentBuilder getDocumentBuilder() {
		try {
			return this.documentBuilderFactory.newDocumentBuilder();
		}
		catch (ParserConfigurationException e) {
			throw new MessagingException("Failed to create a new DocumentBuilder", e);
		}
	}


}
