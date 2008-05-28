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

package org.springframework.integration.ws.handler;

import java.io.IOException;
import java.net.URI;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import org.springframework.integration.message.MessagingException;
import org.springframework.ws.client.core.SourceExtractor;
import org.springframework.ws.client.core.WebServiceMessageCallback;
import org.springframework.xml.transform.StringResult;
import org.springframework.xml.transform.StringSource;
import org.springframework.xml.transform.TransformerObjectSupport;

/**
 * A MessageHandler adapter for invoking a Web Service.
 * 
 * @author Mark Fisher
 */
public class SimpleWebServiceHandler extends AbstractWebServiceHandler {

	private final SourceExtractor sourceExtractor;


	public SimpleWebServiceHandler(URI uri) {
		this(uri, null);
	}

	public SimpleWebServiceHandler(URI uri, SourceExtractor sourceExtractor) {
		super(uri);
		this.sourceExtractor = (sourceExtractor != null) ? sourceExtractor : new DefaultSourceExtractor();
	}


	@Override
	protected Object doHandle(Object requestPayload, WebServiceMessageCallback requestCallback) {
		if (requestPayload instanceof Source) {
			return this.getWebServiceTemplate().sendSourceAndReceive(
					(Source) requestPayload, requestCallback, this.sourceExtractor);
		}
		if (requestPayload instanceof String) {
			StringResult result = new StringResult();
			this.getWebServiceTemplate().sendSourceAndReceiveToResult(
					new StringSource((String) requestPayload), requestCallback, result);
			return result.toString();
		}
		throw new MessagingException("Unsupported payload type '" + requestPayload.getClass() +
				"'. " + this.getClass().getName() + " only supports 'java.lang.String' and '" + Source.class.getName() +
				"'. Consider using the '" + MarshallingWebServiceHandler.class.getName() + "' instead.");
	}


	private static class DefaultSourceExtractor extends TransformerObjectSupport implements SourceExtractor {

		public Object extractData(Source source) throws IOException, TransformerException {
			if (source instanceof DOMSource) {
				return source;
			}
			DOMResult result = new DOMResult();
			this.transform(source, result);
			return new DOMSource(result.getNode());
		}
	}

}
