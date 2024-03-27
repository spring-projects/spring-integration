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

package org.springframework.integration.xml;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;
import org.springframework.xml.DocumentBuilderFactoryUtils;

/**
 * Default implementation of {@link XmlPayloadConverter}. Supports
 * {@link Document}, {@link File}, {@link String}, {@link Node} and
 * {@link DOMSource} payloads.
 *
 * @author Jonas Partner
 * @author Artem Bilan
 * @author Christian Tzolov
 */
public class DefaultXmlPayloadConverter implements XmlPayloadConverter {

	private final DocumentBuilderFactory documentBuilderFactory;

	private final Lock lock = new ReentrantLock();

	public DefaultXmlPayloadConverter() {
		this(DocumentBuilderFactoryUtils.newInstance());
		this.documentBuilderFactory.setNamespaceAware(true);
	}

	public DefaultXmlPayloadConverter(DocumentBuilderFactory documentBuilderFactory) {
		Assert.notNull(documentBuilderFactory, "'documentBuilderFactory' must not be null.");
		this.documentBuilderFactory = documentBuilderFactory;
	}

	@Override
	public Document convertToDocument(Object object) {
		try {
			if (object instanceof Document) {
				return (Document) object;
			}
			else if (object instanceof Node) {
				return nodeToDocument((Node) object);
			}
			else if (object instanceof DOMSource) {
				Node node = ((DOMSource) object).getNode();
				return nodeToDocument(node);
			}
			else if (object instanceof Source) {
				InputSource inputSource = sourceToInputSource((Source) object);
				return getDocumentBuilder().parse(inputSource);
			}
			else if (object instanceof File) {
				return getDocumentBuilder().parse((File) object);
			}
			else if (object instanceof String) {
				return getDocumentBuilder().parse(new InputSource(new StringReader((String) object)));
			}
			else if (object instanceof InputStream) {
				return getDocumentBuilder().parse((InputStream) object);
			}
			else if (object instanceof byte[]) {
				return getDocumentBuilder().parse(new ByteArrayInputStream((byte[]) object));
			}
		}
		catch (Exception e) {
			throw new MessagingException("failed to parse " + object.getClass() + " payload '" + object + "'", e);
		}

		throw new MessagingException("unsupported payload type [" + object.getClass().getName() + "]");
	}

	private static InputSource sourceToInputSource(Source source) {
		InputSource inputSource = SAXSource.sourceToInputSource(source);
		if (inputSource == null) {
			inputSource = new InputSource(source.getSystemId());
		}
		return inputSource;
	}

	protected Document nodeToDocument(Node node) {
		if (node instanceof Document) {
			return (Document) node;
		}
		Document document = getDocumentBuilder().newDocument();
		document.appendChild(document.importNode(node, true));
		return document;
	}

	@Override
	public Node convertToNode(Object object) {
		Node node;
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

	@Override
	public Source convertToSource(Object object) {
		if (object instanceof Source) {
			return (Source) object;
		}
		else if (object instanceof Document) {
			return new DOMSource((Document) object);
		}
		else {
			return convertToSource(convertToDocument(object));
		}
	}

	protected DocumentBuilder getDocumentBuilder() {
		this.lock.lock();
		try {
			return this.documentBuilderFactory.newDocumentBuilder();
		}
		catch (ParserConfigurationException e) {
			throw new MessagingException("failed to create a new DocumentBuilder", e);
		}
		finally {
			this.lock.unlock();
		}
	}

}
