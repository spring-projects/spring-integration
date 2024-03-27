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
