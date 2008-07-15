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

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.transformer.MessageTransformer;
import org.springframework.xml.transform.StringResult;

/**
 * Creates a {@link Document} from a {@link Result} payload.
 * 
 * @author Jonas Partner
 */
public class ResultToDocumentTransformer implements MessageTransformer {

	// Not guaranteed to be thread safe
	private final DocumentBuilderFactory documentBuilderFactory;


	public ResultToDocumentTransformer(DocumentBuilderFactory documentBuilderFactory) {
		this.documentBuilderFactory = documentBuilderFactory;
	}

	public ResultToDocumentTransformer() {
		this(DocumentBuilderFactory.newInstance());
	}


	@SuppressWarnings("unchecked")
	public Message<?> transform(Message message) {
		Document doc = null;
		if (DOMResult.class.isAssignableFrom(message.getPayload().getClass())) {
			doc = createDocumentFromDomResult(message, (DOMResult) message.getPayload());
		}
		else if (StringResult.class.isAssignableFrom(message.getPayload().getClass())) {
			doc = createDocumentFromStringResult(message, (StringResult) message.getPayload());
		}
		else {
			throw new MessagingException(message, "Failed to create document from payload type ["
					+ message.getPayload().getClass().getName() + "]");
		}
		return new GenericMessage<Document>(doc, message.getHeader());
	}

	@SuppressWarnings("unchecked")
	protected Document createDocumentFromDomResult(Message message, DOMResult domResult) {
		return (Document) domResult.getNode();
	}

	@SuppressWarnings("unchecked")
	protected Document createDocumentFromStringResult(Message message, StringResult stringResult) {
		try {
			return getDocumentBuilder().parse(new InputSource(new StringReader(stringResult.toString())));
		}
		catch (Exception e) {
			throw new MessagingException(message, "Exception creating Document form StringResult payload", e);
		}
	}

	protected synchronized DocumentBuilder getDocumentBuilder() {
		try {
			return documentBuilderFactory.newDocumentBuilder();
		}
		catch (ParserConfigurationException e) {
			throw new MessagingException("Failed to create a new DocumentBuilder", e);
		}
	}

}
