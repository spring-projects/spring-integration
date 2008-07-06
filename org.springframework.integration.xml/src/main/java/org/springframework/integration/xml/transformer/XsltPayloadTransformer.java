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

import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.springframework.core.io.Resource;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.transformer.MessageTransformer;
import org.springframework.xml.transform.StringResult;
import org.springframework.xml.transform.StringSource;
import org.w3c.dom.Document;

/**
 * Simple XSLT transformer implementation which returns a transformed
 * {@link Source}, {@link Document}, or {@link String}.
 * 
 * @author Jonas Partner
 */
public class XsltPayloadTransformer implements MessageTransformer {

	private final Templates templates;

	public XsltPayloadTransformer(Templates templates) {
		this.templates = templates;
	}

	public XsltPayloadTransformer(Resource xslResource) throws Exception {
		this.templates = TransformerFactory.newInstance().newTemplates(new StreamSource(xslResource.getInputStream()));
	}

	@SuppressWarnings("unchecked")
	public void transform(Message message) {
		try {
			if (Source.class.isAssignableFrom(message.getPayload().getClass())) {
				this.transformSource(message);
			}
			else {
				throw new MessagingException(message,
						"Unsupported payload type for transformation expected javax.xml.transform.Source but got : "
								+ message.getPayload().getClass().getName());
			}
		}
		catch (TransformerException e) {
			throw new MessagingException(message, "XSLT transformation failed", e);
		}
	}

	@SuppressWarnings("unchecked")
	protected void transformSource(Message message) throws TransformerException {
		StringResult result = new StringResult();
		this.templates.newTransformer().transform((Source) message.getPayload(), result);
		message.setPayload(new StringSource(result.toString()));
	}

}
