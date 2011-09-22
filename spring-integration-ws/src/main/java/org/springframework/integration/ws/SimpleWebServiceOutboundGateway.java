/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.integration.ws;

import java.io.IOException;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;

import org.springframework.integration.MessagingException;
import org.springframework.ws.WebServiceMessageFactory;
import org.springframework.ws.client.core.SourceExtractor;
import org.springframework.ws.client.core.WebServiceMessageCallback;
import org.springframework.ws.client.support.destination.DestinationProvider;
import org.springframework.xml.transform.StringResult;
import org.springframework.xml.transform.StringSource;
import org.springframework.xml.transform.TransformerObjectSupport;

/**
 * An outbound Messaging Gateway for invoking a Web Service.
 * 
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class SimpleWebServiceOutboundGateway extends AbstractWebServiceOutboundGateway {

	private final SourceExtractor<?> sourceExtractor;


	public SimpleWebServiceOutboundGateway(DestinationProvider destinationProvider) {
		this(destinationProvider, null, null);
	}

	public SimpleWebServiceOutboundGateway(DestinationProvider destinationProvider, SourceExtractor<?> sourceExtractor) {
		this(destinationProvider, sourceExtractor, (WebServiceMessageFactory) null);
	}

	public SimpleWebServiceOutboundGateway(DestinationProvider destinationProvider, SourceExtractor<?> sourceExtractor, WebServiceMessageFactory messageFactory) {
		super(destinationProvider, messageFactory);
		this.sourceExtractor = (sourceExtractor != null) ? sourceExtractor : new DefaultSourceExtractor();
	}


	@Override
	protected Object doHandle(String uri, Object requestPayload, WebServiceMessageCallback requestCallback) {
		if (requestPayload instanceof Source) {
			return this.getWebServiceTemplate().sendSourceAndReceive(
					uri, (Source) requestPayload, requestCallback, this.sourceExtractor);
		}
		if (requestPayload instanceof String) {
			StringResult result = new StringResult();
			this.getWebServiceTemplate().sendSourceAndReceiveToResult(
					uri, new StringSource((String) requestPayload), requestCallback, result);
			return result.toString();
		}
		if (requestPayload instanceof Document) {
			DOMResult result = new DOMResult();
			this.getWebServiceTemplate().sendSourceAndReceiveToResult(
					uri, new DOMSource((Document) requestPayload), requestCallback, result);
			return result.getNode();
		}
		throw new MessagingException("Unsupported payload type '" + requestPayload.getClass() +
				"'. " + this.getClass().getName() + " only supports 'java.lang.String', '" + Source.class.getName() +
				"', and '" + Document.class.getName() + "'. Consider either using the '"
				+ MarshallingWebServiceOutboundGateway.class.getName() + "' or a Message Transformer.");
	}


	private static class DefaultSourceExtractor extends TransformerObjectSupport implements SourceExtractor<DOMSource> {

		public DOMSource extractData(Source source) throws IOException, TransformerException {
			if (source instanceof DOMSource) {
				return (DOMSource)source;
			}
			DOMResult result = new DOMResult();
			this.transform(source, result);
			return new DOMSource(result.getNode());
		}
	}

}
