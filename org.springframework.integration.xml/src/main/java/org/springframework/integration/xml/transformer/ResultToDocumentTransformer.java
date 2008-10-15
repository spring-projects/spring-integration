/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.xml.transformer;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMResult;

import org.springframework.integration.core.MessagingException;
import org.springframework.xml.transform.StringResult;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * Creates a {@link Document} from a {@link Result} payload. Supports
 * {@link DOMResult} and {@link StringResult} implementations.
 * 
 * @author Jonas Partner
 */
public class ResultToDocumentTransformer implements ResultTransformer {

	// Not guaranteed to be thread safe
	private final DocumentBuilderFactory documentBuilderFactory;

	public ResultToDocumentTransformer(DocumentBuilderFactory documentBuilderFactory) {
		this.documentBuilderFactory = documentBuilderFactory;
	}

	public ResultToDocumentTransformer() {
		this.documentBuilderFactory = DocumentBuilderFactory.newInstance();
		this.documentBuilderFactory.setNamespaceAware(true);
	}

	public Object transformResult(Result res) {
		Document doc = null;
		if (DOMResult.class.isAssignableFrom(res.getClass())) {
			doc = createDocumentFromDomResult((DOMResult) res);
		}
		else if (StringResult.class.isAssignableFrom(res.getClass())) {
			doc = createDocumentFromStringResult((StringResult) res);
		}
		else {
			throw new MessagingException("Failed to create document from payload type [" + res.getClass().getName()
					+ "]");
		}
		return doc;
	}

	protected Document createDocumentFromDomResult(DOMResult domResult) {
		return (Document) domResult.getNode();
	}

	protected Document createDocumentFromStringResult(StringResult stringResult) {
		try {
			return getDocumentBuilder().parse(new InputSource(new StringReader(stringResult.toString())));
		}
		catch (Exception e) {
			throw new MessagingException("Failed to create Document from StringResult payload", e);
		}
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
