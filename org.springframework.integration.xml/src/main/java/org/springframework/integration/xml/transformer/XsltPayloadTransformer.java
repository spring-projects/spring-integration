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

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.springframework.core.io.Resource;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.transformer.MessageTransformer;
import org.springframework.integration.xml.result.DomResultFactory;
import org.springframework.integration.xml.result.ResultFactory;
import org.springframework.integration.xml.source.DomSourceFactory;
import org.springframework.integration.xml.source.SourceFactory;
import org.w3c.dom.Document;

/**
 * Simple XSLT transformer implementation which returns a transformed
 * {@link Source}, {@link Document}, or {@link String}.
 * 
 * @author Jonas Partner
 */
public class XsltPayloadTransformer implements MessageTransformer {

	private final Templates templates;

	private SourceFactory sourceFactory = new DomSourceFactory();

	private ResultFactory resultFactory = new DomResultFactory();

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
				transformSource(message, (Source) message.getPayload());
			}
			else {
				Source source = sourceFactory.getSourceForMessage(message);
				transformSource(message, source);
			}
		}
		catch (TransformerException e) {
			throw new MessagingException(message, "XSLT transformation failed", e);
		}
	}

	@SuppressWarnings("unchecked")
	protected void transformSource(Message message, Source source) throws TransformerException {
		Result result = resultFactory.getNewResult(message);
		this.templates.newTransformer().transform(source, result);
		message.setPayload(result);
	}

	public void setSourceFactory(SourceFactory sourceFactory) {
		this.sourceFactory = sourceFactory;
	}

	public void setResultFactory(ResultFactory resultFactory) {
		this.resultFactory = resultFactory;
	}

}
