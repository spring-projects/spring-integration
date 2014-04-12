/*
 * Copyright 2002-2014 the original author or authors.
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

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;
import org.springframework.ws.WebServiceMessage;
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
 * @author Artem Bilan
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

	public SimpleWebServiceOutboundGateway(String uri) {
		this(uri, null, null);
	}

	public SimpleWebServiceOutboundGateway(String uri, SourceExtractor<?> sourceExtractor) {
		this(uri, sourceExtractor, (WebServiceMessageFactory) null);
	}

	public SimpleWebServiceOutboundGateway(String uri, SourceExtractor<?> sourceExtractor, WebServiceMessageFactory messageFactory) {
		super(uri, messageFactory);
		this.sourceExtractor = (sourceExtractor != null) ? sourceExtractor : new DefaultSourceExtractor();
	}

	@Override
	public String getComponentType() {
		return "ws:outbound-gateway(simple)";
	}

	@Override
	protected Object doHandle(String uri, final Message<?> requestMessage, final WebServiceMessageCallback requestCallback) {
		Object requestPayload = requestMessage.getPayload();
		Result responseResultInstance = null;
		if (requestPayload instanceof String) {
			responseResultInstance = new StringResult();
		}
		else if (requestPayload instanceof Document) {
			responseResultInstance = new DOMResult();
		}
		return this.getWebServiceTemplate().sendAndReceive(uri,
				new SimpleRequestMessageCallback(requestCallback, requestMessage), new SimpleResponseMessageExtractor(responseResultInstance));
	}

	private class SimpleRequestMessageCallback extends RequestMessageCallback {

		public SimpleRequestMessageCallback(WebServiceMessageCallback requestCallback, Message<?> requestMessage){
			super(requestCallback, requestMessage);
		}

		@Override
		public void doWithMessageInternal(WebServiceMessage message, Object payload) throws IOException, TransformerException {
			Source source = this.extractSource(payload);
			this.transform(source, message.getPayloadResult());
		}

		private Source extractSource(Object requestPayload) throws IOException, TransformerException{
			Source source = null;

			if (requestPayload instanceof Source) {
				source = (Source) requestPayload;
				Object o = sourceExtractor.extractData(source);
				Assert.isInstanceOf(Source.class, o);
				source = (Source) o;
			}
			else if (requestPayload instanceof String) {
				source = new StringSource((String) requestPayload);
			}
			else if (requestPayload instanceof Document) {
				source = new DOMSource((Document) requestPayload);
			}
			else {
				throw new MessagingException("Unsupported payload type '" + requestPayload.getClass() +
						"'. " + this.getClass().getName() + " only supports 'java.lang.String', '" + Source.class.getName() +
						"', and '" + Document.class.getName() + "'. Consider either using the '"
						+ MarshallingWebServiceOutboundGateway.class.getName() + "' or a Message Transformer.");
			}

			return source;
		}

	}

	private class SimpleResponseMessageExtractor extends ResponseMessageExtractor {

		private final Result result;

		public SimpleResponseMessageExtractor(Result result){
			super();
			this.result = result;
		}

		@Override
		public Object doExtractData(WebServiceMessage message) throws IOException, TransformerException{
			Source payloadSource = message.getPayloadSource();

			if (payloadSource != null && this.result != null) {
				this.transform(payloadSource, this.result);
				if (this.result instanceof StringResult){
					return this.result.toString();
				}
				else if (this.result instanceof DOMResult){
					return  ((DOMResult)this.result).getNode();
				}
				else {
					return this.result;
				}
			}

			return payloadSource;
		}

	}


	private static class DefaultSourceExtractor extends TransformerObjectSupport implements SourceExtractor<DOMSource> {

		@Override
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
