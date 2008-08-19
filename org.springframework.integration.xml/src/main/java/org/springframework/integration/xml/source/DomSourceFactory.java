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

package org.springframework.integration.xml.source;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.springframework.integration.message.MessagingException;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * {@link SourceFactory} implementation which supports creation of a
 * {@link DOMSource} from a {@link Document} or {@link String} payload.
 * 
 * @author Jonas Partner
 * @author Mark Fisher
 */
public class DomSourceFactory implements SourceFactory {

	private final DocumentBuilderFactory docBuilderFactory;

	public DomSourceFactory() {
		this.docBuilderFactory = DocumentBuilderFactory.newInstance();
	}

	public DomSourceFactory(DocumentBuilderFactory docBuilderFactory) {
		this.docBuilderFactory = docBuilderFactory;
	}


	public Source createSource(Object payload) {
		Source source = null;
		if (payload instanceof Document) {
			source =  createDomSourceForDocument((Document) payload);
		}
		else if (payload instanceof String) {
			source = createDomSourceForString((String) payload);
		}
		
		if(source == null){
		throw new MessagingException("Failed to create Source for payload type ["
				+ payload.getClass().getName() + "]");
		} 
		return source;
	}

	protected DOMSource createDomSourceForDocument(Document document) {
		DOMSource source = new DOMSource(document.getDocumentElement());
		return source;
	}

	protected DOMSource createDomSourceForString(String s) {
		try {
			Document doc = getNewDocumentBuilder().parse(new InputSource(new StringReader(s)));
			DOMSource source = new DOMSource(doc.getDocumentElement());
			return source;
		} catch (Exception e) {
			throw new MessagingException("Exception creating DOMSource", e);
		}
	}

	protected  DocumentBuilder getNewDocumentBuilder() throws ParserConfigurationException{
		synchronized (docBuilderFactory) {
			return docBuilderFactory.newDocumentBuilder();
		}
		
	}
}
