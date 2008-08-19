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
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import org.springframework.core.io.Resource;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.transformer.PayloadTransformer;
import org.springframework.integration.xml.result.DomResultFactory;
import org.springframework.integration.xml.result.ResultFactory;
import org.springframework.integration.xml.source.DomSourceFactory;
import org.springframework.integration.xml.source.SourceFactory;
import org.springframework.xml.transform.StringResult;
import org.springframework.xml.transform.StringSource;
import org.w3c.dom.Document;

/**
 * XSLT transformer implementation which returns a transformed {@link Source},
 * {@link Document}, or {@link String}. If alwaysUseSourceResultFactories is
 * false (default) the following logic occurs
 * 
 * {@link String} payload in results in {@link String} payload out
 * 
 * {@link Document} payload in {@link Document} payload out
 * 
 * {@link Source} payload in {@link Result} payload out, type will be determined
 * by the {@link ResultFactory}, {@link DomResultFactory} by default. If an
 * instance of {@link ResultTransformer} is registered this will be used to
 * convert the result.
 * 
 * If alwaysUseSourceResultFactories is true then the ResultFactory and
 * {@link SourceFactory} will be used to create the {@link Source} from the
 * payload and the {@link Result} to pass into the transformer. An instance of
 * {@link ResultTransformer} can also be provided to convert the Result prior to
 * returnign
 * 
 * 
 * @author Jonas Partner
 * @author Mark Fisher
 */
public class XsltPayloadTransformer implements
		PayloadTransformer<Object, Object> {

	private final Templates templates;

	private SourceFactory sourceFactory = new DomSourceFactory();

	private ResultFactory resultFactory = new DomResultFactory();

	private boolean alwaysUseSourceResultFactories = false;

	private ResultTransformer resultTransformer;

	public XsltPayloadTransformer(Templates templates)
			throws ParserConfigurationException {
		this.templates = templates;
		resultFactory = new DomResultFactory();
	}

	public XsltPayloadTransformer(Resource xslResource) throws Exception {
		this(TransformerFactory.newInstance().newTemplates(
				new StreamSource(xslResource.getInputStream())));
	}

	public void setSourceFactory(SourceFactory sourceFactory) {
		this.sourceFactory = sourceFactory;
	}

	public void setResultFactory(ResultFactory resultFactory) {
		this.resultFactory = resultFactory;
	}

	public void setAlwaysUseSourceResultFactories(
			boolean alwaysUserSourceResultFactories) {
		this.alwaysUseSourceResultFactories = alwaysUserSourceResultFactories;
	}

	public Object transform(Object payload) throws TransformerException {
		Object transformedPayload;
		if (alwaysUseSourceResultFactories) {
			transformedPayload = transformUsingFactories(payload);
		} else if (payload instanceof String) {
			transformedPayload = transformString((String) payload);
		} else if (Document.class.isAssignableFrom(payload.getClass())) {
			transformedPayload = transformDocument((Document) payload);
		} else if (Source.class.isAssignableFrom(payload.getClass())) {
			transformedPayload = transformSource((Source) payload, payload);
		} else {
			// fall back to trying factories
			transformedPayload = transformUsingFactories(payload);
		}
		return transformedPayload;

	}

	protected Object transformUsingFactories(Object payload)
			throws TransformerException {
		Source source = sourceFactory.createSource(payload);
		return transformSource(source, payload);
	}

	protected Object transformSource(Source source, Object payload)
			throws TransformerException {
		Result result = resultFactory.createResult(payload);
		this.templates.newTransformer().transform(source, result);
		if (resultTransformer != null) {
			return resultTransformer.transformResult(result);
		} else {
			return result;
		}
	}

	protected String transformString(String stringPayload)
			throws TransformerException {
		StringResult result = new StringResult();
		this.templates.newTransformer().transform(
				new StringSource(stringPayload), result);
		return result.toString();
	}

	protected Document transformDocument(Document documentPayload)
			throws TransformerException {
		DOMSource source = new DOMSource(documentPayload);
		Result result = resultFactory.createResult(documentPayload);
		if (!DOMResult.class.isAssignableFrom(result.getClass())) {
			throw new MessagingException(
					"Document to Document conversion requires a DOMResult producing ResultFactory implementation");
		}
		DOMResult domResult = (DOMResult) result;
		this.templates.newTransformer().transform(source, domResult);
		return (Document) domResult.getNode();
	}

	public void setResultTransformer(ResultTransformer resultTransformer) {
		this.resultTransformer = resultTransformer;
	}

}
