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

import javax.xml.transform.TransformerException;

import org.springframework.integration.Message;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.util.Assert;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.WebServiceMessageFactory;
import org.springframework.ws.client.core.WebServiceMessageCallback;
import org.springframework.ws.client.core.WebServiceMessageExtractor;
import org.springframework.ws.client.support.destination.DestinationProvider;
import org.springframework.ws.soap.SoapHeader;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.support.MarshallingUtils;
import org.springframework.xml.transform.TransformerObjectSupport;

/**
 * An outbound Messaging Gateway for invoking Web Services that also supports
 * marshalling and unmarshalling of the request and response messages.
 * 
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @see Marshaller
 * @see Unmarshaller
 */
public class MarshallingWebServiceOutboundGateway extends AbstractWebServiceOutboundGateway {
	
	private volatile Marshaller marshaller;
	private volatile Unmarshaller unmarshaller;

	public MarshallingWebServiceOutboundGateway(DestinationProvider destinationProvider, Marshaller marshaller, Unmarshaller unmarshaller, WebServiceMessageFactory messageFactory) {
		super(destinationProvider, messageFactory);
		this.configureMarshallers(marshaller, unmarshaller);
	}

	public MarshallingWebServiceOutboundGateway(DestinationProvider destinationProvider, Marshaller marshaller, Unmarshaller unmarshaller) {
		this(destinationProvider, marshaller, unmarshaller, null);
	}

	public MarshallingWebServiceOutboundGateway(DestinationProvider destinationProvider, Marshaller marshaller, WebServiceMessageFactory messageFactory) {
		this(destinationProvider, marshaller, null, messageFactory);
	}

	public MarshallingWebServiceOutboundGateway(DestinationProvider destinationProvider, Marshaller marshaller) {
		this(destinationProvider, marshaller, (WebServiceMessageFactory) null);
	}

	public MarshallingWebServiceOutboundGateway(String uri, Marshaller marshaller, Unmarshaller unmarshaller, WebServiceMessageFactory messageFactory) {
		super(uri, messageFactory);
		this.configureMarshallers(marshaller, unmarshaller);
	}

	public MarshallingWebServiceOutboundGateway(String uri, Marshaller marshaller, Unmarshaller unmarshaller) {
		this(uri, marshaller, unmarshaller, null);
	}

	public MarshallingWebServiceOutboundGateway(String uri, Marshaller marshaller, WebServiceMessageFactory messageFactory) {
		this(uri, marshaller, null, messageFactory);
	}

	public MarshallingWebServiceOutboundGateway(String uri, Marshaller marshaller) {
		this(uri, marshaller, (WebServiceMessageFactory) null);
	}


	/**
	 * Sets the provided Marshaller and Unmarshaller on this gateway's WebServiceTemplate.
	 * Neither may be null.
	 */
	private void configureMarshallers(Marshaller marshaller, Unmarshaller unmarshaller) {
		Assert.notNull(marshaller, "marshaller must not be null");
		if (unmarshaller == null){
			Assert.isInstanceOf(Unmarshaller.class, marshaller,
					"Marshaller [" + marshaller + "] does not implement the Unmarshaller interface. " +
					"Please set an Unmarshaller explicitly by using one of the constructors that accepts " +
					"both Marshaller and Unmarshaller arguments.");
			unmarshaller = (Unmarshaller) marshaller;
		}
		
		Assert.notNull(unmarshaller, "unmarshaller must not be null");
		this.marshaller = marshaller;
		this.unmarshaller = unmarshaller;
	}

	@Override
	protected Object doHandle(String uri, Object requestPayload, WebServiceMessageCallback requestCallback) {
		Object reply = this.getWebServiceTemplate().sendAndReceive(uri, 
				new RequestMessageCallback(requestCallback, requestPayload), new ResponseMessageextractor());
		return reply;
	}
	
	
	
	private class RequestMessageCallback extends TransformerObjectSupport implements WebServiceMessageCallback {
		
		private final WebServiceMessageCallback requestCallback;
		private final Object source;
		
		public RequestMessageCallback(WebServiceMessageCallback requestCallback, Object source){
			this.requestCallback = requestCallback;
			this.source = source;
		}

		public void doWithMessage(WebServiceMessage message) throws IOException, TransformerException {
			MarshallingUtils.marshal(marshaller, source, message);
            if (requestCallback != null) {
                requestCallback.doWithMessage(message);
            }
		}
		
	}
	
	private class ResponseMessageextractor extends TransformerObjectSupport implements WebServiceMessageExtractor<Object> {
		
		public Object extractData(WebServiceMessage message)
				throws IOException, TransformerException {
			
            Object unmarshalledObject = MarshallingUtils.unmarshal(unmarshaller, message);
            
            if (message instanceof SoapMessage){			
				SoapHeader soapHeader = ((SoapMessage)message).getSoapHeader();
				Map<String, Object> mappedMessageHeaders = headerMapper.toHeadersFromReply(soapHeader);
				Message<?> siMessage = MessageBuilder.withPayload(unmarshalledObject).copyHeaders(mappedMessageHeaders).build();
				return siMessage;
			}
			else {
				return message.getPayloadSource();
			}
		}	
	}
}
