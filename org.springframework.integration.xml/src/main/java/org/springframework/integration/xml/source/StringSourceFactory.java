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

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;

import org.springframework.integration.message.MessagingException;
import org.springframework.xml.transform.StringResult;
import org.springframework.xml.transform.StringSource;
import org.w3c.dom.Document;

/**
 * 
 * @author Jonas Partner
 *
 */
public class StringSourceFactory implements SourceFactory {

	private final TransformerFactory transformerFactory;

	public StringSourceFactory() {
		this(TransformerFactory.newInstance());
	}

	public StringSourceFactory(TransformerFactory transformerFactory) {
		this.transformerFactory = transformerFactory;
	}

	public Source createSource(Object payload) {
		if (Document.class.isAssignableFrom(payload.getClass())) {
			return createStringSourceForDocument((Document) payload);
		} else if (payload instanceof String) {
			return new StringSource((String) payload);
		}
		throw new MessagingException(
				"Failed to create Source for payload type ["
						+ payload.getClass().getName() + "]");
	}

	protected StringSource createStringSourceForDocument(Document doc) {
		try {
			StringResult result = new StringResult();
			Transformer transformer = getTransformer();
			transformer.transform(new DOMSource(doc), result);
			return new StringSource(result.toString());
		} catch (Exception e) {
			throw new MessagingException(
					"Exception creating StringSource from document", e);
		}
	}

	protected synchronized Transformer getTransformer() {
		try {
			return transformerFactory.newTransformer();
		} catch (Exception e) {
			throw new MessagingException("Exception creating transformer", e);
		}
	}

}
