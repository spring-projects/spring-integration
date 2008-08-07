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

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;

import org.springframework.core.io.Resource;
import org.springframework.integration.transformer.PayloadTransformer;
import org.springframework.integration.xml.result.DomResultFactory;
import org.springframework.integration.xml.result.ResultFactory;
import org.springframework.integration.xml.source.DomSourceFactory;
import org.springframework.integration.xml.source.SourceFactory;

/**
 * Simple XSLT transformer implementation which returns a transformed
 * {@link Source}, {@link Document}, or {@link String}.
 * 
 * @author Jonas Partner
 * @author Mark Fisher
 */
public class XsltPayloadTransformer implements PayloadTransformer<Object, Result> {

	private final Templates templates;

	private SourceFactory sourceFactory = new DomSourceFactory();

	private ResultFactory resultFactory;


	public XsltPayloadTransformer(Templates templates) throws ParserConfigurationException {
		this.templates = templates;
		resultFactory = new DomResultFactory();
	}

	public XsltPayloadTransformer(Resource xslResource) throws Exception {
		this(TransformerFactory.newInstance().newTemplates(new StreamSource(xslResource.getInputStream())));
	}


	public void setSourceFactory(SourceFactory sourceFactory) {
		this.sourceFactory = sourceFactory;
	}

	public void setResultFactory(ResultFactory resultFactory) {
		this.resultFactory = resultFactory;
	}

	public Result transform(Object payload) throws TransformerException {
		if (Source.class.isAssignableFrom(payload.getClass())) {
			return this.transformSource((Source) payload);
		}
		Source source = this.sourceFactory.createSource(payload);
		return this.transformSource(source);
	}

	protected Result transformSource(Source source) throws TransformerException {
		Result result = resultFactory.createResult(source);
		this.templates.newTransformer().transform(source, result);
		return result;
	}

}
