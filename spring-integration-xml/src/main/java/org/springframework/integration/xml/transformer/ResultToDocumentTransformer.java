/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xml.transformer;

import java.io.StringReader;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMResult;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;
import org.springframework.xml.DocumentBuilderFactoryUtils;
import org.springframework.xml.transform.StringResult;

/**
 * Creates a {@link Document} from a {@link Result} payload. Supports
 * {@link DOMResult} and {@link StringResult} implementations.
 *
 * @author Jonas Partner
 * @author Artem Bilan
 * @author Christian Tzolov
 */
public class ResultToDocumentTransformer implements ResultTransformer {

	private final Lock lock = new ReentrantLock();

	// Not guaranteed to be thread safe
	private final DocumentBuilderFactory documentBuilderFactory;

	public ResultToDocumentTransformer() {
		this(DocumentBuilderFactoryUtils.newInstance());
		this.documentBuilderFactory.setNamespaceAware(true);
	}

	public ResultToDocumentTransformer(DocumentBuilderFactory documentBuilderFactory) {
		Assert.notNull(documentBuilderFactory, "'documentBuilderFactory' must not be null.");
		this.documentBuilderFactory = documentBuilderFactory;
	}

	public Object transformResult(Result result) {
		Document document;
		if (DOMResult.class.isAssignableFrom(result.getClass())) {
			document = createDocumentFromDomResult((DOMResult) result);
		}
		else if (StringResult.class.isAssignableFrom(result.getClass())) {
			document = createDocumentFromStringResult((StringResult) result);
		}
		else {
			throw new MessagingException("failed to create document from payload type [" +
					result.getClass().getName() + "]");
		}
		return document;
	}

	private Document createDocumentFromDomResult(DOMResult domResult) {
		return (Document) domResult.getNode();
	}

	private Document createDocumentFromStringResult(StringResult stringResult) {
		try {
			return getDocumentBuilder().parse(new InputSource(new StringReader(stringResult.toString())));
		}
		catch (Exception e) {
			throw new MessagingException("failed to create Document from StringResult payload", e);
		}
	}

	private DocumentBuilder getDocumentBuilder() {
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
