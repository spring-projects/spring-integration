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

package org.springframework.integration.xml.source;

import java.io.File;
import java.io.StringReader;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;
import org.springframework.xml.DocumentBuilderFactoryUtils;

/**
 * {@link SourceFactory} implementation which supports creation of a {@link DOMSource}
 * from a {@link Document}, {@link File} or {@link String} payload.
 *
 * @author Jonas Partner
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Christian Tzolov
 */
public class DomSourceFactory implements SourceFactory {

	private final Lock lock = new ReentrantLock();

	private final DocumentBuilderFactory documentBuilderFactory;

	public DomSourceFactory() {
		this(DocumentBuilderFactoryUtils.newInstance());
		this.documentBuilderFactory.setNamespaceAware(true);
	}

	public DomSourceFactory(DocumentBuilderFactory documentBuilderFactory) {
		Assert.notNull(documentBuilderFactory, "'documentBuilderFactory' must not be null.");
		this.documentBuilderFactory = documentBuilderFactory;
	}

	public Source createSource(Object payload) {
		Source source = null;
		if (payload instanceof Document) {
			source = createDomSourceForDocument((Document) payload);
		}
		else if (payload instanceof String) {
			source = createDomSourceForString((String) payload);
		}
		else if (payload instanceof File) {
			source = createDomSourceForFile((File) payload);
		}
		if (source == null) {
			throw new MessagingException("failed to create Source for payload type [" +
					payload.getClass().getName() + "]");
		}
		return source;
	}

	private DOMSource createDomSourceForDocument(Document document) {
		return new DOMSource(document.getDocumentElement());
	}

	private DOMSource createDomSourceForString(String s) {
		try {
			Document document = getNewDocumentBuilder().parse(new InputSource(new StringReader(s)));
			return new DOMSource(document.getDocumentElement());
		}
		catch (Exception e) {
			throw new MessagingException("failed to create DOMSource for String payload", e);
		}
	}

	private DOMSource createDomSourceForFile(File file) {
		try {
			Document document = getNewDocumentBuilder().parse(file);
			return new DOMSource(document.getDocumentElement());
		}
		catch (Exception e) {
			throw new MessagingException("failed to create DOMSource for File payload", e);
		}
	}

	private DocumentBuilder getNewDocumentBuilder() throws ParserConfigurationException {
		this.lock.lock();
		try {
			return this.documentBuilderFactory.newDocumentBuilder();
		}
		finally {
			this.lock.unlock();
		}
	}

}
