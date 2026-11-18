/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.xml;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stax.StAXSource;

import org.jspecify.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;
import org.springframework.util.xml.StaxUtils;
import org.springframework.xml.DocumentBuilderFactoryUtils;

/**
 * Default implementation of {@link XmlPayloadConverter}. Supports
 * {@link Document}, {@link Node}, {@link DOMSource}, {@link SAXSource},
 * {@link javax.xml.transform.stream.StreamSource}, {@link StAXSource}, {@link File},
 * {@link String}, {@link InputStream} and {@code byte[]} payloads.
 * A {@link StAXSource} wraps a reader that the application has already created,
 * so this converter cannot change how that reader parses its input.
 * DTD processing and external entity resolution are controlled by the {@code XMLInputFactory}
 * that created the reader: for untrusted input, create the reader from a factory with
 * {@code XMLInputFactory.SUPPORT_DTD} and {@code XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES}
 * set to {@code false}.
 * The {@link StAXSource} conversion always uses the JDK's built-in {@link TransformerFactory},
 * so it is not affected by the {@code javax.xml.transform.TransformerFactory} system property
 * or a {@code META-INF/services} entry on the classpath.
 * Any other {@link Source}, or one that only carries a system ID, is resolved from that system ID URI,
 * so such a {@link Source} must not be built from untrusted input.
 *
 * @author Jonas Partner
 * @author Artem Bilan
 * @author Christian Tzolov
 * @author Glenn Renfro
 */
public class DefaultXmlPayloadConverter implements XmlPayloadConverter {

	private static final String CONSUMED_STAX_SOURCE =
			"The 'StAXSource' reader is not at a document or element start; it may have been consumed already";

	private final DocumentBuilderFactory documentBuilderFactory;

	private @Nullable TransformerFactory transformerFactory;

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
			else if (object instanceof StAXSource staxSource) {
				return staxSourceToDocument(staxSource);
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

	private Document staxSourceToDocument(StAXSource source) throws TransformerException, XMLStreamException {
		StAXSource staxSource = source;
		XMLEventReader eventReader = source.getXMLEventReader();
		if (eventReader != null) {
			XMLEvent next = eventReader.hasNext() ? eventReader.peek() : null;
			Assert.state(next != null && (next.isStartDocument() || next.isStartElement()), CONSUMED_STAX_SOURCE);
			// The JDK's XMLEventReader bridge mishandles a reader positioned at START_ELEMENT
			XMLStreamReader streamReader = StaxUtils.createEventStreamReader(eventReader);
			staxSource = new StAXSource(streamReader);
		}
		else {
			int eventType = source.getXMLStreamReader().getEventType();
			Assert.state(eventType == XMLStreamConstants.START_DOCUMENT
					|| eventType == XMLStreamConstants.START_ELEMENT, CONSUMED_STAX_SOURCE);
		}
		Transformer transformer;
		this.lock.lock();
		try {
			if (this.transformerFactory == null) {
				this.transformerFactory = createTransformerFactory();
			}
			transformer = this.transformerFactory.newTransformer();
		}
		finally {
			this.lock.unlock();
		}
		DOMResult result = new DOMResult(getDocumentBuilder().newDocument());
		transformer.transform(staxSource, result);
		return (Document) result.getNode();
	}

	/**
	 * Use the JDK's built-in factory, so the StAX conversion does not depend on the classpath.
	 * The external access attributes are defense in depth: they do not affect a {@link StAXSource},
	 * whose reader is already configured by the application, but they replace the JDK default of
	 * {@code "all"} should this factory ever parse a source itself.
	 */
	private static TransformerFactory createTransformerFactory() {
		TransformerFactory factory = TransformerFactory.newDefaultInstance();
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
		return factory;
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
