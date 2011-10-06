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
import java.util.Map;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.WebServiceMessageFactory;
import org.springframework.ws.client.core.SourceExtractor;
import org.springframework.ws.client.core.WebServiceMessageCallback;
import org.springframework.ws.client.core.WebServiceMessageExtractor;
import org.springframework.ws.client.support.destination.DestinationProvider;
import org.springframework.ws.soap.SoapHeader;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.xml.transform.StringResult;
import org.springframework.xml.transform.StringSource;
import org.springframework.xml.transform.TransformerObjectSupport;
import org.w3c.dom.Document;

/**
 * An outbound Messaging Gateway for invoking a Web Service.
 * 
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class SimpleWebServiceOutboundGateway extends AbstractWebServiceOutboundGateway {

	public SimpleWebServiceOutboundGateway(DestinationProvider destinationProvider) {
		this(destinationProvider, null, null);
	}

	public SimpleWebServiceOutboundGateway(DestinationProvider destinationProvider, SourceExtractor<?> sourceExtractor) {
		this(destinationProvider, sourceExtractor, (WebServiceMessageFactory) null);
	}

	public SimpleWebServiceOutboundGateway(DestinationProvider destinationProvider, SourceExtractor<?> sourceExtractor, WebServiceMessageFactory messageFactory) {
		super(destinationProvider, messageFactory);
	}

	public SimpleWebServiceOutboundGateway(String uri) {
		this(uri, null, null);
	}

	public SimpleWebServiceOutboundGateway(String uri, SourceExtractor<?> sourceExtractor) {
		this(uri, sourceExtractor, (WebServiceMessageFactory) null);
	}

	public SimpleWebServiceOutboundGateway(String uri, SourceExtractor<?> sourceExtractor, WebServiceMessageFactory messageFactory) {
		super(uri, messageFactory);
	}


	@Override
	protected Object doHandle(String uri, final Object requestPayload, final WebServiceMessageCallback requestCallback) {
		Object reply = null;
		if (requestPayload instanceof Source) {
			reply = this.getWebServiceTemplate().sendAndReceive(uri, 
					new RequestMessageCallback(requestCallback, (Source) requestPayload), new ResponseMessageextractor(null));
		}
		else if (requestPayload instanceof String) {
			reply = this.getWebServiceTemplate().sendAndReceive(uri, 
						new RequestMessageCallback(requestCallback, new StringSource((String) requestPayload)), new ResponseMessageextractor(new StringResult()));
		}
		else if (requestPayload instanceof Document) {
			reply = this.getWebServiceTemplate().sendAndReceive(uri, 
					new RequestMessageCallback(requestCallback, new DOMSource((Document) requestPayload)), new ResponseMessageextractor(new DOMResult()));
		}
		else {
			throw new MessagingException("Unsupported payload type '" + requestPayload.getClass() +
					"'. " + this.getClass().getName() + " only supports 'java.lang.String', '" + Source.class.getName() +
					"', and '" + Document.class.getName() + "'. Consider either using the '"
					+ MarshallingWebServiceOutboundGateway.class.getName() + "' or a Message Transformer.");
		}
		return reply;
	}

	private class RequestMessageCallback extends TransformerObjectSupport implements WebServiceMessageCallback {
		
		private final WebServiceMessageCallback requestCallback;
		private final Source source;
		
		public RequestMessageCallback(WebServiceMessageCallback requestCallback, Source source){
			this.requestCallback = requestCallback;
			this.source = source;
		}

		public void doWithMessage(WebServiceMessage message) throws IOException, TransformerException {
            this.transform(this.source, message.getPayloadResult());
            if (requestCallback != null) {
                requestCallback.doWithMessage(message);
            }
		}
		
	}
	
	private class ResponseMessageextractor extends TransformerObjectSupport implements WebServiceMessageExtractor<Object> {
		
		private final Result result;
		
		public ResponseMessageextractor(Result result){
			this.result = result;
		}

		public Object extractData(WebServiceMessage message)
				throws IOException, TransformerException {
			Source payloadSource = message.getPayloadSource();
			Object payload = null;
			
			if (this.result != null){
				this.transform(payloadSource, this.result);
				if (this.result instanceof StringResult){
					payload = this.result.toString();
				}
				else if (this.result instanceof DOMResult){
					payload = ((DOMResult)this.result).getNode();
				}
				else {
					payload = this.result;
				}
			}
			else {
				payload = payloadSource;
			}
					
			if (message instanceof SoapMessage){			
				SoapHeader soapHeader = ((SoapMessage)message).getSoapHeader();
				Map<String, Object> mappedMessageHeaders = headerMapper.toHeaders(soapHeader);
				Message<?> siMessage = MessageBuilder.withPayload(payload).copyHeaders(mappedMessageHeaders).build();
				return siMessage;
			}
			else {
				return message.getPayloadSource();
			}
		}	
	}

}
