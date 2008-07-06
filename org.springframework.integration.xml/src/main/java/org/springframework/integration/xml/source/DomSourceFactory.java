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

package org.springframework.integration.xml.source;

import java.io.StringReader;

import javax.sound.sampled.SourceDataLine;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessagingException;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * {@link SourceDataLine} implementation which supports creation of a
 * {@link DOMSource} from a {@link Document} or {@link String} payload
 * 
 * @author Jonas Partner
 * 
 */
public class DomSourceFactory implements SourceFactory {

	private final DocumentBuilderFactory docBuilderFactory;

	public DomSourceFactory() {
		this.docBuilderFactory = DocumentBuilderFactory.newInstance();
	}

	public DomSourceFactory(DocumentBuilderFactory docBuilderFactory) {
		this.docBuilderFactory = docBuilderFactory;
	}

	public Source getSourceForMessage(Message<?> message) {
		if (Document.class.isAssignableFrom(message.getPayload().getClass())) {
			return createDomSourceForDocument(message);
		}
		else if (message.getPayload() instanceof String) {
			return createDomSourceForString(message);
		}
		throw new MessagingException(message, "Could not create Source for payload type "
				+ message.getPayload().getClass().getName());
	}

	protected DOMSource createDomSourceForDocument(Message<?> message) {
		DOMSource source = new DOMSource(((Document) message.getPayload()).getDocumentElement());
		return source;
	}

	protected DOMSource createDomSourceForString(Message<?> message) {
		try {
			String str = (String) message.getPayload();
			Document doc = docBuilderFactory.newDocumentBuilder().parse(new InputSource(new StringReader(str)));
			DOMSource source = new DOMSource(doc.getDocumentElement());
			return source;
		}
		catch (Exception e) {
			throw new MessagingException(message, "Exception creating DOMSource", e);
		}
	}

}
